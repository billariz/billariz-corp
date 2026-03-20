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
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.data.RepositoryReadWriteFull;
import com.billariz.corp.database.model.PointOfService;
import com.billariz.corp.database.projection.inPointOfService;

@RepositoryRestResource(excerptProjection = inPointOfService.class)
public interface PointOfServiceRepository extends RepositoryReadWriteDelete<PointOfService, Long>
{

    @Query("SELECT p FROM PointOfService p WHERE (:id IS NULL OR p.id=:id) " +
        "AND (:reference IS NULL OR lower(p.reference)=lower(:reference))" +
        "AND (:tgoCode IS NULL OR p.tgoCode=:tgoCode) " +
        "AND (:dgoCode IS NULL OR p.dgoCode=:dgoCode) " +
        "AND (:direction IS NULL OR p.direction=:direction) " +
        "AND (:deliveryStatus IS NULL OR p.deliveryStatus=:deliveryStatus)  " +
        "AND (:deliveryState IS NULL OR p.deliveryState=:deliveryState)  " +
        "AND (:contractId IS NULL OR p.id IN (SELECT cp.posId FROM ContractPointOfService cp WHERE cp.contractId=:contractId))  " +
        "AND (:contractRef IS NULL OR p.id IN (SELECT cp.posId FROM ContractPointOfService cp WHERE lower(cp.contract.reference)=lower(:contractRef)))  " +
        "AND (:readingCycle IS NULL OR p.readingCycle.id=:readingCycle) "+
        "AND (:market IS NULL OR p.market=:market) "+
        "AND (:temporaryConnection IS NULL OR p.temporaryConnection=:temporaryConnection)  " +
        "AND (:temporaryConnectionType IS NULL OR p.temporaryConnectionType=:temporaryConnectionType)")
    Page<PointOfService> findPointOfService(@Param("id") Long id, 
                                            @Param("reference") String reference, 
                                            @Param("tgoCode") String tgoCode, 
                                            @Param("dgoCode") String dgoCode, 
                                            @Param("direction") String direction, 
                                            @Param("deliveryState") String deliveryState, 
                                            @Param("deliveryStatus") String deliveryStatus, 
                                            @Param("contractId") Long contractId, 
                                            @Param("contractRef") String contractRef,
                                            @Param("readingCycle") String readingCycle, 
                                            @Param("market") String market, 
                                            @Param("temporaryConnection") Boolean temporaryConnection, 
                                            @Param("temporaryConnectionType") String temporaryConnectionType, 
                                            Pageable pageable);

}
