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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.billariz.corp.database.model.CollectionClassControl;

@Repository
public interface CollectionClassControlRepository extends org.springframework.data.repository.Repository<CollectionClassControl, Long>
{
    List<CollectionClassControl> findByCollectionActivityType(String collectionActivityType);

    @Query("SELECT c FROM CollectionClassControl c WHERE active=true and (paymentMode=:paymentMode or paymentMode='*') and (customerType=:customerType or customerType='*') and (customerCategory=:customerCategory or customerCategory='*')")
    List<CollectionClassControl> findByRules(@Param("paymentMode") String paymentMode, @Param("customerType") String customerType, @Param("customerCategory") String customerCategory);
}