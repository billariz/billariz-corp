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
import com.billariz.corp.database.model.TermOfService;
import com.billariz.corp.database.model.TermOfServiceType;
import com.billariz.corp.database.model.enumeration.TosStatus;

@Projection(name = "inTermOfService", types = { TermOfService.class })
public interface inTermOfService
{
    Long getId();

    String getTosTypeId();

    TermOfServiceType getTosType();

    LocalDate getStartDate();

    LocalDate getEndDate();

    Long getContractId();

    long getServiceId();

    String getMarket();

    String getDirection();

    String getTouGroup();

    boolean getEstimateAuthorized();

    String getPriceType();

    LocalDate getRefDateForFixedPrice();

    Integer getInitialDuration();

    Integer getMinimumDuration();

    boolean getTosDefault();

    boolean getMaster();

    boolean getExclusive();

    TosStatus getStatus();

    Journal                      getJournal();
}
