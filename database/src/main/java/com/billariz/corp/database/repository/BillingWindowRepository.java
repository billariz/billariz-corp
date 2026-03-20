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
import java.util.Optional;

import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWrite;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.BillingCycle;
import com.billariz.corp.database.model.BillingWindow;

@RepositoryRestResource
public interface BillingWindowRepository extends RepositoryReadWriteDelete<BillingWindow, Long>
{
    Optional<BillingWindow> findAllById(String id);

    List<BillingWindow> findAll();

    BillingWindow findFirstByBillingCycle(BillingCycle billingCycle);
}