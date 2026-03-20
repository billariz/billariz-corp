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
@Table(name = "BL_BILLING_WINDOW")
public class BillingWindow
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long         id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billingCycleId", referencedColumnName = "billingCycle")
    @ToString.Exclude
    private BillingCycle billingCycle;

    @Column
    private String       billingFrequency;

    @Column
    private String       startDate;

}
