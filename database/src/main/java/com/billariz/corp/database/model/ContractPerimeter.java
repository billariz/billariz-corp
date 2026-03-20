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
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "CC_CONTRACT_PERIMETER")
public class ContractPerimeter
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long      id;

    @Column(insertable = false, updatable = false)
    private long      contractId;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "contractId", referencedColumnName = "id")
    private Contract  contract;

    @Column(insertable = false, updatable = false)
    private long      perimeterId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perimeterId", referencedColumnName = "perimeterId")
    private Perimeter perimeter;

    @CreatedDate
    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;
}