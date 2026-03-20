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

package com.billariz.corp.app.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import com.billariz.corp.app.config.AuthorizationHeaderFilter;
import com.billariz.corp.app.config.UserActionLoggingFilter;

@Configuration
public class SecurityConfig {

    private final AuthorizationHeaderFilter authorizationHeaderFilter;
    private final UserActionLoggingFilter userActionLoggingFilter;

    public SecurityConfig(AuthorizationHeaderFilter authorizationHeaderFilter,
                          UserActionLoggingFilter userActionLoggingFilter) {
        this.authorizationHeaderFilter = authorizationHeaderFilter;
        this.userActionLoggingFilter = userActionLoggingFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
        .cors()
        .and()
            .authorizeRequests()
            //TODO A ajuster pour affiner les athorisations
                //.antMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
                .antMatchers( "/**").permitAll()
                //.antMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(authorizationHeaderFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(userActionLoggingFilter, AuthorizationHeaderFilter.class);

        return http.build();
    }
}
