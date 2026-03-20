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

package com.billariz.corp.app.service;

import org.springframework.stereotype.Service;
import com.billariz.corp.database.model.User;
import com.billariz.corp.database.repository.RolePermissionRepository;
import com.billariz.corp.database.repository.UserPermissionRepository;

@Service
public class AuthorizationService {
    /**
     * Vérifie si l'utilisateur authentifié a la permission pour effectuer une action donnée sur une entité.
     *
     * @param entity        Nom de l'entité (par ex., "Invoice")
     * @param action        Action à vérifier (par ex., "read", "create", "update", "delete")
     * @param user Objet User de l'utilisateur courant
     * @return true si l'utilisateur a la permission, false sinon
     */

    private final RolePermissionRepository  rolePermissionRepository;

    private final UserPermissionRepository  userPermissionRepository;

    public AuthorizationService( RolePermissionRepository  rolePermissionsRepository, UserPermissionRepository  userPermissionsRepository) {
        this.rolePermissionRepository = rolePermissionsRepository;
        this.userPermissionRepository = userPermissionsRepository;
    }

    public String getAuthorization(String entity, String action, User user) {
        // Vérifier si l'utilisateur a la permission via ses rôles
        var perm = user.getRoles().stream()
                            .flatMap(role -> rolePermissionRepository.findByRoleAndEntityAndAction(role.getId(), action, entity).stream())
                            .findFirst().orElse(null);
        if(perm ==null){
            var uPerm = userPermissionRepository.findByUserAndPermisson(user.getId(),action, entity);
            return uPerm == null ? null : uPerm.getRestriction();
        }
        return perm == null ? null : perm.getRestriction();
    }
}