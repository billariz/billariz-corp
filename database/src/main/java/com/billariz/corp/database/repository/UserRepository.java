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

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.User;
import com.billariz.corp.database.projection.inUser;

@RepositoryRestResource(excerptProjection = inUser.class)
public interface UserRepository extends RepositoryReadWriteDelete<User, Long>
{
   @Query("SELECT a.status FROM User a WHERE a.id=:id")
   String findStatusById(Long id);

   Optional<User> findByUserName(String userName);

    @Query("SELECT u FROM User u " +
       "LEFT JOIN u.individual i " + 
       "WHERE (:id IS NULL OR u.id = :id) " +
       "AND (:userName IS NULL OR lower(u.userName) = lower(:userName)) " +
       "AND (:lastName IS NULL OR (i IS NOT NULL AND lower(i.lastName) = lower(:lastName))) " +
       "AND (:firstName IS NULL OR (i IS NOT NULL AND lower(i.firstName) = lower(:firstName))) " +
       "AND (:groupId IS NULL OR u.groupId = :groupId) " +
       "AND (:organismId IS NULL OR u.organismId = :organismId) " +
       "AND (:userRole IS NULL OR u.userRole = :userRole) " +
       "AND (:status IS NULL OR u.status = :status)")
    Page<User> findUser(@Param("id") Long id, 
                    @Param("firstName") String firstName, 
                    @Param("lastName") String lastName, 
                    @Param("userName") String userName, 
                    @Param("status") String status, 
                    @Param("userRole") String userRole, 
                    @Param("groupId") Long groupId, 
                    @Param("organismId") Long organismId, 
                    Pageable pageable);
}
