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
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.projection.inContract;

@RepositoryRestResource(excerptProjection = inContract.class)
public interface ContractRepository extends RepositoryReadWriteDelete<Contract, Long>, JpaSpecificationExecutor<Contract>
{
        @Query("SELECT a.status FROM Contract a WHERE a.id=:id")
        String findStatusById(Long id);

        @Query("SELECT c.id FROM Contract c WHERE c.reference=:reference")
        Optional<Long> getIdByReference(@Param("reference") String reference);

        @Query("SELECT c FROM Contract c WHERE c.reference=:reference")
        Optional<Contract> findContractByReference(@Param("reference") String reference);

        @Query("SELECT c FROM Contract c WHERE c.status IN (:contractStatus) "+
                        "AND c.billingCycle.id=:billingCycle AND "+
                        "c.id NOT IN (SELECT r.secondObjectId FROM Relation r WHERE r.relationType=:relationType AND r.firstObjectId=:runId)")
        List<Contract> findWithStatusInAndBillingCycle(@Param("contractStatus") List<String> contractStatus, 
                                                        @Param("billingCycle") String billingCycle, 
                                                        @Param("relationType") String relationType, 
                                                        @Param("runId") Long runId);

        @Query("SELECT c FROM Contract c "+
                        "WHERE c.status IN (:contractStatus) "+
                        "AND c.contractualEndDate = :billingRunStartDate "+
                        "AND c.id NOT IN (SELECT r.secondObjectId FROM Relation r WHERE r.relationType=:relationType AND r.firstObjectId=:runId)")
        List<Contract> findWithStatusInAndEndDate(@Param("contractStatus") List<String> contractStatus, @Param("billingRunStartDate") LocalDate billingRunStartDate, @Param("relationType") String relationType, @Param("runId") Long runId);

        @Query("SELECT c FROM Contract c "+
        "WHERE c.status IN (:contractStatus) "+
        "AND c.contractualEndDate < :pivotDate "+
        "AND c.id NOT IN (SELECT r.secondObjectId FROM Relation r WHERE r.relationType=:relationType)")
        List<Contract> findAllByStatusInAndEndDateAndBillinRunType(@Param("contractStatus") List<String> contractStatus, @Param("pivotDate") LocalDate pivotDate, @Param("relationType") String relationType);


        @Query("SELECT DISTINCT c FROM Contract c " +
                                "LEFT JOIN ContractPointOfService cp ON c.id = cp.contractId " +
                                "LEFT JOIN ContractPerimeter cpr ON c.id = cpr.contractId " +
                                "LEFT JOIN Perimeter p ON cpr.perimeterId = p.id " +
                                "LEFT JOIN Customer cust ON p.customerId = cust.id " +
                                "WHERE (:billingCycle IS NULL OR c.billingCycle.id = :billingCycle) " +
                                "AND (:posId IS NULL OR cp.posId = :posId) " +
                                "AND (:id IS NULL OR c.id = :id) " +
                                "AND (:market IS NULL OR c.market = :market) " +
                                "AND (:status IS NULL OR c.status = :status) " +
                                "AND (:direction IS NULL OR c.direction = :direction) " +
                                "AND (:billingMode IS NULL OR c.billingMode = :billingMode) " +
                                "AND (:billingFrequency IS NULL OR c.billingFrequency = :billingFrequency) " +
                                "AND (:contractRef IS NULL OR LOWER(c.reference) = LOWER(:contractRef)) " +
                                "AND (:serviceCategory IS NULL OR c.serviceCategory = :serviceCategory) " +
                                "AND (:serviceSubCategory IS NULL OR c.serviceSubCategory = :serviceSubCategory) " +
                                "AND (:perimeterId IS NULL OR cpr.perimeterId = :perimeterId) " +
                                "AND (:perimeterRef IS NULL OR LOWER(p.reference) = LOWER(:perimeterRef)) " +
                                "AND (:customerRef IS NULL OR LOWER(cust.reference) = LOWER(:customerRef)) " +
                                "AND (:customerId IS NULL OR cust.id = :customerId)")
                        Page<Contract> findContract(
                        @Param("id") Long id, 
                        @Param("customerId") Long customerId, 
                        @Param("billingCycle") String billingCycle, 
                        @Param("customerRef") String customerRef, 
                        @Param("contractRef") String contractRef, 
                        @Param("perimeterId") Long perimeterId,
                        @Param("perimeterRef") String perimeterRef, 
                        @Param("market") String market, 
                        @Param("status") String status, 
                        @Param("direction") String direction, 
                        @Param("billingMode") String billingMode, 
                        @Param("billingFrequency") String billingFrequency, 
                        @Param("posId") Long posId, 
                        @Param("serviceCategory") String serviceCategory,
                        @Param("serviceSubCategory") String serviceSubCategory,
                        Pageable pageable);

}
