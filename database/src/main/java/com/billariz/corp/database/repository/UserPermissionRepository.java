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
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.UserPermission;
import com.billariz.corp.database.projection.inUserPermission;


@RepositoryRestResource(excerptProjection = inUserPermission.class)
public interface UserPermissionRepository extends RepositoryReadWriteDelete<UserPermission, Long>
{
    @Query("SELECT u FROM UserPermission u " +
       "WHERE (u.userId = :userId) " +
       "AND (u.permission.action = :action) " +
       "AND (u.permission.entity = :entity)")
    UserPermission findByUserAndPermisson(Long userId, String action, String entity);

     @Query("SELECT u FROM UserPermission u " +
    "WHERE (:userId IS NULL OR u.userId = :userId) " +
    "AND (:category IS NULL OR u.permission.category = :category) " +
    "AND (:action IS NULL OR u.permission.action = :action) " +
    "AND (:entity IS NULL OR u.permission.entity = :entity)")
    Page<UserPermission> findUserPermission(Long userId, 
                         String category, 
                         String action, 
                         String entity, 
                         Pageable pageable);
}
