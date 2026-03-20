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
import com.billariz.corp.data.RepositoryReadWriteFull;
import com.billariz.corp.database.model.PerimeterType;

@RepositoryRestResource
public interface PerimeterTypeRepository extends RepositoryReadWriteDelete<PerimeterType, String>
{
        @Query("SELECT p FROM PerimeterType p " +
        "WHERE (:id IS NULL OR p.id = :id) " +
        "AND (:billable IS NULL OR p.billable = :billable)")
        Page<PerimeterType> findPerimeterType(@Param("billable") Boolean billable, 
                                       @Param("id") String id,
                                       Pageable pageable);
}
