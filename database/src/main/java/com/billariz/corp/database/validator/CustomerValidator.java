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

import java.util.Collection;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import com.billariz.corp.database.model.Customer;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.utils.EntityReferenceGenerator;

@Component
public class CustomerValidator extends BaseValidator
{
    @Autowired
    private PerimeterValidator perimeterValidator;

    @Autowired
    private CompanyValidator companyValidator;

    @Autowired
    private IndividualValidator individualValidator;

    @Autowired
    private ContactValidator contactValidator;

    @Autowired
    private EntityReferenceGenerator referenceUtils;

    public CustomerValidator(ParameterRepository parameterRepository)
    {
        super(parameterRepository);
    }

    @Override
    public boolean supports(Class<?> clazz)
    {
        return Customer.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors)
    {
        var i = (Customer) target;

        checkCustomerCategory(i.getCategory(), errors);
        checkCustomerStatus(i.getStatus(), errors);
        checkCustomerType(i.getType(), errors);
        checkLanguageCode(i.getLanguageCode(), errors);
        if(i.getIndividual() !=null)
            individualValidator.validate(i.getIndividual(), errors);
        if(i.getContact() !=null)
            contactValidator.validate(i.getContact(), errors);
        if(i.getCompany() !=null)
            companyValidator.validate(i.getCompany(), errors);
        if(i.getAddress() !=null)
            checkAddress(i.getAddress(), errors);
        Optional.ofNullable(i.getPerimeters())
            .stream()
            .flatMap(Collection::stream)
            .forEach(perimeter -> {
                perimeterValidator.validate(perimeter, errors);
            });

        if(i.getReference()==null){
            i.setReference(referenceUtils.referenceGenerator(ObjectType.CUSTOMER));
        }
    }
}