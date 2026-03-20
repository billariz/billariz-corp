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
import javax.persistence.Table;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "CC_METER_READ_DETAIL")
public class MeterReadDetail
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long             id;

    // @OneToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "meterReadId", referencedColumnName = "id", insertable = false, updatable = false)
    // @ToString.Exclude 
    // private MeterRead meterRead;

    @Column
    private Long             meterReadId;

    @Column
    private LocalDate        startDate;

    @Column
    private LocalDate        endDate;

    @Column
    private String           startValue;

    @Column
    private String           endValue;

    @Column
    private BigDecimal       quantity;

    @Column
    private String           unit;

    @Column
    private String           tou;

    @Column
    private String           measureType;

    @Column
    private String           gridCode;
}
