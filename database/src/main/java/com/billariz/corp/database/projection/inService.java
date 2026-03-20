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

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.rest.core.config.Projection;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.Service;
import com.billariz.corp.database.model.ServiceType;

@Projection(name = "inService", types = { Service.class })
public interface inService
{

    Long getId();

    long getContractId();

    ServiceType getServiceType();

    String getServiceTypeId();

    LocalDate getStartDate();

    LocalDate getEndDate();

    String getTouGroup();

    String getTou();

    BigDecimal getAmount();

    String getThreshold();

    String getThresholdType();

    String getThresholdBase();

    String getOperand();

    String getOperandType();

    String getFactor();

    String getFactorType();

    String getRateType();

    String getStatus();

    Journal                      getJournal();
}
