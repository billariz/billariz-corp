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

package com.billariz.corp.provider.local;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.billariz.corp.provider.StorageProvider;
import com.billariz.corp.provider.exception.ProviderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class LocalStorageProvider implements StorageProvider
{
    private final String bucketName;

    @Override
    public String generateDirectDownloadLink(String... path) throws ProviderException
    {
        try
        {
            var key = toPath(path);
            log.debug("Generating presigned URL for key: {} of path : {}", key, path);
        }
        catch (Exception e)
        {
            throw new ProviderException("Unable to upload file: " + path, e);
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] read(String... path) throws ProviderException
    {
        try
        {
            var file = toPath(path);

            return Files.readAllBytes(file);
        }
        catch (Exception e)
        {
            throw new ProviderException("Unable to upload file: " + path, e);
        }
    }

    @Override
    public void save(byte[] content, String... path) throws ProviderException
    {
        try
        {
            var file = toPath(path);

            Files.write(file, content);
        }
        catch (Exception e)
        {
            throw new ProviderException("Unable to upload file: " + path, e);
        }
    }

    private Path toPath(String... path) throws IOException
    {
        var p = Path.of(Path.of("__local_data__", bucketName).toString(), path);

        Files.createDirectories(p.getParent());
        return p;
    }
}
