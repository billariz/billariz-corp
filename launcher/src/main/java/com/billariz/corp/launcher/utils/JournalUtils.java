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

package com.billariz.corp.launcher.utils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.model.light.messageCode;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.validator.BaseValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JournalUtils
{
    @Autowired
    private MessageSource messageSource;

    private final JournalRepository journalRepository;

    public String getMessage(String errorCode, Object[] args, Locale locale) {
        return messageSource.getMessage(errorCode, args, locale);
    }

    public static Journal build(Long objectId, String comment, ObjectType objectEnum)
    {
        var journal = new Journal();

        journal.setMethod(JournalAction.LOG.getValue());
        journal.setCreationDate(OffsetDateTime.now());
        journal.setComment(comment);
        journal.setObjectType(objectEnum);
        journal.setObjectId(objectId);
        return journal;
    }

    public Journal addJournal(ObjectType objectType, Long objectId, String coment, Object[] args, Event event, JournalAction action, String userName)
    {
        var journal = new Journal();
        List<messageCode> messages = new ArrayList<>();
        messages.add(new messageCode(coment, args));
        journal.setCreationDate(OffsetDateTime.now());
        journal.setMethod(action.getValue());
        journal.setObjectType(objectType);
        journal.setObjectId(objectId);
        journal.setUserName(userName!=null ? userName : getUserName(event !=null ? event.getExecutionMode() : EventExecutionMode.AUTO));
        journal.setComment(coment);
        journal.setMessageCodes(messages);
        journalRepository.save(journal);
        return journal;
    }

    public Journal addJournal(ObjectType objectType, Long objectId, String coment, Object[] args, Event event, JournalAction action)
    {
        var journal = new Journal();
        List<messageCode> messages = new ArrayList<>();
        messages.add(new messageCode(coment, args));
        journal.setCreationDate(OffsetDateTime.now());
        journal.setMethod(action.getValue());
        journal.setObjectType(objectType);
        journal.setObjectId(objectId);
        journal.setUserName(getUserName(event !=null ? event.getExecutionMode() : EventExecutionMode.AUTO));
        journal.setComment(coment);
        journal.setMessageCodes(messages);
        journalRepository.save(journal);
        return journal;
    }

    public String getUserName(EventExecutionMode exeMod){
        
        switch (exeMod) {
            case EVENT_MANAGER:
                return BaseValidator.EVENT_MANAGER;
            case AUTO:
                return BaseValidator.SYSTEM_AGENT;
            case MANUAL:
                return "REFERER";
            default:
                return "REFERER";
        }
    }
}
