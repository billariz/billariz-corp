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
import com.billariz.corp.database.model.MarketGeoRef;

@RepositoryRestResource
public interface MarketGeoRefRepository extends RepositoryReadWriteDelete<MarketGeoRef, Long>
{
    Optional<MarketGeoRef> findByAreaCodeAndStartDateIsLessThanEqualAndEndDateIsGreaterThan(String areaCode, LocalDate startDate, LocalDate enDate);

    @Query("SELECT a FROM MarketGeoRef a WHERE (:id IS NULL OR a.id=:id) "+
                "AND (:areaCode IS NULL OR a.areaCode=:areaCode) " +
                "AND (:market IS NULL OR a.market=:market)" +
                "AND (:startDate IS NULL OR a.startDate >= CAST(:startDate AS date)) " +
                "AND (:endDate IS NULL OR a.endDate <= CAST(:endDate AS date)) " +
                "AND (:dispatchRate is null or a.dispatchRate=:dispatchRate)" +
                "AND (:netAreaCode is null or a.netAreaCode=:netAreaCode)" +
                "AND (:weatherChannelCode is null or a.weatherChannelCode=:weatherChannelCode)" +
                "AND (:climaticZone is null or a.climaticZone=:climaticZone)" +
                "AND (:tgoCode is null or a.tgoCode=:tgoCode)" +
                "AND (:energyNature is null or a.energyNature=:energyNature)" +
                "AND (:balanceZone is null or a.balanceZone=:balanceZone)"
                )
    Page<MarketGeoRef> findMarketGeoRef(@Param("id") Long id, 
                                    @Param("areaCode") String areaCode, 
                                    @Param("market") String market, 
                                    @Param("startDate") String startDate, 
                                    @Param("endDate") String endDate, 
                                    @Param("dispatchRate") String dispatchRate, 
                                    @Param("netAreaCode") String netAreaCode, 
                                    @Param("weatherChannelCode") String weatherChannelCode, 
                                    @Param("climaticZone") String climaticZone, 
                                    @Param("tgoCode") String tgoCode, 
                                    @Param("energyNature") String energyNature, 
                                    @Param("balanceZone") String balanceZone, 
                                    Pageable pageable);


}
