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

import java.time.DayOfWeek;
import java.time.LocalDate;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ActivityTemplateUtils
{
    public static LocalDate computeNextDate(String periodSystem, int startDatePeriod)
    {
        if ("CALENDAR".equalsIgnoreCase(periodSystem))
            return LocalDate.now().plusDays(startDatePeriod);
        else if ("WORKDAYS".equalsIgnoreCase(periodSystem))
            return forBusinessDaysOnly(LocalDate.now(), startDatePeriod);
        return null;
    }

    private static LocalDate forBusinessDaysOnly(LocalDate date, int afterNumberOfDays)
    {
        var tmpDays = afterNumberOfDays;

        if (tmpDays == 0 && (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY))
            return date;
        if ((date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY))
            tmpDays -= 1;
        return forBusinessDaysOnly(date.plusDays(1), tmpDays);
    }
}
