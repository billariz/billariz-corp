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

package com.billariz.corp.app.controller;

import org.springframework.web.bind.annotation.RestController;
import com.billariz.corp.app.service.ChartService;
import com.billariz.corp.database.model.Chart;
import com.billariz.corp.database.model.chart.TimeSeriesChart;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/charts")
public class ChartController {

    private final ChartService chartService;

    public ChartController(ChartService chartService) {
        this.chartService = chartService;
    }

    @GetMapping("/dashboard")
    public List<TimeSeriesChart> getAllCharts(
        @RequestParam(value = "startDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

        @RequestParam(value = "endDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        
        @RequestParam(value = "granularity", required = false, defaultValue = "DAILY") String granularity
    ) {
        
        if (startDate == null) {
            startDate = getStartDate(granularity);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return chartService.getAllCharts(start, end, granularity);
    }

    @GetMapping("/{chartId}")
    public TimeSeriesChart getChartById(
        @PathVariable String chartId,
        @RequestParam(value = "startDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

        @RequestParam(value = "endDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

        @RequestParam(value = "granularity", required = false, defaultValue = "DAILY") String granularity,

        @RequestParam(value = "posId", required = false) Long posId,

        @RequestParam(value = "contractId", required = false) Long contractId
    ) {
        if (startDate == null) {
            startDate = getStartDate(granularity);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return chartService.generateChart(chartId, start, end, granularity, posId, contractId);
    }

    @GetMapping("/catalog")
    public List<Map<String, String>> getChartCatalog() {
        return chartService.listAvailableCharts();
    }

    private LocalDate getStartDate(String granularity){
        switch (granularity) {
            case "DAILY":
                return LocalDate.now().minusDays(6);
            case "WEEKLY":
                return LocalDate.now().minusWeeks(6);
            case "MONTHLY":
                return LocalDate.now().minusMonths(6);
            case "YEARLY":
                return LocalDate.now().minusYears(6);
            default:
                return LocalDate.now().minusDays(6);
        }
    } 
}
