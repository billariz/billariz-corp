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

import java.time.LocalDateTime;
import org.springframework.data.rest.core.config.Projection;

import com.billariz.corp.database.model.Company;
import com.billariz.corp.database.model.Contact;
import com.billariz.corp.database.model.Customer;
import com.billariz.corp.database.model.Individual;
import com.billariz.corp.database.model.Address;

@Projection(name = "inCustomer", types = { Customer.class })
public interface inCustomer
{
    Long getId();
    
    String          getReference();

    String          getCategory();

    String          getLanguageCode();

    String          getType();

    Company         getCompany();

    Individual      getIndividual();

    Contact         getContact();

    Address         getAddress();

    String          getStatus();

    LocalDateTime   getCreationDate();
    

}
