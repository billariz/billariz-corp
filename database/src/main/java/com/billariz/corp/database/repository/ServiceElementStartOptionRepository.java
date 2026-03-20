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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.ServiceElementStartOption;

@RepositoryRestResource
public interface ServiceElementStartOptionRepository extends RepositoryReadWriteDelete<ServiceElementStartOption, Long>
{
    List<ServiceElementStartOption> findAllByTosTypeAndStartDateBefore(String tosType, LocalDate startDate);

    default List<ServiceElementStartOption> filterByTosTypeAndDate(String tosType, LocalDate date)
    {
        return findAllByTosTypeAndStartDateBefore(tosType, date).stream().filter(i -> i.getEndDate() == null || i.getEndDate().isBefore(date)).toList();
    }

    @Query("SELECT s FROM ServiceElementStartOption s WHERE (:id IS NULL OR s.id=:id) AND (:seType IS NULL OR s.serviceElementType=:seType) AND (:category IS NULL OR s.category=:category) AND (:subCategory IS NULL OR s.subCategory=:subCategory) AND (:tosType IS NULL OR s.tosType=:tosType) AND (:vatRate IS NULL OR s.vatRate=:vatRate) AND (:tou IS NULL OR s.tou=:tou) AND (:touGroup IS NULL OR s.touGroup=:touGroup)")
    Page<ServiceElementStartOption> findServiceElementStartOption(@Param("id") Long id, @Param("seType") String seType, @Param("category") String category, @Param("subCategory") String subCategory, @Param("tosType") String tosType, @Param("vatRate") String vatRate, @Param("tou") String tou, @Param("touGroup") String touGroup, Pageable pageable);

}
