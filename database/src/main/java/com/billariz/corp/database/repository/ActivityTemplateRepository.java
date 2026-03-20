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
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.ActivityTemplate;
import com.billariz.corp.database.projection.inActivityTemplate;

@RepositoryRestResource(excerptProjection = inActivityTemplate.class)
public interface ActivityTemplateRepository extends RepositoryReadWriteDelete<ActivityTemplate, Long>
{
    List<ActivityTemplate> findAllByTypeIdOrderByRank(String activityType);

    @Query("SELECT a FROM ActivityTemplate a WHERE a.type.id=:activityType AND a.rank=:rank")
    Optional<ActivityTemplate> findByActivityTypeIdAndRank(String activityType, Integer rank);

    @Query("SELECT at FROM ActivityTemplate at WHERE (:id IS NULL OR at.id =:id) AND (:eventType IS NULL OR at.eventType=:eventType) AND (:rank IS NULL OR at.rank=:rank) AND (:activityType IS NULL OR at.activityType=:activityType)")
    Page<ActivityTemplate> findActivityTemplate(@Param("id") Long id, @Param("rank") Integer rank, @Param("eventType") String eventType, @Param("activityType") String activityType, Pageable pageable);

}
