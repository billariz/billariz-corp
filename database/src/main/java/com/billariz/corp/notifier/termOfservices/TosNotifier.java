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

package com.billariz.corp.notifier.termOfservices;

import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import com.billariz.corp.database.model.TermOfService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TosNotifier
{
    private final List<TosCaptor> notifiers;

    private static TosNotifier    instance;

    @PostConstruct
    void postConstruct()
    {
        instance = this;
    }

    public static void onPreUpdate(TermOfService tos)
    {
        instance.notifiers.forEach(n -> n.onPreUpdate(tos));
    }

    public static void onPrePersist(TermOfService tos)
    {
        instance.notifiers.forEach(n -> n.onPrePersist(tos));
    }

    public static void onPostPersistOrUpdate(TermOfService tos)
    {
        instance.notifiers.forEach(n -> n.onPostPersistOrUpdate(tos));
    }
}
