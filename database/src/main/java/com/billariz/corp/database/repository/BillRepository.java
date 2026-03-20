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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.Bill;
import com.billariz.corp.database.model.enumeration.BillNature;
import com.billariz.corp.database.model.enumeration.BillStatusEnum;
import com.billariz.corp.database.model.enumeration.BillType;
import com.billariz.corp.database.projection.inBill;

@RepositoryRestResource(excerptProjection = inBill.class)
public interface BillRepository extends RepositoryReadWriteDelete<Bill, Long>
{

    @Query(value = """
                                SELECT
                                        CASE 
                                        WHEN :granularity = 'DAILY' THEN DATE(e.billDate)
                                        WHEN :granularity = 'WEEKLY' THEN DATE_TRUNC('week', e.billDate)
                                        WHEN :granularity = 'MONTHLY' THEN DATE_TRUNC('month', e.billDate)
                                        WHEN :granularity = 'YEARLY' THEN DATE_TRUNC('year', e.billDate)
                                        END as period,
                                        e.status,
                                        COUNT(*) as total
                                FROM bl_bill e
                                WHERE e.billDate BETWEEN :start AND :end
                                GROUP BY period, e.status
                                ORDER BY period, e.status
                                """, nativeQuery = true)
    List<Object[]> countBillByStatus(
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end,
                                @Param("granularity") String granularity
                                );

    @Query(value = """
                                    SELECT
                                            CASE 
                                            WHEN :granularity = 'DAILY' THEN DATE(e.billDate)
                                            WHEN :granularity = 'WEEKLY' THEN DATE_TRUNC('week', e.billDate)
                                            WHEN :granularity = 'MONTHLY' THEN DATE_TRUNC('month', e.billDate)
                                            WHEN :granularity = 'YEARLY' THEN DATE_TRUNC('year', e.billDate)
                                            END as period,
                                            e.status,
                                            SUM(e.totalAmount) as total
                                    FROM bl_bill e
                                    WHERE e.billDate BETWEEN :start AND :end
                                    GROUP BY period, e.status
                                    ORDER BY period, e.status
                                    """, nativeQuery = true)
    List<Object[]> countBillAmountByStatus(
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end,
                                    @Param("granularity") String granularity
                                    );

    @Query("SELECT a.status FROM Bill a WHERE a.id=:id")
    BillStatusEnum findStatusById(Long id);

    List<Bill> findAllByStatusAndContractId(BillStatusEnum billStatus, Long contractId);

    List<Bill> findAllByStatusAndPerimeterId(BillStatusEnum billStatus, Long perimeterId);

    @Modifying
    @Query("UPDATE Bill b SET b.reference=:reference WHERE b.id=:billId")
    void updateReferenceById(@Param("reference") String reference, @Param("billId") Long billId);

    @Modifying
    @Query("UPDATE Bill b SET b.groupBillId=:groupBillId WHERE b.id IN (:billIds)")
    void updateGroupBillId(@Param("groupBillId") Long groupBillId, @Param("billIds") List<Long> billIds);

    @Modifying
    @Query("UPDATE Bill b SET b.status=:status WHERE b.id=:billId")
    void updateStatusById(Long billId, BillStatusEnum status);

    List<Bill> findAllByGroupBillId(Long groupBillId);

    @Query("SELECT b FROM Bill b " +
       "WHERE (:billingRunId IS NULL OR b.billingRunId = :billingRunId) " +
       "AND (:id IS NULL OR b.id = :id) " +
       "AND (:billRef IS NULL OR b.reference = :billRef) " +
       "AND (:billType IS NULL OR b.type = :billType) " +
       "AND (:billNature IS NULL OR b.nature = :billNature) " +
       "AND (:billStatus IS NULL OR b.status = :billStatus) " +
       "AND (:billDate IS NULL OR b.billDate = CAST(:billDate AS date)) " +
       "AND (:accountingDate IS NULL OR b.accountingDate = CAST(:accountingDate AS date)) " +
       "AND (:startDate IS NULL OR b.startDate >= CAST(:startDate AS date)) " +
       "AND (:endDate IS NULL OR b.endDate <= CAST(:endDate AS date)) " +
       "AND (:runDate IS NULL OR b.billingRun.runDate <= CAST(:runDate AS date)) " +
       "AND (:cancelledBillId IS NULL OR b.cancelledBillId = :cancelledBillId) " +
       "AND (:groupBillId IS NULL OR b.groupBillId = :groupBillId) " +
       "AND (:contractId IS NULL OR b.contractId = :contractId) " +
       "AND (:perimeterId IS NULL OR b.perimeterId = :perimeterId) " +
       "AND (:customerId IS NULL OR b.customerId = :customerId) " +
       "AND (:customerRef IS NULL OR b.customer.reference = :customerRef) " +
       "AND (:perimeterRef IS NULL OR b.perimeter.reference = :perimeterRef) " +
       "AND (:contractRef IS NULL OR b.contract.reference = :contractRef) ")
    Page<Bill> findBill(@Param("id") Long id, 
                    @Param("contractId") Long contractId, 
                    @Param("customerId") Long customerId, 
                    @Param("perimeterId") Long perimeterId,
                    @Param("billingRunId") Long billingRunId, 
                    @Param("billRef") String billRef, 
                    @Param("startDate") String startDate, 
                    @Param("endDate") String endDate, 
                    @Param("billDate") String billDate,
                    @Param("accountingDate") String accountingDate,
                    @Param("runDate") String runDate, 
                    @Param("billType") BillType billType, 
                    @Param("billNature") BillNature billNature, 
                    @Param("billStatus") BillStatusEnum billStatus, 
                    @Param("customerRef") String customerRef, 
                    @Param("perimeterRef") String perimeterRef, 
                    @Param("contractRef") String contractRef, 
                    @Param("groupBillId") Long groupBillId, 
                    @Param("cancelledBillId") Long cancelledBillId, 
                    Pageable pageable);

    Bill findBillById(Long id);

    @Query("SELECT id FROM Bill b WHERE b.billingRunId=:billingRunId")
    Set<Long> findAllBillIdByBillingRunId(Long billingRunId);
}