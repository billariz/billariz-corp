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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.ObjectProcessRule;
import com.billariz.corp.database.model.enumeration.ObjectType;

@RepositoryRestResource(collectionResourceRel = "objectProcessRules", path = "objectProcessRules")
public interface ObjectProcessRuleRepository extends RepositoryReadWriteDelete<ObjectProcessRule, Long>
{
    List<ObjectProcessRule> findAll();

    List<ObjectProcessRule> findAllByObjectType(ObjectType objectType);

    List<ObjectProcessRule> findAllByNewStatusAndInitialStatusAndObjectType(String newStatus, String initialStatus, ObjectType objectType);

    @Query("SELECT s FROM ObjectProcessRule s WHERE (:id IS NULL OR s.id=:id) AND (:objectType IS NULL OR s.objectType=:objectType) AND (:activityType IS NULL OR s.activityType=:activityType)")
    Page<ObjectProcessRule> findObjectProcessRule(@Param("id") Long id, @Param("objectType") ObjectType objectType, @Param("activityType") String activityType, Pageable pageable);

}
