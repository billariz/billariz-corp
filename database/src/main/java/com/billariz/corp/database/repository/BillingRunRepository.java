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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.BillingRun;
import com.billariz.corp.database.model.BillingWindow;
import com.billariz.corp.database.model.enumeration.BillType;
import com.billariz.corp.database.projection.inBillingRun;

@RepositoryRestResource(excerptProjection = inBillingRun.class)
public interface BillingRunRepository extends RepositoryReadWriteDelete<BillingRun, Long>
{

    List<BillingRun> findAllByBillingWindowAndStartDate(BillingWindow billingWindow, LocalDate startDate);

    BillingRun findFirstByStatusAndBillTypeAndStartDate(String Status, BillType billType, LocalDate startDate);

    List<BillingRun> findAllByStatusAndEndDateIsBefore(String status, LocalDate date);

    @Query("SELECT a FROM BillingRun a WHERE (:id IS NULL OR a.id=:id) "+
                "AND (:billType IS NULL OR a.billType=:billType) " +
                "AND (:status IS NULL OR a.status=:status) " +
                "AND (:billingCycle IS NULL OR a.billingCycle.id=:billingCycle)" +
                "AND (:startDate IS NULL OR a.startDate >= CAST(:startDate AS date)) " +
                "AND (:endDate is null or a.endDate<= CAST(:endDate AS date))")
    Page<BillingRun> findBillingRun(@Param("id") Long id, 
                                    @Param("billType") BillType billType,
                                    @Param("status") String status, 
                                    @Param("billingCycle") String billingCycle, 
                                    @Param("startDate") String startDate, 
                                    @Param("endDate") String endDate, 
                                    Pageable pageable);
    
    @Query("SELECT a.status FROM BillingRun a WHERE a.id=:id")
    String findStatusById(Long id);
}
