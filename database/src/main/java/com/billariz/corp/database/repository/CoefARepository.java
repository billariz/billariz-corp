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
import com.billariz.corp.data.RepositoryReadWriteFull;
import com.billariz.corp.database.model.CoefA;

@RepositoryRestResource
public interface CoefARepository extends RepositoryReadWriteDelete<CoefA, Long>
{
    Optional<CoefA> findByDgoCodeAndTgoCodeAndEnergyNatureAndStartDateIsLessThanEqualAndEndDateIsGreaterThan(String dgoCode, String tgoCode, String energyName, LocalDate startDate, LocalDate enDate);

    @Query("SELECT a FROM CoefA a WHERE (:id IS NULL OR a.id=:id) "+
                "AND (:startDate IS NULL OR a.startDate >= CAST(:startDate AS date)) " +
                "AND (:endDate IS NULL OR a.endDate <= CAST(:endDate AS date)) " +
                "AND (:tgoCode is null or a.tgoCode=:tgoCode)" +
                "AND (:dgoCode is null or a.dgoCode=:dgoCode)" +
                "AND (:energyNature is null or a.energyNature=:energyNature)"
                )
    Page<CoefA> findCoefA(@Param("id") Long id, 
                                    @Param("startDate") String startDate, 
                                    @Param("endDate") String endDate, 
                                    @Param("tgoCode") String tgoCode, 
                                    @Param("dgoCode") String dgoCode, 
                                    @Param("energyNature") String energyNature, 
                                    Pageable pageable);
}
