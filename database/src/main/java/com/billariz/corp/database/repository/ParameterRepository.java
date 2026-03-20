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
import com.billariz.corp.database.model.Parameter;

@RepositoryRestResource
public interface ParameterRepository extends RepositoryReadWriteDelete<Parameter, Long>
{
    List<Parameter> findAllByTypeAndNameAndStartDateBefore(String type, String name, LocalDate date);

    @Query("SELECT p FROM Parameter p")
    List<Parameter> findAll();

    @Query("SELECT p FROM Parameter p WHERE (p.type='ENUM') AND (:name IS NULL OR p.name=:name) AND (:category IS NULL OR p.category=:category) AND (:subCategory IS NULL OR p.subCategory=:subCategory)")
    List<Parameter> findEnumParameters(@Param("name") String name, @Param("category") String category, @Param("subCategory") String subCategory);

    @Query("SELECT p FROM Parameter p "+
                    "WHERE (:id IS NULL OR p.id=:id) "+
                    "AND (:type IS NULL OR p.type=:type) "+
                    "AND (:name IS NULL OR p.name LIKE %:name%) "+
                    "AND (:category IS NULL OR p.category=:category) "+
                    "AND (:subCategory IS NULL OR p.subCategory=:subCategory)")
    Page<Parameter> findParameter(@Param("id") Long id, 
                                    @Param("type") String type, 
                                    @Param("name") String name, 
                                    @Param("category") String category, 
                                    @Param("subCategory") String subCategory, 
                                    Pageable pageable);

    List<Parameter> findAllByTypeAndNameAndCategoryAndSubCategory(String type, String name, String category, String subCategory);

    boolean existsByTypeAndNameAndValueAndStartDateBefore(String type, String name, String value, LocalDate date);
}
