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
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "VW_BILLING_RUN_OVERVIEW")
public class BillingRunOverview
{
    @Id
    @Column
    private Long       unique_id;

    @Column
    private Long       billingRunId;

    @Column
    private String     step;

    @Column
    private String     status;

    @Column
    private Long       statusCount;

    @Column
    private BigDecimal statusPercent;

    @Column
    private Long       total;
}
