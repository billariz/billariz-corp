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
import com.billariz.corp.database.model.Group;
import com.billariz.corp.database.projection.inGroup;

@RepositoryRestResource(excerptProjection = inGroup.class)
public interface GroupRepository extends RepositoryReadWriteDelete<Group, Long>
{
    boolean existsByGroupNameAndOrganismId(String groupName, Long organismId);

    Optional<Group> findFirstByGroupNameAndOrganismId(String groupName, Long organismId);

    @Query("SELECT s FROM Group s WHERE (:id IS NULL OR s.id=:id) " +
           "AND (:groupName IS NULL OR lower(s.groupName) LIKE lower(concat('%', :groupName, '%'))) " +
           "AND (:category IS NULL OR s.category = :category) " +
           "AND (:subCategory IS NULL OR s.subCategory = :subCategory) " +
           "AND (:organismId IS NULL OR s.organism.id = :organismId) " +
           "AND (:companyName IS NULL OR lower(s.organism.company.companyName) LIKE lower(concat('%', :companyName, '%'))) "
           )
    Page<Group> findGroup(@Param("id") Long id, @Param("groupName") String groupName, @Param("category") String category,
    @Param("subCategory") String subCategory, @Param("organismId") Long organismId, @Param("companyName") String companyName, Pageable pageable);
}
