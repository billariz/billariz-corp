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
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.utils.EntityReferenceGenerator;

@Component
public class ContractValidator extends BaseValidator
{
    @Autowired
    private EntityReferenceGenerator referenceUtils;

    @Autowired
    private ContractPointOfServiceValidator contractPosValidator;

    public ContractValidator(ParameterRepository parameterRepository)
    {
        super(parameterRepository);
    }

    @Override
    public boolean supports(Class<?> clazz)
    {
        return Contract.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors)
    {
        var i = (Contract) target;

        checkBillingMode(i.getBillingMode(), errors);
        checkContractStatus(i.getStatus(), errors);
        checkMarket(i.getMarket(), errors);
        checkDirection(i.getDirection(), errors);
        checkInstallmentFrequency(i.getInstallPeriodicity(), errors);
        checkChannel(i.getChannel(), errors);
        checkSeller(i.getSeller(),errors);
        checkBillingFrequency(i.getBillingFrequency(), errors);

        Optional.ofNullable(i.getContractPointOfServices())
        .stream()
        .flatMap(Collection::stream)
        .forEach(contractPos -> {
            contractPosValidator.validate(contractPos, errors);
        });

        if(i.getReference()==null){
            i.setReference(referenceUtils.referenceGenerator(ObjectType.CONTRACT));
        }
    }
}