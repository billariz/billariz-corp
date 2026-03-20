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

package com.billariz.corp.database.validator;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import com.billariz.corp.database.model.PointOfServiceCapacity;
import com.billariz.corp.database.model.PointOfServiceConfiguration;
import com.billariz.corp.database.repository.ParameterRepository;

@Component
public class PointOfServiceConfigurationValidator extends BaseValidator
{
    public PointOfServiceConfigurationValidator(ParameterRepository parameterRepository)
    {
        super(parameterRepository);
    }

    @Override
    public boolean supports(Class<?> clazz)
    {
        return PointOfServiceConfiguration.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors)
    {
        var i = (PointOfServiceConfiguration) target;

        checkGridRate(i.getGridRate(), errors);
        checkReadingFrequency(i.getReadingFrequency(), errors);
        checkBusinessGridCode(i.getBusinessGridCode(), errors);
        checkMarketGridCode(i.getMarketGridCode(), errors);
        checkProfile( i.getProfile(), errors);
        checkSource(i.getSource(), errors);
        checkTouGroup(i.getTouGroup(), errors);
        checkPosDataStatus(i.getStatus().getValue(), errors);
        //AJouer controle meter


        //TODO règles affectation profil Elec sur la config (https://docs.google.com/spreadsheets/d/0B4Hp1_SagOjgV0x6b3B1M0NSbVE/edit?pli=1&resourcekey=0-_kyp3LPg7i2DvzlZ6GHAsw&gid=215545544#gid=215545544)
        
    }
}