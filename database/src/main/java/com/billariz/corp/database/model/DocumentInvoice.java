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

package com.billariz.corp.database.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "PA_ACCOUNTING_DOCUMENT_INVOICE")
public class DocumentInvoice
{
    @Id
    @Column(name = "billId")
    private String     id;

    @Column
    private String     cancelledBillId;

    @Column
    private LocalDate  billDate;

    @Column
    private LocalDate  dueDate;

    @Column
    private String     billNature;

    @Column
    private LocalDate  endDate;

    @Column
    private LocalDate  startDate;

    @Column
    private LocalDate  runDate;

    @Column
    private String     runId;

    @Column
    private String     paymentMode;

    @Column
    private Long       contractId;

    @Column
    private BigDecimal totalAmount;

    @Column
    private String     vatNR;

    @Column
    private String     vatRR;

    @Column
    private BigDecimal totalVat;

    @Column
    private BigDecimal totalWithoutVat;

    @Column
    private String     status;

    @Column
    private Long       processId;

    @Column
    private BigDecimal amountPaid;

    @Column
    private BigDecimal amountToPay;
}
