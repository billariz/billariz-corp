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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import org.hibernate.annotations.Where;
import com.billariz.corp.database.model.enumeration.PointOfServiceDataStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "CC_POS_CAPACITY")
public class PointOfServiceCapacity
{
    @Id
    @Column(name = "posCapacityId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long                         id;

    @Column
    private Long                       posId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posId", referencedColumnName = "posId", insertable = false, updatable = false)
    @ToString.Exclude
    private PointOfService                     pointOfService;

    @Column
    private LocalDate                    startDate;

    @Column
    private LocalDate                    endDate;

    @Column
    private String                       tou;

    @Column(name = "\"value\"")
    private BigDecimal                   value;

    @Column
    private String                       capacityType;

    @Column
    private String                       unit;

    @Column
    private String                       source;

    @Column
    private PointOfServiceDataStatus status;

    @OneToMany(mappedBy = "objectId", fetch = FetchType.LAZY)
    @Where(clause = "objectType = 'POS_CAPACITY' AND method = 'ERROR' AND EXISTS (SELECT 1 FROM CC_POS_CAPACITY e WHERE e.posCapacityId = objectId AND e.status = 'IN_FAILURE')")
    @OrderBy("id DESC") 
    @ToString.Exclude
    private List<Journal>        journal;

}
