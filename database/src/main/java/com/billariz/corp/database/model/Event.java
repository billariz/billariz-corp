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
import java.time.LocalDateTime;
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
import javax.persistence.PostUpdate;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.notifier.event.EventNotifier;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(of = { "id" })
@Entity
@Schema(description = "Event")
@Table(name = "CC_EVENT")
public class Event
{
    @Id
    @Column(name = "eventId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long               id;

    @Column(insertable = false, updatable = false)
    private Long               activityId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "activityId", referencedColumnName = "activityId")
    @ToString.Exclude
    private Activity           activity;

    @ManyToOne
    @JoinColumn(name = "eventType", referencedColumnName = "eventType")
    @ToString.Exclude
    private EventTemplate      type;

    @Column
    private String             action;

    @Column
    private String             launcherTag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "launcherTag", referencedColumnName = "launcherTagType", insertable = false, updatable = false)
    @ToString.Exclude
    private LauncherTagType    tagType;

    @Column
    private int                rank;

    @Column
    private EventStatus        status;

    @Column
    private EventExecutionMode executionMode;

    @Column
    private Long             userId;

    @OneToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "userId", referencedColumnName = "id", insertable = false, updatable = false)
    User userHolder;

    @Column
    private Long             groupId;

    @Column
    private Long             organismId;

    @Column
    private String             externalEventRef;

    @CreatedDate
    @Column
    private LocalDateTime      creationDate;

    @Column
    private LocalDate          triggerDate;

    @Column
    private LocalDateTime     executionDate;

    @OneToMany(mappedBy = "objectId", fetch = FetchType.LAZY)
    @Where(clause = "objectType = 'EVENT' AND method = 'ERROR' AND EXISTS (SELECT 1 FROM CC_EVENT e WHERE e.eventId = objectId AND e.status = 'IN_FAILURE')")
    @OrderBy("id DESC") 
    @ToString.Exclude
    private List<Journal>        journal;

    @ToString.Exclude
    transient private boolean                cascaded;

    @PreUpdate
    void preUpdate()
    {
        if(!cascaded)
            EventNotifier.onPreUpdate(this);
    }

    @PostUpdate
    void postUpdate()
    {
        if(!cascaded)
            EventNotifier.onPostUpdate(this);
    }
}
