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

package com.billariz.corp.app.exception;

import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import com.billariz.corp.notifier.exception.ErrorDto;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.Filter;

@Component
@Order(Integer.MIN_VALUE)
public class FilterExceptionHandler implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (ResponseStatusException e) {
            if (!response.isCommitted()) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(e.getStatus().value());
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"status\": \"" + e.getRawStatusCode() + "\", \"message\": \"" + e.getReason() + "\"}");
                httpResponse.getWriter().flush();
            }
        }catch (ProcessInterruptedException e) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            if (!httpResponse.isCommitted()) {
                httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"message\": \"" + e.getMessage() + "\"}");
                httpResponse.getWriter().flush();
            }
        } catch (Exception e) {
            if (!response.isCommitted()) {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error" + e);
            }
        }
    }
}
