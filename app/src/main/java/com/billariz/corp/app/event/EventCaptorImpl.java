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

package com.billariz.corp.app.event;

import java.util.Locale;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.ObjectProcessRule;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.ObjectProcessRuleRepository;
import com.billariz.corp.notifier.event.EventCaptor;
import com.billariz.corp.launcher.queue.LauncherQueue;
import com.billariz.corp.launcher.utils.EventUtils;
import com.billariz.corp.launcher.utils.JournalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


@Component
@RequiredArgsConstructor
@Slf4j
public class EventCaptorImpl implements EventCaptor
{
    @PersistenceContext
    private EntityManager                             entityManager;

    private ThreadLocal<Optional<ObjectProcessRule>> ruleActive = new ThreadLocal<>();

    private final ObjectProcessRuleRepository        objectProcessRulesRepository;

    private final EventRepository                     eventRepository;

    private final LauncherQueue                       launcherQueue;

    private final JournalUtils                 journalUtils;

    private final EventUtils                 eventUtils;

    private final Locale locale = LocaleContextHolder.getLocale();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPreUpdate(Event event)
    {
        var oldStatus = getOldValue(event);
        if(oldStatus.equals(EventStatus.IN_PROGRESS))
            throw new IllegalArgumentException (journalUtils
                                .getMessage("EVENT_ALREADY_INPROGRESS", new Object[]{event.getId()}, locale));

        var newStatus = event.getStatus();
        var rules = objectProcessRulesRepository.findAllByNewStatusAndInitialStatusAndObjectType(newStatus.toString(),
                oldStatus != null ? oldStatus.toString() : null, ObjectType.EVENT);
        if (!rules.isEmpty()){
            var rule = rules.stream().filter(eventProcessRules -> checkConditions(eventProcessRules, event)).findFirst();
            //event.setStatus(EventStatus.valueOf(rule.get().getFinalStatus()));
            ruleActive.set(rule);
        }
        else
            throw new IllegalArgumentException (journalUtils
                            .getMessage("NO_RULES_TO_PROCEED", new Object[]{ObjectType.EVENT, event.getId(),newStatus}, locale));
    }

    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPostUpdate(Event event)
    {
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();

        var rule = ruleActive.get();
        if (!rule.isEmpty() && rule.get().getActivityType() != null)
        {
            var activity = getAction(rule.get().getActivityType());
            if (activity.equals(rule.get().getActivityType()))
            {
                journalUtils.addJournal(ObjectType.EVENT, event.getId(),
                                "LAUNCH_ACTIVITY_NOT_IMPLEMENTED", 
                                new Object[]{activity, "EVENT", event.getId()},
                                event,
                                JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                                );
            }
            else
            {
                launchEvent(event, activity);
                journalUtils.addJournal(ObjectType.EVENT, event.getId(),
                            "LAUNCH_ACTIVITY_ON_OBJECT", 
                            new Object[]{activity, "EVENT", event.getId()},
                            event,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
            }
        }
        else 
            eventUtils.triggerOnUpdateEventStatus(event);

        ruleActive.remove();
    }


    private String getAction(String actType)
    {
        int indexDeuxPoints = actType.indexOf(":");
        if (indexDeuxPoints != -1)
        {
            return actType.substring(indexDeuxPoints + 1);
        }
        else
        {
            return actType;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void launchEvent(Event event, String action)
    {
        switch (action)
        {
        case "LAUNCH_EVENT":
            launcherQueue.sendEventToQueue(event);
            break;
        default:
            break;
        }
    }

    private boolean checkConditions(ObjectProcessRule streamRule, Event eventDatabase)
    {
        return true;
    }

    private EventStatus getOldValue(Event event) {

        return Optional.ofNullable(event.getId())
                       .flatMap(eventRepository::findById)
                       .map(Event::getStatus)
                       .orElse(null);
    }
}
