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

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Billariz API",
        version = "v1",
        termsOfService = "https://billariz.com/api-terms",
        license = @License(name = "Proprietary License", url = "https://billariz.com/license"),
        description = """
        This is a RESTful API designed around standard CRUD operations (Create, Read, Update, Delete), exposing business entities such as Customer, Activity, MeterRead, and others.
        
        The API is secured using **Bearer Token (JWT)** authentication, typically issued by AWS Cognito or an equivalent identity provider.
        
        It is accessible through environment-based URLs of the form:  
        `https://[$group_user].api.billariz.com/v1/`  
        where `$group_user` is the target environment identifier (e.g. `demo-dev`, `prod`, etc.).
        
        All requests must include the following HTTP header:  `Authorization: Bearer <token>`  
        """,
        contact = @Contact(name = "Billariz Team", email = "contact@billariz.com")
    ),
    servers = {
        @Server(description = "Demo", url = "https://demo-dev.api.billariz.com/v1"),
    },
    security = @SecurityRequirement(name = "bearerAuth")
)

public class OpenApiConfig {
}
