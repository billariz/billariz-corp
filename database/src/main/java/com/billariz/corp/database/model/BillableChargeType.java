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
import com.billariz.corp.database.model.enumeration.BillableChargeAquisitionStrategy;
import lombok.Data;

@Data
@Entity
@Table(name = "BL_BILLABLE_CHARGE_TYPE")
public class BillableChargeType
{

    @Id
    @Column(name = "billableChargeType")
    private String id;

    @Column
    private BillableChargeAquisitionStrategy aquisitionStrategy;

    @Column
    private String category;

    @Column
    private String subCategory;

    @Column
    private String standardLabel;

    @Column
    private String otherLabel;

    @Column
    private String defaultLabel;
}
