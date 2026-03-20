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
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "CC_ACTIVITY_TEMPLATE")
public class ActivityTemplate
{
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long           id;

    @Column
    private String         activityType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "activityType", referencedColumnName = "activityType", insertable = false, updatable = false)
    private ActivityType   type;

    @Column
    private int            rank;

    @Column
    private ActivityStatus defaultStatus;

    @Column
    private int            startDatePeriod;

    @Column
    private String         eventType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "eventType", referencedColumnName = "eventType", insertable = false, updatable = false)
    private EventTemplate  eventTemplate;
}
