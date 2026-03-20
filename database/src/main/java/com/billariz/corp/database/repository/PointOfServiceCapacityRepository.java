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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.PointOfServiceCapacity;
import com.billariz.corp.database.model.enumeration.PointOfServiceDataStatus;
import com.billariz.corp.database.projection.inPointOfServiceCapacity;

@RepositoryRestResource(excerptProjection = inPointOfServiceCapacity.class)
public interface PointOfServiceCapacityRepository extends RepositoryReadWriteDelete<PointOfServiceCapacity, Long>
{
    
    @Query("SELECT a.status FROM PointOfServiceCapacity a WHERE a.id=:id")
    PointOfServiceDataStatus findStatusById(Long id);

    @Query("SELECT p FROM PointOfServiceCapacity p WHERE p.posId IN (SELECT c.posId FROM ContractPointOfService c WHERE c.contractId=:contractId AND c.endDate is null ORDER BY c.startDate) AND p.status=:status AND p.endDate is null")
    List<PointOfServiceCapacity> findWithContractIdAndStatus(@Param("contractId") Long contractId, @Param("status") PointOfServiceDataStatus status);

    @Query("SELECT p FROM PointOfServiceCapacity p " +
        "LEFT JOIN ContractPointOfService cp ON p.posId = cp.posId " +
        "WHERE cp.contractId = :contractId " +
        "AND p.status IN (:status) ")
    List<PointOfServiceCapacity> findAllByContractIdAndStatusIn(Long contractId, List<PointOfServiceDataStatus> status);

    @Query("SELECT p FROM PointOfServiceCapacity p "+
                "LEFT JOIN ContractPointOfService cp ON p.posId = cp.posId " +
                "WHERE (:id IS NULL OR p.id=:id) "+
                "AND (:tou IS NULL OR p.tou=:tou) "+
                "AND (:source IS NULL OR p.source=:source) "+
                "AND (:status IS NULL OR p.status=:status) "+
                "AND (:startDate IS NULL OR p.startDate >= CAST(:startDate AS date)) " +
                "AND (:endDate IS NULL OR p.endDate <= CAST(:endDate AS date)) " +
                "AND (:posId IS NULL OR p.posId=:posId) "+
                "AND (:posRef IS NULL OR p.pointOfService.reference=:posRef) "+
                "AND (:contractId IS NULL OR cp.contractId = :contractId)")
    Page<PointOfServiceCapacity> findPosCapacity(Long id,
                                                String tou,
                                                String source,
                                                PointOfServiceDataStatus status,
                                                String startDate,
                                                String endDate,
                                                Long contractId, 
                                                Long posId,
                                                String posRef,
                                                Pageable pageable);

    @Modifying
    @Query("UPDATE PointOfServiceCapacity p SET " +
            "p.endDate = :endDate, " +
            "p.status = :status " +
            "WHERE p.posId = :posId " +
            "AND (p.endDate IS NULL OR p.endDate > :endDate)")
    void closeCapacities(@Param("endDate") LocalDate endDate, @Param("posId") Long posId, @Param("status") PointOfServiceDataStatus status);
                                            
}