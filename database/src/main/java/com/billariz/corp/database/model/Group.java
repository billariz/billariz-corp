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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "TR_GROUP")
public class Group
{
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long   id;

    @Column
    private Long   organismId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organismId", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private Organism   organism;

    @Column(name = "\"group\"")
    private String groupName;

    @Column
    private String category;

    @Column
    private String subCategory;

    @Column
    private String defaultLabel;

    @Column
    private String standardLabel;

    @Column
    private String otherLabel;

    @Column
    private String description;

    @Column(name = "\"authorization\"")
    private String authorizations;

}
