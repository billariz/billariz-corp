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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class LanguageProperties {

    private static LanguageProperties instance;
    private Properties properties;

    // Constructeur privé pour empêcher l'instanciation directe
    private LanguageProperties() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("language-mappings.properties")) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load language properties", e);
        }
    }

    // Méthode pour obtenir l'instance unique
    public static synchronized LanguageProperties getInstance() {
        if (instance == null) {
            instance = new LanguageProperties();
        }
        return instance;
    }

    // Méthode pour obtenir une propriété par locale
    public String getLabel(String locale) {
        return properties.getProperty(locale);
    }
}
