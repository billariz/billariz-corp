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

package com.billariz.corp.database.model.light;

import java.time.LocalDate;
import com.billariz.corp.database.model.ServiceElementType;
import com.billariz.corp.database.model.enumeration.ServiceElementTypeCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EligibleServiceElement
{
    private Long                       id;

    private boolean                    metered;

    private String        sqType;

    private ServiceElementType                     type;

    private ServiceElementTypeCategory category;

    private Long                       contractId;

    private boolean              estimateAuthorized;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer minDayForEstimate;

    private String tou;

    private String touGroup;
    
}
