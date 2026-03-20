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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.billariz.corp.provider.exception.ProviderException;

@ActiveProfiles(Constants.PROVIDER_NAME)
@ContextConfiguration(classes = TestConfig.class)
@SpringBootTest
public class JavaQueueProviderTest
{
    @Autowired
    private JavaQueueProvider queueProvider;

    @Test
    void testSimple() throws ProviderException
    {
        queueProvider.publish("msg1", false);
        queueProvider.publish("msg2", false);
        assertEquals(Collections.singletonList("msg1"), queueProvider.consume());
        assertEquals(Collections.singletonList("msg2"), queueProvider.consume());
    }
}
