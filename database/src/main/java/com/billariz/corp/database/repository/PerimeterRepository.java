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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.Perimeter;
import com.billariz.corp.database.projection.inPerimeter;

@RepositoryRestResource(excerptProjection = inPerimeter.class)
public interface PerimeterRepository extends RepositoryReadWriteDelete<Perimeter, Long>
{
        @Query("SELECT a.status FROM Perimeter a WHERE a.id=:id")
        String findStatusById(Long id);

    Optional<Perimeter> findByIdAndCustomerId(Long id, long customerId);

    @Query("SELECT p FROM Perimeter p WHERE p.billingCycle.id=:billingCycle AND (p.endDate IS NULL OR p.endDate > :billingRunStartDate) AND p.id NOT IN (SELECT r.secondObjectId FROM Relation r WHERE r.relationType=:relationType AND r.firstObjectId=:runId)")
    List<Perimeter> findWithStatusInAndBillingCycleAndBillingRun(@Param("billingCycle") String billingCycle, @Param("relationType") String relationType, @Param("runId") Long runId, @Param("billingRunStartDate") LocalDate billingRunStartDate);

    @Query("SELECT p FROM Perimeter p " +
            "WHERE (:billingCycle IS NULL OR p.billingCycle.id=:billingCycle) " +
            "AND (:contractId IS NULL OR p.id IN (SELECT cp.perimeterId FROM ContractPerimeter cp WHERE cp.contract.id=:contractId)) " +
            "AND (:perimeterId IS NULL OR p.id=:perimeterId) " +
            "AND (:customerId IS NULL or p.customer.id=:customerId) " +
            "AND (:billingFrequency IS NULL OR p.billingFrequency=:billingFrequency) " +
            "AND (:customerRef IS NULL or lower(p.customer.reference)=lower(:customerRef)) " +
            "AND (:contractRef IS NULL OR p.id IN (SELECT cp.perimeterId FROM ContractPerimeter cp WHERE lower(cp.contract.reference)=lower(:contractRef))) " +
            "AND (:market IS NULL OR p.market=:market) " +
            "AND (:perimeterType IS NULL OR p.perimeterType=:perimeterType) " +
            "AND (:status IS NULL OR p.status=:status) " +
            "AND (:analyticCode IS NULL OR p.analyticCode=:analyticCode) " +
            "AND (:reference IS NULL OR lower(p.reference)=lower(:reference))")
    Page<Perimeter> findPerimeter(@Param("billingFrequency") String billingFrequency, 
                                    @Param("billingCycle") String billingCycle, 
                                    @Param("customerId") Long customerId, 
                                    @Param("contractId") Long contractId, 
                                    @Param("customerRef") String customerRef, 
                                    @Param("contractRef") String contractRef, 
                                    @Param("perimeterId") Long perimeterId, 
                                    @Param("market") String market, 
                                    @Param("status") String status, 
                                    @Param("perimeterType") String perimeterType, 
                                    @Param("reference") String reference,
                                    @Param("analyticCode") String analyticCode,
                                    Pageable pageable);

}
