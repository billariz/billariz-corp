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
import com.billariz.corp.database.model.BillableCharge;
import com.billariz.corp.database.repository.ParameterRepository;

@Component
public class BillableChargeValidator extends BaseValidator
{
    @Autowired
    private ArticleValidator articleValidator;

    public BillableChargeValidator(ParameterRepository parameterRepository)
    {
        super(parameterRepository);
    }

    @Override
    public boolean supports(Class<?> clazz)
    {
        return BillableCharge.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors)
    {
        var i = (BillableCharge) target;

        checkMarket(i.getMarket(), errors);
        checkDirection(i.getDirection(), errors);
        checkBillableChargeType(i.getType().getValue(), errors);
        checkBillableChargeContext(i.getContext().getValue(), errors);
        checkBillableChargeSource(i.getSource().getValue(), errors);
        CheckBillableChargeStatus(i.getStatus().getValue(), errors);
        
        Optional.ofNullable(i.getBillableChargeDetails())
        .stream()
        .flatMap(Collection::stream)
        .forEach(act -> articleValidator.validate(act, errors));
    }
}