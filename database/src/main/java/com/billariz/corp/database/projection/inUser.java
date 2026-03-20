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

import java.util.List;
import java.util.Set;
import org.springframework.data.rest.core.config.Projection;
import com.billariz.corp.database.model.Contact;
import com.billariz.corp.database.model.Group;
import com.billariz.corp.database.model.Individual;
import com.billariz.corp.database.model.Organism;
import com.billariz.corp.database.model.Permission;
import com.billariz.corp.database.model.Role;
import com.billariz.corp.database.model.User;

@Projection(name = "inUser", types = { User.class })
public interface inUser
{

    Long getId();

    String getUserName();

    String getUserRole();

    Individual getIndividual();

    Contact getContact();

    Group getGroup();

    Organism getOrganism();

    String getPicture();

    String getDefaultLanguage();

    boolean getMaster();

    boolean getReadOnly();

    String getStatus();

    List<Role> getRoles();

    List<Permission> getPermissions();

}
