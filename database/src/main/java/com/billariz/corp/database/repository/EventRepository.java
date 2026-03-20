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
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.EventTemplate;
import com.billariz.corp.database.model.LauncherTagType;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.projection.inEvent;

@RepositoryRestResource(excerptProjection = inEvent.class)
@Transactional
public interface EventRepository extends CrudRepository<Event, Long>
{
        @Query("SELECT e.status, COUNT(e) FROM Event e GROUP BY e.status")
        List<Object[]> countByStatusGrouped();

        @Query(value = """
                SELECT
                        CASE 
                        WHEN :granularity = 'DAILY' THEN DATE(e.creationDate)
                        WHEN :granularity = 'WEEKLY' THEN DATE_TRUNC('week', e.creationDate)
                        WHEN :granularity = 'MONTHLY' THEN DATE_TRUNC('month', e.creationDate)
                        WHEN :granularity = 'YEARLY' THEN DATE_TRUNC('year', e.creationDate)
                        END as period,
                        e.status,
                        COUNT(*) as total
                FROM cc_event e
                WHERE e.creationDate BETWEEN :start AND :end
                GROUP BY period, e.status
                ORDER BY period, e.status
                """, nativeQuery = true)
        List<Object[]> countEventsByGranularity(
                @Param("start") LocalDateTime start,
                @Param("end") LocalDateTime end,
                @Param("granularity") String granularity
                );
        
        @Query(value = """
                        SELECT
                                CASE 
                                WHEN :granularity = 'DAILY' THEN DATE(e.creationDate)
                                WHEN :granularity = 'WEEKLY' THEN DATE_TRUNC('week', e.creationDate)
                                WHEN :granularity = 'MONTHLY' THEN DATE_TRUNC('month', e.creationDate)
                                WHEN :granularity = 'YEARLY' THEN DATE_TRUNC('year', e.creationDate)
                                END as period,
                                e.eventType,
                                COUNT(*) as total
                        FROM cc_event e
                        WHERE e.creationDate BETWEEN :start AND :end
                        GROUP BY period, e.eventType
                        ORDER BY period, e.eventType
                        """, nativeQuery = true)
        List<Object[]> countEventsByType(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end,
                        @Param("granularity") String granularity
                        );
        
        @Query("SELECT a.status FROM Event a WHERE a.id=:id")
        EventStatus findStatusById(Long id);

        List<Event> findAllByActivityId(Long activityId);

        @Query("SELECT e FROM Event e WHERE e.rank=1 AND e.activityId=:activityId")
        Event findFirstByActivityId(Long activityId);

        Optional<Event> findFirstByTypeAndRank(EventTemplate eventType, int rank);

        @Modifying
        @Query("UPDATE Event e SET e.status=:status WHERE e.activityId=:activityId AND e.rank>:rank")
        int updateNextStatus(@Param("activityId") Long activityId, @Param("rank") Integer rank, @Param("status") EventStatus status);

        @Query("SELECT e FROM Event e WHERE e.status=:status AND e.executionMode=:executionMode AND e.type.launcherTagType=:tagTypeId AND e.triggerDate IS NOT NULL AND e.triggerDate<=:pivotDate")
        List<Event> findAllByStatusAndExecutionModeAndTagType(EventStatus status, EventExecutionMode executionMode, String tagTypeId, LocalDate pivotDate);

        @Modifying
        @Query("UPDATE Event e SET e.status=:status WHERE e.id IN (:eventIds)")
        int updateEventsStatusByIdIn(@Param("eventIds") Set<Long> eventIds, @Param("status") EventStatus status);

        @Modifying
        @Query("UPDATE Event e SET e.status=:status WHERE e.activityId = :activityId")
        int updateEventsStatusByActivityId(@Param("activityId") Long activityId, @Param("status") EventStatus status);


        @Modifying
        @Query("UPDATE Event e SET e.status=:status, e.executionMode=:executionMode WHERE e.id IN (:eventIds)")
        int updateEventsStatusAndExecutionModeByIdIn(@Param("eventIds") List<Long> eventIds, 
                                                        @Param("status") EventStatus status,
                                                        @Param("executionMode") EventExecutionMode executionMode
                                                        );
        @Modifying
        @Query("UPDATE Event e SET e.executionMode =:executionMode, e.triggerDate =:date, e.status ='PENDING', e.executionDate = NULL WHERE "+
                "e.status IN ('PENDING', 'SUSPENDED') AND "+
                "e.type.id IN (SELECT id from EventTemplate et where et.category='BILLING' AND et.subCategory=:step) AND "+
                "e.activityId in (SELECT r.firstObjectId FROM Relation r WHERE r.relationType='ACTIVITY_BILLING_RUN' AND r.secondObjectId=:billingRunId)")
        int updateAllByBillingRunAndStatusAndSubCategory(@Param("billingRunId") Long billingRunId, 
                                                        @Param("step")String step, 
                                                        @Param("executionMode") EventExecutionMode executionMode, 
                                                        @Param("date") LocalDate date);

        @Modifying
        @Query("UPDATE Event e SET e.executionMode=:executionMode, e.triggerDate=:date WHERE e.status='PENDING' AND e.type.id IN (SELECT id from EventTemplate et where et.category='BILLING' AND et.subCategory=:step) AND e.activityId in (SELECT r.firstObjectId FROM Relation r WHERE r.relationType='ACTIVITY_INVOICE' AND r.secondObjectId=:billId)")
        int updateAllByBillIdAndStatusAndSubCategory(Long billId, String step, EventExecutionMode executionMode, LocalDate date);

        @Query("SELECT e FROM Event e WHERE e.status='PENDING' AND e.type.id IN (SELECT id from EventTemplate et where et.category='BILLING' AND et.subCategory=:step) AND e.activityId in (SELECT r.firstObjectId FROM Relation r WHERE r.relationType='ACTIVITY_INVOICE' AND r.secondObjectId=:billId)")
        Optional<Event> findByBillIdAndStatusAndSubCategory(Long billId, String step);

        @Query("SELECT DISTINCT e FROM Event e " +
        "LEFT JOIN e.type t " +
        "LEFT JOIN Relation r ON r.firstObjectId = e.activity.id " +
        "LEFT JOIN Contract c ON r.secondObjectId = c.id " +
        "LEFT JOIN Customer cu ON r.secondObjectId = cu.id " +
        "LEFT JOIN Perimeter p ON r.secondObjectId = p.id " +
        "WHERE (:id IS NULL OR e.id = :id) " +
        "AND (:activityId IS NULL OR e.activityId = :activityId) " +
        "AND (:category IS NULL OR t.category = :category) " +
        "AND (:subCategory IS NULL OR t.subCategory = :subCategory) " +
        "AND (:status IS NULL OR e.status = :status) " +
        "AND (:executionMode IS NULL OR e.executionMode = :executionMode) " +
        "AND (:eventType IS NULL OR t.id = :eventType) " +
        "AND (:userId IS NULL OR e.userId = :userId) " +
        "AND (:externalEventRef IS NULL OR e.externalEventRef = :externalEventRef) " +
        "AND (:groupId IS NULL OR e.groupId = :groupId) " +
        "AND (:organismId IS NULL OR e.organismId = :organismId) " +
        "AND (:triggerDate IS NULL OR e.triggerDate = CAST(:triggerDate AS date)) " +
        "AND (:creationDateStart IS NULL OR e.creationDate >= CAST(:creationDateStart AS timestamp)) " +
        "AND (:creationDateEnd IS NULL OR e.creationDate <= CAST(:creationDateEnd AS timestamp)) " +
        "AND (:executionDateStart IS NULL OR e.executionDate >= CAST(:executionDateStart AS timestamp)) " +
        "AND (:executionDateEnd IS NULL OR e.executionDate >= CAST(:executionDateEnd AS timestamp)) " +
        "AND (:relationType IS NULL OR r.relationType = :relationType) " +
        "AND (:objectId IS NULL OR e.activity.id IN " +
        "   (SELECT r.firstObjectId FROM Relation r WHERE r.relationType = :relationType AND r.secondObjectId = :objectId)) " +
        "AND (:objectRef IS NULL OR e.activity.id IN " +
        "   (SELECT r.firstObjectId FROM Relation r WHERE r.relationType = :relationType " +
        "   AND (c.reference = :objectRef OR cu.reference = :objectRef OR p.reference = :objectRef)))")
        Page<Event> findEvent(@Param("id") Long id,
                      @Param("activityId") Long activityId, 
                      @Param("category") String category, 
                      @Param("subCategory") String subCategory, 
                      @Param("status") EventStatus status, 
                      @Param("executionMode") EventExecutionMode executionMode, 
                      @Param("eventType") String eventType, 
                      @Param("relationType") String relationType, 
                      @Param("objectId") Long objectId,
                      @Param("objectRef") String objectRef,
                      @Param("triggerDate") String triggerDate,
                      @Param("creationDateStart") String creationDateStart,
                      @Param("creationDateEnd") String creationDateEnd,
                      @Param("executionDateStart") String executionDateStart,
                      @Param("executionDateEnd") String executionDateEnd,
                      @Param("groupId") Long groupId,
                      @Param("organismId") Long organismId,
                      @Param("userId") Long userId,
                      @Param("externalEventRef") String externalEventRef,
                      Pageable pageable);


         @Query("SELECT e FROM Event e "+
                        "WHERE (:eventType IS NULL OR e.type.id = :eventType) " +
                        "AND (:status IS NULL OR e.status=:status) " +
                        "AND (:relationType IS NULL OR :relationType IN " +
                                "(SELECT r.relationType FROM Relation r WHERE r.firstObjectId = e.activity.id)) " +
                        "AND (:objectId IS NULL OR e.activity.id IN " +
                                "(SELECT r.firstObjectId FROM Relation r WHERE r.relationType = :relationType AND r.secondObjectId = :objectId)) "
                 )
        List<Event> findAllByTypeAndRelationTypeAndObjectIdAndStatus(
                            @Param("eventType") String eventType, 
                            @Param("status") EventStatus status, 
                            @Param("relationType") String relationType, 
                            @Param("objectId") Long objectId);

        @Query("SELECT e FROM Event e WHERE e.status IN (:eventStatus) "+
                                "AND e.type.id IN (SELECT id from EventTemplate et where et.category='BILLING' AND et.subCategory=:step) "+
                                "AND e.activityId in (SELECT r.firstObjectId FROM Relation r WHERE r.relationType='ACTIVITY_INVOICE' AND r.secondObjectId=:billId)")
        Optional<Event> findByBillIdAndStatusInAndSubCategory(Long billId, List<EventStatus> eventStatus ,String step);
}
