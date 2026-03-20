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

package com.billariz.corp.database.projection;

import org.springframework.data.rest.core.config.Projection;
import com.billariz.corp.database.model.Group;
import com.billariz.corp.database.model.Organism;


@Projection(name = "inGroup", types = { Group.class })
public interface inGroup
{

    Long getId();

    Long   getOrganismId();

    Organism getOrganism();

    String getGroupName();

    String getCategory();

    String getSubCategory();

    String getDefaultLabel();

    String getStandardLabel();

    String getOtherLabel();

    String getDescription();

    String getAuthorizations();

}
