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

package com.billariz.corp.notifier.bill;

import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import com.billariz.corp.database.model.Bill;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BillNotifier
{
    private final List<BillCaptor> notifiers;

    private static BillNotifier    instance;

    @PostConstruct
    void postConstruct()
    {
        instance = this;
    }

    public static void onPrePersist(Bill bill)
    {
        instance.notifiers.forEach(n -> n.onPrePersist(bill));
    }

    public static void onPreUpdate(Bill bill)
    {
        instance.notifiers.forEach(n -> n.onPreUpdate(bill));
    }

    public static void onPostPersistOrUpdate(Bill bill)
    {
        instance.notifiers.forEach(n -> n.onPostPersistOrUpdate(bill));
    }

    public static void onPreRemove(Bill bill)
    {
        instance.notifiers.forEach(n -> n.onPreRemove(bill));
    }
}
