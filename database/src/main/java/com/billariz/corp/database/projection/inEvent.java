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

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.rest.core.config.Projection;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.EventTemplate;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.User;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;;

@Projection(name = "inEvent", types = { Event.class })
public interface inEvent
{

    Long getId();

    Long getActivityId();

    EventTemplate getType();

    String getAction();

    String getLauncherTag();

    int getRank();

    EventStatus getStatus();

    EventExecutionMode getExecutionMode();

    String getGroupId();

    String getOrganismId();

    String getExternalEventRef();

    LocalDateTime getCreationDate();

    LocalDate getTriggerDate();

    LocalDateTime getExecutionDate();

    User getUserHolder();

    Journal getJournal();

}
