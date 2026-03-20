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

package com.billariz.corp.notifier.billingRun;

import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import com.billariz.corp.database.model.BillingRun;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BillingRunNotifier
{
    private final List<BillingRunCaptor> notifiers;

    private static BillingRunNotifier    instance;

    @PostConstruct
    void postConstruct()
    {
        instance = this;
    }

    public static void onPrePersistOrUpdate(BillingRun billingRun)
    {
        instance.notifiers.forEach(n -> n.onPrePersistOrUpdate(billingRun));
    }

    public static void onPostPersistOrUpdate(BillingRun billingRun)
    {
        instance.notifiers.forEach(n -> n.onPostPersistOrUpdate(billingRun));
    }
}
