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
import java.util.List;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.MeterRead;
import com.billariz.corp.database.model.MeterReadDetail;
import com.billariz.corp.database.model.enumeration.MeterReadContext;
import com.billariz.corp.database.model.enumeration.MeterReadQuality;
import com.billariz.corp.database.model.enumeration.MeterReadSource;
import com.billariz.corp.database.model.enumeration.MeterReadStatus;
import com.billariz.corp.database.model.enumeration.MeterReadType;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "inMeterRead", types = { MeterRead.class })
public interface inMeterRead {
    Long                  getId();

    String                getOriginalRef();

    String                getPosRef();

    LocalDate             getStartDate();

    LocalDate             getEndDate();

    LocalDate             getReadingDate();

    LocalDate             getReceptionDate();

    MeterReadType         getType();

    MeterReadQuality      getQuality();

    MeterReadContext      getContext();

    MeterReadSource       getSource();

    MeterReadStatus       getStatus();

    BigDecimal            getTotalQuantity();

    String                getUnit();

    String                getClimaticCoef();

    String                getCalorificCoef();

    String                getTouGroup();

   String                getMarket();

   String                getDirection();

   Long                 getCancelledBy();

   Journal              getJournal();

   List<MeterReadDetail> getMeterReadDetails();
}
