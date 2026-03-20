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
import com.billariz.corp.database.model.PointOfServiceConfiguration;
import com.billariz.corp.database.model.enumeration.PointOfServiceDataStatus;
import com.billariz.corp.database.projection.inPointOfServiceConfiguration;

@RepositoryRestResource(excerptProjection = inPointOfServiceConfiguration.class)
public interface PointOfServiceConfigurationRepository extends RepositoryReadWriteDelete<PointOfServiceConfiguration, Long>
{
    @Query("SELECT a.status FROM PointOfServiceConfiguration a WHERE a.id=:id")
    PointOfServiceDataStatus findStatusById(Long id);

    @Query("SELECT p FROM PointOfServiceConfiguration p " +
        "LEFT JOIN ContractPointOfService cp ON p.posId = cp.posId " +
        "WHERE cp.contractId = :contractId " +
        "AND p.endDate IS NULL ORDER BY p.startDate DESC ")
    Optional<PointOfServiceConfiguration> findFirstByContractIdAndEndDateIsNullOrderByStartDateDesc(Long contractId);

    @Query("SELECT p FROM PointOfServiceConfiguration p " +
        "LEFT JOIN ContractPointOfService cp ON p.posId = cp.posId " +
        "WHERE cp.contractId = :contractId " +
        "AND p.status IN (:status) ")
    List<PointOfServiceConfiguration> findAllByContractIdAndStatusIn(Long contractId, List<PointOfServiceDataStatus> status);

    @Query("SELECT p FROM PointOfServiceConfiguration p "+
                "WHERE (:posId IS NULL OR p.posId=:posId) "+
                "AND (:posRef IS NULL OR p.pointOfService.reference=:posRef) "+
                "AND (:id IS NULL OR p.id=:id) "+
                "AND (:touGroup IS NULL OR p.touGroup=:touGroup) "+
                "AND (:source IS NULL OR p.source=:source) "+
                "AND (:posCategory IS NULL OR p.posCategory=:posCategory) "+
                "AND (:profile IS NULL OR p.profile=:profile) "+
                "AND (:readingFrequency IS NULL OR p.readingFrequency=:readingFrequency) "+
                "AND (:gridRate IS NULL OR p.gridRate=:gridRate) "+
                "AND (:status IS NULL OR p.status=:status) "+
                "AND (:startDate IS NULL OR p.startDate >= CAST(:startDate AS date)) " +
                "AND (:endDate IS NULL OR p.endDate <= CAST(:endDate AS date)) " +
                "AND (:contractId IS NULL OR p.posId IN (SELECT cp.posId FROM ContractPointOfService cp WHERE cp.contractId=:contractId))")
    Page<PointOfServiceConfiguration> findPosConfiguration(Long contractId, 
                                                            Long posId, 
                                                            String posRef,
                                                            Long id,
                                                            String touGroup,
                                                            String source,
                                                            PointOfServiceDataStatus status,
                                                            String startDate,
                                                            String endDate,
                                                            String posCategory,
                                                            String profile,
                                                            String gridRate,
                                                            String readingFrequency,
                                                            Pageable pageable);

    @Modifying
    @Query("UPDATE PointOfServiceConfiguration p SET " +
            "p.endDate = :endDate, " +
            "p.status = :status " +
            "WHERE p.posId = :posId " +
            "AND (p.endDate IS NULL OR p.endDate > :endDate)")
    void closeConfigurations(@Param("endDate") LocalDate endDate, @Param("posId") Long posId, @Param("status") PointOfServiceDataStatus status);
     
}