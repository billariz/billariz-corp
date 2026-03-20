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
import com.billariz.corp.database.model.Role;
import com.billariz.corp.database.projection.inRole;

@RepositoryRestResource(excerptProjection = inRole.class)
public interface RoleRepository extends RepositoryReadWriteDelete<Role, Long>
{
    @Query("SELECT a FROM Role a " +
    "WHERE (:id IS NULL OR a.id = :id) " +
    "AND (:name IS NULL OR a.name = :name) " +
    "AND (:label IS NULL OR " +
                "lower(a.defaultLabel) LIKE CONCAT('%', lower(:label), '%') OR " +
                "lower(a.standardLabel) LIKE CONCAT('%', lower(:label), '%') OR " +
                "lower(a.otherLabel) LIKE CONCAT('%', lower(:label), '%')) ")
    Page<Role> findRole(@Param("id") Long id, 
                                 @Param("name") String name, 
                                 @Param("label") String label, 
                                 Pageable pageable);
}
