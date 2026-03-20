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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Where;
import com.billariz.corp.database.model.enumeration.BillType;
import com.billariz.corp.notifier.billingRun.BillingRunNotifier;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "BL_BILLING_RUN")
public class BillingRun
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long                     id;

    @Column
    private BillType                billType;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billingCycleId", referencedColumnName = "billingCycle")
    @ToString.Exclude
    private BillingCycle             billingCycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billingWindowId", referencedColumnName = "id")
    @ToString.Exclude
    private BillingWindow            billingWindow;

    @OneToMany(mappedBy = "billingRunId")
    @Where(clause = "status = 'COMPLETED'")
    @ToString.Exclude
    private List<BillingRunOverview> billingRunOverview;

    @OneToMany(mappedBy = "billingRunId")
    @ToString.Exclude
    private List<BillOverview>       billOverview;

    @Formula("(SELECT COUNT(*) FROM cc_contract c WHERE c.billingCycleId = billingCycleId AND c.status='EFFECTIVE')")
    private int                      contractCount;

    @Column
    private LocalDate                runDate;

    @Column
    private LocalDate                startDate;

    @Column
    private LocalDate                endDate;

    @Column
    private String                   status;

    @ToString.Exclude
    transient private boolean                cascaded;

    @PrePersist
    @PreUpdate
    void prePersistOrUpdate()
    {
        if(!cascaded)
            BillingRunNotifier.onPrePersistOrUpdate(this);
    }

    @PostPersist
    @PostUpdate
    void postPersistOrUpdate()
    {
        if(!cascaded)
            BillingRunNotifier.onPostPersistOrUpdate(this);
    }

}
