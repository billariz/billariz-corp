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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import com.billariz.corp.notifier.contractPointOfService.ContractPointOfServiceNotifier;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "CC_CONTRACT_POS")
public class ContractPointOfService
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long           id;

    @Column
    private Long           contractId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractId", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private Contract       contract;

    @Column
    private Long         posId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posId", referencedColumnName = "posId", insertable = false, updatable = false)
    @ToString.Exclude
    private PointOfService pointOfService;

    @Column
    private LocalDate      startDate;

    @Column
    private LocalDate      endDate;

    @PrePersist
    @PreUpdate
    void updateChilds()
    {
        if(this.getEndDate()!=null)
            ContractPointOfServiceNotifier.onPrePersistOrUpdate(this);
    }

    @PostPersist
    @PostUpdate
    void onPostPersistOrUpdate()
    {
        if(this.getEndDate()!=null)
            ContractPointOfServiceNotifier.onPostPersistOrUpdate(this);
    }


}
