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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.ServiceType;

@RepositoryRestResource
public interface ServiceTypeRepository extends RepositoryReadWriteDelete<ServiceType, String>
{
    @Query("SELECT s.defaultService FROM ServiceType s WHERE s.id=:id")
    boolean findDefaultServiceById(String id);

    @Query("SELECT s FROM ServiceType s WHERE (:serviceType IS NULL OR s.id=:serviceType) AND (:category IS NULL OR s.category=:category) AND (:subCategory IS NULL OR s.subCategory=:subCategory)")
    Page<ServiceType> findServiceType(@Param("serviceType") String serviceType, @Param("category") String category, @Param("subCategory") String subCategory, Pageable pageable);
}
