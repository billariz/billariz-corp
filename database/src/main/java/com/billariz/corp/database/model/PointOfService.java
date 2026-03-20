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

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "CC_POS")
public class PointOfService
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "posId")
    private Long                            id;

    @Column
    String                                  reference;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "addressId", referencedColumnName = "addressId")
    private Address                           address;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "pointOfService", optional = true)
    @JoinColumn( insertable = false, updatable = false)
    @ToString.Exclude
    private ContractPointOfService      contractPointOfService;

    @Column
    private String                            market;

    @Column
    private String                            tgoCode;

    @Column
    private String                            dgoCode;

    @Column
    private String                            deliveryState;

    @Column
    private String                            deliveryStatus;

    @Column
    private boolean                           temporaryConnection;

    @Column
    private String                            temporaryConnectionType;

    @Column
    private String                            direction;

    @Column
    private String                            readingCycleId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "readingCycleId", referencedColumnName = "readingCycle", insertable = false, updatable = false)
    @ToString.Exclude
    private ReadingCycle                 readingCycle;

    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.LAZY)
    @JoinColumn(name = "posId", referencedColumnName = "posId")
    private List<PointOfServiceCapacity>      capacities;

    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.LAZY)
    @JoinColumn(name = "posId", referencedColumnName = "posId")
    private List<PointOfServiceConfiguration> configurations;

    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.LAZY)
    @JoinColumn(name = "posId", referencedColumnName = "posId")
    private List<PointOfServiceEstimate>      estimates;

    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.LAZY)
    @JoinColumn(name = "posId", referencedColumnName = "posId")
    private List<Meter>                       meters;

}
