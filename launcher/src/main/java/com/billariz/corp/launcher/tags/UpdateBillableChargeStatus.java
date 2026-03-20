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
import com.billariz.corp.database.model.BillableCharge;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.ObjectProcessRule;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.BillableChargeStatus;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.model.light.messageCode;
import com.billariz.corp.database.repository.BillableChargeRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
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
public class UpdateBillableChargeStatus implements Launcher
{
    private static final String ACTIVITY_BILLABLE_CHARGE="ACTIVITY_BILLABLE_CHARGE";
    
    private static final String CANCELLED=      "CANCELLED";

    private final BillableChargeRepository          billableChargeRepository;

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
        rules = objectProcessRulesRepository.findAllByObjectType(ObjectType.BILLABLE_CHARGE);
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
        var relation = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_BILLABLE_CHARGE,
                event.getActivityId()).orElseThrow(() ->  new LauncherFatalException("NO_BC_FOUND", new Object[]{event.getId()}));
        log.info("relation[{}] found for Event[{}] ", relation, event);
        
        var billableCharge = billableChargeRepository.findById(relation.getSecondObjectId()).orElseThrow(() 
                            -> new LauncherFatalException("MISSING_BC", new Object[]{relation.getSecondObjectId()}));
        
        var meterReadNewStatus = event.getAction() == null ? "*" : event.getAction();
        var rule = updateBillableChargeStatus(billableCharge, meterReadNewStatus);

        journalUtils.addJournal(ObjectType.BILLABLE_CHARGE,billableCharge.getId(),"STATUS_UPDATED", 
                            new Object[]{event.getId(), billableCharge.getStatus()}, event, JournalAction.STATUS_CHANGE);
        messages.add(new messageCode("STATUS_UPDATE_BY_RULE", new Object[]{billableCharge.getStatus(), event.getId(), rule.toString()}));

        if (rule.getActivityType() != null)
            launcherQueue.createActivityEvent(rule.getActivityType(), billableCharge.getId(), ObjectType.BILLABLE_CHARGE, "systemAgent");
        
        //Traiter le statut des BC annulée par cette BC
        var bcsCancelled = billableChargeRepository.findAllByCancelledBy(billableCharge.getId());
        for(BillableCharge bcCancelled: bcsCancelled){
            var ruleOpt = updateBillableChargeStatus(bcCancelled, CANCELLED);
            if (ruleOpt.getActivityType() != null)
                launcherQueue.createActivityEvent(rule.getActivityType(), bcCancelled.getId(), ObjectType.BILLABLE_CHARGE, "systemAgent");
            //ajouter une relation activity - cancelled billableCharge
            launcherQueue.createRelation(ACTIVITY_BILLABLE_CHARGE,event.getActivityId(),bcCancelled.getId(), ObjectType.BILLABLE_CHARGE);

            //ajouter un journal sur cancelled billableCharge
            journalUtils.addJournal(ObjectType.BILLABLE_CHARGE,bcCancelled.getId(),"SET_TO_CANCEL", 
            new Object[]{ObjectType.BILLABLE_CHARGE, event.getId(), billableCharge.getId()}, event, JournalAction.STATUS_CHANGE);
            messages.add(new messageCode("SET_CANCELLED", new Object[]{ObjectType.BILLABLE_CHARGE, bcCancelled.getId()}));
        }
        journal.setMessageCodes(messages);
    }

    private ObjectProcessRule updateBillableChargeStatus(BillableCharge billableCharge, String billableChargeNew) throws LauncherException
    {
        var ruleOpt = rules.stream().filter(mrRules -> checkConditions(mrRules, billableCharge)
                && (mrRules.getInitialStatus()==null || mrRules.getInitialStatus().equals(billableCharge.getStatus().getValue()))
                && mrRules.getNewStatus().equals(billableChargeNew))
                        .findFirst()
                        .orElseThrow(
                        () -> new LauncherFatalException(
                            "NO_RULE_FOUND", new Object[]{billableCharge.getStatus(), billableChargeNew}));
        billableCharge.setStatus(BillableChargeStatus.valueOf(ruleOpt.getFinalStatus()));
        return ruleOpt;
    }

    private boolean checkConditions(ObjectProcessRule streamRule, BillableCharge billableCharge)
    {
        return (rulesNormalized(streamRule.getMarket(), billableCharge.getMarket()) && rulesNormalized(streamRule.getDirection(), billableCharge.getDirection()));
    }

    private boolean rulesNormalized(String search, String item)
    {
        if ("*".equals(search))
            return true;
        return search.equals(item == null ? "" : item);
    }
}
