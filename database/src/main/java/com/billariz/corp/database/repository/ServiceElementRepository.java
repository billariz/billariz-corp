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
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.ServiceElement;
import com.billariz.corp.database.model.enumeration.SeStatus;
import com.billariz.corp.database.model.light.EligibleServiceElement;
import com.billariz.corp.database.projection.inServiceElement;

@RepositoryRestResource(excerptProjection = inServiceElement.class)
public interface ServiceElementRepository extends RepositoryReadWriteDelete<ServiceElement, Long>
{
        @Query("SELECT a.status FROM ServiceElement a WHERE a.id=:id")
        String findStatusById(Long id);

        @Modifying
        @Query("UPDATE ServiceElement s SET s.status=:status WHERE s.tosId=:tosId")
        int updateStatus(@Param("tosId") Long tosId, @Param("status") SeStatus status);

        @Query("SELECT new com.billariz.corp.database.model.light.EligibleServiceElement(s.id,s.metered,s.sqType,s.seType,s.seType.seTypeCategory,s.tos.contract.id, s.estimateAuthorized, s.startDate, s.endDate, s.minDayForEstimate, s.tou, s.touGroup) FROM ServiceElement s WHERE s.status IN (:seStatus) AND s.master=true AND s.tos.contractId=:contractId")
        List<EligibleServiceElement> findWithStatusInAndMasterIsTrueAndTosContractStatusIn(@Param("seStatus") List<SeStatus> seStatus, @Param("contractId") Long contractId);

        @Query("SELECT s FROM ServiceElement s WHERE s.seType.masterSeTypeId=:masterType AND s.master=false AND s.tos.contractId=:contractId")
        List<ServiceElement> findWithServiceElementMaster(@Param("masterType") String masterType, @Param("contractId") Long contractId);

        @Query("SELECT s FROM ServiceElement s WHERE s.master=true AND s.tos.contractId=:contractId")
        List<ServiceElement> findMasterServiceElementWithContractId(@Param("contractId") Long contractId);

        @Query("SELECT s FROM ServiceElement s WHERE (:id IS NULL OR s.id=:id) "+
                                "AND (:tosId IS NULL OR s.tosId=:tosId) "+
                                "AND (:status IS NULL OR s.status=:status) "+
                                "AND (:category IS NULL OR s.category=:category) "+
                                "AND (:subCategory IS NULL OR s.subCategory=:subCategory) "+
                                "AND (:contractId IS NULL OR s.tos.contract.id=:contractId)" +
                                "AND (:tou IS NULL OR s.tou=:tou)" +
                                "AND (:touGroup IS NULL OR s.touGroup=:touGroup)" +
                                "AND (:serviceElementTypeId IS NULL OR s.seType.id=:serviceElementTypeId)" +
                                "AND (:startDate IS NULL OR s.startDate >= CAST(:startDate AS date)) " +
                                "AND (:endDate IS NULL OR s.endDate <= CAST(:endDate AS date))")
        Page<ServiceElement> findServiceElement(@Param("id") Long id, 
                                                @Param("contractId") Long contractId, 
                                                @Param("tosId") Long tosId, 
                                                @Param("category") String category, 
                                                @Param("subCategory") String subCategory, 
                                                @Param("status") String status,
                                                @Param("tou") String tou, 
                                                @Param("touGroup") String touGroup,
                                                @Param("startDate") String startDate, 
                                                @Param("endDate") String endDate,
                                                @Param("serviceElementTypeId") String serviceElementTypeId,
                                                Pageable pageable);

        List<ServiceElement> findByStatusInAndTosId(@Param("seStatus") List<SeStatus> seStatus, @Param("tosId") Long tosId);

        @Modifying
        @Query("UPDATE ServiceElement s SET s.status=:status, endDate=:endDate WHERE s.tosId =:tosId AND s.status in (:seStatusList) AND (s.endDate IS NULL OR s.endDate > :endDate )")
        int closeServiceElement(@Param("seStatusList") List<SeStatus> seStatusList, @Param("status") SeStatus status,  @Param("tosId") Long tosId, @Param("endDate") LocalDate endDate);
}
