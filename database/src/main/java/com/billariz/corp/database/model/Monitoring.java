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
import javax.persistence.Table;
import com.billariz.corp.database.model.enumeration.MonitoringAction;
import com.billariz.corp.database.model.enumeration.MonitoringObject;
import com.billariz.corp.database.model.enumeration.MonitoringStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "TR_MONITORING")
public class Monitoring
{
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long             id;

    @Column
    private MonitoringObject objectType;

    @Column
    private Long             objectId;

    @Column
    private String           fileName;

    @Column
    private MonitoringAction action;

    @Column
    private MonitoringStatus status;

    @Column
    private String           message;

    @Column
    private OffsetDateTime   date;
}
