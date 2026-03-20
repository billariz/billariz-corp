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

package com.billariz.corp.database.listener;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import javax.persistence.PostLoad;
import org.springframework.stereotype.Component;
import com.billariz.corp.database.model.Relation;
import java.lang.reflect.Method;

@Component
public class RelationListener {

    @PostLoad
    private void resolveSecondObject(Relation relation) {
        String entityName = relation.getSecondObjectType().getValue();
        Class<?> entityClass = getEntityClassFromName(entityName);
        if (entityClass == null) {
            throw new RuntimeException("No class found for second Object of relationType : " + relation.getRelationType());
        }
        if (relation.getSecondObjectId() != null) {
            Object secondObject = findEntityById(entityClass, relation.getSecondObjectId());
            relation.setSecondObject(secondObject);
        }
    }

    private Class<?> getEntityClassFromName(String entityName) {
        try {
            String basePackage = "com.billariz.corp.database.model";
            String className = basePackage + "." + toPascalCase(entityName);
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No class found for entity : " + entityName, e);
        }
    }

    public static String toPascalCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String[] parts = input.toLowerCase().split("[_\\s]+");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)))
                      .append(part.substring(1));
            }
        }
        return result.toString();
    }

    public Object findEntityById(Class<?> entityClass, Long objectId) {
    if (entityClass == null || objectId == null) {
        throw new IllegalArgumentException("Entity class and object ID must not be null");
    }
    String repositoryName = entityClass.getSimpleName() + "Repository";
    repositoryName = Character.toLowerCase(repositoryName.charAt(0)) + repositoryName.substring(1);

    Object repository = SpringContextHelper.getBean(repositoryName);
    if (repository == null) {
        throw new RuntimeException("No repository found for entity: " + entityClass.getSimpleName());
    }

    try {
        Method findByIdMethod = repository.getClass().getMethod("findById", Object.class);
        Optional<?> result = (Optional<?>) findByIdMethod.invoke(repository, objectId);
        return result.orElse(null);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException("Failed to invoke findById on repository for entity: " + entityClass.getSimpleName(), e);
    }
}
}