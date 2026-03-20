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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "RF_MARKET_GEO_REF")
public class MarketGeoRef
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long       id;

    @Column
    private String     areaCode;

    @Column
    private String     market;

    @Column
    private LocalDate  startDate;

    @Column
    private LocalDate  endDate;

    @Column
    private String     dgoRank;

    @Column
    private String     dispatchRate;

    @Column(name = "dgoRank_1")
    private String     dgoRank1;

    @Column(name = "dgoRank_2")
    private String     dgoRank2;

    @Column
    private String     netAreaCode;

    @Column
    private String     netAreaLabel;

    @Column
    private String     weatherChannelCode;

    @Column
    private String     climaticZone;

    @Column
    private BigDecimal proximityRateCoef;

    @Column
    private String     tgoCode;

    @Column
    private String     nbrOfPos;

    @Column
    private String     energyNature;

    @Column
    private BigDecimal regionalRateLevel;

    @Column
    private String     balanceZone;

    @Column
    private String     coefA;
}
