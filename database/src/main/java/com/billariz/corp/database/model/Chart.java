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

package com.billariz.corp.database.model;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Chart {
    private String title; // titre du graphique
    private String type; // ex: "bar", "pie", "line"
    private List<String> labels; // ex: ["Actifs", "En erreur"]
    private List<Number> values; // ex: [12, 5]
    private Map<String, Object> meta; // optionnel : pour infos supplémentaires


    public Chart(String title, String type, List<String> labels, List<Number> values, Map<String, Object> meta) {
        this.title = title;
        this.type = type;
        this.labels = labels;
        this.values = values;
        this.meta = meta;
    }
}

