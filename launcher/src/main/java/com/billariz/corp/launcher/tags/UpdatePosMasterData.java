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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.ContractPointOfService;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.PointOfService;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.model.enumeration.PointOfServiceDataStatus;
import com.billariz.corp.database.model.enumeration.UpdateMasterDataUseCase;
import com.billariz.corp.database.model.light.messageCode;
import com.billariz.corp.database.repository.ContractPointOfServiceRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.PointOfServiceCapacityRepository;
import com.billariz.corp.database.repository.PointOfServiceConfigurationRepository;
import com.billariz.corp.database.repository.PointOfServiceEstimateRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.launcher.Launcher;
import com.billariz.corp.launcher.exception.LauncherFatalException;
import com.billariz.corp.launcher.utils.EventUtils;
import com.billariz.corp.launcher.utils.JournalUtils;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UpdatePosMasterData implements Launcher 
{
    private static final String ACTIVITY_CONTRACT="ACTIVITY_CONTRACT";

    private static final String ACTIVITY_POS="ACTIVITY_POS";

    private final EventRepository                  eventRepository;

    private final RelationRepository               relationRepository;

    private final JournalRepository                journalRepository;

    private final EventUtils                       eventUtils;

    private final PointOfServiceCapacityRepository            pointOfServiceCapacityRepository;

    private final ContractPointOfServiceRepository contractPointOfServiceRepository;

    private final PointOfServiceConfigurationRepository     pointOfServiceConfigurationRepository;

    private final PointOfServiceEstimateRepository     pointOfServiceEstimateRepository;

    private final JournalUtils journalUtils;

    @Override
   @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Iterable<Long> eventIds, EventExecutionMode executionMode)
    {
        var events = eventRepository.findAllById(eventIds);

        events.forEach(e -> process(e, executionMode));
    }

    public void process(Event event, EventExecutionMode executionMode)
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
            var wait = process(event, journal, messages);
            event.setStatus(wait ? EventStatus.COMPLETED : EventStatus.PENDING);
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

    private boolean process(Event event, Journal journal, List<messageCode> messages) throws LauncherFatalException
    {
        if(event.getAction()!=null)
            messages.add(new messageCode("EVENT_USE_CASE", new Object[]{event.getAction()}));

        var relationContract = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_CONTRACT,
                        event.getActivityId()).orElseThrow(
                            () -> new LauncherFatalException("MISSING_CONTRACT", new Object[]{event.getId()}));
        log.info("relation[{}] found for Event[{}] ", relationContract, event);
        // Si relationPos (cas ascendant) est présent alors on exploite le posId de celle celle-ci pour récupérer le POS a traiter
        // Sinon (cas descendant) on traite tous les POS du contrat récupérer via relationContract
        var relationPos = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_POS,
                        event.getActivityId());
        var ctrPos = contractPointOfServiceRepository.findAllByPosIdAndContractId(relationPos.isEmpty() ? null : relationPos.get().getSecondObjectId(), 
                                        relationContract.getSecondObjectId());
        LocalDate endDate = null;
        for(ContractPointOfService cp : ctrPos){
            endDate = relationPos.isEmpty() ? cp.getContract().getContractualEndDate() : cp.getEndDate();
            closePointOfServiceMasterData(endDate, 
                                            cp.getPointOfService(),
                                            cp.getStartDate().isEqual(endDate) ? 
                                                PointOfServiceDataStatus.CANCELLED : PointOfServiceDataStatus.CLOSED
                                            );
            journalUtils.addJournal(ObjectType.POINT_OF_SERVICE, cp.getPointOfService().getId(),"ENDING_POS", 
                                                new Object[]{event.getId()}, event, JournalAction.LOG);
            messages.add(new messageCode("POS_CONF_ENDING", new Object[]{cp.getPointOfService().getReference()}));
        }
        
        return true;
    }

    private void closePointOfServiceMasterData(LocalDate endDate, PointOfService pos, PointOfServiceDataStatus status) throws LauncherFatalException
    {
        pointOfServiceCapacityRepository.closeCapacities(endDate, pos.getId(), status);
        pointOfServiceConfigurationRepository.closeConfigurations(endDate, pos.getId(), status);
        pointOfServiceEstimateRepository.closeEstimates(endDate, pos.getId(), status);
        pos.getMeters().stream().filter(e -> e.getEndDate()==null).forEach(m -> m.setEndDate(endDate));
    }
}
