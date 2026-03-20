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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.HashMap;
import java.util.Map;

public class ModifiableHttpServletRequest extends HttpServletRequestWrapper {
    private final Map<String, String[]> modifiableParameters;

    public ModifiableHttpServletRequest(HttpServletRequest request) {
        super(request);
        this.modifiableParameters = new HashMap<>(request.getParameterMap());
    }

    public void setParameter(String name, String value) {
        modifiableParameters.put(name, new String[]{value});
    }

    public void setParameter(String name, String[] values) {
        modifiableParameters.put(name, values);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return modifiableParameters;
    }

    @Override
    public String getParameter(String name) {
        String[] values = modifiableParameters.get(name);
        return values != null && values.length > 0 ? values[0] : null;
    }

    @Override
    public String[] getParameterValues(String name) {
        return modifiableParameters.get(name);
    }
}
