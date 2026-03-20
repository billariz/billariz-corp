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

package com.billariz.corp.launcher.billing;

import java.util.List;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.PointOfService;
import com.billariz.corp.database.model.PointOfServiceCapacity;
import com.billariz.corp.database.model.PointOfServiceConfiguration;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ContractAndCustomerInfo
{
    private Contract                          contract;

    private String                            customerType;

    private String                            customerCategory;

    private List<PointOfServiceConfiguration> posConfigurations;

    private List<PointOfServiceCapacity>      capacities;

    private PointOfService                    pos;
}
