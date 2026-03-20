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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import org.hibernate.annotations.Where;
import com.billariz.corp.database.model.enumeration.BillSegmentStatus;
import com.billariz.corp.database.model.enumeration.MeterReadQuality;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "BL_BILL_SEGMENT")
public class BillSegment
{
    @Id
    @Column(name = "billSegmentId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long              id;

    @Column
    private Long              seId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seId", referencedColumnName = "seId", insertable = false, updatable = false)
    @ToString.Exclude
    private ServiceElement    se;

    @Column
    private String            seType;

    @Column
    private LocalDate         startDate;

    @Column
    private LocalDate         endDate;

    @Column
    private BigDecimal        quantity;

    @Column
    private String            schema;

    @Column
    private String            quantityUnit;

    @Column
    private String            quantityThreshold;

    @Column
    private String            quantityThresholdBase;

    @Column
    private BigDecimal        price;

    @Column
    private String          priceUnit;

    @Column
    private String            priceThreshold;

    @Column
    private String            priceThresholdBase;

    @Column
    private BigDecimal        amount;

    @Column
    private String            tou;

    @Column
    private String            touGroup;

    @Column
    private String          vatRate;

    @Column
    private MeterReadQuality  nature;

    @Column
    private BillSegmentStatus status;

    @Column
    private Long              billId;

    @Column
    private Long              meterReadId;

    @Column
    private Long              articleId;

    @Column
    private Long              cancelledBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meterReadId", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private MeterRead         meterRead;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "articleId", referencedColumnName = "articleId", insertable = false, updatable = false)
    @ToString.Exclude
    private Article         article;

    @OneToMany(mappedBy = "objectId", fetch = FetchType.LAZY)
    @Where(clause = "objectType = 'BILL_SEGMENT' AND method = 'ERROR' AND EXISTS (SELECT 1 FROM BL_BILL_SEGMENT e WHERE e.billSegmentId = objectId AND e.status = 'IN_FAILURE')")
    @OrderBy("id DESC") 
    @ToString.Exclude
    private List<Journal>        journal;
}
