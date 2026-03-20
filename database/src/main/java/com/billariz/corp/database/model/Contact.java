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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "CC_CONTACT")
public class Contact
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contactId")
    private Long   id;

    @Column
    private String phone1;

    @Column
    private String phone2;

    @Column
    private String phone3;

    @Column
    private String email;

    @Column
    private String fax;

    @Column
    private String deliveryModeCode;
}
