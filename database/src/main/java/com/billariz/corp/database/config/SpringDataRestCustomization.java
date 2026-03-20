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

package com.billariz.corp.database.config;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.metamodel.Type;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import com.billariz.corp.database.validator.BaseValidator;

@Component
public class SpringDataRestCustomization implements RepositoryRestConfigurer {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private List<BaseValidator> validators;

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        config.exposeIdsFor(entityManager.getMetamodel().getEntities().stream()
                .map(Type::getJavaType).toArray(Class[]::new));
        config.getExposureConfiguration().disablePutForCreation();
        config.setReturnBodyForPutAndPost(true);
        cors.addMapping("/**").allowedOrigins("*").allowedMethods("*").maxAge(3600);
    }

    @Override
    public void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener validatingListener) {
        validators.forEach(validator -> {
            validatingListener.addValidator("beforeCreate", validator);
            validatingListener.addValidator("beforeSave", validator);
        });
    }
}