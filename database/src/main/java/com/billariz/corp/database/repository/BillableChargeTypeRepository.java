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
import com.billariz.corp.database.model.BillableChargeType;
import com.billariz.corp.database.model.enumeration.BillableChargeAquisitionStrategy;

@RepositoryRestResource
public interface BillableChargeTypeRepository extends RepositoryReadWriteDelete<BillableChargeType, String> {

    @Query("SELECT p FROM BillableChargeType p "+
                "WHERE (:id IS NULL OR p.id=:id) "+
                "AND (:category IS NULL OR p.category=:category) "+
                "AND (:aquisitionStrategy IS NULL OR p.aquisitionStrategy=:aquisitionStrategy) "+
                "AND (:subCategory IS NULL OR p.subCategory=:subCategory) " +
                "AND (:label IS NULL OR " +
                "lower(p.id) LIKE CONCAT('%', lower(:label), '%') OR " +
                "lower(p.defaultLabel) LIKE CONCAT('%', lower(:label), '%') OR " +
                "lower(p.standardLabel) LIKE CONCAT('%', lower(:label), '%') OR " +
                "lower(p.otherLabel) LIKE CONCAT('%', lower(:label), '%'))"
        )
    Page<BillableChargeType> findBillableChargeType(String id,
                                            String label, 
                                            String category,
                                            String subCategory,
                                            BillableChargeAquisitionStrategy aquisitionStrategy,
                                            Pageable pageable);

}
