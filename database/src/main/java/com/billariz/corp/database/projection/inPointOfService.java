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
import com.billariz.corp.database.model.Address;
import com.billariz.corp.database.model.ContractPointOfService;
import com.billariz.corp.database.model.PointOfService;
import com.billariz.corp.database.model.ReadingCycle;

@Projection(name = "inPointOfService", types = { PointOfService.class })
public interface inPointOfService {
    
    String                            getId();

    String                            getReference();

    Address                           getAddress();

    String                            getMarket();

    String                            getTgoCode();

    String                            getDgoCode();

    String                            getDeliveryState();

    String                            getDeliveryStatus();

    boolean                           getTemporaryConnection();

    String                            getTemporaryConnectionType();

    String                            getDirection();

    String                            getReadingCycleId();

    ReadingCycle                      getReadingCycle();

    ContractPointOfService      getContractPointOfService();
    
}
