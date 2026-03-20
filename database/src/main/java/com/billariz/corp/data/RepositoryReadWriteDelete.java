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

package com.billariz.corp.data;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface RepositoryReadWriteDelete<T, I> extends RepositoryReadWrite<T, I>
{
    void deleteById(I id);

    Page<T> findAll(Pageable pageable);

}
