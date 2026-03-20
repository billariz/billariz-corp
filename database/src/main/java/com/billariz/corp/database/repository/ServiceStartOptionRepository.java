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

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.ServiceStartOption;

@RepositoryRestResource
public interface ServiceStartOptionRepository extends RepositoryReadWriteDelete<ServiceStartOption, Long>
{
    List<ServiceStartOption> findAll();

    @Query("SELECT s FROM ServiceStartOption s WHERE (:id IS NULL OR s.id=:id) AND (:serviceType IS NULL OR s.service=:serviceType) AND (:category IS NULL OR s.serviceCategory=:category) AND (:subCategory IS NULL OR s.serviceSubCategory=:subCategory) AND (:tosType IS NULL OR s.tosType=:tosType)")
    Page<ServiceStartOption> findServiceStartOption(@Param("id") Long id, @Param("serviceType") String serviceType, @Param("category") String category, @Param("subCategory") String subCategory, @Param("tosType") String tosType, Pageable pageable);

}
