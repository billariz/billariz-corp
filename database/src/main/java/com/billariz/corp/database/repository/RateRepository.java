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

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.Rate;

@RepositoryRestResource
public interface RateRepository extends RepositoryReadWriteDelete<Rate, Long>
{
    List<Rate> findAllByTypeAndThresholdBase(String type, String thresholdBase);

    List<Rate> findAllByType(String type);

    @Query("SELECT s FROM Rate s WHERE (:id IS NULL OR s.id=:id) AND (:subCategory IS NULL OR s.rateType.subCategory=:subCategory) AND (:category IS NULL OR s.rateType.category=:category) AND (:rateType IS NULL OR s.type=:rateType) AND (:tou IS NULL OR s.tou=:tou) AND (:channel IS NULL OR s.channel=:channel) AND (:customerCategory IS NULL OR s.customerCategory=:customerCategory) AND (:customerType IS NULL OR s.customerType=:customerType) AND (:dgoCode IS NULL OR s.dgoCode=:dgoCode) AND (:tgoCode IS NULL OR s.tgoCode=:tgoCode) AND (:posCategory IS NULL OR s.posCategory=:posCategory) AND (:market IS NULL OR s.market=:market) AND (:installmentFrequency IS NULL OR s.installmentFrequency=:installmentFrequency) AND (:priceType IS NULL OR s.priceType=:priceType) AND (:gridRate IS NULL OR s.gridRate=:gridRate) AND (:serviceCategory IS NULL OR s.serviceCategory=:serviceCategory) AND (:serviceSubCategory IS NULL OR s.serviceSubCategory=:serviceSubCategory)AND (:touGroup IS NULL OR s.touGroup=:touGroup)")
    Page<Rate> findRate(@Param("id") Long id, @Param("subCategory") String subCategory, @Param("category") String category, @Param("rateType") String rateType, @Param("tou") String tou, @Param("touGroup") String touGroup, @Param("channel") String channel, @Param("serviceCategory") String serviceCategory, @Param("serviceSubCategory") String serviceSubCategory,@Param("customerCategory") String customerCategory, @Param("customerType") String customerType, @Param("dgoCode") String dgoCode, @Param("tgoCode") String tgoCode, @Param("gridRate") String gridRate, @Param("posCategory") String posCategory, @Param("installmentFrequency") String installmentFrequency, @Param("market") String market, @Param("priceType") String priceType, Pageable pageable);

}
