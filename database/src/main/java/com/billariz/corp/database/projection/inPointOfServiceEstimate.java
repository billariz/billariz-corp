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
import com.billariz.corp.database.model.PointOfService;
import com.billariz.corp.database.model.PointOfServiceEstimate;
import com.billariz.corp.database.model.enumeration.PointOfServiceDataStatus;

@Projection(name = "inPointOfServiceEstimate", types = { PointOfServiceEstimate.class })
public interface inPointOfServiceEstimate {
    Long      getId();

    Long    getPosId();

    LocalDate getStartDate();

    LocalDate getEndDate();

    String    getTou();

    BigDecimal    getValue();

    String    getEstimateType();

    String    getUnit();

    String    getSource();

    PointOfServiceDataStatus    getStatus();

    PointOfService             getPointOfService();

    Journal                      getJournal();
}
