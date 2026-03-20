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
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import org.hibernate.annotations.Where;
import com.billariz.corp.database.model.enumeration.TosStatus;
import com.billariz.corp.notifier.termOfservices.TosNotifier;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Schema(description = "Term of Service")
@Table(name = "CC_TERM_OF_SERVICES")
public class TermOfService
{
    @Id
    @Column(name = "tosId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long          id;

    @Column
    private String        tosTypeId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tosTypeId", referencedColumnName = "tosType", insertable = false, updatable = false)
    @ToString.Exclude
    private TermOfServiceType       tosType;

    @Column
    private LocalDate     startDate;

    @Column
    private LocalDate     endDate;

    @Column
    private Long          contractId;

    @Column
    private long          serviceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serviceId", referencedColumnName = "serviceId", insertable = false, updatable = false)
    @ToString.Exclude
    private Service       service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractId", referencedColumnName = "id", insertable = false, updatable = false)
    private Contract      contract;

    @Column
    private String        market;

    @Column
    private String        direction;

    @Column
    private String        touGroup;

    @Column
    private boolean       estimateAuthorized;

    @Column
    private String priceType;

    @Column
    private LocalDate     refDateForFixedPrice;

    @Column
    private Integer       initialDuration;

    @Column
    private Integer       minimumDuration;

    @Column(name = "\"default\"")
    private boolean       tosDefault;

    @Column
    private boolean       master;

    @Column
    private boolean       exclusive;

    @Column
    private TosStatus     status;

    @OneToMany(mappedBy = "objectId", fetch = FetchType.LAZY)
    @Where(clause = "objectType = 'TERM_OF_SERVICE' AND method = 'ERROR' AND EXISTS (SELECT 1 FROM CC_TERM_OF_SERVICES e WHERE e.tosId = objectId AND e.status = 'IN_FAILURE')")
    @OrderBy("id DESC") 
    @ToString.Exclude
    private List<Journal>        journal;

    @ToString.Exclude
    transient private boolean                cascaded;

    @PreUpdate
    void preUpdate()
    {
        TosNotifier.onPreUpdate(this);
    }

    @PrePersist
    void prePersist()
    {
        if(!cascaded)
        TosNotifier.onPrePersist(this);
    }

    @PostPersist
    @PostUpdate
    void postUpdate()
    {
        TosNotifier.onPostPersistOrUpdate(this);
    }
}
