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
import org.springframework.data.rest.core.config.Projection;
import com.billariz.corp.database.model.Article;
import com.billariz.corp.database.model.BillableCharge;
import com.billariz.corp.database.model.BillableChargeType;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.Relation;
import com.billariz.corp.database.model.enumeration.BillableChargeContext;
import com.billariz.corp.database.model.enumeration.BillableChargeSource;
import com.billariz.corp.database.model.enumeration.BillableChargeStatus;
import com.billariz.corp.database.model.enumeration.BillableChargeTypeEnum;

@Projection(name = "inBillableCharge", types = { BillableCharge.class })
public interface inBillableCharge {

    Long               getId();

    String               getExternalId();

    String               getExternalInvoiceRef();

    String               getBillableChargeTypeId();

    BillableChargeType   getBillableChargeType();

    String               getPosRef();

    LocalDate            getStartDate();

    LocalDate            getEndDate();

    LocalDate            getExternalInvoiceDate();

    LocalDate            getReceptionDate();

    BigDecimal           getAmount();

    BillableChargeTypeEnum               getType();

    BillableChargeContext               getContext();

    BillableChargeSource               getSource();

    String               getMarket();

    String               getDirection();

    BillableChargeStatus getStatus();

    List<Article> getBillableChargeDetails();

    Long            getCancelledBy();

    List<Relation>        getRelations();

    Journal getJournal();
    
}
