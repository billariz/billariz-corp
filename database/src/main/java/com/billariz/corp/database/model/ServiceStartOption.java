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
import javax.persistence.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Schema(description = "Service Start Option")
@Table(name = "CC_SERVICE_START_OPTION")
public class ServiceStartOption
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long        id;

    @Column
    private String      seller;

    @Column
    private String      channel;

    @Column
    private LocalDate   startDate;

    @Column
    private LocalDate   endDate;

    @Column
    private String      serviceCategory;

    @Column
    private String      serviceSubCategory;

    @Column
    private String      market;

    @Column
    private String      customerType;

    @Column
    private String      customerCategory;

    @Column
    private String      billingMode;

    @Column
    private String      paymentMode;

    @Column
    private String      posCategory;

    @Column
    private String      direction;

    @Column
    private String      dgoCode;

    @Column
    private String      tgoCode;

    @Column
    private String      consumptionThreshold;

    @Column
    private String      touGroup;

    @Column
    private String      service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service", referencedColumnName = "serviceType", insertable = false, updatable = false)
    @ToString.Exclude
    private ServiceType serviceType;

    @Column
    private String      tosType;

}
