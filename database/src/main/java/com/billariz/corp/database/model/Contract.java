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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import org.hibernate.annotations.Where;
import com.billariz.corp.notifier.contract.ContractNotifier;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "CC_CONTRACT")
public class Contract
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long                         id;

    @Column
    private String                       reference;

    @Column
    private String                       status;

    @Column
    private String                       market;

    @Column
    private LocalDate                    contractualStartDate;

    @Column
    private LocalDate                    contractualEndDate;

    @Column
    private String       installPeriodicity;

    @Column
    private String              billingMode;

    @Column
    private String         billingFrequency;

    @Column
    private String                       billingCycleId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billingCycleId", referencedColumnName = "billingCycle", insertable = false, updatable = false)
    @ToString.Exclude
    private BillingCycle                 billingCycle;

    @Column
    private LocalDate                    billAfterDate;

    @Column
    private String                       channel;

    @Column
    private String                       seller;

    @Column
    private String                       serviceCategory;

    @Column
    private String                       serviceSubCategory;

    @Column
    private String                       direction;

    @Column
    private LocalDate                    effectiveEndDate;

    @Column
    private LocalDate                    subscriptionDate;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "contract")
    @ToString.Exclude
    private ContractPerimeter            contractPerimeter;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "contract")
    private List<ContractPointOfService> contractPointOfServices;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "contractId", referencedColumnName = "id")
    private List<Service>                services;

    @OneToMany(mappedBy = "objectId", fetch = FetchType.LAZY)
    @Where(clause = "objectType = 'CONTRACT' AND method = 'ERROR' AND EXISTS (SELECT 1 FROM CC_CONTRACT e WHERE e.id = objectId AND e.status = 'IN_FAILURE')")
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
            ContractNotifier.onPrePersistOrUpdate(this);
    }

    @PostPersist
    @PostUpdate
    void updateChilds()
    {
        if (contractPointOfServices != null)
            contractPointOfServices.forEach(c -> c.setContractId(getId()));
        if (services != null)
            services.forEach(s -> s.setContractId(getId()).setContract(this).setCascaded(true));
        
        if(!cascaded)
            ContractNotifier.onPostPersistOrUpdate(this);
    }
}