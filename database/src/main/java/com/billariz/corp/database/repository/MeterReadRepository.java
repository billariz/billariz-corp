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
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.MeterRead;
import com.billariz.corp.database.model.enumeration.MeterReadContext;
import com.billariz.corp.database.model.enumeration.MeterReadQuality;
import com.billariz.corp.database.model.enumeration.MeterReadSource;
import com.billariz.corp.database.model.enumeration.MeterReadStatus;
import com.billariz.corp.database.model.enumeration.MeterReadType;
import com.billariz.corp.database.projection.inMeterRead;

@RepositoryRestResource(excerptProjection = inMeterRead.class)
public interface MeterReadRepository extends RepositoryReadWriteDelete<MeterRead, Long>
{
        @Query(value = """
                        SELECT
                                CASE 
                                WHEN :granularity = 'DAILY' THEN DATE(e.readingDate)
                                WHEN :granularity = 'WEEKLY' THEN DATE_TRUNC('week', e.readingDate)
                                WHEN :granularity = 'MONTHLY' THEN DATE_TRUNC('month', e.readingDate)
                                WHEN :granularity = 'YEARLY' THEN DATE_TRUNC('year', e.readingDate)
                                END as period,
                                e.status,
                                COUNT(*) as total
                        FROM cc_meter_read e
                        WHERE e.readingDate BETWEEN :start AND :end
                        GROUP BY period, e.status
                        ORDER BY period, e.status
                        """, nativeQuery = true)
        List<Object[]> countMeterReadByStatus(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end,
                        @Param("granularity") String granularity
                        );
        
        @Query(value = """
                SELECT
                        CASE 
                        WHEN :granularity = 'DAILY' THEN DATE(e.readingDate)
                        WHEN :granularity = 'WEEKLY' THEN DATE_TRUNC('week', e.readingDate)
                        WHEN :granularity = 'MONTHLY' THEN DATE_TRUNC('month', e.readingDate)
                        WHEN :granularity = 'YEARLY' THEN DATE_TRUNC('year', e.readingDate)
                        END as period,
                        e.status,
                        SUM(e.totalQuantity) as total
                FROM cc_meter_read e
                WHERE e.readingDate BETWEEN :start AND :end
                GROUP BY period, e.status
                ORDER BY period, e.status
                """, nativeQuery = true)
        List<Object[]> countMeterReadQtByStatus(
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end,
                                @Param("granularity") String granularity
                                );

        @Query(value = """
                SELECT
                        CASE 
                        WHEN :granularity = 'DAILY' THEN DATE(e.readingDate)
                        WHEN :granularity = 'WEEKLY' THEN DATE_TRUNC('week', e.readingDate)
                        WHEN :granularity = 'MONTHLY' THEN DATE_TRUNC('month', e.readingDate)
                        WHEN :granularity = 'YEARLY' THEN DATE_TRUNC('year', e.readingDate)
                        END as period,
                        e.status,
                        SUM(e.totalQuantity) as total
                FROM cc_meter_read e
                WHERE   (e.readingDate BETWEEN :start AND :end) AND
                        (:posId IS NULL OR e.id=:posId) AND
                        (:contractId IS NULL OR e.id IN (SELECT cp.posId FROM cc_contract_pos cp WHERE cp.contractId=:contractId))
                GROUP BY period, e.status
                ORDER BY period, e.status
                """, nativeQuery = true)
        List<Object[]> countMeterReadQtByContractOrPos(
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end,
                                        @Param("granularity") String granularity,
                                        @Param("posId") Long posId,
                                        @Param("contractId") Long contractId
                                        );

        @Query("SELECT a.status FROM MeterRead a WHERE a.id=:id")
        MeterReadStatus findStatusById(Long id);

        MeterRead findMeterReadById(@Param("id") Long id);

        @Query("SELECT m FROM MeterRead m WHERE " +
                        "m.status=:status " +
                        "AND m.posRef IN (SELECT c.pointOfService.reference FROM ContractPointOfService c WHERE c.contractId=:contractId AND m.endDate > c.startDate AND (c.endDate IS NULL OR m.endDate <= c.endDate)) " +
                        "AND m.endDate >= :startDate AND m.endDate <= :endDate")
        List<MeterRead> findByStatusAndContractIdAndisWithinDateRange(@Param("status") MeterReadStatus status, @Param("contractId") Long contractId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
                                "FROM MeterRead m " +
                                "WHERE m.status = :status " +
                                "AND m.posRef IN (" +
                                "    SELECT c.pointOfService.reference " +
                                "    FROM ContractPointOfService c " +
                                "    WHERE c.contractId = :contractId " +
                                "    AND m.endDate > c.startDate " +
                                "    AND (c.endDate IS NULL OR m.endDate <= c.endDate)" +
                                ") " +
                                "AND m.endDate BETWEEN :startDate AND :endDate")
        boolean existsByStatusAndContractIdAndIsWithinDateRange(
                                @Param("status") MeterReadStatus status, 
                                @Param("contractId") Long contractId, 
                                @Param("startDate") LocalDate startDate, 
                                @Param("endDate") LocalDate endDate);

        @Query("SELECT m FROM MeterRead m WHERE m.status=:status "+
                        "AND m.posRef IN (SELECT c.pointOfService.reference FROM ContractPointOfService c WHERE c.contractId=:contractId AND m.startDate >= c.startDate AND (c.endDate IS NULL OR m.endDate <= c.endDate)) "+
                        "AND m.endDate <= :cutOffDate "+
                        "AND m.source IN (:source)")
        List<MeterRead> findWithStatusAndContractAndSource(@Param("status") MeterReadStatus status, 
                                                        @Param("contractId") Long contractId,
                                                        @Param("cutOffDate") LocalDate cutOffDate, 
                                                        @Param("source") List<MeterReadSource> source);

        @Query("SELECT m FROM MeterRead m " +
                                "WHERE (:id IS NULL OR m.id=:id) "+
                                "AND (:context IS NULL OR m.context=:context) "+
                                "AND (:type IS NULL OR m.type=:type) "+
                                "AND (:quality IS NULL OR m.quality=:quality) "+
                                "AND (:source IS NULL OR m.source=:source) "+
                                "AND (:status IS NULL OR m.status=:status) "+
                                "AND (:direction IS NULL OR m.direction=:direction) "+
                                "AND (:posRef IS NULL OR m.posRef=:posRef) "+
                                "AND (:contractId IS NULL OR m.posRef IN (SELECT cp.pointOfService.reference FROM ContractPointOfService cp WHERE cp.contractId=:contractId)) "+
                                "AND (:contractRef IS NULL OR m.posRef IN (SELECT cp.pointOfService.reference FROM ContractPointOfService cp WHERE cp.contract.reference=:contractRef)) "+
                                "AND (:posId IS NULL OR m.posRef IN (SELECT cp.pointOfService.reference FROM ContractPointOfService cp WHERE cp.posId=:posId)) "+
                                "AND (:startDate IS NULL OR m.startDate >= CAST(:startDate AS date)) " +
                                "AND (:endDate IS NULL OR m.endDate <= CAST(:endDate AS date)) ")
        Page<MeterRead> findMeterRead(Long id, 
                                        Long posId, 
                                        MeterReadContext context,
                                        MeterReadType type,
                                        MeterReadQuality quality,
                                        MeterReadSource source,
                                        MeterReadStatus status,
                                        String direction,
                                        Long contractId, 
                                        String posRef, 
                                        String contractRef, 
                                        String startDate, 
                                        String endDate, 
                                        Pageable pageable);

        @Query("""
                SELECT DISTINCT m
                FROM MeterRead m
                JOIN BillSegment b ON b.meterReadId = m.id
                WHERE b.billId = :billId
                """)
        List<MeterRead> findMeterReadByBillId(@Param("billId") Long billId);

        @Query("SELECT m FROM MeterRead m WHERE (m.status NOT IN :statusList) AND (m.posRef IN (SELECT cp.pointOfService.reference FROM ContractPointOfService cp WHERE cp.contract.id=:contractId)) AND (m.endDate =:endDate)")
        List<MeterRead> findContinueMeterRead(List<MeterReadStatus> statusList, Long contractId, LocalDate endDate);

        @Query("SELECT m FROM MeterRead m WHERE " +
                                "(m.status NOT IN :statusList) AND " +
                                "(m.posRef IN (SELECT cp.pointOfService.reference FROM ContractPointOfService cp WHERE cp.contract.id = :contractId)) AND " +
                                "(m.endDate BETWEEN :startDate AND :endDate)")
        List<MeterRead> findContinueMeterReadWithTolerance(@Param("statusList") List<MeterReadStatus> statusList,
                                                   @Param("contractId") Long contractId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

        @Query("SELECT m FROM MeterRead m WHERE (m.id <> :mrId) " +
                "AND (m.status NOT IN :statusList) " +
                "AND (m.type IN :typeList) " +
                "AND (m.posRef IN (SELECT cp.pointOfService.reference FROM ContractPointOfService cp WHERE cp.contract.id = :contractId)) " +
                "AND (m.startDate < :endDate AND m.endDate > :startDate)")
        List<MeterRead> findOverLapMeterRead(Long mrId, List<MeterReadStatus> statusList, List<MeterReadType> typeList, Long contractId, LocalDate startDate, LocalDate endDate);
        
        @Query("SELECT m FROM MeterRead m WHERE (m.id <> :mrId) " +
                                "AND (m.status NOT IN :statusList) " +
                                "AND (m.type IN :typeList) " +
                                "AND (m.posRef IN (SELECT cp.pointOfService.reference FROM ContractPointOfService cp WHERE cp.contract.id = :contractId)) " +
                                "AND (m.startDate < :endDateWithTolerance AND m.endDate > :startDateWithTolerance)")
        List<MeterRead> findOverLapMeterReadWithTolerance(
                                        @Param("mrId") Long mrId,
                                        @Param("statusList") List<MeterReadStatus> statusList,
                                        @Param("typeList") List<MeterReadType> typeList,
                                        @Param("contractId") Long contractId,
                                        @Param("startDateWithTolerance") LocalDate startDateWithTolerance,
                                        @Param("endDateWithTolerance") LocalDate endDateWithTolerance);
        
        @Modifying
        @Query("UPDATE MeterRead m SET m.status=:status WHERE m.id IN (:ids)")
        @RestResource(exported = false)
        int updateStatus(@Param("ids") Iterable<Long> ids, @Param("status") MeterReadStatus status);

        @Query("SELECT m FROM MeterRead m WHERE m.status IN (:status) " +
                        "AND m.posRef IN (SELECT c.pointOfService.reference FROM ContractPointOfService c WHERE c.contractId=:contractId AND m.startDate >= c.startDate AND (c.endDate IS NULL OR m.endDate <= c.endDate)) " +
                        "AND m.source IN (:source) " +
                        "AND (:touGroup IS NULL OR touGroup=:touGroup)")
        List<MeterRead> findWithStatusInAndContractAndSourceAndTouGroup(@Param("status") List<MeterReadStatus> status, @Param("contractId") Long contractId, @Param("source") List<MeterReadSource> source, @Param("touGroup") String touGroup);

        List<MeterRead> findAllByCancelledBy(Long cancelledBy);

        @Query("SELECT m FROM MeterRead m WHERE m.status IN (:status) " +
                        "AND m.posRef IN (SELECT c.pointOfService.reference FROM ContractPointOfService c " +
                       "WHERE c.contractId = :contractId " +
                       "AND m.startDate >= c.startDate " +
                       "AND (c.endDate IS NULL OR m.endDate <= c.endDate))")
        List<MeterRead> findAllByStatusInAndContractId(@Param("status") List<MeterReadStatus> status, 
                                               @Param("contractId") Long contractId);
}
