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
import javax.persistence.Table;
import org.hibernate.annotations.Where;
import com.billariz.corp.database.model.enumeration.SeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@EqualsAndHashCode(of = { "id" })
@Schema(description = "Service Element")
@Table(name = "BL_SE")
public class ServiceElement
{
    @Id
    @Column(name = "seId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long                 id;

    @Column
    private Long                 tosId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tosId", referencedColumnName = "tosId", insertable = false, updatable = false)
    @ToString.Exclude
    private TermOfService       tos;

    @Column
    private String               seTypeId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seTypeId", referencedColumnName = "seType", insertable = false, updatable = false)
    @ToString.Exclude
    private ServiceElementType               seType;

    @Column(name = "seMaster")
    private boolean              master;

    @Column
    private String             vatRate;

    @Column
    private String               rateType;

    @Column
    private String               operand;

    @Column
    private String               operandType;

    @Column
    private String               factor;

    @Column
    private String               factorType;

    @Column
    private boolean              metered;

    @Column
    private String    billingScheme;

    @Column
    private String  accountingScheme;

    @Column
    private boolean              estimateAuthorized;

    @Column
    private String               touGroup;

    @Column
    private String               tou;

    @Column
    private LocalDate            startDate;

    @Column
    private LocalDate            endDate;

    @Column
    private SeStatus             status;

    @Column
    private Integer              minDayForEstimate;

    @Column
    private String               seListBaseForSq;

    @Column
    private String               threshold;

    @Column
    private String               thresholdType;

    @Column
    private String               thresholdBase;

    @Column
    private String  sqType;

    @Column
    private String               category;

    @Column
    private String               subCategory;

    @OneToMany(mappedBy = "objectId", fetch = FetchType.LAZY)
    @Where(clause = "objectType = 'SERVICE_ELEMENT' AND method = 'ERROR' AND EXISTS (SELECT 1 FROM BL_SE e WHERE e.seId = objectId AND e.status = 'IN_FAILURE')")
    @OrderBy("id DESC") 
    @ToString.Exclude
    private List<Journal>        journal;
}
