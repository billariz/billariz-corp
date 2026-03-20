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

@Data
@Entity
@Table(name = "BL_RATE")
public class Rate
{
    @Id
    @Column(name = "rateId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long       id;

    @Column(name = "rateType")
    private String     type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rateType", referencedColumnName = "rateType", insertable = false, updatable = false)
    private RateType   rateType;

    @Column
    private String     priceType;

    @Column
    private String     market;

    @Column
    private LocalDate  startDate;

    @Column
    private LocalDate  endDate;

    @Column
    private String     customerType;

    @Column
    private String     customerCategory;

    @Column
    private String     channel;

    @Column
    private String     installmentFrequency;

    @Column
    private String     serviceCategory;

    @Column
    private String     serviceSubCategory;

    @Column
    private String     posCategory;

    @Column
    private String     tou;

    @Column
    private String     touGroup;

    @Column
    private String     gridRate;

    @Column
    private String     dgoCode;

    @Column
    private String     tgoCode;

    @Column
    private String   unit;

    @Column
    private BigDecimal threshold;

    @Column
    private String     thresholdType;

    @Column
    private String     thresholdBase;

    @Column
    private BigDecimal price;
}
