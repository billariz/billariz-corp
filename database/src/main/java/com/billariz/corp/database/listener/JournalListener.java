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

package com.billariz.corp.database.listener;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import com.billariz.corp.database.model.Journal;
import java.util.List;
import java.util.Locale;
import javax.persistence.PostLoad;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class JournalListener {

    private static MessageSource messageSource;

    @Autowired
    public void setMessageSource(MessageSource messageSource) {
        JournalListener.messageSource = messageSource;
    }

    @PostLoad
    public void translateErrorMessages(Journal journal) {
        if (journal.getMessageCodes() == null)
            return; // Pas de messages à traduire
            
        Locale locale = LocaleContextHolder.getLocale();
        try {
            List<String> messages = journal.getMessageCodes().stream()
                    .map(entry -> getMessage(entry.getName(), entry.getArgs(), locale))
                    .toList();
            journal.setMessages(messages);
        } catch (Exception e) {
            journal.setMessages(List.of("Translation not available for: " + journal.getMessageCodes()));
        }
    }
    
    private String getMessage(String code, Object[] args, Locale locale) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            return "Translation not available for: " + code;
        }
    }
}
