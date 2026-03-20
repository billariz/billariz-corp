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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.billariz.corp.database.model.chart.Series;
import com.billariz.corp.database.model.chart.TimeSeriesChart;
import com.billariz.corp.database.repository.BillRepository;
import com.billariz.corp.database.repository.BillSegmentRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.MeterReadRepository;

@Service
public class ChartService {

    private final EventRepository eventRepository;

    private final MeterReadRepository meterReadRepository;

    private final BillSegmentRepository billSegmentRepository;

    private final BillRepository billRepository;

    public ChartService(EventRepository eventRepository, 
                        MeterReadRepository meterReadRepository,
                        BillSegmentRepository billSegmentRepository,
                        BillRepository billRepository
    ) {
        this.eventRepository = eventRepository;
        this.meterReadRepository = meterReadRepository;
        this.billSegmentRepository= billSegmentRepository;
        this.billRepository = billRepository;
    }

    public List<TimeSeriesChart> getAllCharts(LocalDateTime start, LocalDateTime end, String granularity) {
        List<TimeSeriesChart> charts = new ArrayList<>();

        charts.add(buildEventsPerPeriodGroupedByStatus(start, end, granularity));
        charts.add(buildEventsPerPeriodGroupedByType(start, end, granularity));
        charts.add(buildMeterReadPerPeriodGroupedByStatus(start, end, granularity));
        charts.add(buildMeterReadQtPerPeriodGroupedByStatus(start, end, granularity));
        charts.add(buildBillSegmentPerPeriodGroupedByStatus(start, end, granularity));
        charts.add(buildBillPerPeriodGroupedByStatus(start, end, granularity));
        charts.add(buildBillAmountPerPeriodGroupedByStatus(start, end, granularity));
        
        return charts;
    }

    public TimeSeriesChart buildEventsPerPeriodGroupedByStatus(LocalDateTime start, LocalDateTime end, String granularity) {
        List<Object[]> rows = eventRepository.countEventsByGranularity(start, end, granularity);
        return buildGroupedTimeSeriesChart(
            "eventsByStatus",
            rows,
            "eventStatus",
            "verticalBar",
            start,
            end,
            granularity,
            "events"
        );
    }

    public TimeSeriesChart buildEventsPerPeriodGroupedByType(LocalDateTime start, LocalDateTime end, String granularity) {
        List<Object[]> rows = eventRepository.countEventsByType(start, end, granularity);
        return buildGroupedTimeSeriesChart(
            "eventsByType",
            rows,
            "eventType",
            "verticalBar",
            start,
            end,
            granularity,
            "events"
        );
    }

    public TimeSeriesChart buildMeterReadPerPeriodGroupedByStatus(LocalDateTime start, LocalDateTime end, String granularity) {
        List<Object[]> rows = meterReadRepository.countMeterReadByStatus(start, end, granularity);
        return buildGroupedTimeSeriesChart(
            "meterReadByStatus",
            rows,
            "meterReadStatus",
            "verticalBar",
            start,
            end,
            granularity,
            "meterReads"
        );
    }

    public TimeSeriesChart buildMeterReadQtPerPeriodGroupedByStatus(LocalDateTime start, LocalDateTime end, String granularity) {
        List<Object[]> rows = meterReadRepository.countMeterReadQtByStatus(start, end, granularity);
        return buildGroupedTimeSeriesChart(
            "meterReadQuantityByStatus",
            rows,
            "meterReadStatus",
            "donut",
            start,
            end,
            granularity,
            "kWh"
        );
    }

    public TimeSeriesChart buildMeterReadQtPerPeriodAndContract(LocalDateTime start, LocalDateTime end, String granularity, Long posId, Long contractId) {
        List<Object[]> rows = meterReadRepository.countMeterReadQtByContractOrPos(start, end, granularity, posId, contractId);
        return buildGroupedTimeSeriesChart(
            "meterReadQuantityByContractOrPos",
            rows,
            "",
            "bar",
            start,
            end,
            granularity,
            "kWh"
        );
    }

    public TimeSeriesChart buildBillSegmentPerPeriodGroupedByStatus(LocalDateTime start, LocalDateTime end, String granularity) {
        List<Object[]> rows = billSegmentRepository.countBillSegmentByStatus(start, end, granularity);
        return buildGroupedTimeSeriesChart(
            "billSegmentByStatus",
            rows,
            "billSegmentStatus",
            "donut",
            start,
            end,
            granularity,
            "€"
        );
    }

    public TimeSeriesChart buildBillPerPeriodGroupedByStatus(LocalDateTime start, LocalDateTime end, String granularity) {
        List<Object[]> rows = billRepository.countBillByStatus(start, end, granularity);
        return buildGroupedTimeSeriesChart(
            "billByStatus",
            rows,
            "billStatus",
            "verticalBar",
            start,
            end,
            granularity,
            "bill"
        );
    }

    public TimeSeriesChart buildBillAmountPerPeriodGroupedByStatus(LocalDateTime start, LocalDateTime end, String granularity) {
        List<Object[]> rows = billRepository.countBillAmountByStatus(start, end, granularity);
        return buildGroupedTimeSeriesChart(
            "billAmountByStatus",
            rows,
            "billStatus",
            "line",
            start,
            end,
            granularity,
            "€"
        );
    }

    public TimeSeriesChart generateChart(String chartId, LocalDateTime start, LocalDateTime end, String granularity, Long posId, Long contractId) {
        return switch (chartId) {
            case "eventsByStatus" -> buildEventsPerPeriodGroupedByStatus(start, end, granularity);
            case "eventsByType" -> buildEventsPerPeriodGroupedByType(start, end, granularity);
            case "meterReadByStatus" -> buildMeterReadPerPeriodGroupedByStatus(start, end, granularity);
            case "meterReadQuantityByStatus" -> buildMeterReadQtPerPeriodGroupedByStatus(start, end, granularity);
            case "billSegmentByStatus" -> buildBillSegmentPerPeriodGroupedByStatus(start, end, granularity);
            case "billByStatus" -> buildBillPerPeriodGroupedByStatus(start, end, granularity);
            case "billAmountByStatus" -> buildBillAmountPerPeriodGroupedByStatus(start, end, granularity);
            case "meterReadQuantityByContractOrPos" -> buildMeterReadQtPerPeriodAndContract(start, end, granularity, posId, contractId);
            default -> throw new IllegalArgumentException("Unknown chartId: " + chartId);
        };
    }

    public List<Map<String, String>> listAvailableCharts() {
        List<Map<String, String>> charts = new ArrayList<>();

        charts.add(Map.of("chartId", "eventsByStatus", "label", "Number of events per status"));
        charts.add(Map.of("chartId", "eventsByType", "label", "Number of events per type"));
        charts.add(Map.of("chartId", "meterReadByStatus", "label", "number of meter read per status"));
        charts.add(Map.of("chartId", "meterReadQuantityByStatus", "label", "Quantities per status"));
        charts.add(Map.of("chartId", "billSegmentByStatus", "label", "Bill segments per status"));
        charts.add(Map.of("chartId", "billByStatus", "label", "Bills per status"));
        charts.add(Map.of("chartId", "billAmountByStatus", "label", "Amounts billed per bill status"));
        charts.add(Map.of("chartId", "meterReadQuantityByContractOrPos", "label", "Quantities per point of service and/or contract"));

        return charts;
    }

    private TimeSeriesChart buildGroupedTimeSeriesChart(
        String title,
        List<Object[]> rows,
        String labelKey,
        String chartType,
        LocalDateTime start,
        LocalDateTime end,
        String granularity,
        String unit
    ) {
        Map<LocalDate, Map<String, Long>> tempMap = new TreeMap<>();

        for (Object[] row : rows) {
            LocalDate date = ((java.sql.Timestamp) row[0]).toLocalDateTime().toLocalDate();
            String group = row[1].toString();
            Long count = ((Number) row[2]).longValue();

            tempMap.computeIfAbsent(date, d -> new HashMap<>()).put(group, count);
        }

        List<String> labels = tempMap.keySet().stream()
            .map(LocalDate::toString)
            .collect(Collectors.toList());

        Set<String> allGroups = tempMap.values().stream()
            .flatMap(m -> m.keySet().stream())
            .collect(Collectors.toSet());

        List<Series> seriesList = new ArrayList<>();

        for (String group : allGroups) {
            List<Number> data = new ArrayList<>();
            for (LocalDate date : tempMap.keySet()) {
                data.add(tempMap.get(date).getOrDefault(group, 0L));
            }
            seriesList.add(new Series(group, data));
        }

        return TimeSeriesChart.builder()
            .title(title)
            .type(chartType)
            .labels(labels)
            .series(seriesList)
            .meta(Map.of(
                "unit", unit,
                "titleTranslationKey", "chartsTitle",
                "labelTranslationKey", "",
                "serieTranslationKey", labelKey,
                "granularity", granularity,
                "granularityType", "eventChartGranularity",
                "startDate", start.toString(),
                "endDate", end.toString()
            ))
            .build();
    }
}
