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
import com.billariz.corp.database.model.TermOfService;
import com.billariz.corp.database.model.enumeration.TosStatus;
import com.billariz.corp.database.projection.inTermOfService;

@RepositoryRestResource(excerptProjection = inTermOfService.class, collectionResourceRel = "termOfServices", path = "termOfServices")
public interface TermOfServiceRepository extends RepositoryReadWriteDelete<TermOfService, Long>
{
    @Query("SELECT a.status FROM TermOfService a WHERE a.id=:id")
    TosStatus findStatusById(Long id);

    List<TermOfService> findAllByTosTypeIdAndContractIdAndStatusNotIn(String tosTypeId, Long contractId, List<TosStatus> status);

    @Query("SELECT t.id from TermOfService t WHERE contractId=:contractId")
    List<Long> findAllIds(@Param("contractId") Long contractId);

    @Query("SELECT t FROM TermOfService t WHERE (:id IS NULL OR t.id=:id) "+
                        "AND (:serviceId IS NULL OR t.serviceId=:serviceId) "+
                        "AND (:contractId IS NULL OR t.contractId=:contractId) "+
                        "AND (:status IS NULL OR t.status=:status) "+
                        "AND (:category IS NULL OR t.tosType.category=:category) "+
                        "AND (:subCategory IS NULL OR t.tosType.subCategory=:subCategory)"+
                        "AND (:tosTypeId IS NULL OR t.tosType.id=:tosTypeId) " +
                        "AND (:direction IS NULL OR t.direction=:direction) " +
                        "AND (:priceType IS NULL OR t.priceType=:priceType) " +
                        "AND (:touGroup IS NULL OR t.touGroup=:touGroup) " +
                        "AND (:startDate IS NULL OR t.startDate >= CAST(:startDate AS date)) " +
                        "AND (:endDate IS NULL OR t.endDate <= CAST(:endDate AS date))")
    Page<TermOfService> findTermOfService(@Param("id") Long id,
                                             @Param("serviceId") Long serviceId, 
                                             @Param("contractId") Long contractId, 
                                             @Param("status") TosStatus status, 
                                             @Param("category") String category, 
                                             @Param("subCategory") String subCategory, 
                                             @Param("startDate") String startDate, 
                                             @Param("endDate") String endDate, 
                                             @Param("tosTypeId") String tosTypeId,
                                             @Param("direction") String direction,
                                             @Param("priceType") String priceType,
                                             @Param("touGroup") String touGroup,
                                             Pageable pageable);

    boolean existsByContractIdAndStatus(Long contractId, TosStatus status);

    @Query("SELECT tos FROM TermOfService tos " +
       "WHERE tos.contractId = :contractId " +
       "AND tos.status IN :statusList " +
       "AND tos.direction = :direction " +
       "AND (tos.endDate IS NULL OR tos.endDate > :endDate)")
    List<TermOfService> findAllByContractIdAndStatusInAndDirectionAndEndDateIsNullOrEndDateIsAfter(Long contractId, List<TosStatus> statusList, String direction, LocalDate endDate );

    boolean existsByServiceIdAndStatusNotIn(Long ServiceId, List<TosStatus> statusList);

    List<TermOfService> findAllByServiceIdInAndStatusIn(List<Long> ServiceId, List<TosStatus> statusList);

}
