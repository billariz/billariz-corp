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

import java.time.LocalDate;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "CC_ACTOR")
public class Actor
{
    @Id
    @Column(name = "actorId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long      id;

    @Column
    private long      perimeterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perimeterId", referencedColumnName = "perimeterId", insertable = false, updatable = false)
    @ToString.Exclude
    private Perimeter perimeter;

    @Column //(insertable = false, updatable = false)
    private long      thirdId;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "thirdId", referencedColumnName = "thirdId", insertable = false, updatable = false)
    private Third         third;

    @Column
    private String role;

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

}
