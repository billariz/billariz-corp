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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import com.billariz.corp.database.model.enumeration.SeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Entity
@Schema(description = "Service Element Start Options")
@Table(name = "BL_SE_START_OPTION")
public class ServiceElementStartOption
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long                 id;

    @Column
    private String               tosType;

    @ManyToOne
    @JoinColumn(name = "seType", referencedColumnName = "seType", insertable = false, updatable = false)
    private ServiceElementType               seType;

    @Column(name = "seType")
    private String               serviceElementType;

    @Column
    private LocalDate            startDate;

    @Column
    private LocalDate            endDate;

    @Column
    private String             vatRate;

    @Column
    private String               premiseCondition;

    @Column
    private String               premiseValue;

    @Column
    private String               rateType;

    @Column
    private String               operand;

    @Column
    private String               operandType;

    @Column
    private String               factor;

    @Column
    private String               factorType;

    @Column
    private String    billingScheme;

    @Column
    private String accountingScheme;

    @Column
    private boolean              estimateAuthorized;

    @Column
    private String  sqType;

    @Column
    private String               touGroup;

    @Column
    private String               tou;

    @Column
    private SeStatus             defaultSeStatus;

    @Column
    private Integer              minDayForEstimate;

    @Column
    private String               analyticCode;

    @Column
    private String               additionalCode;

    @Column
    private String               threshold;

    @Column
    private String               thresholdType;

    @Column
    private String               thresholdBase;

    @Column
    private String               seListBaseForSq;

    @Column
    private String               category;

    @Column
    private String               subCategory;
}
