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

package com.billariz.corp.launcher.queue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import javax.transaction.Transactional;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import com.billariz.corp.database.model.Activity;
import com.billariz.corp.database.model.ActivityTemplate;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.EventTemplate;
import com.billariz.corp.database.model.Relation;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.EventTriggerDateMode;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.repository.ActivityRepository;
import com.billariz.corp.database.repository.ActivityTemplateRepository;
import com.billariz.corp.database.repository.ActivityTypeRepository;
import com.billariz.corp.database.repository.ContractRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.launcher.utils.ActivityUtils;
import com.billariz.corp.launcher.utils.EventUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LauncherQueue
{
    private final static String ACTIVITY_CONTRACT="ACTIVITY_CONTRACT";

    private final static String ACTIVITY_METER_READ="ACTIVITY_METER_READ";

    private final static String ACTIVITY_PERIMETER="ACTIVITY_PERIMETER";

    private final static String ACTIVITY_INVOICE="ACTIVITY_INVOICE";

    private final static String ACTIVITY_USER="ACTIVITY_USER";

    private final static String ACTIVITY_BILLABLE_CHARGE="ACTIVITY_BILLABLE_CHARGE";

    private final static String ACTIVITY_POS ="ACTIVITY_POS";

    private static final String              BAD = "BAD";

    private final ActivityRepository         activityRepository;

    private final ActivityTemplateRepository activityTemplateRepository;

    private final ActivityTypeRepository     activityTypeRepository;

    private final EventRepository            eventRepository;

    private final QueueProducer              queueProducer;

    private final RelationRepository         relationRepository;

    private final ContractRepository contractRepository;

    public void createActivityEvent(String activityType, Long objectId, ObjectType objectType, String createdBy)
    {
        var at = activityTypeRepository.findById(activityType);
        var activityTemplates = activityTemplateRepository.findAllByTypeIdOrderByRank(activityType);

        if (at.isPresent() && !activityTemplates.isEmpty())
        {
            var activityFirstTemplate = activityTemplates.get(0);
            var activity = ActivityUtils.create(at.get(), activityFirstTemplate, createdBy);
            activityRepository.save(activity);

            createRelation(defineRelationType(objectType), activity.getId(), objectId, objectType);
            for (ActivityTemplate activityTemplate : activityTemplates)
            {
                var eventTemplate = activityTemplate.getEventTemplate();

                if (eventTemplate != null)
                if(eventTemplate.getTriggerDateMode().equals(BAD))
                    createEvent(contractRepository.findById(objectId).get(), activity, activityTemplate, eventTemplate, null, null);
                else 
                    createEvent(null, activity, activityTemplate, eventTemplate, null, null);
            }
        }
    }

    public void createActivityEvent(String activityType, String createdBy,
                                        Long objectId, ObjectType ObjectType, 
                                        Long secondObjectId, ObjectType secondObjectType
                                    )
    {
        var at = activityTypeRepository.findById(activityType);
        var activityTemplates = activityTemplateRepository.findAllByTypeIdOrderByRank(activityType);

        if (at.isPresent() && !activityTemplates.isEmpty())
        {
            var activityFirstTemplate = activityTemplates.get(0);
            var activity = ActivityUtils.create(at.get(), activityFirstTemplate, createdBy);
            activityRepository.save(activity);

            createRelation(defineRelationType(ObjectType), activity.getId(), objectId, ObjectType);

            if (secondObjectType != null)
                createRelation(defineRelationType(secondObjectType), activity.getId(), secondObjectId, secondObjectType);

            for (ActivityTemplate activityTemplate : activityTemplates)
            {
                var eventTemplate = activityTemplate.getEventTemplate();
                if (eventTemplate != null){
                    if(eventTemplate.getTriggerDateMode().equals(BAD))
                        createEvent(contractRepository.findById(objectId).get(), activity, activityTemplate, eventTemplate, null, null);
                    else 
                        createEvent(null, activity, activityTemplate, eventTemplate, null, null);
                }
            }
        }
    }

    public void createActivityEvent(String activityType, String createdBy,
                                        Long objectId, ObjectType ObjectType, 
                                        Long secondObjectId, ObjectType secondObjectType, 
                                        LocalDate cutOffDate, String action)
    {
        var at = activityTypeRepository.findById(activityType);
        var activityTemplates = activityTemplateRepository.findAllByTypeIdOrderByRank(activityType);

        if (at.isPresent() && !activityTemplates.isEmpty())
        {
            var activityFirstTemplate = activityTemplates.get(0);
            var activity = ActivityUtils.create(at.get(), activityFirstTemplate, createdBy);
            activityRepository.save(activity);

            createRelation(defineRelationType(ObjectType), activity.getId(), objectId, ObjectType);

            if (secondObjectType != null)
                createRelation(defineRelationType(secondObjectType), activity.getId(), secondObjectId, secondObjectType);

            for (ActivityTemplate activityTemplate : activityTemplates)
            {
                var eventTemplate = activityTemplate.getEventTemplate();
                if (eventTemplate != null){
                    if(eventTemplate.getTriggerDateMode().equals(BAD))
                        createEvent(contractRepository.findById(objectId).get(), activity, activityTemplate, eventTemplate, cutOffDate, action);
                    else 
                        createEvent(null, activity, activityTemplate, eventTemplate, cutOffDate, action);
                }
            }
        }
    }

    public String defineRelationType(ObjectType objectType)
    {
        switch (objectType)
        {
        case CONTRACT:
            return ACTIVITY_CONTRACT;
        case METER_READ:
            return ACTIVITY_METER_READ;
        case PERIMETER:
            return ACTIVITY_PERIMETER;
        case BILL:
            return ACTIVITY_INVOICE;
        case USER:
            return ACTIVITY_USER;
        case BILLABLE_CHARGE:
            return ACTIVITY_BILLABLE_CHARGE;
        case POINT_OF_SERVICE:
            return ACTIVITY_POS;
        case BILLING_RUN:
            return "ACTIVITY_BILLING_RUN";
        default:
            return ACTIVITY_CONTRACT;
        }
    }

    public void createEvent(Contract ctr, Activity activity, ActivityTemplate activityTemplate, EventTemplate eventTemplate,
                                LocalDate cutOffDate, String action)
    {
        var event = new Event();

        event.setActivity(activity);
        event.setType(eventTemplate);
        event.setRank(activityTemplate.getRank());
        event.setStatus(eventTemplate.getDefaultStatus());
        event.setExecutionMode(EventExecutionMode.valueOf(eventTemplate.getDefaultExecutionMode()));
        //TODO affecter comme holder le defauttHolder du template sinon au user master du group et entity sinon à l'eventManager
        event.setUserId(eventTemplate.getDefaultHolder());
        event.setGroupId(eventTemplate.getGroupId());
        event.setOrganismId(eventTemplate.getOrganismId());
        if(eventTemplate.getAction()!=null)
            event.setAction(eventTemplate.getAction().equals("?") ? action : eventTemplate.getAction());
        else event.setAction(null);
        event.setTagType(eventTemplate.getTagType());
        event.setCreationDate(LocalDateTime.now());
        event.setTriggerDate(cutOffDate!=null ? cutOffDate : EventUtils.calculateTriggerDate(ctr, eventRepository, event, eventTemplate, activityTemplate));
        event.setCascaded(true);
        eventRepository.save(event);
        if (event.getRank() == 1)
            sendFirstEventToQueue(event);
    }

    public void createRelation(String relationType, Long firstObjectId, Long secondObjectId, ObjectType secondobjectType)
    {
        if (!relationRepository.existsByRelationTypeAndFirstObjectIdAndSecondObjectId(relationType, firstObjectId, secondObjectId))
        {
            var relation = new Relation();
            relation.setRelationType(relationType);
            relation.setFirstObjectId(firstObjectId);
            relation.setSecondObjectId(secondObjectId);
            relation.setSecondObjectType(secondobjectType);
            relation.setCreatedAt(LocalDateTime.now());
            relationRepository.save(relation);
        }
    }

    public void sendFirstEventToQueue(Event event)
    {
        setActivityInProgress(event);
        event.setCascaded(true);
        if (isSelfLaunchingEventMode(event) && checkEventStatus(event.getStatus()))
        {
            var eventTemplate = event.getType();
            var activityType = event.getActivity().getType();
            var startDatePeriod = activityTemplateRepository.findByActivityTypeIdAndRank(activityType, event.getRank()).map(
                    ActivityTemplate::getStartDatePeriod).orElse(0);
            if (eventTemplate != null && checkTriggerDateMode(EventTriggerDateMode.valueOf(eventTemplate.getTriggerDateMode()), event, startDatePeriod))
            {
                var eventManagerData = new EventManagerData(event.getTagType().getId(), event.getTagType().isSynchronous(),
                        event.getExecutionMode());
                eventManagerData.ids().add(event.getId());
                queueProducer.publish(eventManagerData);
                event.setStatus(EventStatus.TRANSMITTED);
            }
        }
        else
        {
            event.setStatus(EventStatus.PENDING);
        }
    }

    public boolean checkEventStatus(EventStatus status)
    {
        return (status.equals(EventStatus.INITIALIZED) || status.equals(EventStatus.PENDING) 
                    || status.equals(EventStatus.IN_PROGRESS)) || status.equals(EventStatus.SUSPENDED) ;
    }

    public void sendEventToQueue(Event event)
    {
        log.debug("*** Processing sendEventToQueue:", event);
        if (isSelfLaunchingEventMode(event) && checkEventStatus(event.getStatus()))
        {
            var eventTemplate = event.getType();
            var activityType = event.getActivity().getType();
            var startDatePeriod = activityTemplateRepository.findByActivityTypeIdAndRank(activityType, event.getRank()).map(
                    ActivityTemplate::getStartDatePeriod).orElse(0);

            if (eventTemplate != null && checkTriggerDateMode(EventTriggerDateMode.valueOf(eventTemplate.getTriggerDateMode()), event, startDatePeriod))
            {
                var eventManagerData = new EventManagerData(eventTemplate.getTagType().getId(),
                        eventTemplate.getTagType().isSynchronous(), event.getExecutionMode());
                eventManagerData.ids().add(event.getId());
                queueProducer.publish(eventManagerData);
                event.setCascaded(true);
                event.setStatus(EventStatus.TRANSMITTED);
                setActivityInProgress(event);
            }
        }
    }

    private Boolean isSelfLaunchingEventMode(Event event)
    {
        return event.getExecutionMode().equals(EventExecutionMode.AUTO);
    }

    public void setActivityCompleted(Event event)
    {
        var activity = event.getActivity();

        activity.setStatus(ActivityStatus.COMPLETED);
        activity.setEndDate(OffsetDateTime.now());
    }

    public void setActivityInProgress(Event event)
    {
        var activity = event.getActivity();
        if(!ActivityStatus.IN_PROGRESS.equals(activity.getStatus())){
            activity.setStatus(ActivityStatus.IN_PROGRESS);
            if(activity.getStartDate()==null) 
                    activity.setStartDate(OffsetDateTime.now());
        }
    }

    private boolean checkTriggerDateMode(EventTriggerDateMode triggerDateMode, Event event, int startDatePeriod)
    {
        switch (triggerDateMode) {
            case OFFICE:
                    return checkEventTriggerDate(event.getTriggerDate());
            case FIELD:
                if (event.getTriggerDate() == null)
                {
                    event.setTriggerDate(LocalDate.now().plusDays(startDatePeriod));
                    eventRepository.save(event);
                }
                return checkEventTriggerDate(event.getTriggerDate());
            case BAD:
                return checkEventTriggerDate(event.getTriggerDate());
            default:
                break;
        }
        return false;
    }

    private boolean checkEventTriggerDate(LocalDate triggerDate)
    {
        return LocalDate.now().equals(triggerDate) || triggerDate.isBefore(LocalDate.now());
    }
}
