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

package com.billariz.corp.database.projection;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.rest.core.config.Projection;
import com.billariz.corp.database.model.BillOverview;
import com.billariz.corp.database.model.BillingCycle;
import com.billariz.corp.database.model.BillingRun;
import com.billariz.corp.database.model.BillingRunOverview;
import com.billariz.corp.database.model.enumeration.BillType;

@Projection(name = "inBillingRun", types = { BillingRun.class })

public interface inBillingRun
{

    Long getId();

    BillingCycle getBillingCycle();

    BillType getBillType();

    LocalDate getRunDate();

    LocalDate getStartDate();

    LocalDate getEndDate();

    List<BillingRunOverview> getBillingRunOverview();

    List<BillOverview> getBillOverview();

    String getStatus();

    int getContractCount();

}
