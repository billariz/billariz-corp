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
import com.billariz.corp.database.model.UserRole;
import com.billariz.corp.database.projection.inUserRole;

@RepositoryRestResource(excerptProjection = inUserRole.class)
public interface UserRoleRepository extends RepositoryReadWriteDelete<UserRole, Long>
{
    @Query("SELECT u FROM UserRole u " +
    "WHERE (:userId IS NULL OR u.userId = :userId) " +
    "AND (:userName IS NULL OR u.user.userName = :userName) " +
    "AND (:roleId IS NULL OR u.roleId = :roleId) " +
    "AND (:roleName IS NULL OR u.role.name = :roleName) " +
    "AND (:roleLabel IS NULL OR " +
                "lower(u.role.defaultLabel) LIKE CONCAT('%', lower(:roleLabel), '%') OR " +
                "lower(u.role.standardLabel) LIKE CONCAT('%', lower(:roleLabel), '%') OR " +
                "lower(u.role.otherLabel) LIKE CONCAT('%', lower(:roleLabel), '%')) ")
    Page<UserRole> findUserRole(Long userId, 
                         String userName,
                         Long roleId, 
                         String roleName, 
                         String roleLabel, 
                         Pageable pageable);
}
