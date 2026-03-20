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
import com.billariz.corp.database.model.Organism;
import com.billariz.corp.database.model.Rate;
import com.billariz.corp.database.repository.ParameterRepository;

@Component
public class OrganismValidator extends BaseValidator
{
    public OrganismValidator(ParameterRepository parameterRepository)
    {
        super(parameterRepository);
    }

    @Override
    public boolean supports(Class<?> clazz)
    {
        return Organism.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors)
    {
        var i = (Organism) target;

        checkOrganismCategory(i.getCategory(), errors);
        checkOrganismSubCategory(i.getSubCategory(), errors);
        checkLegalForm(i.getCompany().getLegalFormCode(), errors);
        checkNaceCode(i.getCompany().getNaceCode(), errors);

    }
}