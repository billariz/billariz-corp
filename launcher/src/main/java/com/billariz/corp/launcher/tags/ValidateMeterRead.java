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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.ContractPointOfService;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.MeterRead;
import com.billariz.corp.database.model.Parameter;
import com.billariz.corp.database.model.PointOfServiceEstimate;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.MeterReadContext;
import com.billariz.corp.database.model.enumeration.MeterReadSource;
import com.billariz.corp.database.model.enumeration.MeterReadStatus;
import com.billariz.corp.database.model.enumeration.MeterReadType;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.model.enumeration.PointOfServiceDataStatus;
import com.billariz.corp.database.model.light.messageCode;
import com.billariz.corp.database.repository.ContractPointOfServiceRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.MeterReadRepository;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.database.repository.PointOfServiceEstimateRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.launcher.Launcher;
import com.billariz.corp.launcher.exception.LauncherFatalException;
import com.billariz.corp.launcher.queue.LauncherQueue;
import com.billariz.corp.launcher.utils.EventUtils;
import com.billariz.corp.launcher.utils.JournalUtils;
import com.billariz.corp.launcher.utils.MeterReadUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ValidateMeterRead implements Launcher
{
    private static final String ACTIVITY_CONTRACT="ACTIVITY_CONTRACT";

    private static final String ACTIVITY_METER_READ="ACTIVITY_METER_READ";

    private static final String ESTIMATE="ESTIMATE";

    private final MeterReadRepository              meterReadRepository;

    private final EventRepository                  eventRepository;

    private final JournalRepository                journalRepository;

    private final RelationRepository               relationRepository;

    private final EventUtils                       eventUtils;

    private final JournalUtils journalUtils;

    private final ParameterRepository           parameterRepository;

    private final LauncherQueue                    launcherQueue;

    private final ContractPointOfServiceRepository ctrPosRepository;

    private final PointOfServiceEstimateRepository pointOfServiceEstimateRepository;

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
        event.setExecutionDate(LocalDateTime.now());
        event.setExecutionMode(executionMode);
        if (event.getRank() == 1)
                    event.getActivity().setStatus(ActivityStatus.IN_PROGRESS);
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
            journal.setComment(e.getMessage());
            journal.setMethod(JournalAction.ERROR.getValue());
            messages.add(new messageCode(e.getMessage(), e.getArgs()));
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
        var relation = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_METER_READ,
                event.getActivityId()).orElseThrow(() -> new LauncherFatalException("NO_MR_FOUND", new Object[]{event.getId()}));
        log.info("relation[{}] found for Event[{}] ", relation, event);
        var meterRead = meterReadRepository.findById(relation.getSecondObjectId())
                                .orElseThrow(() -> new LauncherFatalException("MISSING_MR", new Object[]{relation.getSecondObjectId()}));
        meterRead.setCascaded(true);
        var contract = checkContract(meterRead, event);
        messages.add(new messageCode("CHECK_CONTRACT_OK", new Object[]{contract.getId()}));
        launcherQueue.createRelation(ACTIVITY_CONTRACT, event.getActivityId(), contract.getId(), ObjectType.CONTRACT);

        checkContinuity(contract, meterRead, event);
        messages.add(new messageCode("CHECK_CONTINUITY_OK", null));

        checkOverlap(contract, meterRead, event);
        messages.add(new messageCode("CHECK_OVERLAP_OK", null));

        //Controle des seuils de variation MIn et MAX de la quantité
        checkThreshold(contract, meterRead, event);
        messages.add(new messageCode("CHECK_THRESHOLDS_OK", null));

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

    private boolean checkThreshold(Contract contract, MeterRead meterRead, Event event) throws LauncherFatalException
    {
        var upTolerance= parameterRepository.findAllByTypeAndNameAndStartDateBefore("METER_READ",
                                        "quantityUpTolerancePercentage",LocalDate.now())
                                    .stream().map(Parameter::getValue)
                                    .findFirst()
                                    .orElseThrow(() -> new LauncherFatalException("MISSING_PARAMETER", new Object[]{"quantityUpTolerancePercentage"}));
        var lessTolerance= parameterRepository.findAllByTypeAndNameAndStartDateBefore("METER_READ",
                                    "quantityLessTolerancePercentage",LocalDate.now())
                                   .stream().map(Parameter::getValue)
                                   .findFirst()
                                   .orElseThrow(() -> new LauncherFatalException("MISSING_PARAMETER", new Object[]{"quantityLessTolerancePercentage"}));
        int upTolerancePercentage = Integer.parseInt(upTolerance);
        int lessTolerancePercentage = Integer.parseInt(lessTolerance);
        
        // Récupérer la quantité usuelle du client
        BigDecimal usualQuantity = getUsualQuantity(contract,meterRead);
                               
        if (usualQuantity == null) {
                throw new LauncherFatalException("MISSING_USUAL_QUANTITY ", new Object[]{meterRead.getPosRef(), contract.getId()});
        }
        // Calculer les seuils
        BigDecimal lowerThreshold = usualQuantity.subtract(
                usualQuantity.multiply(BigDecimal.valueOf(lessTolerancePercentage))
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP));
        BigDecimal upperThreshold = usualQuantity.add(
                usualQuantity.multiply(BigDecimal.valueOf(upTolerancePercentage))
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP));
                               
        // Vérifier si la quantité dépasse les seuils
        if (meterRead.getTotalQuantity().compareTo(lowerThreshold) < 0 || meterRead.getTotalQuantity().compareTo(upperThreshold) > 0){
            meterRead.setStatus(MeterReadStatus.IN_FAILURE);
            Object[] args = {meterRead.getTotalQuantity(),lowerThreshold,upperThreshold};
            journalUtils.addJournal(ObjectType.METER_READ,meterRead.getId(),"QUANTITY_OUT_OF_BOUNDS", 
                                                args, event, JournalAction.ERROR);
            throw new LauncherFatalException("QUANTITY_OUT_OF_BOUNDS", args);
        }
        return true;
    }

    private BigDecimal getUsualQuantity(Contract contract, MeterRead meterRead) throws LauncherFatalException
    {
        List<MeterRead> mrs = meterReadRepository.findWithStatusInAndContractAndSourceAndTouGroup(
                    Arrays.asList(MeterReadStatus.BILLED, 
                      MeterReadStatus.COMPUTED, 
                      MeterReadStatus.INITIALIZED, 
                      MeterReadStatus.VALUATED, 
                      MeterReadStatus.AVAILABLE),
                    contract.getId(),
                    Arrays.asList(MeterReadSource.MARKET, MeterReadSource.USER),
                    null
                    );
        if(MeterReadUtils.hasContinuous12MonthHistoryOfMr(mrs))
                   return MeterReadUtils.calculateProRataQuantityFromMr(mrs,meterRead.getStartDate(),meterRead.getEndDate());
        else{
            List<PointOfServiceEstimate> posEstim = pointOfServiceEstimateRepository.findAllByPosId(contract.getContractPointOfServices().get(0).getPosId());
            List<PointOfServiceEstimate> estimates = posEstim.stream()
                            .filter(es -> (es.getStatus().equals(PointOfServiceDataStatus.VALIDATED) || es.getStatus().equals(PointOfServiceDataStatus.CLOSED))
                                    && ("MARKET".equals(es.getEstimateType()) || "SUPPLIER".equals(es.getEstimateType())
                                    && (meterRead.getEndDate().isAfter(es.getStartDate()) 
                                    && meterRead.getStartDate().isBefore(es.getEndDate()==null? meterRead.getStartDate().plusDays(1): es.getEndDate()))
                                    ))
                            .collect(Collectors.toList());
            if (estimates.isEmpty())
                throw new LauncherFatalException("MISSING_ESTIMATE", new Object[]{meterRead.getPosRef()});
            
            BigDecimal esimate = estimates.stream()
                         .map(PointOfServiceEstimate::getValue)
                         .reduce(BigDecimal.ZERO, BigDecimal::add);
            return esimate.multiply(BigDecimal.valueOf(MeterReadUtils.calculateDaysBetween(meterRead.getStartDate(),meterRead.getEndDate())))
                        .divide(BigDecimal.valueOf(360), RoundingMode.HALF_UP);
        }
    }

    private Contract checkContract(MeterRead meterRead, Event event) throws LauncherFatalException
    {
        var contractPosList = ctrPosRepository.findAllByPosRef(meterRead.getPosRef());
        var contractPos = contractPosList.stream().filter(ctrpos -> contractMatch(ctrpos, meterRead)).findFirst().orElseThrow(
                () -> { 
                        meterRead.setCascaded(true);
                        meterRead.setStatus(MeterReadStatus.IN_FAILURE);
                        Object[] args = new Object[]{meterRead.getId()};
                        journalUtils.addJournal(ObjectType.METER_READ,meterRead.getId(),"MISSING_CONTRACT_FOR_MR", 
                                                            args, event, JournalAction.ERROR);
                        return new LauncherFatalException("MISSING_CONTRACT_FOR_MR", args); }
                );
        return contractPos.getContract();
    }

    private boolean contractMatch(ContractPointOfService contractPos, MeterRead meterRead)
    {
        return ((meterRead.getStartDate().isAfter(contractPos.getStartDate()) || meterRead.getStartDate().isEqual(contractPos.getStartDate()))
                && (contractPos.getEndDate() == null || meterRead.getStartDate().isBefore(contractPos.getEndDate())) && (contractPos.getEndDate() == null
                        || meterRead.getEndDate().isBefore(contractPos.getEndDate()) || meterRead.getEndDate().isEqual(contractPos.getEndDate())));
    }

    private boolean checkContinuity(Contract contract, MeterRead meterRead, Event event) throws LauncherFatalException {
        // Définir une tolérance en jours
        var td= parameterRepository.findAllByTypeAndNameAndStartDateBefore("METER_READ",
                                 "continuityToleranceDays",LocalDate.now())
                                .stream().map(Parameter::getValue)
                                .findFirst()
                                .orElseThrow(() -> new LauncherFatalException("MISSING_PARAMETER", new Object[]{"continuityToleranceDays"} 
                                ));
        int toleranceDays = Integer.parseInt(td);
        // Vérifier si la date de début de la meterRead tombe dans l'intervalle
        if (!meterRead.getStartDate().isBefore(contract.getContractualStartDate().minusDays(toleranceDays)) 
                && !meterRead.getStartDate().isAfter(contract.getContractualStartDate().plusDays(toleranceDays))) {
            return true;
        }
        // Si aucune continuité n'est trouvée, poursuivre la logique existante
        List<MeterReadStatus> listStatus = List.of(MeterReadStatus.CANCELLED);
        List<MeterRead> continuedMr = meterReadRepository.findContinueMeterReadWithTolerance(
            listStatus, contract.getId(), meterRead.getStartDate().minusDays(toleranceDays), 
                                        meterRead.getStartDate().plusDays(toleranceDays));
        if (continuedMr.isEmpty()) {
            meterRead.setCascaded(true);
            meterRead.setStatus(MeterReadStatus.IN_FAILURE);
            journalUtils.addJournal(ObjectType.METER_READ,meterRead.getId(),"CHECK_CONTINUITY_KO", 
                                                new Object[]{meterRead.getId()}, event, JournalAction.ERROR);
            throw new LauncherFatalException("CHECK_CONTINUITY_KO", new Object[]{meterRead.getId()});
        }
        return true;
    }

    private boolean checkOverlap(Contract contract, MeterRead meterRead, Event event) throws LauncherFatalException
    {
        var td= parameterRepository.findAllByTypeAndNameAndStartDateBefore("METER_READ",
                                 "overlapToleranceDays",LocalDate.now())
                                .stream().map(Parameter::getValue)
                                .findFirst()
                                .orElseThrow(() -> new LauncherFatalException("MISSING_PARAMETER", new Object[]{"overlapToleranceDays"} 
                                ));
        int toleranceDays = Integer.parseInt(td);

        List<MeterReadStatus> listStatusExcluded = new ArrayList<>();
        List<MeterReadType> listTypeIncluded = new ArrayList<>();
        switch (meterRead.getType())
        {
        case INITIAL:
            listStatusExcluded.add(MeterReadStatus.CANCELLED);
            listStatusExcluded.add(MeterReadStatus.IN_FAILURE);
            listTypeIncluded.add(MeterReadType.INITIAL);
            var overLapMr = meterReadRepository.findOverLapMeterReadWithTolerance(meterRead.getId(), listStatusExcluded, listTypeIncluded, contract.getId(),
                    meterRead.getStartDate().plusDays(toleranceDays), meterRead.getEndDate().minusDays(toleranceDays));
            if (!overLapMr.isEmpty())
            {
                //Vérifier les chevauchements avec des mr non estimate
                var mr = overLapMr.stream().filter(m -> !m.getContext().equals(MeterReadContext.ESTIMATE)).findFirst();
                if(mr.isPresent()){
                    meterRead.setCascaded(true);
                    meterRead.setStatus(MeterReadStatus.IN_FAILURE);
                    journalUtils.addJournal(ObjectType.METER_READ,meterRead.getId(),"CHECK_OVERLAP_KO", 
                                new Object[]{mr.get().getId()}, event, JournalAction.ERROR);
                    throw new LauncherFatalException("CHECK_OVERLAP_KO", new Object[]{mr.get().getId()});
                }
                //Traiter les chevauchement avec des mr estimate
                else {
                    for(MeterRead omr: overLapMr){
                        omr.setCascaded(true);
                        omr.setCancelledBy(meterRead.getId());
                        journalUtils.addJournal(ObjectType.METER_READ,omr.getId(),"CANCELED_ESTIMATE_MR", 
                                new Object[]{meterRead.getId()}, event, JournalAction.LOG);
                        journalUtils.addJournal(ObjectType.METER_READ,meterRead.getId(),"ESTIMATE_MR_CANCELLED", 
                                new Object[]{omr.getId()}, event, JournalAction.LOG);
                    }
                }
            }
            return true;

        case CANCELLATION:
            listStatusExcluded.add(MeterReadStatus.CANCELLED);
            listTypeIncluded.add(MeterReadType.INITIAL);
            var overLapMrC = meterReadRepository.findOverLapMeterRead(meterRead.getId(), listStatusExcluded, listTypeIncluded, contract.getId(),
                    meterRead.getStartDate(), meterRead.getEndDate());
            if (overLapMrC.isEmpty())
            {
                meterRead.setCascaded(true);
                meterRead.setStatus(MeterReadStatus.IN_FAILURE);
                throw new LauncherFatalException("NO_INITIAL_MR", new Object[]{meterRead.getId()});
            }
            return true;

        case CORRECTION:
            listStatusExcluded.add(MeterReadStatus.CANCELLED);
            listTypeIncluded.add(MeterReadType.CANCELLATION);
            var overLapMrCO = meterReadRepository.findOverLapMeterRead(meterRead.getId(), listStatusExcluded, listTypeIncluded, contract.getId(),
                    meterRead.getStartDate(), meterRead.getEndDate());
            if (overLapMrCO.isEmpty())
            {
                meterRead.setCascaded(true);
                meterRead.setStatus(MeterReadStatus.IN_FAILURE);
                throw new LauncherFatalException("NO_CANCELLATION_MR", new Object[]{meterRead.getId()});
            }
            return true;

        default:
            return true;
        }
    }
}
