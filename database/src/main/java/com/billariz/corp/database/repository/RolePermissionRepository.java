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
import com.billariz.corp.database.model.RolePermission;
import com.billariz.corp.database.projection.inRolePermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RepositoryRestResource(excerptProjection = inRolePermission.class)
public interface RolePermissionRepository extends RepositoryReadWriteDelete<RolePermission, Long>
{
    @Query("SELECT u FROM RolePermission u " +
       "WHERE (u.roleId = :roleId) " +
       "AND (:action IS NULL OR u.permission.action = :action) " +
       "AND (:entity IS NULL OR u.permission.entity = :entity)")
    List<RolePermission> findByRoleAndEntityAndAction(Long roleId, String action, String entity);


    @Query("SELECT u FROM RolePermission u " +
    "WHERE (:roleId IS NULL OR u.roleId = :roleId) " +
    "AND (:category IS NULL OR u.permission.category = :category) " +
    "AND (:action IS NULL OR u.permission.action = :action) " +
    "AND (:entity IS NULL OR u.permission.entity = :entity)")
    Page<RolePermission> findRolePermission(Long roleId, 
                         String category, 
                         String action, 
                         String entity, 
                         Pageable pageable);
}
