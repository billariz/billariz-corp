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

package com.billariz.corp.launcher.queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.billariz.corp.provider.BaseConstants;
import com.billariz.corp.provider.QueueProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueProducer
{
    @Autowired
    @Qualifier(BaseConstants.BEAN_QUEUE_EVENT_MANAGER)
    private QueueProvider      provider;

    private final ObjectMapper objectMapper;

    public void publish(EventManagerData eventManagerData)
    {
        try
        {
            var message = objectMapper.writeValueAsString(eventManagerData);

            provider.publish(message, eventManagerData.isSynchronous());
        }
        catch (Exception e)
        {
            log.error("ERROR publish", e);
        }
    }
}
