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

package com.billariz.corp.app.utils;

import org.springframework.data.domain.Pageable;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PageUtils
{
    public static final Pageable defaultPage = Pageable.ofSize(55);

    public Pageable getOrDefault(Pageable pageable)
    {
        if (pageable == null)
            return defaultPage;
        return pageable;
    }
}
