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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import com.billariz.corp.provider.exception.ProviderException;

@ActiveProfiles(Constants.PROVIDER_NAME)
@ContextConfiguration(classes = TestConfig.class)
@SpringBootTest
public class LocalStorageProviderTest
{
    @Autowired
    private LocalStorageProvider storageProvider;

    @Test
    void testDirectLink()
    {
        assertThrows(UnsupportedOperationException.class, () -> storageProvider.generateDirectDownloadLink("test1"));
    }

    @Test
    void testSimple() throws ProviderException
    {
        byte[] data = new byte[] { 0x01, 0x02, 0x03 };
        var path = "test1";

        storageProvider.save(data, path);
        assertArrayEquals(data, storageProvider.read(path));
    }
}
