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
import com.billariz.corp.database.model.BillSegment;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.ServiceElement;
import com.billariz.corp.database.model.enumeration.BillSegmentStatus;
import com.billariz.corp.database.model.enumeration.MeterReadQuality;

@Projection(name = "inBillSegment", types = { BillSegment.class })

public interface inBillSegment
{
    Long              getId();

    ServiceElement    getSe();

    String            getSeType();

    LocalDate         getStartDate();

    LocalDate         getEndDate();

    BigDecimal        getQuantity();

    String            getSchema();

    String            getQuantityUnit();

    String            getQuantityThreshold();

    String            getQuantityThresholdBase();

    BigDecimal        getPrice();

    String          getPriceUnit();

    String            getPriceThreshold();

    String            getPriceThresholdBase();

    BigDecimal        getAmount();

    String            getTou();

    String            getTouGroup();

    String          getVatRate();

    MeterReadQuality  getNature();

    BillSegmentStatus getStatus();

    Long              getBillId();

    Long              getMeterReadId();

    Long              getArticleId();

    Long              getCancelledBy();

    Journal             getJournal();

}