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

import java.time.OffsetDateTime;
import java.util.List;
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
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import com.billariz.corp.database.listener.JournalListener;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.model.light.messageCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@EntityListeners(JournalListener.class)
@Table(name = "TR_JOURNAL")
public class Journal 
{
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long             id;

    @Column
    private ObjectType objectType;

    @Column
    private Long             objectId;

    @Column
    private String           userName;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userName", referencedColumnName = "userName", insertable = false, updatable = false)
    User user;

    @Column
    private OffsetDateTime   creationDate;

    @Column
    private String    method;

    @Column
    private String ipAdress;

    @Column
    private String userAgent;

    @Column
    private String forwardedFor;

    @Column
    private String           comment;

    @Column
    private String           newStatus;

    @Column
    private String apiPath;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private List<messageCode> messageCodes;

    transient private List<String> messages;

}
