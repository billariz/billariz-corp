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

import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface RepositoryReadSingle<T, I> extends Repository<T, I>
{
    long count();

    boolean existsById(I id);

    Optional<T> findById(I id);
}
