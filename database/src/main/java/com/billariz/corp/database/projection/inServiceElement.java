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
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.ServiceElementType;
import com.billariz.corp.database.model.ServiceElement;

@Projection(name = "inServiceElement", types = { ServiceElement.class })
public interface inServiceElement
{
    Long getId();

    Long getTosId();

    String getSeTypeId();

    ServiceElementType getSeType();

    boolean getMaster();

    String getVatRate();

    String getRateType();

    String getOperand();

    String getOperandType();

    String getFactor();

    String getFactorType();

    boolean getMetered();

    String getBillingScheme();

    String getAccountingScheme();

    boolean getEstimateAuthorized();

    String getTouGroup();

    String getTou();

    LocalDate getStartDate();

    LocalDate getEndDate();

    String getStatus();

    Integer getMinDayForEstimate();

    String getSeListBaseForSq();

    String getThreshold();

    String getThresholdType();

    String getThresholdBase();

    String getSqType();

    String getCategory();

    String getSubCategory();

    Journal   getJournal();
}
