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

package com.billariz.corp.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import de.codecentric.boot.admin.server.config.EnableAdminServer;

@EnableAdminServer
@EnableScheduling
@ConfigurationPropertiesScan(basePackages = "com.billariz.corp")
@SpringBootApplication(scanBasePackages = "com.billariz.corp")
public class AppApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(AppApplication.class, args);
    }
}
