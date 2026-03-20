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
import javax.persistence.OrderBy;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import org.hibernate.annotations.Where;
import com.billariz.corp.database.model.enumeration.BillableChargeContext;
import com.billariz.corp.database.model.enumeration.BillableChargeSource;
import com.billariz.corp.database.model.enumeration.BillableChargeStatus;
import com.billariz.corp.database.model.enumeration.BillableChargeTypeEnum;
import com.billariz.corp.notifier.billableCharge.BillableChargeNotifier;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "BL_BILLABLE_CHARGE")
public class BillableCharge
{
    @Id
    @Column(name = "billableChargeId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long               id;

    @Column
    private String               externalId;

    @Column
    private String               externalInvoiceRef;

    @Column
    private String               billableChargeTypeId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billableChargeTypeId", referencedColumnName = "billableChargeType", insertable = false, updatable = false)
    @ToString.Exclude
    private BillableChargeType   billableChargeType;

    @Column
    private String               posRef;

    @Column
    private LocalDate            startDate;

    @Column
    private LocalDate            endDate;

    @Column
    private LocalDate            externalInvoiceDate;

    @Column
    private LocalDate            receptionDate;

    @Column
    private BigDecimal           amount;

    @Column
    private BillableChargeTypeEnum               type;

    @Column
    private BillableChargeContext               context;

    @Column
    private BillableChargeSource               source;

    @Column
    private String               market;

    @Column
    private String               direction;

    @Column
    private BillableChargeStatus status;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "billableChargeId", referencedColumnName = "billableChargeId")
    @ToString.Exclude
    private List<Article> billableChargeDetails;

    @Column
    private Long            cancelledBy;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "secondObjectId", fetch = FetchType.LAZY)
    @Where(clause = "relationType = 'ACTIVITY_BILLABLE_CHARGE'")
    @ToString.Exclude
    private List<Relation>        relations;

    @OneToMany(mappedBy = "objectId", fetch = FetchType.LAZY)
    @Where(clause = "objectType = 'BILLABLE_CHARGE' AND method = 'ERROR' AND EXISTS (SELECT 1 FROM BL_BILLABLE_CHARGE e WHERE e.billableChargeId = objectId AND e.status = 'IN_FAILURE')")
    @OrderBy("id DESC") 
    @ToString.Exclude
    private List<Journal>        journal;

    @ToString.Exclude
    transient private boolean                cascaded;

    @PrePersist
    @PreUpdate
    void prePersistOrUpdate()
    {
        if(!cascaded)
            BillableChargeNotifier.onPrePersistOrUpdate(this);
    }

    @PostPersist
    @PostUpdate
    void updateChilds()
    {
        if (billableChargeDetails != null)
        billableChargeDetails.forEach(p -> p.setBillableChargeId(getId()));

        if(!cascaded)
            BillableChargeNotifier.onPostPersistOrUpdate(this);
    }
}
