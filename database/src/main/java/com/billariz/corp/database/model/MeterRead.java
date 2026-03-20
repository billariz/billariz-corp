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
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import org.hibernate.annotations.Where;
import com.billariz.corp.database.model.enumeration.MeterReadContext;
import com.billariz.corp.database.model.enumeration.MeterReadQuality;
import com.billariz.corp.database.model.enumeration.MeterReadSource;
import com.billariz.corp.database.model.enumeration.MeterReadStatus;
import com.billariz.corp.database.model.enumeration.MeterReadType;
import com.billariz.corp.notifier.meterRead.MeterReadNotifier;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "CC_METER_READ")
public class MeterRead
{
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long                  id;

    @Column
    private String                originalRef;

    @Column
    private String                posRef;

    @Column
    private LocalDate             startDate;

    @Column
    private LocalDate             endDate;

    @Column
    private LocalDate             readingDate;

    @Column
    private LocalDate             receptionDate;

    @Column
    private MeterReadType         type;

    @Column
    private MeterReadQuality      quality;

    @Column
    private MeterReadContext                context;

    @Column
    private MeterReadSource       source;

    @Column
    private MeterReadStatus       status;

    @Column
    private BigDecimal            totalQuantity;

    @Column
    private String                unit;

    @Column
    private String                climaticCoef;

    @Column
    private String                calorificCoef;

    @Column
    private String                touGroup;

    @Column
    private String                market;

    @Column
    private String                direction;

    @Column
    private Long                cancelledBy;

    @OneToMany(mappedBy = "secondObjectId", fetch = FetchType.LAZY)
    @Where(clause = "relationType = 'ACTIVITY_METER_READ'")
    @ToString.Exclude
    private List<Relation>        relations;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "meterReadId", referencedColumnName = "id")
    @ToString.Exclude
    private List<MeterReadDetail> meterReadDetails;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "objectId", referencedColumnName = "id", insertable = false, updatable = false)
    @Where(clause = "objectType = 'METER_READ' AND method = 'ERROR' AND EXISTS (SELECT 1 FROM CC_METER_READ e WHERE e.id = objectId AND e.status = 'IN_FAILURE')")
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
            MeterReadNotifier.onPrePersistOrUpdate(this);
    }

    @PostPersist
    @PostUpdate
    void updateChilds()
    {
        if (meterReadDetails != null)
            meterReadDetails.forEach(p -> p.setMeterReadId(getId()));
        
        if(!cascaded)
            MeterReadNotifier.onPostPersistOrUpdate(this);
    }

    @PostLoad
    void postLoad() 
    {
        this.cascaded = true;
    }
}
