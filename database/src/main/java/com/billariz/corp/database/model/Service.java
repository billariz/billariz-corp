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
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import org.hibernate.annotations.Where;
import com.billariz.corp.database.model.enumeration.ServiceStatus;
import com.billariz.corp.notifier.service.ServiceNotifier;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Schema(description = "Service")
@Table(name = "CC_SERVICE")
public class Service
{
    @Id
    @Column(name = "serviceId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long          id;

    @Column
    private long          contractId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractId", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private Contract      contract;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serviceTypeId", referencedColumnName = "serviceType", insertable = false, updatable = false)
    @ToString.Exclude
    private ServiceType   serviceType;

    @Column
    private String        serviceTypeId;

    @Column
    private LocalDate     startDate;

    @Column
    private LocalDate     endDate;

    @Column
    private String        touGroup;

    @Column
    private String        tou;

    @Column
    private String        direction;

    @Column
    private BigDecimal    amount;

    @Column
    private String        threshold;

    @Column
    private String        thresholdType;

    @Column
    private String        thresholdBase;

    @Column
    private String        operand;

    @Column
    private String        operandType;

    @Column
    private String        factor;

    @Column
    private String        factorType;

    @Column
    private String        rateType;

    @Column
    private ServiceStatus status;

    @OneToMany(mappedBy = "objectId", fetch = FetchType.LAZY)
    @Where(clause = "objectType = 'SERVICE' AND method = 'ERROR' AND EXISTS (SELECT 1 FROM CC_SERVICE e WHERE e.serviceId = objectId AND e.status = 'IN_FAILURE')")
    @OrderBy("id DESC") 
    @ToString.Exclude
    private List<Journal>        journal;

    @ToString.Exclude
    transient private boolean                cascaded;

    @PrePersist
    void prePersist()
    {
        if(!cascaded && (getServiceType()==null || !getServiceType().isDefaultService()))
            ServiceNotifier.onPrePersistOrUpdate(this);
    }

    @PostPersist
    void postPersist()
    {
        if(!cascaded && (getServiceType()==null || !getServiceType().isDefaultService()))
            ServiceNotifier.onPostPersistOrUpdate(this);
    }

    @PreUpdate
    void preUpdate()
    {
        if(!cascaded)
            ServiceNotifier.onPrePersistOrUpdate(this);
    }

    @PostUpdate
    void postUpdate()
    {
        if(!cascaded)
            ServiceNotifier.onPostPersistOrUpdate(this);
    }
}
