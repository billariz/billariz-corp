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

import java.util.Comparator;
import com.billariz.corp.database.model.light.EligibleServiceElement;

public class BillingEligibleSEComparator extends BillingBaseComparator implements Comparator<EligibleServiceElement>
{
    @Override
    public int compare(EligibleServiceElement o1, EligibleServiceElement o2)
    {
        return comparatorConditions(o1.getSqType(), o2.getSqType());
    }
}