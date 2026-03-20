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
import com.billariz.corp.database.model.Article;
import com.billariz.corp.database.model.enumeration.BillableChargeStatus;
import com.billariz.corp.database.projection.inArticle;

@RepositoryRestResource(excerptProjection = inArticle.class)
public interface ArticleRepository extends RepositoryReadWriteDelete<Article, Long>
{
    @Query("SELECT p FROM Article p "+
                "WHERE (:id IS NULL OR p.id=:id) "+
                "AND (:billableChargeId IS NULL OR p.billableChargeId=:billableChargeId) "+
                "AND (:status IS NULL OR p.status=:status) "+
                "AND (:startDate IS NULL OR p.startDate >= CAST(:startDate AS date)) " +
                "AND (:endDate IS NULL OR p.endDate <= CAST(:endDate AS date)) " +
                "AND (:effectiveDate is null or p.effectiveDate= CAST(:effectiveDate AS date)) " +
                "AND (:billId is null or p.billId=:billId) " +
                "AND (:externalArticleId is null or p.externalArticleId=:externalArticleId) " +
                "AND (:posRef is null or p.billableCharge.posRef=:posRef) " +
                "AND (:articleType is null or p.articleType.id=:articleType) "+
                "AND (:contractId IS NULL OR p.billableCharge.posRef IN (SELECT cp.pointOfService.reference FROM ContractPointOfService cp WHERE cp.contract.id=:contractId))"
                )
    Page<Article> findArticle(Long id,
                            Long billableChargeId,
                            BillableChargeStatus status,
                                                String startDate,
                                                String endDate,
                                                String effectiveDate,
                                                Long contractId, 
                                                String posRef,
                                                String externalArticleId,
                                                Long billId,
                                                String articleType,
                                                Pageable pageable);

}