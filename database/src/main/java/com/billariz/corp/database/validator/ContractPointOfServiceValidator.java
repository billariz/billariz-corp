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
import com.billariz.corp.database.model.ContractPointOfService;
import com.billariz.corp.database.repository.ContractPointOfServiceRepository;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.database.repository.PointOfServiceRepository;

@Component
public class ContractPointOfServiceValidator extends BaseValidator
{
    @Autowired
    private PointOfServiceValidator posValidator;

    @Autowired
    private ContractPointOfServiceRepository contractPointOfServiceRepository;

    @Autowired
    private PointOfServiceRepository pointOfServiceRepository;

    public ContractPointOfServiceValidator(ParameterRepository parameterRepository)
    {
        super(parameterRepository);
    }

    @Override
    public boolean supports(Class<?> clazz)
    {
        return ContractPointOfService.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors)
    {
        var i = (ContractPointOfService) target;
        
        if(i.getPointOfService()!=null){
                posValidator.validate(i.getPointOfService(), errors);
            if(i.getPointOfService().getId() == null){
                var pos = pointOfServiceRepository.save(i.getPointOfService());
                i.setPosId(pos.getId());
            }
        
            var ctrPos = contractPointOfServiceRepository.findFirstByReferenceAndEndDateIsNull(i.getPointOfService().getReference());
            if( i!=null && ctrPos != null && ctrPos.getId()!=i.getId()){
                ctrPos.setEndDate(i.getStartDate());
            }
        }
    }
}