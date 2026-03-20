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

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "CC_METER")
public class Meter
{
    @Id
    @Column(name = "meterId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long      id;

    @Column
    private Long    posId;

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Column
    private String    meterNumber;

    @Column
    private String    meterType;

    @Column
    private String    smartMeterStatus;

    @Column
    private String    convertCoef;

    @Column
    private String    digitNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posId", referencedColumnName = "posId", insertable = false, updatable = false)
    @ToString.Exclude
    private PointOfService  pointOfService;
}
