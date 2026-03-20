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
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.ArticleType;

@RepositoryRestResource
public interface ArticleTypeRepository extends RepositoryReadWriteDelete<ArticleType, String>{
    
    @Query("SELECT p FROM ArticleType p "+
                "WHERE (:id IS NULL OR p.id=:id) "+
                "AND (:category IS NULL OR p.category=:category) "+
                "AND (:market IS NULL OR p.market=:market) "+
                "AND (:counterpart IS NULL OR p.counterpart=:counterpart) "+
                "AND (:label IS NULL OR " +
                "lower(p.id) LIKE CONCAT('%', lower(:label), '%') OR " +
                "lower(p.defaultLabel) LIKE CONCAT('%', lower(:label), '%') OR " +
                "lower(p.standardLabel) LIKE CONCAT('%', lower(:label), '%') OR " +
                "lower(p.otherLabel) LIKE CONCAT('%', lower(:label), '%') " +
                "AND (:subCategory IS NULL OR p.subCategory=:subCategory))"
        )
    Page<ArticleType> findArticleType(String id,
                                    String category,
                                    String subCategory,
                                    String market,
                                    String counterpart,
                                    String label,
                                    Pageable pageable);

}
