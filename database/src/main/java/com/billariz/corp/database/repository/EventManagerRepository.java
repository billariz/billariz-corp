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

package com.billariz.corp.database.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.data.RepositoryReadWriteFull;
import com.billariz.corp.database.model.EventManager;

@RepositoryRestResource
public interface EventManagerRepository extends RepositoryReadWriteDelete<EventManager, Long>
{
    @Query("SELECT mn.rank FROM EventManager mn WHERE mn.id IN (SELECT max(id) FROM EventManager)")
    List<Long> findLatestRanksExecuted();

}
