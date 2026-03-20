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
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "VW_BILL_OVERVIEW")
public class BillOverview
{
    @Id
    @Column
    private Long   unique_id;

    @Column
    private Long   billingRunId;

    @Column
    private String status;

    @Column
    private Long   totalCount;

    @Column
    private int    totalAmount;

    @Column
    private Long   totalAmountVated;
}
