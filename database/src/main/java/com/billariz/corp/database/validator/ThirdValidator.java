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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import com.billariz.corp.database.model.Third;
import com.billariz.corp.database.repository.ParameterRepository;

@Component
public class ThirdValidator extends BaseValidator
{
    @Autowired
    private CompanyValidator companyValidator;

    @Autowired
    private IndividualValidator individualValidator;

    @Autowired
    private ContactValidator contactValidator;

    @Autowired
    private FinancialInformationValidator financialInformationValidator;

    public ThirdValidator(ParameterRepository parameterRepository)
    {
        super(parameterRepository);
    }

    @Override
    public boolean supports(Class<?> clazz)
    {
        return Third.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors)
    {
        var i = (Third) target;

        checkThirdType(i.getType(), errors);
        checkAddress(i.getAddress(), errors);
        individualValidator.validate(i.getIndividual(), errors);
        contactValidator.validate(i.getContact(), errors);
        if(i.getFinancialInformation() !=null)
            financialInformationValidator.validate(i.getFinancialInformation(), errors);
        if(i.getCompany() !=null)
            companyValidator.validate(i.getCompany(), errors);
    }
}