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

import com.billariz.corp.app.service.AuthorizationService;
import com.billariz.corp.app.utils.WebRequestUtils;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.Group;
import com.billariz.corp.database.model.Organism;
import com.billariz.corp.database.model.User;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.GroupRepository;
import com.billariz.corp.database.repository.OrganismRepository;
import com.billariz.corp.database.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.LocaleResolver;

@Component
@Slf4j
public class AuthorizationHeaderFilter extends OncePerRequestFilter {

    
    private final AuthorizationService authorizationService;

    private final UserRepository userRepository;

    private final GroupRepository groupRepository;

    private final OrganismRepository organismRepository;

    private final EventRepository eventRepository;

    private MessageSource messageSource;

    private final LocaleResolver localeResolver;

    private final String client;

    private final String instance;

    private String userName;
    private List<String> groups;


    public AuthorizationHeaderFilter( EventRepository eventRepository, OrganismRepository organismRepository, 
                                        AuthorizationService authorizationService, UserRepository userRepository,
                                        GroupRepository groupRepository, MessageSource messageSource, LocaleResolver localeResolver) {
        this.authorizationService = authorizationService;
        this.userRepository = userRepository;
        this.organismRepository = organismRepository;
        this.eventRepository = eventRepository;
        this.groupRepository = groupRepository;
        this.messageSource = messageSource;
        this.localeResolver = localeResolver;
        this.client = System.getenv("CLIENT");
        this.instance = System.getenv("INSTANCE");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        Locale locale = localeResolver.resolveLocale(request);
        LocaleContextHolder.setLocale(locale);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isActuator(request) || isExplorer(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null) {
            String token = (authorizationHeader.startsWith("Bearer ")) ? authorizationHeader.substring(7) : authorizationHeader; // Supprime "Bearer " pour obtenir le JWT

            extractUsernameAndGroupsFromToken(token, locale);

            if("Anonymous".equals(this.userName))
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, messageSource.getMessage("USER_NOT_FOUND", new Object[]{this.userName}, locale));
            
            // CHECK WORKSPACE
            if (this.groups == null ||
                            this.groups.stream().noneMatch(g -> g.equalsIgnoreCase(this.client + "-" + this.instance))) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        messageSource.getMessage("WORKSPACE_MISMATCH",
                            new Object[]{userName, this.client + "-" + this.instance},
                            locale));
            }

            UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userName, null, null);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        // CHECK PERMISSIONS
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        if (authenticated == null) 
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,messageSource.getMessage("USER_NOT_AUTHENTIFICATED", new Object[]{authenticated}, locale));
            
        //Extract entityName from HTTP URI
        String entityName = resolveEntityFromUri(request.getRequestURI(),2);
        if (entityName == null) 
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,messageSource.getMessage("ENTITY_NOT_FOUND", new Object[]{entityName}, locale));
        
        // 5️⃣ Check if entity exist
        Class<?> entityClass = getEntityClassFromName(entityName);
        if (entityClass==null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,messageSource.getMessage("ENTITY_CLASSE_NOT_FOUND", new Object[]{entityName}, locale));
       
        //Récupérer l'utilisateur depuis la base de données
        String userName = authenticated.getName();
        var user = userRepository.findByUserName(userName)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,messageSource.getMessage("USER_NOT_FOUND", new Object[]{userName}, locale)));
        // Extract action from HTTP method
        String action = resolveActionFromHttpMethod(request.getMethod());
        if (action == null) 
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,messageSource.getMessage("HTTP_METHOD_NOT_FOUND", new Object[]{action}, locale));

        // Check permissions for the requested action
        String restriction = authorizationService.getAuthorization(entityName, action, user);
        if (restriction==null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,messageSource.getMessage("UNAUTHORIZED", new Object[]{action, entityName}, locale));
        }
        if(isMonitoredByUser(entityClass) && !restriction.equals("FULL")){
            var pathCase = resolveEntityFromUri(request.getRequestURI(),3);
            if(action.equals("READ")){
                if(pathCase!=null && pathCase.equals("SEARCH")){
                    Map<String, String[]> modifiedParams = transformParamsBasedOnPermissions(request, user, restriction, entityClass);

                    // 6️⃣ Injecter les paramètres modifiés dans la requête via un HttpServletRequestWrapper
                    ModifiableHttpServletRequest wrappedRequest = new ModifiableHttpServletRequest(request);
                    for (Map.Entry<String, String[]> entry : modifiedParams.entrySet()) {
                        wrappedRequest.setParameter(entry.getKey(), entry.getValue());
                    }
                    // 7️⃣ Passer la requête modifiée au reste du pipeline
                    filterChain.doFilter(wrappedRequest, response);
                    return;
                }
                else {
                        try {
                            var id = Long.parseLong(pathCase);
                            checkPathConditions(restriction, user, id, action, entityClass, locale);
                        } catch (NumberFormatException e) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, messageSource.getMessage("INVALID_ID_FORMAT", new Object[]{pathCase}, locale));
                        }
                }
            }
            else {
                if(!action.equals("CREATE")){
                    try {
                        var id = Long.parseLong(pathCase);
                        checkPathConditions(restriction, user, id, action, entityClass, locale);
                    } catch (NumberFormatException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, messageSource.getMessage("INVALID_ID_FORMAT", new Object[]{pathCase}, locale));
                    }
                }
            } 
        }
        // Passer la requête originale au reste du pipeline
        filterChain.doFilter(request, response);
    }

    private void checkPathConditions(String condition, User user, Long id, String action, Class<?> entityClass, Locale locale){

        switch (entityClass.getSimpleName()) {
            case "User":
                checkUserCondition(condition, user, id, action, locale);
                break;
            case "Group":
                checkGroupCondition(condition, user, id, action, locale);
                break;
            case "Organism":
                checkOrganismCondition(condition, user, id, action, locale);
                break;
            case "Event":
                checkEventCondition(condition, user, id, action, locale);
                break;
            default:
                break;
        }
    }

    private void checkUserCondition(String condition, User user, Long id, String action, Locale locale){
        switch (condition) {
            case "SELF":
                if(id != user.getId())
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,messageSource.getMessage("UNAUTHORIZED", 
                                                                                        new Object[]{action, "User"}, locale));
                break;
            case "GROUP":
                var userG = userRepository.findById(id).orElseThrow();
                if(userG.getGroupId() != user.getGroupId())
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,messageSource.getMessage("UNAUTHORIZED", 
                                                                                        new Object[]{action, "User"}, locale));
                break;
            case "ORGANISM":
                var userO = userRepository.findById(id).orElseThrow();
                if(userO.getOrganismId() != user.getOrganismId())
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,messageSource.getMessage("UNAUTHORIZED", 
                                                                                        new Object[]{action, "User"}, locale));
                break;
            default:
                break;
        }
    }

    private void checkGroupCondition(String condition, User user, Long id, String action, Locale locale){
        switch (condition) {
            case "SELF":
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,messageSource.getMessage("UNAUTHORIZED", 
                                                                                    new Object[]{action, "User"}, locale));
            case "GROUP":
                if(user.getGroupId() != id)
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,messageSource.getMessage("UNAUTHORIZED", 
                                                                                        new Object[]{action, "User"}, locale));
                break;
            case "ORGANISM":
                var grp = groupRepository.findById(id).orElseThrow();
                if(grp.getOrganismId() != user.getOrganismId())
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,messageSource.getMessage("UNAUTHORIZED", 
                                                                                        new Object[]{action, "User"}, locale));
                break;
            default:
                break;
        }
    }

    private void checkOrganismCondition(String condition, User user, Long id, String action, Locale locale){
        switch (condition) {
            case "SELF":
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,messageSource.getMessage("UNAUTHORIZED", 
                                                                                        new Object[]{action, "User"}, locale));
            case "GROUP":
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,messageSource.getMessage("UNAUTHORIZED", 
                                                                                        new Object[]{action, "User"}, locale));
            case "ORGANISM":
                var org = organismRepository.findById(id).orElseThrow();
                if(org.getId() != user.getOrganismId())
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,messageSource.getMessage("UNAUTHORIZED", 
                                                                                        new Object[]{action, "User"}, locale));
                break;
            default:
                break;
        }
    }

    private void checkEventCondition(String condition, User user, Long id, String action, Locale locale){
        var event = eventRepository.findById(id).orElseThrow();
        switch (condition) {
            case "SELF":
                if(event.getUserId() !=null)
                    if(event.getUserId() !=user.getId()) 
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,messageSource.getMessage("UNAUTHORIZED", 
                                                                                        new Object[]{action, "User"}, locale));
                break;
            case "GROUP":
                if(event.getGroupId() !=null)
                    if(event.getGroupId() !=user.getGroupId()) 
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,messageSource.getMessage("UNAUTHORIZED", 
                                                                                        new Object[]{action, "User"}, locale));
                break;
            case "ORGANISM":
                if(event.getOrganismId() !=null)
                    if(event.getOrganismId() !=user.getOrganismId()) 
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,messageSource.getMessage("UNAUTHORIZED", 
                                                                                        new Object[]{action, "User"}, locale));
                break;
            default:
                break;
        }
    }

    private boolean isMonitoredByUser(Class<?> entityClass) {
        var monitoredEntities = List.of(User.class, Group.class, Organism.class, Event.class);
        return monitoredEntities.stream().anyMatch(e -> e.getSimpleName().equals(entityClass.getSimpleName()));
    }

    private Class<?> getEntityClassFromName(String entityName) {
        try {
            //les entités se trouvent dans ce package
            String basePackage = "com.billariz.corp.database.model";
            String className = basePackage + "." + WebRequestUtils.toPascalCase(entityName);
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            System.err.println("⚠️ Class not found for the entity : " + entityName);
            return null;
        }
    }
    // "/admin/instances".equalsIgnoreCase(request.getRequestURI())
    private boolean isActuator(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/v1/admin/") || path.startsWith("/v1/actuator/")){ 
            return true;
        }
        else return false;
    }

        private boolean isExplorer(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/v1/explorer/") || path.startsWith("/v1/")){ 
            return true;
        }
        else return false;
    }

    private void extractUsernameAndGroupsFromToken(String token, Locale locale) {
        try {
            // Décoder le payload du JWT
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
               throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    messageSource.getMessage("INVALID_TOKEN", null, locale));
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            // Parser le payload JSON
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> claims = mapper.readValue(payload, Map.class);

            // Extraire le champ "username" ou "sub" (selon la structure du token)
            this.userName =  claims.getOrDefault("cognito:username", claims.getOrDefault("sub", "Anonymous")).toString();
            // Récupération des groupes
            this.groups = (List<String>) claims.getOrDefault("cognito:groups",null);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                messageSource.getMessage("INVALID_TOKEN", null, locale));
        }
    }

     // 🔥 Récupération et modification des paramètres en fonction des permissions
    private Map<String, String[]> transformParamsBasedOnPermissions(HttpServletRequest request, User user, String condition, Class<?> entityClass) {
        Map<String, String[]> transformedParams = new HashMap<>(request.getParameterMap());
        
        if ("SELF".equalsIgnoreCase(condition)) {
            transformedParams.put(entityClass==User.class ? "id" : "userId", new String[]{user.getId().toString()});
        }
        if ("GROUP".equalsIgnoreCase(condition)) {
            transformedParams.put(entityClass==Group.class ? "id" : "groupId", new String[]{user.getGroupId().toString()});
        }
        if ("ORGANISM".equalsIgnoreCase(condition)) {
            transformedParams.put(entityClass==Group.class ? "id" : "organismId", new String[]{user.getOrganismId().toString()});
        }
        return transformedParams;
    }

    // 🔥 Extraction de l'entité à partir de l'URL
    private String resolveEntityFromUri(String uri, int part) {
        String[] parts = uri.split("/");
        if (parts.length <= part) {
            return null;
        }
        String entity = WebRequestUtils.toSingularUppercase(parts[part]);
        // Si "DOWNLOAD" est détecté, on avance d'un index supplémentaire si possible
        if ("DOWNLOAD".equals(entity) && parts.length > part + 1) {
            entity = WebRequestUtils.toSingularUppercase(parts[part + 1]);
        }
        return entity;
    }

    // 🔥 Détermination de l'action en fonction de la méthode HTTP
    private String resolveActionFromHttpMethod(String method) {
        return switch (method.toUpperCase()) {
            case "GET" -> "READ";
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> null;
        };
    }

}


