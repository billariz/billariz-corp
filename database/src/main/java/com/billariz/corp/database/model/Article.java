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
import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.ToString;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "BL_ARTICLE")
public class Article {

    @Id
    @Column(name = "articleId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long billableChargeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billableChargeId", referencedColumnName = "billableChargeId", insertable = false, updatable = false)
    @ToString.Exclude
    private BillableCharge billableCharge;

    @Column
    private String externalArticleId;

    @Column
    private String articleTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "articleTypeId", referencedColumnName = "articleType", insertable = false, updatable = false)
    @ToString.Exclude
    private ArticleType   articleType;

    @Column
    private LocalDate startDate;
    
    @Column
    private LocalDate endDate;
    
    @Column
    private LocalDate effectiveDate;

    @Column
    private String tou;

    @Column
    private BigDecimal unitPrice;
    
    @Column
    private String unitOfUnitPrice;

    @Column
    private BigDecimal quantity;

    @Column
    private String unitOfQuantity;

    @Column
    private BigDecimal amount;

    @Column
    private String vatRate;

    @Column
    private String status;

    @Column
    private Long billId;

}
