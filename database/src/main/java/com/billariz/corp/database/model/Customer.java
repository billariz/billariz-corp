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

import java.time.LocalDateTime;
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
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.lang.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Schema(description = "Customer")
@Table(name = "CC_CUSTOMER")
public class Customer
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long            id;

    @Column(updatable = false, insertable = true, nullable = false, unique = true)
    private String          reference;

    @Column
    private String          category;

    @Column
    private String          languageCode;

    @Column
    private String          type;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "companyId", referencedColumnName = "companyId")
    @Nullable
    private Company         company;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "individualId", referencedColumnName = "individualId")
    private Individual      individual;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "contactId", referencedColumnName = "contactId")
    private Contact         contact;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "addressId", referencedColumnName = "addressId")
    private Address         address;

    @Column
    private String          status;

    @Column
    @CreationTimestamp
    private LocalDateTime   creationDate;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "customer", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Perimeter> perimeters;

    @PostPersist
    @PostUpdate
    void updateChilds()
    {
        if (perimeters != null)
            perimeters.forEach(p -> p.setCustomerId(getId()));
    }
}

