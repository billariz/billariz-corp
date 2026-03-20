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
import com.billariz.corp.database.model.ServiceElementType;
import com.billariz.corp.database.model.enumeration.ServiceElementTypeCategory;

@RepositoryRestResource
public interface ServiceElementTypeRepository extends RepositoryReadWriteDelete<ServiceElementType, String>
{
    @Query("SELECT s FROM ServiceElementType s WHERE (:id IS NULL OR s.id=:id) AND (:category IS NULL OR s.seTypeCategory=:category) AND (:market IS NULL OR s.market=:market) AND (:direction IS NULL OR s.direction=:direction) AND (:metered IS NULL OR s.metered=:metered)")
    Page<ServiceElementType> findServiceElementType(@Param("id") String id, @Param("category") ServiceElementTypeCategory category, @Param("market") String market, @Param("direction") String direction, @Param("metered") Boolean metered, Pageable pageable);
}
