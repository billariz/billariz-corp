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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

@Configuration
public class LocaleConfig {

    @Bean
    public LocaleResolver localeResolver() {
        return new CustomLocaleResolver();
    }

    /**
     * Résolveur personnalisé pour gérer la locale dans l'ordre de priorité :
     * 1. Paramètre `lang` dans la requête.
     * 2. En-tête `Accept-Language`.
     * 3. Locale par défaut.
     */
    public static class CustomLocaleResolver extends AcceptHeaderLocaleResolver {

        private final Locale defaultLocale = Locale.ENGLISH; // Locale par défaut

        @Override
        public Locale resolveLocale(HttpServletRequest request) {
            // 1. Vérifier si le paramètre `lang` est présent
            String lang = request.getParameter("lang");
            if (lang != null && !lang.isEmpty()) {
                return Locale.forLanguageTag(lang);
            }

            // 2. Vérifier l'en-tête `Accept-Language`
            Locale acceptHeaderLocale = super.resolveLocale(request);
            if (acceptHeaderLocale != null) {
                return acceptHeaderLocale;
            }

            // 3. Utiliser la locale par défaut
            return defaultLocale;
        }
    }
}