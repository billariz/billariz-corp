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
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import com.billariz.corp.database.model.enumeration.EventStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "CC_EVENT_TEMPLATE")
public class EventTemplate
{
    @Id
    @Column(name = "eventType")
    private String               id;

    @Column
    private String               defaultLabel;

    @Column
    private String   defaultExecutionMode;

    @Column
    private String               category;

    @Column
    private String               subCategory;

    @Column
    private String               launcherTagType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "launcherTagType", referencedColumnName = "launcherTagType", insertable = false, updatable = false)
    private LauncherTagType      tagType;

    @Column
    private EventStatus          defaultStatus;

    @Column
    private String               action;

    @Column
    private Long               defaultHolder;

    @Column
    private Long               groupId;

    @Column
    private Long               organismId;

    @Column
    private String              triggerDateMode;

    @Column
    private String               periodSystem;

    @Column
    private String               standardLabel;

    @Column
    private String               otherLabel;

    @Column
    private int                  recurrencePeriod;

    @Column
    private String               recurrencePeriodType;
}
