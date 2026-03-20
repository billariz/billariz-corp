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
import javax.persistence.Table;
import org.hibernate.annotations.Where;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "CC_ACTIVITY")
public class Activity
{
    @Id
    @Column(name = "activityId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long           id;

    @Column(name = "activityType")
    private String         type;

    @OneToOne
    @JoinColumn(name = "activityType", referencedColumnName = "activityType", insertable = false, updatable = false)
    @ToString.Exclude
    private ActivityType   activityType;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "firstObjectId", referencedColumnName = "activityId", insertable = false, updatable = false)
    @Where(clause = "relationType LIKE 'ACTIVITY%'")
    @ToString.Exclude
    private List<Relation> relations;

    @OneToMany
    @JoinColumn(name = "activityId", referencedColumnName = "activityId", insertable = false, updatable = false)
    @ToString.Exclude
    private List<Event> events;

    @Column
    private String         category;

    @Column
    private String         subCategory;

    @Column
    private ActivityStatus status;

    
    @Column
    private OffsetDateTime startDate;

    @Column
    private OffsetDateTime endDate;

    @JsonIgnore
    @Column
    private String         createdBy;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "createdBy", referencedColumnName = "userName", insertable = false, updatable = false)
    User CreatedByUser;

}
