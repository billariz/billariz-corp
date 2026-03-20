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

package com.billariz.corp.database.projection;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.rest.core.config.Projection;
import com.billariz.corp.database.model.Activity;
import com.billariz.corp.database.model.ActivityType;
import com.billariz.corp.database.model.Relation;
import com.billariz.corp.database.model.User;
import com.billariz.corp.database.model.enumeration.ActivityStatus;

@Projection(name = "inActivity", types = { Activity.class })
public interface inActivity
{

    Long getId();

    String getType();

    ActivityType getActivityType();

    List<Relation> getRelations();

    //List<inEvent> getEvents();

    String getCategory();

    String getSubCategory();

    ActivityStatus getStatus();

    OffsetDateTime getStartDate();

    OffsetDateTime getEndDate();

    User getCreatedByUser();

}
