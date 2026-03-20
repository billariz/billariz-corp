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

package com.billariz.corp.launcher.billing;

public abstract class BillingBaseComparator
{
    public static final String SUM_EUR_FIX_BASED_ON_SA =  "SUM_EUR_FIX_BASED_ON_SA";

    int comparatorConditions(String sq1, String sq2)
    {
        if (SUM_EUR_FIX_BASED_ON_SA.equals(sq1)  && !SUM_EUR_FIX_BASED_ON_SA.equals(sq2))
            return 1;
        else if (SUM_EUR_FIX_BASED_ON_SA.equals(sq2) && !SUM_EUR_FIX_BASED_ON_SA.equals(sq1))
            return -1;
        return 0;
    }
}