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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.springframework.lang.Nullable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = { "id" })
@javax.persistence.Entity
@Table(name = "TR_ORGANISM")
public class Organism
{
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long        id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "companyId", referencedColumnName = "companyId")
    @Nullable
    private Company     company;

    // @OneToMany(cascade = CascadeType.ALL, mappedBy = "organismId", fetch = FetchType.LAZY)
    // private List<Group> groups;

    @Column(name = "isMaster")
    private boolean     master;

    @Column
    private Long        masterOrganismId;

    @Column
    private String      category;

    @Column
    private String      subCategory;

    @Column
    private String      defaultLabel;

    @Column
    private String      standardLabel;

    @Column
    private String      otherLabel;

    @Column
    private String      description;

    @Column
    private String      picture;

}
