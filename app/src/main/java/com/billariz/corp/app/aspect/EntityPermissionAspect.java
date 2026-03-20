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

// package com.billariz.corp.app.aspect;
// import org.aspectj.lang.ProceedingJoinPoint;
// import org.aspectj.lang.annotation.Around;
// import org.aspectj.lang.annotation.Aspect;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.context.MessageSource;
// import org.springframework.context.i18n.LocaleContextHolder;
// import org.springframework.data.jpa.domain.Specification;
// import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
// import org.springframework.security.access.AccessDeniedException;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.stereotype.Component;
// import org.springframework.web.context.request.RequestAttributes;
// import org.springframework.web.context.request.RequestContextHolder;
// import org.springframework.web.context.request.ServletRequestAttributes;
// import com.billariz.corp.app.security.AuthorizationService;
// import com.billariz.corp.app.utils.WebRequestUtils;
// import com.billariz.corp.database.model.Group;
// import com.billariz.corp.database.model.Organism;
// import com.billariz.corp.database.model.Permission;
// import com.billariz.corp.database.model.User;
// import com.billariz.corp.database.repository.UserRepository;
// import com.billariz.corp.database.specification.PermissionSpecification;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Locale;
// import java.util.Map;
// import javax.servlet.http.HttpServletRequest;
// import org.springframework.data.domain.PageRequest;
// import org.springframework.data.domain.Pageable;
// import javax.persistence.criteria.*;


// // a transfomer en filtre si dans le futur il n'apparait pas de besoin de pratiquer des restriction au niveau de la couche métier
//     // ce qui semble être le cas
// // comment filtrer les permissions selon les permissions du userCourant
// @Component
// @Aspect
// public class EntityPermissionAspect {

//     @Autowired
//     private AuthorizationService authorizationService;

//     @Autowired
//     private UserRepository userRepository;

//     @Autowired
//     private MessageSource messageSource;

//     private static final ThreadLocal<Boolean> isAspectExecuting = ThreadLocal.withInitial(() -> false);

//     private final Locale locale = LocaleContextHolder.getLocale();

//     private static final Logger logger = LoggerFactory.getLogger(EntityPermissionAspect.class);
//     /**
//      * Aspect to intercept repository actions and apply permission checks.
//      */
//     @Around("execution(* org.springframework.data.repository.CrudRepository+.*(..))")
//     public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
//         if (isAspectExecuting.get()) {
//             return joinPoint.proceed(); // Empêche la récursion
//         }
//         try {
//             isAspectExecuting.set(true);
//             if(isHttpRequest() && !isActuator()) {
//                 //Retrieve the authenticated user
//                 Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//                 if (authentication == null) 
//                     throw new AccessDeniedException(messageSource.getMessage("USER_NOT_AUTHENTIFICATED", new Object[]{authentication}, locale));
//                 //Récupérer l'utilisateur depuis la base de données
//                 String userName = authentication.getName();
//                 var user = userRepository.findByUserName(userName)
//                     .orElseThrow(() -> new AccessDeniedException(messageSource.getMessage("USER_NOT_FOUND", new Object[]{userName}, locale)));
//                 //Extract entityName from HTTP URI
//                 String entityName = resolveEntityFromUri(getRequestUri());
//                 if (entityName == null) 
//                     throw new AccessDeniedException(messageSource.getMessage("ENTITY_NOT_FOUND", new Object[]{entityName}, locale));
//                 // Extract action from HTTP method
//                 String action = resolveActionFromHttpMethod();
//                 if (action == null) 
//                     throw new AccessDeniedException(messageSource.getMessage("HTTP_METHOD_NOT_FOUND", new Object[]{action}, locale));

//                 // Check permissions for the requested action
//                 Permission permission = authorizationService.getAuthorization(entityName, action, user);
//                 if (permission==null) {
//                     throw new AccessDeniedException(messageSource.getMessage("UNAUTHORIZED", new Object[]{action, entityName}, locale));
//                 }

//                 String condition = permission.getCondition() == null ? "full" : permission.getCondition();
//                 Object repository = joinPoint.getTarget();
//                 if (repository instanceof JpaSpecificationExecutor && !condition.equalsIgnoreCase("full")) {
//                     Class<?> entityClass = getEntityClassFromName(entityName);
//                     if (entityClass != null) {
//                         //A GARDER POUR EXMPLE SPECIFICATION
//                         // Specification<Object> permissionSpec = (Specification<Object>) PermissionSpecification.withFilters(user, condition, entityClass);
//                         // Map<String, String[]> params = getHttpRequestParams();
//                         // Specification<Object> querySpec = (Specification<Object>) buildSpecificationFromParams(params, entityClass);
//                         // Specification<Object> finalSpec = permissionSpec.and(querySpec);
//                         // Pageable pageable = extractPageable(joinPoint.getArgs());
//                         // return ((JpaSpecificationExecutor<Object>) repository).findAll(finalSpec, pageable);
//                        

//                         Map<String, String[]> params = getHttpRequestParams(user, condition, entityClass);
//                         //RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
//                         // if (requestAttributes instanceof ServletRequestAttributes) {
//                         //     ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
//                         //     HttpServletRequest request = servletRequestAttributes.getRequest();
                        
//                         //     // Créer un wrapper autour de la requête
//                         //     ModifiableHttpServletRequest wrappedRequest = new ModifiableHttpServletRequest(request);
                        
//                         //     // Modifier les paramètres
//                         //     for (Map.Entry<String, String[]> entry : params.entrySet()) {
//                         //         wrappedRequest.setParameter(entry.getKey(), entry.getValue());
//                         //     }
                        
//                         //     // Réinjecter la requête modifiée
//                         //     ServletRequestAttributes updatedAttributes = new ServletRequestAttributes(wrappedRequest);
//                         //     RequestContextHolder.setRequestAttributes(updatedAttributes);

//                         // }
//                     }
//                 }
//             }
//             return joinPoint.proceed(); // Continuer si ce n'est pas une requête HTTP ou entityClass standard
//         } finally {
//             isAspectExecuting.set(false); // Libère le garde-fou
//         }
//     }

//     private Pageable extractPageable(Object[] args) {
//         for (Object arg : args) {
//             if (arg instanceof Pageable) {
//                 return (Pageable) arg;
//             }
//         }
//         return PageRequest.of(0, 10); // Valeur par défaut si aucun Pageable n'est trouvé
//     }

//     private <T> Specification<T> buildSpecificationFromParams(Map<String, String[]> params, Class<T> entityClass) {
//         return (root, query, criteriaBuilder) -> {
//             Predicate predicate = criteriaBuilder.conjunction(); // Initialise une condition "true"
//             for (Map.Entry<String, String[]> entry : params.entrySet()) {
//                 String field = entry.getKey();
//                 String[] values = entry.getValue();
    
//                 if (values.length == 1) {
//                     // Vérifie si le champ existe dans l'entité
//                     try {
//                         entityClass.getDeclaredField(field);
//                         predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get(field), values[0]));
//                     } catch (NoSuchFieldException e) {
//                         logger.warn("⚠️ Champ {} non trouvé dans {}", field, entityClass.getSimpleName());
//                     }
//                 } else if (values.length > 1) {
//                     try {
//                         entityClass.getDeclaredField(field);
//                         predicate = criteriaBuilder.and(predicate, root.get(field).in((Object[]) values));
//                     } catch (NoSuchFieldException e) {
//                         logger.warn("⚠️ Champ {} non trouvé dans {}", field, entityClass.getSimpleName());
//                     }
//                 }
//             }
//             return predicate;
//         };
//     }

//     private Map<String, String[]> getHttpRequestParams() {
//         RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
//         if (requestAttributes instanceof ServletRequestAttributes) {
//             HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
//             return request.getParameterMap(); // Récupère tous les paramètres sous forme de Map
//         }
//         return new HashMap<>();
//     }

//     private Map<String, String[]> getHttpRequestParams(User user, String condition, Class<?> entityClass) {
//         RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
//         if (requestAttributes instanceof ServletRequestAttributes) {
//             HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
//             Map<String, String[]> params = new HashMap<>(request.getParameterMap()); // Copie des paramètres actuels
    
//             // 🔥 Appliquer les mêmes filtres que `PermissionSpecification.withFilters`
//             Map<String, String[]> transformedParams = transformParamsBasedOnPermissions(user, condition, params, entityClass);
            
//             return transformedParams;
//         }
//         return new HashMap<>();
//     }

//     private Map<String, String[]> transformParamsBasedOnPermissions(User user, String condition, Map<String, String[]> params, Class<?> entityClass) {
//         Map<String, String[]> transformedParams = new HashMap<>(params);
        
//         if ("SELF".equalsIgnoreCase(condition)) {
//             transformedParams.put(entityClass==User.class ? "id" : "userId", new String[]{user.getId().toString()});
//         }
//         if ("GROUP".equalsIgnoreCase(condition)) {
//             transformedParams.put(entityClass==Group.class ? "id" : "groupId", new String[]{user.getGroupId().toString()});
//         }
//         if ("ORGANISM".equalsIgnoreCase(condition)) {
//             transformedParams.put(entityClass==Group.class ? "id" : "organismId", new String[]{user.getOrganismId().toString()});
//         }
//         // Vérifier et ajuster les champs en fonction de la classe de l'entité
//         // A VOIR SI INTERET
//         transformedParams.entrySet().removeIf(entry -> {
//             String field = entry.getKey();
//             try {
//                 entityClass.getDeclaredField(field); // Vérifie si le champ existe
//                 return false; // Le champ est valide
//             } catch (NoSuchFieldException e) {
//                 logger.warn("⚠️ Le champ {} n'existe pas dans {}", field, entityClass.getSimpleName());
//                 return true; // Supprime le champ non valide
//             }
//         });
    
//         return transformedParams;
//     }

//     private Class<?> getEntityClassFromName(String entityName) {
//         try {
//             //les entités se trouvent dans ce package
//             String basePackage = "com.billariz.corp.database.model";
//             String className = basePackage + "." + WebRequestUtils.toPascalCase(entityName);
//             return Class.forName(className);
//         } catch (ClassNotFoundException e) {
//             System.err.println("⚠️ Impossible de trouver la classe pour l'entité : " + entityName);
//             return null;
//         }
//     }

//     private boolean isHttpRequest() {
//         RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
//         return requestAttributes instanceof ServletRequestAttributes;
//     }

//     private boolean isActuator() {
//         String path = getRequestUri();
//         if (path.startsWith("/build/admin/") || path.startsWith("/build/actuator/")){ 
//             return true;
//         }
//         else return false;
//     }

//     /**
//      * Resolves the action based on the HTTP method.
//      * @param httpMethod HTTP method of the request
//      * @return Corresponding action ("read", "create", "update", "delete") or null if unsupported
//      */
//     private String resolveActionFromHttpMethod() {
//         RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
//         if (requestAttributes instanceof ServletRequestAttributes) {
//             HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
//             String httpMethod = request.getMethod();
//             if ("GET".equals(httpMethod)) {
//                 return "read";
//             } else if ("POST".equals(httpMethod)) {
//                 return "create";
//             } else if ("PUT".equals(httpMethod) || "PATCH".equals(httpMethod)) {
//                 return "update";
//             } else if ("DELETE".equals(httpMethod)) {
//                 return "delete";
//             }
//         }
//         return null;
//     }

//     private String getRequestUri() {
//         RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
//         if (requestAttributes instanceof ServletRequestAttributes) {
//             HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
//             return request.getRequestURI();
//         }
//         return null;
//     }

//     private String resolveEntityFromUri(String uri) {
//         // Extraire l'entité depuis l'URI (par exemple : "/users", "/contracts")
//         String[] segments = uri.split("/");
//         if (segments.length > 1) {
//             return WebRequestUtils.toSingularUppercase(segments[2]); // Retourne le 2nd segment (par exemple, "users")
//         }
//         return null;
//     }  


    
// }