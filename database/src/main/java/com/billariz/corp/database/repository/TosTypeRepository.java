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
import com.billariz.corp.database.model.TermOfServiceType;

@RepositoryRestResource
public interface TosTypeRepository extends RepositoryReadWriteDelete<TermOfServiceType, String>
{
    @Query("SELECT s FROM TermOfServiceType s WHERE (:tosType IS NULL OR s.id=:tosType) AND (:market IS NULL OR s.market=:market) AND (:category IS NULL OR s.category=:category) AND (:subCategory IS NULL OR s.subCategory=:subCategory)")
    Page<TermOfServiceType> findTermOfServiceType(@Param("tosType") String tosType, @Param("market") String market, @Param("category") String category, @Param("subCategory") String subCategory, Pageable pageable);
}
