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
import com.billariz.corp.data.RepositoryReadWriteFull;
import com.billariz.corp.database.model.LauncherTagType;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;

@RepositoryRestResource
public interface LauncherTagTypeRepository extends RepositoryReadWriteFull<LauncherTagType, String>
{

    @Query("SELECT nlt FROM LauncherTagType nlt WHERE nlt.rank IN(SELECT min(tr.rank) FROM LauncherTagType tr where tr.active=true and tr.rank >:previousRank)")
    Optional<LauncherTagType> findNextByActiveIsTrueAndPreviousRank(@Param("previousRank") Long previousRank);

    @Query("SELECT nlt FROM LauncherTagType nlt WHERE nlt.rank IN(SELECT min(tr.rank) FROM LauncherTagType tr where tr.active=true)")
    LauncherTagType findFirstByActiveIsTrue();

    @Query("SELECT m FROM LauncherTagType m WHERE (:id IS NULL OR m.id=:id) "+
                            "AND (:rank IS NULL OR m.rank=:rank) "+
                            "AND (:label IS NULL OR " +
                            "lower(m.id) LIKE CONCAT('%', lower(:label), '%') OR " +
                            "lower(m.defaultLabel) LIKE CONCAT('%', lower(:label), '%') OR " +
                            "lower(m.standardLabel) LIKE CONCAT('%', lower(:label), '%') OR " +
                            "lower(m.otherLabel) LIKE CONCAT('%', lower(:label), '%')) " +
                            "AND (:executionMode IS NULL OR m.executionMode >=:executionMode)")
    Page<LauncherTagType> findLauncherTagType(String id, 
                                            Long rank, 
                                            EventExecutionMode executionMode, 
                                            String label, 
                                            Pageable pageable);

}
