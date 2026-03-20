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
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.User;
import com.billariz.corp.database.model.enumeration.ObjectType;

@Projection(name = "inJournal", types = { Journal.class })
public interface inJournal
{

    Long getId();

    ObjectType getObjectType();

    Long             getObjectId();

    User           getUser();

    //String getUserName();

    OffsetDateTime   getCreationDate();

    String    getMethod();

    String getIpAdress();

    String getUserAgent();

    String getForwardedFor();

    String           getComment();

    String           getNewStatus();

    String getApiPath();

    List<String> getMessages();

}
