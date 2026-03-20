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
import org.springframework.data.rest.core.config.Projection;
import com.billariz.corp.database.model.BillingCycle;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.Perimeter;
import com.billariz.corp.database.model.PerimeterType;

@Projection(name = "inPerimeter", types = { Perimeter.class })
public interface inPerimeter
{

    Long getId();

    String                  getReference();

    long                    getCustomerId();

    LocalDate               getStartDate();

    LocalDate               getEndDate();

    String                  getAnalyticCode();

    String                  getPerimeterType();

    PerimeterType           getType();

    String                  getBillingCycleId();

    BillingCycle                  getBillingCycle();

    LocalDate               getBillAfterDate();

    String                  getBillingFrequency();

    String                  getMarket();

    String                  getStatus();

    Journal                      getJournal();

}
