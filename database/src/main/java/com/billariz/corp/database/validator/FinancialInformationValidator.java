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
import com.billariz.corp.database.model.FinancialInformation;
import com.billariz.corp.database.repository.ParameterRepository;

@Component
public class FinancialInformationValidator extends BaseValidator
{
    public FinancialInformationValidator(ParameterRepository parameterRepository)
    {
        super(parameterRepository);
    }

    @Override
    public boolean supports(Class<?> clazz)
    {
        return FinancialInformation.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors)
    {
        var i = (FinancialInformation) target;

        if(i.getPaymentMode()!=null){
            checkPaymentMode(i.getPaymentMode(), errors);
            if(i.getPaymentMode().equals("DIRECT_DEBIT")){
                checkIban(i.getIban(), errors);
                checkDomicilationId(i.getDomicilationId(), errors);
                checkDomicilationStatus(i.getDomicilationStatus(), errors);
            }
        }
    }
}