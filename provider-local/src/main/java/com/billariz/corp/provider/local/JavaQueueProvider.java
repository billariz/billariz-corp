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

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.billariz.corp.provider.QueueProvider;
import com.billariz.corp.provider.exception.ProviderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class JavaQueueProvider implements QueueProvider
{
    private final String        queueName;

    private final Queue<String> queue = new ConcurrentLinkedQueue<>();

    @Override
    public List<String> consume() throws ProviderException
    {
        try
        {
            var message = queue.poll();

            log.debug("receiveMessage {} from {}", message, queueName);
            return Collections.singletonList(message);
        }
        catch (Exception e)
        {
            throw new ProviderException("Unable to consume message", e);
        }
    }

    @Override
    public void publish(String message, boolean isFifo) throws ProviderException
    {
        try
        {
            log.debug("sendMessage {} to {}", message, queueName);
            queue.offer(message);
        }
        catch (Exception e)
        {
            throw new ProviderException("Unable to send message: " + message, e);
        }
    }
}
