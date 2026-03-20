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
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "BL_BILL_DETAIL")
public class BillDetail
{
    @Id
    @Column(name = "billDetailId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long       id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billId", referencedColumnName = "billId")
    @ToString.Exclude
    private Bill       bill;

    @Column
    private String     billLineCategory;

    @Column
    private String     billLineSubCategory;

    @Column
    private LocalDate  startDate;

    @Column
    private LocalDate  endDate;

    @Column
    private BigDecimal quantity;

    @Column
    private String     quantityUnit;

    @Column
    private BigDecimal price;

    @Column
    private String   priceUnit;

    @Column
    private String   vatRate;

    @Column
    private BigDecimal totalWithoutVat;

    @Column
    private String     lineType;

    @Column
    private String     tou;
}
