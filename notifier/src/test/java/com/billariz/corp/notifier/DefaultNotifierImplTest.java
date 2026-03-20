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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;
import com.billariz.corp.database.model.Contact;
import com.billariz.corp.database.model.Individual;
import com.billariz.corp.database.model.User;

@SpringBootTest(classes = { DefaultNotifierImpl.class, TemplateConfig.class })
@ContextConfiguration
public class DefaultNotifierImplTest
{
    @Autowired
    private DefaultNotifierImpl         notifier;

    @MockBean
    private JavaMailSender              mailSender;

    @Captor
    private ArgumentCaptor<MimeMessage> captorMimeMessage;

    @Test
    public void testCreateUser() throws Exception
    {
        var request = new User().setContact((new Contact().setEmail("email@fake.com"))).setIndividual(
                new Individual().setFirstName("firstName").setLastName("lastName"));

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        assertDoesNotThrow(() -> notifier.newUser(request, "password"));
        verify(mailSender, atLeastOnce()).send(captorMimeMessage.capture());
        assertEquals("[com SYSTEM - DEV] Accès à votre interface dédiée - firstName lastName", captorMimeMessage.getValue().getSubject());
    }

    @Test
    public void testResetUserPassword() throws Exception
    {
        var request = new User().setUserName("userName").setContact((new Contact().setEmail("email@fake.com"))).setIndividual(
                new Individual().setFirstName("firstName").setLastName("lastName"));

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        assertDoesNotThrow(() -> notifier.resetUserPassword(request, "password"));
        verify(mailSender, atLeastOnce()).send(captorMimeMessage.capture());
        assertEquals("Votre nouveau mot de passe", captorMimeMessage.getValue().getSubject());
    }
}
