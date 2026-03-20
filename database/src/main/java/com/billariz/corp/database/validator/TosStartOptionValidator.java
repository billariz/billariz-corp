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
import com.billariz.corp.database.model.ServiceStartOption;
import com.billariz.corp.database.model.TermOfServiceStartOption;
import com.billariz.corp.database.repository.ParameterRepository;

@Component
public class TosStartOptionValidator extends BaseValidator
{
    public TosStartOptionValidator(ParameterRepository parameterRepository)
    {
        super(parameterRepository);
    }

    @Override
    public boolean supports(Class<?> clazz)
    {
        return TermOfServiceStartOption.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors)
    {
        var i = (TermOfServiceStartOption) target;

        checkMarket(i.getMarket(), errors);
        checkPriceType(i.getPriceType(), errors);
        checkPriceMode(i.getPriceMode(), errors);
        checkRefDateTypeForFixedPrice(i.getRefDateTypeForFixedPrice(), errors);

    }
}