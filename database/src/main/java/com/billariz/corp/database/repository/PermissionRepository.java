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
import com.billariz.corp.database.model.Permission;

@RepositoryRestResource
public interface PermissionRepository extends RepositoryReadWriteDelete<Permission, Long>
{
  @Query("SELECT p FROM Permission p "+
  "WHERE (:id IS NULL OR p.id = :id) " +
  "AND (:category IS NULL OR p.category=:category) "+
  "AND (:action IS NULL OR p.action=:action) "+
  "AND (:entity IS NULL OR p.entity=:entity) "
  )
  Page<Permission> findPermission(Long id, 
                                String category, 
                                String action, 
                                String entity,
                                Pageable pageable);

  @Query("""
    SELECT DISTINCT p FROM Permission p
    WHERE (:entity IS NULL OR p.entity = :entity)
      AND (
        p.id IN (
          SELECT up.permissionId
          FROM UserPermission up
          WHERE up.user.userName = :userName
            AND (up.expirationDate IS NULL OR up.expirationDate > CURRENT_TIMESTAMP)
        )
        OR p.id IN (
          SELECT rp.permissionId
          FROM RolePermission rp
          JOIN UserRole ur ON rp.roleId = ur.roleId
          WHERE ur.user.userName = :userName
        )
      )
  """)
  List<Permission> findPermissionsByUserNameAndEntity(@Param("userName") String userName, @Param("entity") String entity);

}
