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

import com.billariz.corp.database.model.Bill;
import com.billariz.corp.database.model.BillDetail;
import com.billariz.corp.database.model.BillingRun;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.Customer;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.Perimeter;
import com.billariz.corp.database.model.VatDetail;
import com.billariz.corp.database.model.enumeration.BillNature;
import com.billariz.corp.database.model.enumeration.BillStatusEnum;
import com.billariz.corp.database.model.enumeration.BillType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "inBill", types = { Bill.class })
public interface inBill {
    
    Long             getId();

    String           getReference();

    BillingRun       getBillingRun();

    Long             getCancelledByBillId();

    BillStatusEnum   getStatus();

    BillType         getType();

    BillNature       getNature();

    Bill             getCancelledBill();

    Contract         getContract();

    Customer         getCustomer();

    Perimeter        getPerimeter();

    BigDecimal       getTotalVat();

    BigDecimal       getTotalWithoutVat();

    BigDecimal       getTotalAmount();

    LocalDate        getBillDate();

    LocalDate        getStartDate();

    LocalDate        getEndDate();

    LocalDate        getAccountingDate();

    Long             getGroupBillId();

    Boolean getGroup();

    List<BillDetail> getDetails();

    List<VatDetail> getVatDetails();

    String  getPath();

     Journal getJournal();

}
