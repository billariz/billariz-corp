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
import com.billariz.corp.database.model.Actor;
import com.billariz.corp.database.model.Article;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.database.repository.ThirdRepository;

@Component
public class ArticleValidator extends BaseValidator
{

    public ArticleValidator(ParameterRepository parameterRepository)
    {
        super(parameterRepository);
    }

    @Override
    public boolean supports(Class<?> clazz)
    {
        return Article.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors)
    {
        var i = (Article) target;
        
        checkSqType(i.getUnitOfQuantity(), errors);
        checkPriceUnit(i.getUnitOfUnitPrice(), errors);
        checkTou(i.getTou(), errors);
        checkVatRate(i.getVatRate(), errors);
        
    }
}