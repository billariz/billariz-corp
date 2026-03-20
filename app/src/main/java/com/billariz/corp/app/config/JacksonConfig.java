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

package com.billariz.corp.app.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Configuration
public class JacksonConfig
{
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer()
    {
        return builder -> builder.mixIn(Object.class, IgnoreHibernatePropertiesInJackson.class);
    }

    @JsonIgnoreProperties({ "hibernateLazyInitializer" })
    private static class IgnoreHibernatePropertiesInJackson
    {
    }
}
