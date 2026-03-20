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

package com.billariz.corp.launcher.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.ContractPointOfService;
import com.billariz.corp.database.model.MeterRead;
import com.billariz.corp.database.model.MeterReadDetail;
import com.billariz.corp.database.model.light.EligibleServiceElement;
import com.billariz.corp.database.repository.ContractPointOfServiceRepository;
import com.billariz.corp.launcher.exception.LauncherFatalException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class MeterReadUtils
{
    public static Contract checkContract(ContractPointOfServiceRepository ctrPosRepository, MeterRead meterRead)
    {
        var contractPosList = ctrPosRepository.findAllByPosRef(meterRead.getPosRef());
        var contractPos = contractPosList.stream().filter(ctrpos -> contractMatch(ctrpos, meterRead)).findFirst();
        return contractPos.get().getContract();
    }

    private boolean contractMatch(ContractPointOfService contractPos, MeterRead meterRead)
    {
        return ((meterRead.getStartDate().isAfter(contractPos.getStartDate()) || meterRead.getStartDate().isEqual(contractPos.getStartDate()))
                && (contractPos.getEndDate() == null || meterRead.getStartDate().isBefore(contractPos.getEndDate())) && (contractPos.getEndDate() == null
                        || meterRead.getEndDate().isBefore(contractPos.getEndDate()) || meterRead.getEndDate().isEqual(contractPos.getEndDate())));
    }

    public static BigDecimal calculateProRataQuantity(
            List<MeterReadDetail> periods,
            LocalDate targetStart,
            LocalDate targetEnd
    ) throws LauncherFatalException {
        // Vérifier si la liste ou les dates d'entrée sont invalides
        if (periods == null || periods.isEmpty() || targetStart == null || targetEnd == null || targetStart.isAfter(targetEnd)) {
            throw new LauncherFatalException("DATA_NOT_COMPLAINT", new Object[]{"calculateProRataQuantity"});
        }

        BigDecimal totalQuantity = BigDecimal.ZERO;

        for (MeterReadDetail period : periods) {
            // Trouver l'intersection entre la période actuelle et la période cible
            LocalDate intersectionStart = period.getStartDate().isAfter(targetStart) ? period.getStartDate() : targetStart;
            LocalDate intersectionEnd = period.getEndDate().isBefore(targetEnd) ? period.getEndDate() : targetEnd;

            // Si les dates d'intersection sont valides, calculer le prorata
            if (!intersectionStart.isAfter(intersectionEnd)) {
                long intersectionDays = ChronoUnit.DAYS.between(intersectionStart, intersectionEnd) + 1; // +1 pour inclure les deux dates
                long periodDays = ChronoUnit.DAYS.between(period.getStartDate(), period.getEndDate()) + 1;

                BigDecimal intersectionDaysBD = BigDecimal.valueOf(intersectionDays);
                BigDecimal periodDaysBD = BigDecimal.valueOf(periodDays);

                // Calculer la quantité au prorata pour cette période
                BigDecimal prorataQuantity = intersectionDaysBD
                .divide(periodDaysBD, MathContext.DECIMAL128) // Division avec précision
                .multiply(period.getQuantity());

                totalQuantity = totalQuantity.add(prorataQuantity); // Ajouter au total
            }
        }
        return totalQuantity;
    }

    public static BigDecimal calculateProRataQuantityFromMr(
            List<MeterRead> periods,
            LocalDate targetStart,
            LocalDate targetEnd
    ) throws LauncherFatalException {
        // Vérifier si la liste ou les dates d'entrée sont invalides
        if (periods == null || periods.isEmpty() || targetStart == null || targetEnd == null || targetStart.isAfter(targetEnd)) {
            throw new LauncherFatalException("DATA_NOT_COMPLAINT", new Object[]{"calculateProRataQuantityFromMr"});
        }

        BigDecimal totalQuantity = BigDecimal.ZERO;

        for (MeterRead period : periods) {
            // Trouver l'intersection entre la période actuelle et la période cible
            LocalDate intersectionStart = period.getStartDate().isAfter(targetStart) ? period.getStartDate() : targetStart;
            LocalDate intersectionEnd = period.getEndDate().isBefore(targetEnd) ? period.getEndDate() : targetEnd;

            // Si les dates d'intersection sont valides, calculer le prorata
            if (!intersectionStart.isAfter(intersectionEnd)) {
                long intersectionDays = ChronoUnit.DAYS.between(intersectionStart, intersectionEnd) + 1; // +1 pour inclure les deux dates
                long periodDays = ChronoUnit.DAYS.between(period.getStartDate(), period.getEndDate()) + 1;

                BigDecimal intersectionDaysBD = BigDecimal.valueOf(intersectionDays);
                BigDecimal periodDaysBD = BigDecimal.valueOf(periodDays);

                // Calculer la quantité au prorata pour cette période
                BigDecimal prorataQuantity = intersectionDaysBD
                .divide(periodDaysBD, MathContext.DECIMAL128) // Division avec précision
                .multiply(period.getTotalQuantity());

                totalQuantity = totalQuantity.add(prorataQuantity); // Ajouter au total
            }
        }
        return totalQuantity;
    }

    public static boolean hasContinuous12MonthHistory(List<MeterReadDetail> mrds) {
        // Vérifier si la liste est vide ou insuffisante
        if (mrds == null || mrds.isEmpty()) {
            return false;
        }

        // Trier les périodes par date de début
        mrds.sort((p1, p2) -> p1.getStartDate().compareTo(p2.getStartDate()));

        // Calculer la date de début et de fin de l'ensemble
        LocalDate overallStart = mrds.get(0).getStartDate();
        LocalDate overallEnd = mrds.get(mrds.size() - 1).getEndDate();

        // Vérifier si la période totale couvre au moins 12 mois
        if (ChronoUnit.DAYS.between(overallStart, overallEnd) < 365) {
            return false;
        }

        // Vérifier la continuité des périodes
        for (int i = 0; i < mrds.size() - 1; i++) {
            MeterReadDetail current = mrds.get(i);
            MeterReadDetail next = mrds.get(i + 1);

            // Vérifier qu'il n'y a pas d'interruption entre deux périodes
            if (!current.getEndDate().plusDays(1).equals(next.getStartDate())) {
                return false;
            }
        }
        // Si toutes les vérifications passent
        return true;
    }

    public static boolean hasContinuous12MonthHistoryOfMr(List<MeterRead> mrs) {
        // Vérifier si la liste est vide ou insuffisante
        if (mrs == null || mrs.isEmpty()) {
            return false;
        }
        // Trier les périodes par date de début
        mrs.sort((p1, p2) -> p1.getStartDate().compareTo(p2.getStartDate()));

        // Calculer la date de début et de fin de l'ensemble
        LocalDate overallStart = mrs.get(0).getStartDate();
        LocalDate overallEnd = mrs.get(mrs.size() - 1).getEndDate();

        // Vérifier si la période totale couvre au moins 12 mois
        if (ChronoUnit.DAYS.between(overallStart, overallEnd) < 365) {
            return false;
        }

        // Vérifier la continuité des périodes
        for (int i = 0; i < mrs.size() - 1; i++) {
            MeterRead current = mrs.get(i);
            MeterRead next = mrs.get(i + 1);

            // Vérifier qu'il n'y a pas d'interruption entre deux périodes
            if (!current.getEndDate().plusDays(1).equals(next.getStartDate())) {
                return false;
            }
        }
        // Si toutes les vérifications passent
        return true;
    }

    public LocalDate getMinDate(LocalDate dateTime1, LocalDate dateTime2) {
        if (dateTime1 == null) return dateTime2;
        if (dateTime2 == null) return dateTime1;
        return dateTime1.isBefore(dateTime2) ? dateTime1 : dateTime2;
    }

    public LocalDate getMaxDate(LocalDate dateTime1, LocalDate dateTime2) {
        if (dateTime1 == null) return dateTime2;
        if (dateTime2 == null) return dateTime1;
        return dateTime1.isAfter(dateTime2) ? dateTime1 : dateTime2;
    }

    public boolean checkMinimumNbrOfDay(EligibleServiceElement seMaster, LocalDate sd, LocalDate ed) throws LauncherFatalException
    {
        return calculateDaysBetween(sd,ed) >= seMaster.getMinDayForEstimate();
    }

    public Integer calculateDaysBetween(LocalDate startDate, LocalDate endDate) throws LauncherFatalException 
    {
        if (startDate == null || endDate == null) {
            throw new LauncherFatalException("DATA_NOT_COMPLAINT", new Object[]{"calculateDaysBetween - dates can't be null"});
        }
        long daysBetween =  ChronoUnit.DAYS.between(startDate, endDate);

        if (daysBetween > Integer.MAX_VALUE || daysBetween < Integer.MIN_VALUE) {
            throw new LauncherFatalException("DATA_NOT_COMPLAINT", new Object[]{"calculateDaysBetween - Integer capacities exceeded"});
        }
        return (int) daysBetween;
    }
    
}
