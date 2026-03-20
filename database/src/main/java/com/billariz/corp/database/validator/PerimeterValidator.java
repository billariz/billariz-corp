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
import com.billariz.corp.database.model.Perimeter;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.utils.EntityReferenceGenerator;

@Component
public class PerimeterValidator extends BaseValidator
{
    @Autowired
    private EntityReferenceGenerator referenceUtils;

    @Autowired
    private ContractValidator contractValidator;

    @Autowired
    private ActorValidator actorValidator;

    @Autowired
    private ContractPointOfServiceValidator contractPosValidator;

    public PerimeterValidator(ParameterRepository parameterRepository)
    {
        super(parameterRepository);
    }

    @Override
    public boolean supports(Class<?> clazz)
    {
        return Perimeter.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors)
    {
        var i = (Perimeter) target;

        checkPerimeterStatus(i.getStatus(), errors);
        checkPerimeterType(i.getPerimeterType(), errors);
        if(i.getBillingFrequency()!=null)
            checkBillingFrequency(i.getBillingFrequency(), errors);
        checkMarket(i.getMarket(), errors);

        Optional.ofNullable(i.getActors())
        .stream()
        .flatMap(Collection::stream)
        .forEach(act -> actorValidator.validate(act, errors));

        Optional.ofNullable(i.getContractPerimeters())
                    .stream()
                    .flatMap(Collection::stream)
                    .forEach(contractPerimeter -> {
                        if(contractPerimeter.getContract() !=null){
                            contractValidator.validate(contractPerimeter.getContract(), errors);
                        }
                    });

        if(i.getReference()==null){
            i.setReference(referenceUtils.referenceGenerator(ObjectType.PERIMETER));
        }

    }
}