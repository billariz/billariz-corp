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

package com.billariz.corp.app.utils;

import java.util.Base64;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.NativeWebRequest;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;


@UtilityClass
@Slf4j
public class WebRequestUtils
{
    private String getAccessToken(NativeWebRequest nativeWebRequest) {
        var authorizationHeader = nativeWebRequest.getHeader(HttpHeaders.AUTHORIZATION);
        
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);  // Extrait le token en retirant "Bearer "
        }
    
        log.warn("Authorization header is missing or does not start with 'Bearer'");
        return null;
    }
    
    public String getUserNameFromAccessToken(NativeWebRequest nativeWebRequest) {
        
        var token = getAccessToken(nativeWebRequest);
    
        if (token == null) {
            log.warn("No access token found in request");
            return "NO_TOKEN";
        }
    
        try {
            var decoder = Base64.getUrlDecoder();
            var chunks = token.split("\\.");
            
            if (chunks.length < 2) {
                log.error("Token format is invalid. Expected 3 parts but found {}", chunks.length);
                return "INVALID_TOKEN";
            }
    
            var payload = new String(decoder.decode(chunks[1]));
    
            var json = new JSONObject(payload);
            String username = json.optString("cognito:username", "ANONYMOUS");
    
            log.info("Extracted username from token: {}", username);
            return username;
        } catch (Exception e) {
            log.error("Failed to decode token payload", e);
            return "TOKEN_NO_NAME";
        }
    }

    /**
     * Convertit une chaîne en PascalCase (ex: "USER" -> "User", "BILLING_CHARGE" -> "BillingCharge")
     * @param input la chaîne d'entrée
     * @return la chaîne en PascalCase
     */
    public static String toPascalCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Normaliser en minuscules et séparer les parties par "_"
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

    /**
     * Convertit une chaîne en camelCase (ex: "USER" -> "user", "BILLING_CHARGE" -> "billingCharge")
     * @param input la chaîne d'entrée
     * @return la chaîne en camelCase
     */
    public static String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String pascalCase = toPascalCase(input);
        return Character.toLowerCase(pascalCase.charAt(0)) + pascalCase.substring(1);
    }


    public String toSingularUppercase(String plural) {
        String result;
        
        // Gérer les pluriels réguliers
        if (plural.endsWith("ies")) {
            result = plural.substring(0, plural.length() - 3) + "y";
        } else if (plural.endsWith("ves")) {
            result = plural.substring(0, plural.length() - 3) + "f";
        } else if (plural.endsWith("s") && !plural.endsWith("ss")) {
            result = plural.substring(0, plural.length() - 1);
        } else {
            result = plural;
        }
    
        // Insérer des underscores
        result = result.replaceAll("(.)([A-Z])", "$1_$2");
    
        // Convertir en majuscules
        return result = result.toUpperCase();
    }
}
