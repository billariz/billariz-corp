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

import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import com.billariz.corp.notifier.user.UserNotifier;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Table(name = "TR_USER")
public class User implements Serializable
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long       id;

    @Column(unique = true, nullable = false)
    private String     userName;

    @Column
    private String     userRole;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "individualId", referencedColumnName = "individualId")
    private Individual individual;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "contactId", referencedColumnName = "contactId")
    private Contact    contact;

    @Column
    private Long       groupId;

    @Column
    private Long       organismId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupId", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private Group      group;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organismId", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private Organism   organism;

    @Column
    private String     picture;

    @Column
    private String     defaultLanguage;

    @Column
    private String     status;

    @Column
    private boolean    master;

    @Column
    private boolean    readOnly;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "TR_USER_ROLE",
        joinColumns = @JoinColumn(name = "userId"),
        inverseJoinColumns = @JoinColumn(name = "roleId")
    )
    private List<Role> roles;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "TR_USER_PERMISSION",
            joinColumns = @JoinColumn(name = "userId"),
            inverseJoinColumns = @JoinColumn(name = "permissionId")
    )
    private List<Permission> permissions;

    @ToString.Exclude
    transient private boolean                cascaded;

    @PrePersist
    @PreUpdate
    void prePersistOrUpdate()
    {
        if(!cascaded)
            UserNotifier.onPrePersistOrUpdate(this);
    }

    @PreRemove
    void preRemove() {
        UserNotifier.onPreRemove(this);
    }


    @PostPersist
    @PostUpdate
    void postPersistOrUpdate()
    {
        if(!cascaded)
            UserNotifier.onPostPersistOrUpdate(this);
    }


}
