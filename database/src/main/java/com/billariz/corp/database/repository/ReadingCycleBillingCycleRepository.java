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
import com.billariz.corp.database.model.ReadingCycle;
import com.billariz.corp.database.model.ReadingCycleBillingCycle;

@RepositoryRestResource
public interface ReadingCycleBillingCycleRepository extends RepositoryReadWriteDelete<ReadingCycleBillingCycle, Long>
{
        Optional<ReadingCycleBillingCycle> findFirstByReadingCycleAndBillingFrequency(ReadingCycle readingCycle, String billingFrequency);

        @Query("SELECT a FROM ReadingCycleBillingCycle a WHERE (:id IS NULL OR a.id=:id) "+
                "AND (:market IS NULL OR a.market=:market)" +
                "AND (:readingCycle IS NULL OR a.readingCycle=:readingCycle)" +
                "AND (:billingFrequency is null or a.billingFrequency=:billingFrequency)"
                )
        Page<ReadingCycleBillingCycle> findReadingCycleBillingCycle(@Param("id") Long id,
                                    @Param("market") String market, 
                                    @Param("readingCycle") ReadingCycle readingCycle,
                                    @Param("billingFrequency") String billingFrequency,
                                    Pageable pageable);
}