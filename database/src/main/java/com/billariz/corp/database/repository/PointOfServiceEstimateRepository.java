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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.PointOfServiceEstimate;
import com.billariz.corp.database.model.enumeration.PointOfServiceDataStatus;
import com.billariz.corp.database.projection.inPointOfServiceEstimate;

@RepositoryRestResource(excerptProjection = inPointOfServiceEstimate.class)
public interface PointOfServiceEstimateRepository extends RepositoryReadWriteDelete<PointOfServiceEstimate, Long>
{
        @Query("SELECT a.status FROM PointOfServiceEstimate a WHERE a.id=:id")
        PointOfServiceDataStatus findStatusById(Long id);

        List<PointOfServiceEstimate> findAllByPosId(Long posId);
        @Query("SELECT p FROM PointOfServiceEstimate p " +
        "LEFT JOIN ContractPointOfService cp ON p.posId = cp.posId " +
        "WHERE cp.contractId = :contractId " +
        "AND p.posId = :posId " +
        "AND p.status = :status " +
        "AND p.estimateType = :estimateType")
        Optional<PointOfServiceEstimate> findByContractIdAndPosIdAndStatusAndEstimateType(Long contractId, Long posId, String status, String estimateType);
    
        @Query("SELECT p FROM PointOfServiceEstimate p "+
                    "WHERE (:posId IS NULL OR p.posId=:posId) "+
                    "AND (:posRef IS NULL OR p.pointOfService.reference=:posRef) "+
                    "AND (:id IS NULL OR p.id=:id) "+
                    "AND (:tou IS NULL OR p.tou=:tou) "+
                    "AND (:source IS NULL OR p.source=:source) "+
                    "AND (:status IS NULL OR p.status=:status) "+
                    "AND (:startDate IS NULL OR p.startDate >= CAST(:startDate AS date)) " +
                    "AND (:endDate IS NULL OR p.endDate <= CAST(:endDate AS date)) " +
                    "AND (:contractId IS NULL OR p.posId IN (SELECT cp.posId FROM ContractPointOfService cp WHERE cp.contractId=:contractId))")
        Page<PointOfServiceEstimate> findPosEstimate(Long contractId, 
                                                Long posId, 
                                                String posRef,
                                                Long id,
                                                String tou,
                                                String source,
                                                PointOfServiceDataStatus status,
                                                String startDate,
                                                String endDate,  
                                                Pageable pageable);

        @Modifying
        @Query("UPDATE PointOfServiceEstimate p SET " +
                "p.endDate = :endDate, " +
                "p.status = :status " +
                "WHERE p.posId = :posId " +
                "AND (p.endDate IS NULL OR p.endDate > :endDate)")
        void closeEstimates(@Param("endDate") LocalDate endDate, 
                                @Param("posId") Long posId, 
                                @Param("status") PointOfServiceDataStatus status); 
}