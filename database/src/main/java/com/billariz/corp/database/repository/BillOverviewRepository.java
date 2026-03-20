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
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.database.model.BillOverview;

@RepositoryRestResource
public interface BillOverviewRepository extends CrudRepository<BillOverview, Long>
{
    @Query("SELECT a FROM BillOverview a WHERE a.billingRunId=:billingRunId AND (:status IS null OR a.status=:status)")
    List<BillOverview> findBillOverview(Long billingRunId, String status);

}
