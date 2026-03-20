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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.billariz.corp.app.utils.WebRequestUtils;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.model.light.messageCode;
import com.billariz.corp.database.repository.JournalRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Log4j2
public class UserActionLoggingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final JournalRepository journalRepository;

    // Injection via constructeur
    public UserActionLoggingFilter(ObjectMapper objectMapper, JournalRepository journalRepository) {
        this.objectMapper = objectMapper;
        this.journalRepository = journalRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/v1/admin/") || path.startsWith("/v1/actuator/")){ 
            filterChain.doFilter(request, response);
            return;
        }
        
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method)){
            CachedBodyHttpServletResponse wrappedResponse = new CachedBodyHttpServletResponse(response);
            filterChain.doFilter(request, wrappedResponse);
            int responseStatus = wrappedResponse.getCapturedStatus();
            if (isHttpStatusOk(responseStatus)) {
                String responseBody = wrappedResponse.getCapturedBody();
                String responseObjectId = extractIdFromResponse(responseBody);
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String username = (auth != null) ? auth.getName() : "Anonymous";
                logUserAction(username, request, responseObjectId);
            }
            wrappedResponse.copyBodyToResponse();
            return;
        }
        if ("PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            int responseStatus = response.getStatus();
            if (isHttpStatusOk(responseStatus)) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String username = (auth != null) ? auth.getName() : "Anonymous";
                logUserAction(username, request, null);
            }
            return;
        } 
        filterChain.doFilter(request, response);
    }

    private void logUserAction(String username, HttpServletRequest request, String objectId) {
        Journal journal = new Journal();
        journal.setMethod(request.getMethod());
        journal.setUserName(username);
        journal.setApiPath(request.getRequestURI());
        journal.setIpAdress(getClientIp(request));
        journal.setUserAgent(request.getHeader("User-Agent"));
        journal.setForwardedFor(request.getHeader("X-Forwarded-For"));
        journal.setObjectType(ObjectType.valueOf(WebRequestUtils.toSingularUppercase(getUriPart(request.getRequestURI(),1))));
        journal.setObjectId((objectId!=null) 
                        ? ((objectId.matches("\\d+")) ? Long.parseLong(objectId) : 0L)
                        : ((getUriPart(request.getRequestURI(),2).matches("\\d+")) ? Long.parseLong(getUriPart(request.getRequestURI(),2)) : 0L));
        journal.setCreationDate(OffsetDateTime.now());
        journal.setComment("HTTP_EVENT");
        List<messageCode> messages = new ArrayList<>();
        messages.add(new messageCode("HTTP_EVENT", null));
        journal.setMessageCodes(messages);
        journalRepository.save(journal);
    }

    public boolean isHttpStatusOk(int responseStatus) {
        return responseStatus >= 200 && responseStatus < 300;
    }

    private String extractIdFromResponse(String responseBody) {
        try {
            // Suppose que l'ID est dans un champ "id" du JSON
            JsonNode rootNode = objectMapper.readTree(responseBody);
            return rootNode.has("id") ? rootNode.get("id").asText() : null;
        } catch (Exception e) {
            System.err.println("Failed to extract ID from response: " + e.getMessage());
            return null;
        }
    }

    private String getUriPart(String uri, int part){
        String[] parts = uri.split("/");
        if(parts[1].equals("v1"))
                part++;
        if(part < parts.length) 
                return parts[part];
         else return "";
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
