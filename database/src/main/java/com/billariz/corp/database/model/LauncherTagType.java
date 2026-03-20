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
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "TR_LAUNCHER_TAG_TYPE")
public class LauncherTagType
{
    @Id
    @Column(name = "launcherTagType")
    private String             id;

    @Column
    private Long               rank;

    @Column
    private String             defaultLabel;

    @Column
    private String             category;

    @Column
    private String             subCategory;

    @Column
    private String             standardLabel;

    @Column
    private String             otherLabel;

    @Column
    private EventExecutionMode executionMode;

    @Column
    private boolean            synchronous;

    @Column
    private int                packetSize;

    @Column
    private boolean            active;

}
