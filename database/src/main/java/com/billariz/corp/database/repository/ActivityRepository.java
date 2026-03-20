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
import com.billariz.corp.database.model.Activity;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.projection.inActivity;

@RepositoryRestResource(excerptProjection = inActivity.class)
public interface ActivityRepository extends RepositoryReadWriteDelete<Activity, Long>
{
    @Query("SELECT DISTINCT a FROM Activity a " +
       "LEFT JOIN Relation r ON r.firstObjectId = a.id " +
       "LEFT JOIN Contract c ON r.secondObjectId = c.id " +
       "LEFT JOIN Customer cu ON r.secondObjectId = cu.id " +
       "LEFT JOIN Perimeter p ON r.secondObjectId = p.id " +
       "WHERE (:id IS NULL OR a.id = :id) " +
       "AND (:type IS NULL OR a.type = :type) " +
       "AND (:relationType IS NULL OR :relationType = r.relationType) " +
       "AND (:objectId IS NULL OR r.secondObjectId = :objectId) " +
       "AND (:objectRef IS NULL OR (c.reference = :objectRef OR cu.reference = :objectRef OR p.reference = :objectRef)) " +
       "AND (:status IS NULL OR a.status = :status) " +
       "AND (:category IS NULL OR a.category = :category) " +
       "AND (:startDate IS NULL OR a.startDate >= CAST(:startDate AS date)) " +
       "AND (:endDate IS NULL OR a.endDate <= CAST(:endDate AS date)) " +
       "AND (:subCategory IS NULL OR a.subCategory = :subCategory)")
    Page<Activity> findActivity(@Param("id") Long id, 
                            @Param("type") String type, 
                            @Param("status") ActivityStatus status, 
                            @Param("relationType") String relationType, 
                            @Param("objectId") Long objectId,
                            @Param("objectRef") String objectRef,  
                            @Param("category") String category, 
                            @Param("subCategory") String subCategory,
                            @Param("startDate") String startDate,
                            @Param("endDate") String endDate,
                            Pageable pageable);
    
    @Query("SELECT a FROM Activity a "+
                             "WHERE (:type IS NULL OR a.type=:type) " +
                             "AND (:status IS NULL OR a.status=:status) " +
                             "AND (:relationType IS NULL OR :relationType IN (SELECT relationType FROM Relation WHERE firstObjectId=a.id))" +
                             "AND (:objectId IS NULL OR a.id IN (SELECT firstObjectId FROM Relation WHERE relationType=:relationType AND secondObjectId=:objectId)) "
                             )
    List<Activity> findAllByTypeAndRelationTypeAndObjectIdAndStatus(
                            @Param("type") String type, 
                            @Param("status") ActivityStatus status, 
                            @Param("relationType") String relationType, 
                            @Param("objectId") Long objectId);

   @Query("SELECT a.status FROM Activity a WHERE a.id=:id")
   ActivityStatus findStatusById(Long id);
}
