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

package com.billariz.corp.provider;

import com.billariz.corp.provider.exception.ProviderException;

public interface StorageProvider
{
    String generateDirectDownloadLink(String... path) throws ProviderException;

    byte[] read(String... path) throws ProviderException;

    void save(byte[] content, String... path) throws ProviderException;
}
