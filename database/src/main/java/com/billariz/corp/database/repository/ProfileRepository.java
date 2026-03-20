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
import com.billariz.corp.database.model.Profile;

@RepositoryRestResource
public interface ProfileRepository extends RepositoryReadWriteDelete<Profile, Long> 
{
    @Query("SELECT s FROM Profile s "+
                "WHERE (:id IS NULL OR s.id=:id) "+
                "AND (:profile IS NULL OR s.profile=:profile) "+
                "AND (:subProfile IS NULL OR s.subProfile=:subProfile) "+
                "AND (:profileType IS NULL OR s.profileType=:profileType) "+
                "AND (:market IS NULL OR s.market=:market) "+
                "AND (:date IS NULL OR s.date= CAST(:date AS date))"
                )
    Page<Profile> findProfile(@Param("id") Long id, 
                            @Param("profile") String profile, 
                            @Param("subProfile") String subProfile, 
                            @Param("profileType") String profileType, 
                            @Param("market") String market, 
                            @Param("date") String date,
                            Pageable pageable);

}
