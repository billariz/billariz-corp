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

package com.billariz.corp.launcher;

import static java.util.stream.Collectors.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.EventManager;
import com.billariz.corp.database.model.LauncherTagType;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventRecurrencePeriodType;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.model.light.messageCode;
import com.billariz.corp.database.repository.EventManagerRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.LauncherTagTypeRepository;
import com.billariz.corp.launcher.queue.EventManagerData;
import com.billariz.corp.launcher.queue.QueueProducer;
import com.billariz.corp.launcher.utils.JournalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventMonitor
{

    private final EventManagerRepository    eventManagerRepository;

    private final LauncherTagTypeRepository launcherTagTypeRepository;

    private final EventRepository           eventRepository;

    private final QueueProducer             queueProducer;

    private final JournalUtils journalUtils;

    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    void process() {
        log.info("[EVENTS MONITOR 🚀] ** START 🚀**");
        List<messageCode> messages = new ArrayList<>();

        Long latestEvent = eventManagerRepository.findLatestRanksExecuted()
                .stream().findFirst().orElse(1L);
        log.info("Latest Processed event Rank : {}", latestEvent);

        LauncherTagType launcherTagType = launcherTagTypeRepository
                .findNextByActiveIsTrueAndPreviousRank(latestEvent)
                .orElseGet(() -> {
                    log.debug("New account from rank 0");
                    return launcherTagTypeRepository.findFirstByActiveIsTrue();
                });

        if (launcherTagType == null) {
            log.warn("No active LauncherTagType found.");
            return;
        }

        log.info("Processing LauncherTagType ID => {}", launcherTagType.getId());
        messages.add(new messageCode("PROCESS_LAUNCHER_TAG", new Object[]{launcherTagType.getId()}));

        var eventList = getEventList(EventStatus.PENDING, EventExecutionMode.EVENT_MANAGER, launcherTagType);
        if (!eventList.isEmpty()) {
            log.info("Total Eligible Events: {} for LauncherTagType ID => {}", eventList.size(), launcherTagType.getId());
            messages.add(new messageCode("PROCESS_LIST_ON_LAUNCHER_TAG", new Object[]{eventList.size(), launcherTagType.getId()}));
            sendMessageToEventManagerQueue(eventList, launcherTagType);
        } else {
            messages.add(new messageCode("NO_OBJECT_TO_PROCESS_FOR_LAUNCHER_TAG", new Object[]{launcherTagType.getId()}));
            log.info("No Eligible Events for LauncherTagType ID => {}", launcherTagType.getId());
        }

        updateEventManagerMonitor(launcherTagType, messages);

        log.info("[EVENTS MONITOR ✅] ** END ✅**");
    }

    private Set<Long> getEventList(EventStatus status, EventExecutionMode executionMode, LauncherTagType launcherTagType)
    {
        List<Event> eventList = eventRepository.findAllByStatusAndExecutionModeAndTagType(status, executionMode, launcherTagType.getId(), LocalDate.now());
        if (!eventList.isEmpty()){
            log.debug("Total Event List Size : {} for launcherTagType => {}", eventList.size(), launcherTagType.getId());
        
            eventList.removeIf(e -> !checkEventExecutionDate(e));
        }
        return eventList.stream().collect(mapping(Event::getId, toSet()));
    }

    private boolean checkEventExecutionDate(Event event)
    {
        if (event.getExecutionDate() != null)
        {
            ChronoUnit chronoUnit = getChronoUnit(EventRecurrencePeriodType.valueOf(event.getType().getRecurrencePeriodType()));
            if (chronoUnit != null)
            {
                return (chronoUnit.between(event.getExecutionDate(), OffsetDateTime.now()) >= event.getType().getRecurrencePeriod());
            }
        }
        return true;
    }

    private ChronoUnit getChronoUnit(EventRecurrencePeriodType recurrencePeriodType)
    {
        switch (recurrencePeriodType)
        {
        case DAY:
            return ChronoUnit.DAYS;
        case HOUR:
            return ChronoUnit.HOURS;
        case MINUTE:
            return ChronoUnit.MINUTES;
        case WEEK:
            return ChronoUnit.WEEKS;
        case MONTH:
            return ChronoUnit.MONTHS;
        case YEAR:
            return ChronoUnit.YEARS;
        default:
        }
        return null;
    }

    private void sendMessageToEventManagerQueue(Set<Long> eventsToSend, LauncherTagType launcherTagType)
    {
        boolean paginationCondition = true;
        int offset = 0;
        int packetSize = launcherTagType.getPacketSize();
        EventManagerData eventManagerData = new EventManagerData(launcherTagType.getId(), launcherTagType.isSynchronous(),
                EventExecutionMode.EVENT_MANAGER);

        while (paginationCondition)
        {
            Set<Long> eventsIds = eventsToSend.stream().skip(offset).limit(packetSize).collect(toSet());
            eventsIds.forEach(evtId -> eventManagerData.ids().add(evtId));
            paginationCondition = (eventsIds.size() == packetSize);
            offset += packetSize;

            log.info("Data sent to EVENT_LAUNCHER => " + eventManagerData);
            queueProducer.publish(eventManagerData);
            eventRepository.updateEventsStatusByIdIn(eventsIds, EventStatus.TRANSMITTED);
            eventManagerData.ids().clear();
        }
    }
    
    private void updateEventManagerMonitor(LauncherTagType launcherTagType, List<messageCode> messages)
    {
        EventManager eventManager = new EventManager();
        eventManager.setCreatedAt(OffsetDateTime.now());
        eventManager.setRank(launcherTagType.getRank());
        eventManager.setLauncherTagType(launcherTagType);
        eventManager.setDefaultLabel(launcherTagType.getDefaultLabel());
        eventManager.setExecutionMode(EventExecutionMode.EVENT_MANAGER);
        eventManagerRepository.save(eventManager);

        //Journalization
        messages.forEach(m -> journalUtils.addJournal(ObjectType.EVENT_MANAGER, eventManager.getId(), m.getName(), 
                                                m.getArgs(), null, JournalAction.LOG));
    }
}
