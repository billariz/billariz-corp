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
import com.billariz.corp.database.model.enumeration.ServiceElementTypeCategory;
import lombok.Data;

@Data
@Entity
@Table(name = "BL_SE_TYPE")
public class ServiceElementType
{
    @Id
    @Column(name = "seType")
    private String                     id;

    @Column
    private boolean                    seMaster;

    @Column
    private String                     masterSeTypeId;

    @Column
    private String                     standardLabel;

    @Column
    private String                     otherLabel;

    @Column
    private String                     defaultLabel;

    @Column
    private String                     description;

    @Column
    private ServiceElementTypeCategory seTypeCategory;

    @Column
    private String                     market;

    @Column
    private String                     direction;

    @Column
    private boolean                    metered;

}
