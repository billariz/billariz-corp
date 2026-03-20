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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import lombok.Data;

@Data
@Entity
@IdClass(ProcessingLauncherRules.PALId.class)
@Table(name = "PA_PROCESSING_LAUNCHER_RULES")
public class ProcessingLauncherRules
{
    @Id
    private String processingToLaunch;

    @Id
    private String processingToCheck;

    @Column
    private String statusProcessingToCheck;

    @Data
    public static class PALId implements Serializable
    {
        private final String processingToLaunch;

        private final String processingToCheck;
    }
}
