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
@Table(name = "PA_ADHOC_PAYMENT")
public class AdhocPayment
{
    @Id
    @Column(name = "adhocPaymentId")
    private String     id;

    @Column
    private LocalDate  creationDate;

    @Column
    private LocalDate  dueDate;

    @Column
    private LocalDate  startDate;

    @Column
    private LocalDate  endDate;

    @Column
    private String     paymentStatus;

    @Column
    private String     paymentNature;

    @Column
    private String     paymentMode;

    @Column
    private BigDecimal amount;

    @Column
    private String     externalReference;

    @Column
    private String     intermediate;

    @Column
    private String     initialTransactionId;

    @Column
    private String     initialTransactionStatus;

    @Column
    private Long       contractId;

    @Column
    private String     pieceReference;

    @Column
    private Long       processId;

    @Column
    private BigDecimal amountToPay;

    @Column
    private BigDecimal amountPaid;

    @Column
    private String     sepaMandate;
}
