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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import com.billariz.corp.database.model.Contact;
import com.billariz.corp.database.model.User;
import com.billariz.corp.provider.exception.ProviderException;

@ActiveProfiles(Constants.PROVIDER_NAME)
@ContextConfiguration(classes = NoAuthentificationProvider.class)
@SpringBootTest
public class NoAuthentificationProviderTest
{
    @Autowired
    private NoAuthentificationProvider authentificationProvider;

    @Test
    void testSimple() throws ProviderException
    {
        User user = new User();
        Contact contact = new Contact();
        contact.setEmail("userEmail");
        user.setContact(contact);

        authentificationProvider.createUser(user, "***");
        // authentificationProvider.changePassword("accessToken", "***", "***");
        // authentificationProvider.deleteUser("userName");
        // assertNotNull(authentificationProvider.loginUser("userName", "***"));
        // authentificationProvider.resetPassword("userName", "***");
    }
}
