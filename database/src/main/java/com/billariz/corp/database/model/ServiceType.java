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

import java.io.Serializable;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "CC_SERVICE_TYPE")
public class ServiceType implements Serializable
{
    @Id
    @Column(name = "serviceType")
    private String      id;

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Column
    private String    category;

    @Column
    private String    subCategory;

    @Column
    private String    defaultLabel;

    @Column
    private String    standardLabel;

    @Column
    private String    otherLabel;

    @Column
    private String    description;

    @Column(name = "isDefaultService")
    private boolean   defaultService;

}
