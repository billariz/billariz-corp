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
import org.springframework.data.rest.core.annotation.RestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.BillableCharge;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.BillableChargeContext;
import com.billariz.corp.database.model.enumeration.BillableChargeSource;
import com.billariz.corp.database.model.enumeration.BillableChargeStatus;
import com.billariz.corp.database.model.enumeration.BillableChargeTypeEnum;
import com.billariz.corp.database.projection.inBillableCharge;

@RepositoryRestResource(excerptProjection = inBillableCharge.class)
public interface BillableChargeRepository extends RepositoryReadWriteDelete<BillableCharge, Long>
{
    boolean existsByPosRefInAndStatus(Iterable<String> posRef, BillableChargeStatus status);

    List<BillableCharge> findAllByCancelledBy(Long cancelledBy);

    @Query("SELECT m FROM BillableCharge m WHERE " +
                                "(m.status NOT IN :statusList) AND " +
                                "(m.posRef IN (SELECT cp.pointOfService.reference FROM ContractPointOfService cp WHERE cp.contract.id = :contractId)) AND " +
                                "(m.endDate BETWEEN :startDate AND :endDate)")
    List<BillableCharge> findContinueBillableChargeWithTolerance(@Param("statusList") List<BillableChargeStatus> statusList,
                                                   @Param("contractId") Long contractId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    @Query("SELECT m FROM BillableCharge m WHERE (m.id <> :mrId) " +
                                "AND (m.status NOT IN :statusList) " +
                                "AND (m.type IN :typeList) " +
                                "AND (m.posRef IN (SELECT cp.pointOfService.reference FROM ContractPointOfService cp WHERE cp.contract.id = :contractId)) " +
                                "AND (m.startDate < :endDateWithTolerance AND m.endDate > :startDateWithTolerance)")
    List<BillableCharge> findOverLapBillableChargeWithTolerance(
                                        @Param("mrId") Long mrId,
                                        @Param("statusList") List<BillableChargeStatus> statusList,
                                        @Param("typeList") List<BillableChargeTypeEnum> typeList,
                                        @Param("contractId") Long contractId,
                                        @Param("startDateWithTolerance") LocalDate startDateWithTolerance,
                                        @Param("endDateWithTolerance") LocalDate endDateWithTolerance);
    
    @Query("SELECT m FROM BillableCharge m WHERE (m.id <> :mrId) " +
                "AND (m.status NOT IN :statusList) " +
                "AND (m.type IN :typeList) " +
                "AND (m.posRef IN (SELECT cp.pointOfService.reference FROM ContractPointOfService cp WHERE cp.contract.id = :contractId)) " +
                "AND (m.startDate < :endDate AND m.endDate > :startDate)")
    List<BillableCharge> findOverLapBillableCharge(Long mrId, List<BillableChargeStatus> statusList, 
                                                List<BillableChargeTypeEnum> typeList, 
                                                Long contractId, 
                                                LocalDate startDate, 
                                                LocalDate endDate);
      

    @Query("SELECT m FROM BillableCharge m "+
                "WHERE m.status=:status "+
                "AND m.posRef IN (SELECT c.pointOfService.reference FROM ContractPointOfService c WHERE c.contractId=:contractId AND m.startDate >= c.startDate AND (c.endDate IS NULL OR m.endDate <= c.endDate)) "+
                "AND m.source IN (:source) " +
                "AND m.endDate <=:cutOffDate " +
                "AND m.billableChargeType.category =:category"
                )
    List<BillableCharge> findWithStatusAndContractAndSourceAndCategory(
                @Param("status") BillableChargeStatus status, 
                @Param("contractId") Long contractId,
                @Param("source") List<BillableChargeSource> source,
                @Param("category") String category,
                @Param("cutOffDate") LocalDate cutOffDate
                );


    @Query("SELECT p FROM BillableCharge p "+
                "WHERE (:id IS NULL OR p.id=:id) "+
                "AND (:posRef IS NULL OR p.posRef=:posRef) "+
                "AND (:billableChargeType IS NULL OR p.billableChargeType.id=:billableChargeType) "+
                "AND (:externalInvoiceRef IS NULL OR p.externalInvoiceRef=:externalInvoiceRef) "+
                "AND (:externalId IS NULL OR p.externalId=:externalId) "+
                "AND (:status IS NULL OR p.status=:status) "+
                "AND (:startDate IS NULL OR p.startDate >= CAST(:startDate AS date)) " +
                "AND (:endDate IS NULL OR p.endDate <= CAST(:endDate AS date)) " +
                "AND (:externalInvoiceDate is null or p.externalInvoiceDate= CAST(:externalInvoiceDate AS date)) " +
                "AND (:receptionDate is null or p.receptionDate= CAST(:receptionDate AS date)) " +
                "AND (:type is null or p.type=:type) " +
                "AND (:direction is null or p.direction=:direction) " +
                "AND (:source is null or p.source=:source) " +
                "AND (:context is null or p.context=:context) " +
                "AND (:contractId IS NULL OR p.posRef IN (SELECT cp.pointOfService.reference FROM ContractPointOfService cp WHERE cp.contractId=:contractId))" +
                "AND (:contractRef IS NULL OR p.posRef IN (SELECT cp.pointOfService.reference FROM ContractPointOfService cp WHERE cp.contract.reference=:contractRef))" +
                "AND (:posId IS NULL OR p.posRef IN (SELECT cp.pointOfService.reference FROM ContractPointOfService cp WHERE cp.posId=:posId))")
    Page<BillableCharge> findBillableCharge(Long id,
                                                String billableChargeType,
                                                String externalInvoiceRef,
                                                String externalId,
                                                BillableChargeStatus status,
                                                String direction,
                                                String startDate,
                                                String endDate,
                                                String externalInvoiceDate,
                                                String receptionDate,
                                                Long contractId, 
                                                Long posId, 
                                                String posRef,
                                                String contractRef,
                                                BillableChargeTypeEnum type,
                                                BillableChargeSource source,
                                                BillableChargeContext context,
                                                Pageable pageable);

    @Modifying
    @Query("UPDATE BillableCharge m SET m.status=:status WHERE m.id IN (:ids)")
    @RestResource(exported = false)
    int updateStatus(@Param("ids") Iterable<Long> ids, @Param("status") BillableChargeStatus status);

    @Query("SELECT a.status FROM BillableCharge a WHERE a.id=:id")
    BillableChargeStatus findStatusById(Long id);

}