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

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "TR_EVENT_MANAGER")
public class EventManager
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long               id;

    @Column
    private OffsetDateTime     createdAt;

    @Column
    private Long               rank;

    @ManyToOne
    @JoinColumn(name = "launcherTagType", referencedColumnName = "launcherTagType")
    private LauncherTagType    launcherTagType;

    @Column
    private String             defaultLabel;

    @Column
    private EventExecutionMode executionMode;

}
