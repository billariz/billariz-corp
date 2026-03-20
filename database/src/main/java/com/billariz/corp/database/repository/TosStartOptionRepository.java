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
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.TermOfServiceStartOption;

@RepositoryRestResource
public interface TosStartOptionRepository extends RepositoryReadWriteDelete<TermOfServiceStartOption, Long>
{
    List<TermOfServiceStartOption> findAllByTypeAndStartDateBefore(String tosType, LocalDate startDate);

    default Optional<TermOfServiceStartOption> filterByTosTypeAndDate(String tosType, LocalDate date)
    {
        return findAllByTypeAndStartDateBefore(tosType, date).stream().filter(i -> i.getEndDate() == null || i.getEndDate().isBefore(date)).findAny();
    }

    @Query("SELECT s FROM TermOfServiceStartOption s WHERE (:id IS NULL OR s.id=:id) AND (:tosType IS NULL OR s.type=:tosType) AND (:priceMode IS NULL OR s.priceMode=:priceMode) AND (:priceType IS NULL OR s.priceType=:priceType) AND (:market IS NULL OR s.market=:market)")
    Page<TermOfServiceStartOption> findTermOfServiceStartOption(@Param("id") Long id, @Param("tosType") String tosType, @Param("market") String market, @Param("priceMode") String priceMode, @Param("priceType") String priceType, Pageable pageable);

}
