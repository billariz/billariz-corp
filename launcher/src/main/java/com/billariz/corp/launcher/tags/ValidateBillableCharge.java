/**
 * Copyright (C) 2025 Uppli SAS — Billariz
 *
 * This file is part of Billariz, licensed under the GNU Affero General
 * Public License v3.0 (AGPL-3.0). You may use, modify and distribute
 * this software under the terms of the AGPL-3.0.
 *
 * For commercial use without AGPL obligations, contact:
 * contact@billariz.com | contact@uppli.fr
 * https://billariz.com
 */

package com.billariz.corp.launcher.tags;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.BillableCharge;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.ContractPointOfService;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.Parameter;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.BillableChargeAquisitionStrategy;
import com.billariz.corp.database.model.enumeration.BillableChargeContext;
import com.billariz.corp.database.model.enumeration.BillableChargeStatus;
import com.billariz.corp.database.model.enumeration.BillableChargeTypeEnum;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.model.light.messageCode;
import com.billariz.corp.database.repository.BillableChargeRepository;
import com.billariz.corp.database.repository.ContractPointOfServiceRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.launcher.Launcher;
import com.billariz.corp.launcher.exception.LauncherFatalException;
import com.billariz.corp.launcher.queue.LauncherQueue;
import com.billariz.corp.launcher.utils.EventUtils;
import com.billariz.corp.launcher.utils.JournalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ValidateBillableCharge implements Launcher
{
    private static final String ACTIVITY_CONTRACT="ACTIVITY_CONTRACT";

    private static final String ACTIVITY_BILLABLE_CHARGE="ACTIVITY_BILLABLE_CHARGE";

    private final BillableChargeRepository              billableChargeRepository;

    private final EventRepository                  eventRepository;

    private final JournalRepository                journalRepository;

    private final RelationRepository               relationRepository;

    private final EventUtils                       eventUtils;

    private final ParameterRepository           parameterRepository;

    private final LauncherQueue                    launcherQueue;

    private final JournalUtils journalUtils;

    private final ContractPointOfServiceRepository ctrPosRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Iterable<Long> eventIds, EventExecutionMode executionMode)
    {
        var events = eventRepository.findAllById(eventIds);

        events.forEach(e -> process(e, executionMode));
    }

    private void process(Event event, EventExecutionMode executionMode)
    {
        // Prepare
        List<messageCode> messages = new ArrayList<>();
        event.setCascaded(true);
        event.setStatus(EventStatus.IN_PROGRESS);
        if (event.getRank() == 1)
                    event.getActivity().setStatus(ActivityStatus.IN_PROGRESS);
        event.setExecutionDate(LocalDateTime.now());
        event.setExecutionMode(executionMode);
        var journal = eventUtils.addJournal(event);
        // Execute
        try
        {
            handle(event, journal, messages);
            event.setStatus(EventStatus.COMPLETED);
        }
        catch (LauncherFatalException e)
        {
            log.error("handle: " + e.getMessage(), e);
            event.setStatus(EventStatus.IN_FAILURE);
            journal.setMethod(JournalAction.ERROR.getValue());
            messages.add(new messageCode(e.getMessage(), e.getArgs()));
            journal.setComment(e.getMessage());
            event.getActivity().setStatus(ActivityStatus.BLOCKED);
        }
        // Finish
        log.info("Event[{}] processed, strating next process", event);
        eventUtils.triggerOnUpdateEventStatus(event);
        journal.setNewStatus(Objects.toString(event.getStatus()));
        journal.setMessageCodes(messages);
        journalRepository.save(journal);
    }

    private void handle(Event event, Journal journal, List<messageCode> messages) throws LauncherFatalException
    {
        var relation = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_BILLABLE_CHARGE,
                event.getActivityId())
                .orElseThrow(() -> new LauncherFatalException("NO_BC_FOUND", new Object[]{event.getId()}));
        log.info("relation[{}] found for Event[{}] ", relation, event);
        var billableCharge = billableChargeRepository.findById(relation.getSecondObjectId())
                    .orElseThrow(() -> new LauncherFatalException("MISSING_BC", new Object[]{relation.getSecondObjectId()}));
        billableCharge.setCascaded(true); //permet d'inhiber le déclenchement du listner sur ce billableCharge

        var contract = checkContract(billableCharge, event);
        messages.add(new messageCode("CHECK_CONTRACT_OK", new Object[]{contract.getId()}));
        launcherQueue.createRelation(ACTIVITY_CONTRACT, event.getActivityId(), contract.getId(), ObjectType.CONTRACT);

        if(BillableChargeAquisitionStrategy.AS_CYCLICAL_INVOICE.equals(billableCharge.getBillableChargeType().getAquisitionStrategy()))
        {
            //Controle de la continuité
            checkContinuity(contract, billableCharge, event);
            messages.add(new messageCode("CHECK_CONTINUITY_OK", null));

            //controle du chevauchement
            checkOverlap(contract, billableCharge, event);
            messages.add(new messageCode("CHECK_OVERLAP_OK", null));
        }
        //Rejoue des events de validation mr en erreurs (pour le meme contrat)
        activateEventsInFailure(contract, event);
        messages.add(new messageCode("ACTIVATING_EVENT_IN_FAILURE_OK", null));
        
        journal.setMessageCodes(messages);
    }

    private void activateEventsInFailure(Contract contract, Event event)
    {
        var eventIds = eventRepository.findAllByTypeAndRelationTypeAndObjectIdAndStatus(event.getType().getId(), 
                    EventStatus.IN_FAILURE, ACTIVITY_CONTRACT, contract.getId()).stream().map(Event::getId).toList();
        if(!eventIds.isEmpty())
            eventRepository.updateEventsStatusAndExecutionModeByIdIn(eventIds,EventStatus.PENDING,EventExecutionMode.EVENT_MANAGER);
    }

    private Contract checkContract(BillableCharge billableCharge, Event event) throws LauncherFatalException
    {
        var contractPosList = ctrPosRepository.findAllByPosRef(billableCharge.getPosRef());
        var contractPos = contractPosList.stream().filter(ctrpos -> contractMatch(ctrpos, billableCharge)).findFirst().orElseThrow(
                () -> { 
                        billableCharge.setCascaded(true);
                        billableCharge.setStatus(BillableChargeStatus.IN_FAILURE);
                        journalUtils.addJournal(ObjectType.BILLABLE_CHARGE, billableCharge.getId(),"MISSING_CONTRACT_FOR_BC", 
                                new Object[]{billableCharge.getId()}, event, JournalAction.ERROR);
                        return new LauncherFatalException("MISSING_CONTRACT_FOR_BC", new Object[]{billableCharge.getId()});
                        }
                );
        return contractPos.getContract();
    }

    private boolean contractMatch(ContractPointOfService contractPos, BillableCharge billableCharge)
    {
        return ((billableCharge.getStartDate().isAfter(billableCharge.getStartDate()) || billableCharge.getStartDate().isEqual(contractPos.getStartDate()))
                && (contractPos.getEndDate() == null || billableCharge.getStartDate().isBefore(contractPos.getEndDate())) && (contractPos.getEndDate() == null
                        || billableCharge.getEndDate().isBefore(contractPos.getEndDate()) || billableCharge.getEndDate().isEqual(contractPos.getEndDate())));
    }

    private boolean checkContinuity(Contract contract, BillableCharge billableCharge, Event event) throws LauncherFatalException {
        // Définir une tolérance en jours
        var td= parameterRepository.findAllByTypeAndNameAndStartDateBefore("METER_READ",
                                 "continuityToleranceDays",LocalDate.now())
                                .stream().map(Parameter::getValue)
                                .findFirst()
                                .orElseThrow(() -> new LauncherFatalException("MISSING_PARAMETER",
                                                                     new Object[]{"continuityToleranceDays"} 
                                ));
        int toleranceDays = Integer.parseInt(td);
        // Vérifier si la date de début de la meterRead tombe dans l'intervalle
        if (!billableCharge.getStartDate().isBefore(contract.getContractualStartDate().minusDays(toleranceDays)) 
                && !billableCharge.getStartDate().isAfter(contract.getContractualStartDate().plusDays(toleranceDays))) {
            return true;
        }
        // Si aucune continuité n'est trouvée, poursuivre la logique existante
        List<BillableChargeStatus> listStatus = List.of(BillableChargeStatus.CANCELLED);
        List<BillableCharge> continuedBc = billableChargeRepository.findContinueBillableChargeWithTolerance(
            listStatus, contract.getId(), billableCharge.getStartDate().minusDays(toleranceDays), 
            billableCharge.getStartDate().plusDays(toleranceDays));
        if (continuedBc.isEmpty()) {
            billableCharge.setCascaded(true);
            billableCharge.setStatus(BillableChargeStatus.IN_FAILURE);
            journalUtils.addJournal(ObjectType.BILLABLE_CHARGE, billableCharge.getId(),"BC_CHECK_CONTINUITY_KO", 
                                                new Object[]{billableCharge.getId()}, event, JournalAction.ERROR);
            throw new LauncherFatalException("BC_CHECK_CONTINUITY_KO", new Object[]{billableCharge.getId()});
        }
        return true;
    }

    private boolean checkOverlap(Contract contract, BillableCharge billableCharge, Event event) throws LauncherFatalException
    {
        var td= parameterRepository.findAllByTypeAndNameAndStartDateBefore("METER_READ",
                                 "overlapToleranceDays",LocalDate.now())
                                .stream().map(Parameter::getValue)
                                .findFirst()
                                .orElseThrow(() -> new LauncherFatalException("MISSING_PARAMETER", new Object[]{"overlapToleranceDays"} 
                                ));
        int toleranceDays = Integer.parseInt(td);

        List<BillableChargeStatus> listStatusExcluded = new ArrayList<>();
        List<BillableChargeTypeEnum> listTypeIncluded = new ArrayList<>();
        switch (billableCharge.getType())
        {
        case INITIAL:
            listStatusExcluded.add(BillableChargeStatus.CANCELLED);
            listStatusExcluded.add(BillableChargeStatus.IN_FAILURE);
            listTypeIncluded.add(BillableChargeTypeEnum.INITIAL);
            var overLapBc = billableChargeRepository.findOverLapBillableChargeWithTolerance(billableCharge.getId(), listStatusExcluded, listTypeIncluded, contract.getId(),
                            billableCharge.getStartDate().minusDays(toleranceDays), billableCharge.getEndDate().plusDays(toleranceDays));
            if (!overLapBc.isEmpty())
            {
                //Vérifier les chevauchements avec des bc non estimate
                var bc = overLapBc.stream().filter(m -> !m.getContext().equals(BillableChargeContext.ESTIMATE)).findFirst();
                if(bc.isPresent()){
                    billableCharge.setCascaded(true);
                    billableCharge.setStatus(BillableChargeStatus.IN_FAILURE);
                    journalUtils.addJournal(ObjectType.BILLABLE_CHARGE,billableCharge.getId(),"BC_CHECK_OVERLAP_KO", 
                                new Object[]{bc.get().getId()}, event, JournalAction.ERROR);
                    throw new LauncherFatalException("BC_CHECK_OVERLAP_KO", new Object[]{bc.get().getId()});
                }
                //Traiter les chevauchement avec des bc estimate
                else {
                    for(BillableCharge omr: overLapBc){
                        omr.setCancelledBy(billableCharge.getId());
                        journalUtils.addJournal(ObjectType.METER_READ,omr.getId(),"CANCELED_ESTIMATE_BC", 
                                new Object[]{billableCharge.getId()}, event, JournalAction.LOG);
                        journalUtils.addJournal(ObjectType.METER_READ,billableCharge.getId(),"ESTIMATE_BC_CANCELLED", 
                                new Object[]{omr.getId()}, event, JournalAction.LOG);
                    }
                }
            }
            return true;

        case CANCELLATION:
            listStatusExcluded.add(BillableChargeStatus.CANCELLED);
            listTypeIncluded.add(BillableChargeTypeEnum.INITIAL);
            var overLapBcC = billableChargeRepository.findOverLapBillableCharge(billableCharge.getId(), listStatusExcluded, listTypeIncluded, contract.getId(),
            billableCharge.getStartDate(), billableCharge.getEndDate());
            if (overLapBcC.isEmpty())
            {
                billableCharge.setCascaded(true);
                billableCharge.setStatus(BillableChargeStatus.IN_FAILURE);
                throw new LauncherFatalException("NO_INITIAL_BC", new Object[]{billableCharge.getId()});
            }
            return true;

        case CORRECTION:
            listStatusExcluded.add(BillableChargeStatus.CANCELLED);
            listTypeIncluded.add(BillableChargeTypeEnum.CANCELLATION);
            var overLapBcCO = billableChargeRepository.findOverLapBillableCharge(billableCharge.getId(), listStatusExcluded, listTypeIncluded, contract.getId(),
            billableCharge.getStartDate(), billableCharge.getEndDate());
            if (overLapBcCO.isEmpty())
            {
                billableCharge.setCascaded(true);
                billableCharge.setStatus(BillableChargeStatus.IN_FAILURE);
                throw new LauncherFatalException("NO_CANCELLATION_BC", new Object[]{billableCharge.getId()});
            }
            return true;

        default:
            return true;
        }
    }
}
