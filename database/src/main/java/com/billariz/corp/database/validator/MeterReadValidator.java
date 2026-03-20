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
import com.billariz.corp.database.model.MeterRead;
import com.billariz.corp.database.model.MeterReadDetail;
import com.billariz.corp.database.model.PointOfServiceCapacity;
import com.billariz.corp.database.repository.ParameterRepository;

@Component
public class MeterReadValidator extends BaseValidator
{
    @Autowired
    private MeterReadDetailValidator meterReadDetailValidator;

    public MeterReadValidator(ParameterRepository parameterRepository)
    {
        super(parameterRepository);
    }

    @Override
    public boolean supports(Class<?> clazz)
    {
        return MeterRead.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors)
    {
        var i = (MeterRead) target;

        checkTouGroup(i.getTouGroup(), errors);
        checkMarket(i.getMarket(), errors);
        checkDirection(i.getDirection(), errors);
        checkSqType(i.getUnit(), errors);
        checkMeterReadType(i.getType().getValue(), errors);
        checkMeterReadContext(i.getContext().getValue(), errors);
        checkMeterReadSource(i.getSource().getValue(), errors);
        CheckMeterReadStatus(i.getStatus().getValue(), errors);


        Optional.ofNullable(i.getMeterReadDetails())
        .stream()
        .flatMap(Collection::stream)
        .forEach(act -> meterReadDetailValidator.validate(act, errors));
    }
}