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
import com.billariz.corp.database.model.Meter;
import com.billariz.corp.database.projection.inMeter;

@RepositoryRestResource(excerptProjection = inMeter.class)
public interface MeterRepository extends RepositoryReadWriteDelete<Meter, Long>
{
    @Query("SELECT p FROM Meter p "+
                    "WHERE (:posId IS NULL OR p.posId=:posId) "+
                    "AND (:posRef IS NULL OR p.pointOfService.reference=:posRef) "+
                    "AND (:id IS NULL OR p.id=:id) "+
                    "AND (:meterNumber IS NULL OR p.meterNumber=:meterNumber) "+
                    "AND (:smartMeterStatus IS NULL OR p.smartMeterStatus=:smartMeterStatus) "+
                    "AND (:meterType IS NULL OR p.meterType=:meterType) "+
                    "AND (:startDate IS NULL OR p.startDate >= CAST(:startDate AS date)) " +
                    "AND (:endDate IS NULL OR p.endDate <= CAST(:endDate AS date)) " +
                    "AND (:contractId IS NULL OR p.posId IN (SELECT cp.posId FROM ContractPointOfService cp WHERE cp.contractId=:contractId))")
    Page<Meter> findMeter(Long contractId, 
                            Long id,
                            Long posId, 
                            String posRef,
                            String meterNumber,
                            String smartMeterStatus,
                            String meterType,
                            String startDate,
                            String endDate,
                            Pageable pageable);

}
