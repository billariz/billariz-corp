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

import java.time.OffsetDateTime;
import com.billariz.corp.database.model.Activity;
import com.billariz.corp.database.model.ActivityTemplate;
import com.billariz.corp.database.model.ActivityType;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.repository.ActivityRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.RelationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityUtils
{
    private final EventRepository  eventRepository;

    private final ActivityRepository  activityRepository;

    private final RelationRepository relationRepository;

    private final JournalRepository journalRepository;

    private final static String ACTIVITY_CONTRACT="ACTIVITY_CONTRACT";

    private final static String ACTIVITY_METER_READ="ACTIVITY_METER_READ";

    private final static String ACTIVITY_PERIMETER="ACTIVITY_PERIMETER";

    private final static String ACTIVITY_INVOICE="ACTIVITY_INVOICE";

    private final static String ACTIVITY_USER="ACTIVITY_USER";

    private final static String ACTIVITY_BILLABLE_CHARGE="ACTIVITY_BILLABLE_CHARGE";

    public static Activity create(ActivityType activityType, ActivityTemplate activityTemplate, String createdBy)
    {
        var activity = new Activity();

        activity.setType(activityType.getId());
        activity.setCategory(activityType.getCategory());
        activity.setSubCategory(activityType.getSubCategory());
        activity.setCreatedBy(createdBy);
        activity.setStatus(activityTemplate.getDefaultStatus());
        activity.setStartDate(OffsetDateTime.now());
        return activity;
    }

    public void suspendActivity(ObjectType object, Long objectId, String comment)
    {
        var relatioType=defineRelationType(object);
        var relation = relationRepository.findFirstByRelationTypeAndSecondObjectId(relatioType,objectId);
        if(relation.isPresent() && !relation.isEmpty()){
            var act = activityRepository.findById(relation.get().getFirstObjectId()).orElseThrow();
            act.setStatus(ActivityStatus.SUSPENDED);
            eventRepository.updateEventsStatusByActivityId(relation.get().getFirstObjectId(),EventStatus.SUSPENDED);
            journalRepository.save(JournalUtils.build(relation.get().getFirstObjectId(), comment, ObjectType.ACTIVITY));
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
        default:
            return ACTIVITY_CONTRACT;
        }
    }
}
