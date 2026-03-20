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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import com.billariz.corp.database.model.enumeration.CollectionMode;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "PA_COLLECTION_CLASS_CONTROL")
public class CollectionClassControl
{
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long           id;

    @Column
    private String         collectionClassCode;

    @Column
    private String         customerCategory;

    @Column
    private String         customerType;

    @Column
    private String         social;

    @Column
    private String         paymentMode;

    @Column
    private String         intermediate;

    @Column
    private String  debtNature;

    @Column
    private CollectionMode collectionMode;

    @Column
    private int            debtAgeThreshold;

    @Column
    private BigDecimal     debtAmountThreshold;

    @Column
    private int            gracePeriod;

    @Column
    private String         scoreThreshold;

    @Column
    private String         collectionActivityType;

    @Column
    private boolean        active;
}