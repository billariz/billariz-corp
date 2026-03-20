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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import com.billariz.corp.database.model.enumeration.ObjectType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "TR_OBJECT_PROCESS_RULES")
public class ObjectProcessRule
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long       id;

    @Column
    private ObjectType objectType;

    @Column
    private String     newStatus;

    @Column
    private String     initialStatus;

    @Column
    private String     seller;

    @Column
    private String     channel;

    @Column
    private String     direction;

    @Column
    private String     serviceCategory;

    @Column
    private String     serviceSubCategory;

    @Column
    private String     customerCategory;

    @Column
    private String     market;

    @Column
    private String     finalStatus;

    @Column
    private String     activityType;
}