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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.MeterRead;
import com.billariz.corp.database.model.ObjectProcessRule;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.MeterReadStatus;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.model.light.messageCode;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.MeterReadRepository;
import com.billariz.corp.database.repository.ObjectProcessRuleRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.launcher.Launcher;
import com.billariz.corp.launcher.exception.LauncherException;
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
public class UpdateMeterReadStatus implements Launcher
{
    private static final String ACTIVITY_METER_READ="ACTIVITY_METER_READ";
    
    private static final String CANCELLED=      "CANCELLED";

    private final MeterReadRepository          meterReadRepository;

    private final ObjectProcessRuleRepository objectProcessRulesRepository;

    private final EventRepository              eventRepository;

    private final JournalRepository            journalRepository;

    private final LauncherQueue                launcherQueue;

    private final RelationRepository           relationRepository;

    private final EventUtils                   eventUtils;

    private final JournalUtils                 journalUtils;

    private List<ObjectProcessRule>           rules;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Iterable<Long> eventIds, EventExecutionMode executionMode)
    {
        var events = eventRepository.findAllById(eventIds);
        rules = objectProcessRulesRepository.findAllByObjectType(ObjectType.METER_READ);
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
        catch (LauncherException e)
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

    private void handle(Event event, Journal journal, List<messageCode> messages) throws LauncherException
    {
        var relation = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_METER_READ,
                event.getActivityId()).orElseThrow(() -> new LauncherFatalException("NO_MR_FOUND", new Object[]{event.getId()}));
        log.info("relation[{}] found for Event[{}] ", relation, event);
        var meterRead = meterReadRepository.findById(relation.getSecondObjectId()).orElseThrow(
                                        () -> new LauncherFatalException("MISSING_MR", new Object[]{relation.getSecondObjectId()}));
        var meterReadNewStatus = event.getAction() == null ? "*" : event.getAction();
        var rule = updateMeterReadStatus(meterRead, meterReadNewStatus);

        journalUtils.addJournal(ObjectType.METER_READ,meterRead.getId(),"STATUS_UPDATED", 
                            new Object[]{event.getId(), meterRead.getStatus()}, event, JournalAction.STATUS_CHANGE);
        messages.add(new messageCode("STATUS_UPDATE_BY_RULE", new Object[]{meterRead.getStatus(), event.getId(), rule.toString()}));

        if (rule.getActivityType() != null)
            launcherQueue.createActivityEvent(rule.getActivityType(), meterRead.getId(), ObjectType.METER_READ, "systemAgent");
        
        //Traiter le statut des mr annulée par cette mr
        var mrsCancelled = meterReadRepository.findAllByCancelledBy(meterRead.getId());
        for(MeterRead mrCancelled: mrsCancelled){
            var ruleOpt = updateMeterReadStatus(mrCancelled, CANCELLED);
            if (ruleOpt.getActivityType() != null)
                launcherQueue.createActivityEvent(rule.getActivityType(), mrCancelled.getId(), ObjectType.METER_READ, "systemAgent");
            
            //ajouter une relation activity - cancelled meterread
            launcherQueue.createRelation(ACTIVITY_METER_READ,event.getActivityId(),mrCancelled.getId(), ObjectType.METER_READ);
            
            //ajouter un journal sur cancelled meterread
            journalUtils.addJournal(ObjectType.METER_READ,mrCancelled.getId(),"SET_TO_CANCEL", 
                            new Object[]{ObjectType.METER_READ, event.getId(), meterRead.getId()}, event, JournalAction.STATUS_CHANGE);
            messages.add(new messageCode("SET_CANCELLED", new Object[]{ObjectType.METER_READ, mrCancelled.getId()}));
        }
        journal.setMessageCodes(messages);
    }

    private ObjectProcessRule updateMeterReadStatus(MeterRead meterRead, String meterReadNew) throws LauncherException
    {
        var ruleOpt = rules.stream().filter(mrRules -> checkConditions(mrRules, meterRead)
                && (mrRules.getInitialStatus()==null || mrRules.getInitialStatus().equals(meterRead.getStatus().getValue()))
                && mrRules.getNewStatus().equals(meterReadNew))
                        .findFirst()
                        .orElseThrow(
                        () -> new LauncherFatalException(
                                "NO_RULE_FOUND", new Object[]{ObjectType.METER_READ,meterRead.getStatus(), meterReadNew}));
        meterRead.setStatus(MeterReadStatus.valueOf(ruleOpt.getFinalStatus()));
        return ruleOpt;
    }

    private boolean checkConditions(ObjectProcessRule streamRule, MeterRead meterRead)
    {
        return (rulesNormalized(streamRule.getMarket(), meterRead.getMarket()) && rulesNormalized(streamRule.getDirection(), meterRead.getDirection()));
    }

    private boolean rulesNormalized(String search, String item)
    {
        if ("*".equals(search))
            return true;
        return search.equals(item == null ? "" : item);
    }
}
