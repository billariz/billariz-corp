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
import com.billariz.corp.database.model.Rate;
import com.billariz.corp.database.repository.ParameterRepository;

@Component
public class RateValidator extends BaseValidator
{
    public RateValidator(ParameterRepository parameterRepository)
    {
        super(parameterRepository);
    }

    @Override
    public boolean supports(Class<?> clazz)
    {
        return Rate.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors)
    {
        var i = (Rate) target;

        checkMarket(i.getMarket(), errors);
        checkPriceType(i.getPriceType(), errors);
        checkCustomerType(i.getCustomerType(), errors);
        checkCustomerCategory(i.getCustomerCategory(), errors);
        checkChannel(i.getChannel(), errors);
        checkInstallmentFrequency(i.getInstallmentFrequency(), errors);
        checkServiceCategory(i.getServiceCategory(), errors);
        checkPosCategory(i.getPosCategory(), errors);
        checkTou(i.getTou(), errors);
        checkTouGroup(i.getTouGroup(), errors);
        checkGridRate(i.getGridRate(), errors);
        checkDgoCode(i.getDgoCode(), errors);
        checkTgoCode(i.getTgoCode(), errors);
        checkPriceUnit(i.getUnit(), errors);
        checkThresholdType(i.getThresholdType(), errors);
        checkThresholdBase(i.getThresholdBase(), errors);

    }
}