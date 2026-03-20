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
import com.billariz.corp.database.model.PostalCode;

@RepositoryRestResource
public interface PostalCodeRepository extends RepositoryReadWriteDelete<PostalCode, Long>
{
    Optional<PostalCode> findByPostalCodeAndCityName(String postalCode, String cityName);

    @Query("SELECT p FROM PostalCode p WHERE (:id IS NULL OR p.id=:id) AND (:postalCode IS NULL OR p.postalCode LIKE :postalCode%) AND (:cityName IS NULL OR p.cityName=:cityName)")
    Page<PostalCode> findPostalCode(@Param("id") Long id, @Param("postalCode") String postalCode, @Param("cityName") String cityName, Pageable pageable);

}
