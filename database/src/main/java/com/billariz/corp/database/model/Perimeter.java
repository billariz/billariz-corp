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
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.Table;
import org.hibernate.annotations.Where;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "CC_PERIMETER")
public class Perimeter
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "perimeterId")
    private Long                    id;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "perimeter", fetch = FetchType.LAZY)
    private List<Actor>             actors;

    @Column
    private String                  reference;

    @Column
    private long                    customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerId", referencedColumnName = "id", insertable = false, updatable = false)
    private Customer                customer;

    @Column
    private LocalDate               startDate;

    @Column
    private LocalDate               endDate;

    @Column
    private String                  analyticCode;

    @Column
    private String                  perimeterType;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "perimeterType", referencedColumnName = "perimeterType", insertable = false, updatable = false)
    private PerimeterType           type;

    @Column
    private String                       billingCycleId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billingCycleId", referencedColumnName = "billingCycle", insertable = false, updatable = false)
    @ToString.Exclude
    private BillingCycle                 billingCycle;

    @Column
    private LocalDate               billAfterDate;

    @Column
    private String    billingFrequency;

    @Column
    private String                  market;

    @Column
    private String                  status;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "perimeter")
    private List<ContractPerimeter> contractPerimeters;

    @OneToMany(mappedBy = "objectId", fetch = FetchType.LAZY)
    @Where(clause = "objectType = 'PERIMETER' AND method = 'ERROR' AND EXISTS (SELECT 1 FROM CC_PERIMETER e WHERE e.perimeterId = objectId AND e.status = 'IN_FAILURE')")
    @OrderBy("id DESC") 
    @ToString.Exclude
    private List<Journal>        journal;

    @PostPersist
    @PostUpdate
    void updateChilds()
    {
        if (actors != null)
            actors.forEach(a -> a.setPerimeterId(getId()));
        if (contractPerimeters != null)
            contractPerimeters.forEach(c -> c.setPerimeter(this));
    }
}
