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

package com.billariz.corp.launcher.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.ActivityTemplate;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.EventTemplate;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.repository.ActivityTemplateRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.validator.BaseValidator;
import com.billariz.corp.launcher.queue.LauncherQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventUtils
{
    private static final String              CALENDAR = "CALENDAR";

    private static final String              WORKDAYS = "WORKDAYS";

    private static final String              OFFICE = "OFFICE";

    private static final String              FIELD = "FIELD";

    private static final String              BAD = "BAD";

    private final EventRepository            eventRepository;

    private final ActivityTemplateRepository activityTemplateRepository;

    private final LauncherQueue              launcherQueue;

    public static LocalDate calculateTriggerDate(Contract ctr, EventRepository eventRepository, Event event, EventTemplate eventTemplate, ActivityTemplate activityTemplate)
    {
        if (eventTemplate.getTriggerDateMode() != null)
        {
            switch (eventTemplate.getTriggerDateMode()) {
                case OFFICE:
                        var existingEvent = eventRepository.findFirstByTypeAndRank(eventTemplate, Math.max((event.getRank() - 1), 1));
                        if (existingEvent.isPresent())
                            return handleTriggerDate(existingEvent.get().getTriggerDate(), eventTemplate, activityTemplate);
                    break;
                case FIELD:
                        if (event.getRank() == 1)
                            return handleTriggerDate(LocalDate.now(), eventTemplate, activityTemplate);
                    break;
                case BAD:
                        if(ctr !=null) 
                            return handleTriggerDate(ctr.getBillAfterDate(), eventTemplate, activityTemplate);
                default:
                    break;
            }
        }
        return null;
    }

    private static LocalDate handleTriggerDate(LocalDate date, EventTemplate eventTemplate, ActivityTemplate activityTemplate)
    {
        if (CALENDAR.equalsIgnoreCase(eventTemplate.getPeriodSystem()))
            return date.plusDays(activityTemplate.getStartDatePeriod());
        else if (WORKDAYS.equalsIgnoreCase(eventTemplate.getPeriodSystem()))
            return forBusinessDaysOnly(date, activityTemplate.getStartDatePeriod());
        return null;
    }

    private static LocalDate forBusinessDaysOnly(LocalDate date, int afterNumberOfDays)
    {
        int tmpDays = afterNumberOfDays;

        if (tmpDays == 0 && (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY))
            return date;
        else if ((date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY))
            tmpDays -= 1;
        return forBusinessDaysOnly(date.plusDays(1), tmpDays);
    }

    public void triggerOnUpdateEventStatus(Event event)
    {
        // eventRepository.save(event);
        log.debug("triggerOnUpdateEventStatus: {}", event);
        switch (event.getStatus())
        {
        case COMPLETED:
            caseCompleted(event);
        case CANCELLED:
            caseCanceled(event);
        case SUSPENDED:
            caseSuspended(event);
        case PENDING:
            casePending(event);
        default:
            break;
        }
    }

    private void caseCompleted(Event event)
    {
        log.debug("*** Case event Completed");
        var eventList= eventRepository.findAllByActivityId(event.getActivityId());
        var nextEvent = eventList.stream().filter(i -> i.getRank() == event.getRank() + 1).findFirst();
        if (nextEvent.isPresent())
        {
            Event eventNextRankOpt = nextEvent.get();
            var startDatePeriod = activityTemplateRepository.findByActivityTypeIdAndRank(eventNextRankOpt.getType().getId(), eventNextRankOpt.getRank()).map(
                    ActivityTemplate::getStartDatePeriod).orElse(0);
            eventNextRankOpt.setCascaded(true);
            eventNextRankOpt.setTriggerDate(ActivityTemplateUtils.computeNextDate(eventNextRankOpt.getType().getPeriodSystem(), startDatePeriod));
            eventNextRankOpt.setStatus(EventStatus.PENDING);
            if (eventNextRankOpt.getExecutionMode().equals(EventExecutionMode.AUTO)) {
                launcherQueue.sendEventToQueue(eventNextRankOpt);
            }
        }
        else
            launcherQueue.setActivityCompleted(event);
    }

    private void caseCanceled(Event event)
    {
        eventRepository.updateNextStatus(event.getActivityId(), event.getRank(), EventStatus.CANCELLED);
    }

    private void caseSuspended(Event event)
    {
        eventRepository.updateNextStatus(event.getActivityId(), event.getRank(), EventStatus.SUSPENDED);
    }

    private void casePending(Event event)
    {
        if (event.getExecutionMode().equals(EventExecutionMode.AUTO))
                launcherQueue.sendEventToQueue(event);
    }

    public Journal addJournal(Event event)
    {
        var journal = new Journal();
        journal.setCreationDate(OffsetDateTime.now());
        journal.setMethod(JournalAction.LOG.getValue());
        journal.setObjectType(ObjectType.EVENT);
        journal.setObjectId(event.getId());
        journal.setUserName(getUserName(event.getExecutionMode()));
        return journal;
    }

    public Journal addJournalFull(Event event, String comment, JournalAction action)
    {
        var journal = new Journal();
        journal.setCreationDate(OffsetDateTime.now());
        journal.setMethod(action.getValue());
        journal.setObjectType(ObjectType.EVENT);
        journal.setObjectId(event.getId());
        journal.setUserName(getUserName(event.getExecutionMode()));
        journal.setComment(comment);
        if (action == JournalAction.STATUS_CHANGE)
            journal.setNewStatus(event.getStatus().toString());
        return journal;
    }

    public Journal addJournal(Event event, String message, JournalAction action)
    {
        var journal = new Journal();
        journal.setCreationDate(OffsetDateTime.now());
        journal.setMethod(action.getValue());
        journal.setObjectType(ObjectType.EVENT);
        journal.setObjectId(event.getId());
        journal.setUserName(getUserName(event.getExecutionMode()));
        journal.setComment(message);
        return journal;
    }

    public String getUserName(EventExecutionMode exeMod){
        switch (exeMod) {
            case EVENT_MANAGER:
                return BaseValidator.EVENT_MANAGER;
            case AUTO:
                return BaseValidator.SYSTEM_AGENT;
            case MANUAL:
                return "REFERER";
            default:
                return "REFERER";
        }
    }
}
