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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.ActivityType;

@RepositoryRestResource
public interface ActivityTypeRepository extends RepositoryReadWriteDelete<ActivityType, String>
{
    @Query("SELECT at FROM ActivityType at WHERE (:id IS NULL OR at.id LIKE %:id%) AND (:category IS NULL OR at.category=:category) AND (:subCategory IS NULL OR at.subCategory=:subCategory)")
    Page<ActivityType> findActivityType(@Param("id") String id, @Param("category") String category, @Param("subCategory") String subCategory, Pageable pageable);

}
