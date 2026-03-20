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

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.data.RepositoryReadWriteFull;
import com.billariz.corp.database.model.ClimaticRef;

@RepositoryRestResource
public interface ClimaticRefRepository extends RepositoryReadWriteDelete<ClimaticRef, Long>
{
    Optional<ClimaticRef> findByWeatherChannelCodeAndProfile(String weatherChannelCode, String profile);

    @Query("SELECT a FROM ClimaticRef a WHERE (:id IS NULL OR a.id=:id) "+
                "AND (:market IS NULL OR a.market=:market)" +
                "AND (:weatherChannelCode is null or a.weatherChannelCode=:weatherChannelCode)" +
                "AND (:profile is null or a.profile=:profile)"
                )
    Page<ClimaticRef> findClimaticRef(@Param("id") Long id, 
                                    @Param("market") String market, 
                                    @Param("weatherChannelCode") String weatherChannelCode, 
                                    @Param("profile") String profile,
                                    Pageable pageable);
}
