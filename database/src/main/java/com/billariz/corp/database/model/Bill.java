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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Where;
import org.springframework.lang.Nullable;
import com.billariz.corp.database.model.enumeration.BillNature;
import com.billariz.corp.database.model.enumeration.BillStatusEnum;
import com.billariz.corp.database.model.enumeration.BillType;
import com.billariz.corp.notifier.bill.BillNotifier;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "BL_BILL")
public class Bill
{
    @Id
    @Column(name = "billId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long             id;

    @Column(name = "billRef")
    private String           reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billingRunId", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private BillingRun       billingRun;

    @Column
    private Long             billingRunId;

    @Column(insertable = false, updatable = false)
    private Long             cancelledBillId;

    @Column
    private Long             cancelledByBillId;

    @Column
    private BillStatusEnum   status;

    @Column
    private BillType         type;

    @Column
    private BillNature       nature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelledBillId", referencedColumnName = "billId")
    @ToString.Exclude
    private Bill             cancelledBill;

    @Column
    private Long             perimeterId;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "contractId", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    @Nullable
    private Contract         contract;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerId", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private Customer         customer;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perimeterId", referencedColumnName = "perimeterId", insertable = false, updatable = false)
    @ToString.Exclude
    private Perimeter        perimeter;

    @Column
    private Long             contractId;

    @Column
    private Long             customerId;

    @Column
    private BigDecimal       totalVat;

    @Column
    private BigDecimal       totalWithoutVat;

    @Column
    private BigDecimal       totalAmount;

    @Column
    private LocalDate        billDate;

    @Column
    private LocalDate        startDate;

    @Column
    private LocalDate        endDate;

    @Column
    private LocalDate        accountingDate;

    @Column
    private Long             groupBillId;

    @Column(name = "\"group\"")
    private Boolean group;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "bill")
    @ToString.Exclude
    private List<BillDetail> details;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "billId", referencedColumnName = "billId")
    @ToString.Exclude
    private List<VatDetail> vatDetails;

    @Formula("(SELECT d.path FROM tr_document d WHERE d.objectType = 'BILL' AND d.type='BILL' AND d.objectId=billId ORDER BY d.id DESC LIMIT 1)")
    private String  path;

    @OneToMany(mappedBy = "objectId", fetch = FetchType.LAZY)
    @Where(clause = "objectType = 'BILL' AND method = 'ERROR' AND EXISTS (SELECT 1 FROM BL_BILL e WHERE e.billId = objectId AND e.status = 'IN_FAILURE')")
    @OrderBy("id DESC") 
    @ToString.Exclude
    private List<Journal>        journal;

    @ToString.Exclude
    transient private boolean                cascaded;

    @ToString.Exclude
    transient private String                action;

    @PrePersist
    void prePersist()
    {
        if(!cascaded)
            BillNotifier.onPrePersist(this);
    }

    @PreUpdate
    void preUpdate()
    {
        if(!cascaded)
            BillNotifier.onPreUpdate(this);
    }

    @PostUpdate
    void postPersistOrUpdate()
    {
        if(!cascaded)
            BillNotifier.onPostPersistOrUpdate(this);
    }

    @PreRemove
    void preRemove() {
        if(!cascaded)
            BillNotifier.onPreRemove(this);
    }

    // INFO Cela inhibe le lancement des avoir
    // @PostLoad
    // void postLoad() 
    // {
    //     this.cascaded = true;
    // }
}
