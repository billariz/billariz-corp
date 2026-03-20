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
import com.billariz.corp.database.model.PointOfService;
import com.billariz.corp.database.repository.ParameterRepository;

@Component
public class PointOfServiceValidator extends BaseValidator
{

    @Autowired
    PointOfServiceCapacityValidator posCapaValidator;

    @Autowired
    PointOfServiceConfigurationValidator posConfigValidator;

    @Autowired
    PointOfServiceEstimateValidator posEstimateValidator;

    public PointOfServiceValidator(ParameterRepository parameterRepository)
    {
        super(parameterRepository);
    }

    @Override
    public boolean supports(Class<?> clazz)
    {
        return PointOfService.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors)
    {
        var i = (PointOfService) target;

        checkMarket(i.getMarket(), errors);
        checkDgoCode(i.getDgoCode(), errors);
        checkTgoCode(i.getTgoCode(), errors);
        checkDirection(i.getDirection(), errors);
        checkDeliveryState(i.getDeliveryState(), errors);
        checkDeliveryStatus(i.getDeliveryStatus(), errors);
        
        Optional.ofNullable(i.getCapacities())
                    .stream()
                    .flatMap(Collection::stream)
                    .forEach(capacity -> {
                        posCapaValidator.validate(capacity, errors);
                    });
        Optional.ofNullable(i.getConfigurations())
                    .stream()
                    .flatMap(Collection::stream)
                    .forEach(configuration -> {
                        posConfigValidator.validate(configuration, errors);
                    });
        Optional.ofNullable(i.getEstimates())
                    .stream()
                    .flatMap(Collection::stream)
                    .forEach(estimate -> {
                        posEstimateValidator.validate(estimate, errors);
                    });
    }
}