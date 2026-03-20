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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.BillSegment;
import com.billariz.corp.database.model.enumeration.BillSegmentStatus;
import com.billariz.corp.database.model.enumeration.MeterReadQuality;
import com.billariz.corp.database.projection.inBillSegment;

@RepositoryRestResource(excerptProjection = inBillSegment.class)
public interface BillSegmentRepository extends RepositoryReadWriteDelete<BillSegment, Long>
{
    @Query(value = """
                                SELECT
                                        CASE 
                                        WHEN :granularity = 'DAILY' THEN DATE(e.endDate)
                                        WHEN :granularity = 'WEEKLY' THEN DATE_TRUNC('week', e.endDate)
                                        WHEN :granularity = 'MONTHLY' THEN DATE_TRUNC('month', e.endDate)
                                        WHEN :granularity = 'YEARLY' THEN DATE_TRUNC('year', e.endDate)
                                        END as period,
                                        e.status,
                                        SUM(e.amount) as total
                                FROM bl_bill_segment e
                                WHERE e.endDate BETWEEN :start AND :end
                                GROUP BY period, e.status
                                ORDER BY period, e.status
                                """, nativeQuery = true)
        List<Object[]> countBillSegmentByStatus(
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end,
                                @Param("granularity") String granularity
                                );

    @Query("SELECT a.status FROM BillSegment a WHERE a.id=:id")
    BillSegmentStatus findStatusById(Long id);

    Optional<BillSegment> findFirstBySeIdAndStatusNotInOrderByEndDateDesc(Long seId, List<BillSegmentStatus> bsStatusList);

    List<BillSegment> findAllByBillId(Long billId);

    Optional<BillSegment> findFirstBySeIdOrderByEndDateAscStartDateDesc(Long seId);

    List<BillSegment> findAllByMeterReadIdIn(List<Long> meterReadId);

    @Query("SELECT b FROM BillSegment b JOIN FETCH b.article s WHERE s.billableChargeId IN (:billableChargeId)")
    List<BillSegment> findAllByBillableChargeIdIn(List<Long> billableChargeId);

    @Query("SELECT b FROM BillSegment b JOIN FETCH b.se s WHERE s.status IN (:seStatus) AND s.tosId IN(:tosId)")
    List<BillSegment> findWithStatusInAndTosIdIn(@Param("seStatus") List<BillSegmentStatus> seStatus, @Param("tosId") List<Long> tosId);

    @Query("SELECT b FROM BillSegment b JOIN FETCH b.se s WHERE s.tosId IN(:tosId) AND b.status=:billSegmentStatus")
    List<BillSegment> findByTosIdInAndStatus(@Param("tosId") List<Long> tosId, @Param("billSegmentStatus") BillSegmentStatus billSegmentStatus);

    @Query("SELECT sum(b.amount) FROM BillSegment b WHERE b.seId IN (SELECT s.id FROM ServiceElement s WHERE s.seType.masterSeTypeId=:masterSeTypeId AND s.tos.contractId=:contractId AND s.subCategory IN (:subCategories))")
    BigDecimal computeSumOfBillSegmentsBySeMasterBySubCategory(@Param("masterSeTypeId") String masterSeTypeId, @Param("contractId") Long contractId, @Param("subCategories") List<String> subCategories);

    @Query("SELECT COUNT(b.id) FROM BillSegment b WHERE b.status =:bsStatus AND b.se.tosId IN(:tosId)")
    long countingByStatusAndTosIdIn(@Param("bsStatus") BillSegmentStatus bsStatus, @Param("tosId") List<Long> tosId);

    @Modifying
    @Query("UPDATE BillSegment b SET b.status =:bsStatus, b.billId=:billId WHERE b.id IN (:ids)")
    void updateBillSegmentStatusAndBillIdById(@Param("bsStatus") BillSegmentStatus bsStatus, @Param("billId") Long billId, @Param("ids") List<Long> ids);

    @Query("SELECT b FROM BillSegment b WHERE (:id IS NULL OR b.id=:id) "+
                        "AND (:contractId IS NULL OR b.se.tos.contract.id=:contractId) "+
                        "AND (:tou IS NULL OR b.tou=:tou)" +
                        "AND (:touGroup IS NULL OR b.touGroup=:touGroup)" +
                        "AND (:status IS NULL OR b.status=:status) "+
                        "AND (:seId IS NULL OR b.seId=:seId) "+
                        "AND (:tosId IS NULL OR b.se.tos.id=:tosId) "+
                        "AND (:billId IS NULL OR b.billId=:billId) " +
                        "AND (:articleId IS NULL OR b.articleId=:articleId) " +
                        "AND (:meterReadId IS NULL OR b.meterReadId=:meterReadId) " +
                        "AND (:nature IS NULL OR b.nature=:nature) " +
                        "AND (:vatRate IS NULL OR b.vatRate=:vatRate) " +
                        "AND (:schema IS NULL OR b.schema=:schema) " +
                        "AND (:category IS NULL OR b.se.category=:category) " +
                        "AND (:subCategory IS NULL OR b.se.subCategory=:subCategory) " +
                        "AND (:startDate IS NULL OR b.startDate >= CAST(:startDate AS date)) " +
                        "AND (:endDate IS NULL OR b.endDate <= CAST(:endDate AS date))")
    Page<BillSegment> findBillSegment(@Param("id") Long id, 
                                    @Param("contractId") Long contractId, 
                                    @Param("status") BillSegmentStatus status, 
                                    @Param("seId") Long seId, 
                                    @Param("tosId") Long tosId, 
                                    @Param("billId") Long billId, 
                                    @Param("articleId") Long articleId, 
                                    @Param("meterReadId") Long meterReadId,
                                    @Param("tou") String tou, 
                                    @Param("touGroup") String touGroup,
                                    @Param("startDate") String startDate, 
                                    @Param("endDate") String endDate,
                                    @Param("nature") MeterReadQuality nature,
                                    @Param("vatRate") String vatRate,
                                    @Param("schema") String schema,
                                    @Param("category") String category,
                                    @Param("subCategory") String subCategory,
                                    Pageable pageable);

     @Query("SELECT b FROM BillSegment b WHERE b.status IN (:seStatus) AND b.se.tosId=:tosId AND b.endDate > :endDate")
    List<BillSegment> findByStatusInAndTosIdAndEndDateIsAfter(@Param("seStatus") List<BillSegmentStatus> seStatus, @Param("tosId") Long tosId, LocalDate endDate);

}   