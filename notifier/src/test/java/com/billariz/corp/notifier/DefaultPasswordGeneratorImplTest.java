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

package com.billariz.corp.notifier;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import com.billariz.corp.notifier.utils.DefaultPasswordGeneratorImpl;

@SpringBootTest(classes = { DefaultPasswordGeneratorImpl.class })
@ContextConfiguration
public class DefaultPasswordGeneratorImplTest
{
    @Autowired
    private DefaultPasswordGeneratorImpl passwordGenerator;

    @Test
    public void testPasswords()
    {
        var uniqPassword = new HashSet<>();

        for (int i = 0; i < 3; ++i)
        {
            String password = passwordGenerator.generate();

            assertTrue(uniqPassword.add(password), "Existing password: " + password);
        }
    }
}
