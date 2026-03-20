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
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.GeoFactor;

@RepositoryRestResource
public interface GeoFactorRepository extends RepositoryReadWriteDelete<GeoFactor, Long>
{
    Optional<GeoFactor> findByAreaCodeAndStartDateIsLessThanEqualAndEndDateIsGreaterThan(String areaCode, LocalDate startDate, LocalDate enDate);

    @Query("SELECT a FROM GeoFactor a WHERE (:id IS NULL OR a.id=:id) "+
                "AND (:areaCode IS NULL OR a.areaCode=:areaCode) " +
               "AND (:startDate IS NULL OR a.startDate >= CAST(:startDate AS date)) " +
       "AND (:endDate IS NULL OR a.endDate <= CAST(:endDate AS date)) "
                )
    Page<GeoFactor> findGeoFactor(@Param("id") Long id, 
                                    @Param("areaCode") String areaCode, 
                                    @Param("startDate") String startDate, 
                                    @Param("endDate") String endDate, 
                                    Pageable pageable);
}
