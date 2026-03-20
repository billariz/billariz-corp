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
import com.billariz.corp.database.model.Service;
import com.billariz.corp.database.model.enumeration.ServiceStatus;
import com.billariz.corp.database.projection.inService;

@RepositoryRestResource(excerptProjection = inService.class)
public interface ServiceRepository extends RepositoryReadWriteDelete<Service, Long>
{
    @Query("SELECT a.status FROM Service a WHERE a.id=:id")
    ServiceStatus findStatusById(Long id);

    List<Service> findAllByContractIdAndStatusIn(Long contractId, List<ServiceStatus> status);

    List<Service> findAllByContractIdAndStatusNotIn(Long contractId, List<ServiceStatus> status);

    @Query("SELECT s FROM Service s WHERE (:id IS NULL OR s.id=:id) " +
                "AND (:contractId IS NULL OR s.contract.id=:contractId) " +
                "AND (:status IS NULL OR s.status=:status) " +
                "AND (:serviceTypeId IS NULL OR s.serviceTypeId=:serviceTypeId) " +
                "AND (:category IS NULL OR s.serviceType.category=:category) " +
                "AND (:subCategory IS NULL OR s.serviceType.subCategory=:subCategory) " +
                "AND (:isDefault IS NULL OR s.serviceType.defaultService=:isDefault) " +
                "AND (:startDate IS NULL OR s.startDate >= CAST(:startDate AS date)) " +
                "AND (:endDate IS NULL OR s.endDate <= CAST(:endDate AS date)) ")
    Page<Service> findService(@Param("id") Long id, 
                                @Param("contractId") Long contractId, 
                                @Param("status") ServiceStatus status, 
                                @Param("category") String category, 
                                @Param("subCategory") String subCategory, 
                                @Param("startDate") String startDate, 
                                @Param("endDate") String endDate, 
                                @Param("serviceTypeId") String serviceTypeId,
                                @Param("isDefault") Boolean isDefault,
                                Pageable pageable);

    boolean existsByContractIdAndStatusIn(Long contractId, List<ServiceStatus> status);

    boolean existsByContractIdAndStatusNotIn(Long contractId, List<ServiceStatus> statusList);

    @Query("SELECT s FROM Service s WHERE "+
            "s.contract.id=:contractId "+
            "AND s.status=:status "+
            "AND s.endDate=:endDate "+
            "AND s.serviceType.defaultService=false")
    List<Service> findByContractIdAndStatusAndIsDefault(Long contractId, ServiceStatus status, LocalDate endDate);

}
