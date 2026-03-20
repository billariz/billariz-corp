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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.billariz.corp.launcher.tags.BillingPrintShop;
import com.billariz.corp.provider.BaseConstants;
import com.billariz.corp.provider.QueueProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrintShopQueueConsumer
{

    private final ObjectMapper       objectMapper;

    private final BillingPrintShop          billingPrintShop;

    private boolean                  isSynchronousMode = true;

    @Autowired
    @Qualifier(BaseConstants.BEAN_QUEUE_INVOICE_RESPONSE)
    private QueueProvider            provider;

    private ExecutorService          executorService;

    @PostConstruct
    void postConstruct()
    {
        executorService = Executors.newSingleThreadExecutor();
    }

    @Scheduled(initialDelay = 30_000, fixedDelay = 50_000)
    void processQueue()
    {
        try
        {
            var messages = provider.consume();

            messages.stream().forEach(this::consumeMessage);
        }
        catch (Exception e)
        {
            log.error("ERROR consume message from queue", e);
        }
    }

    private void consumeMessage(String message)
    {
        try
        {
            log.info("** Message from PrintShop:{}", message);
            if (message != null)
                consumeMessage(objectMapper.readValue(message, PrintShopDataResponse.class));
        }
        catch (JsonProcessingException e)
        {
            log.error("ERROR consumeMessage from PrintShop: {}", e);
        }
    }

    private void consumeMessage(PrintShopDataResponse message)
    {
        if (isSynchronousMode)
            processMessage(message);
        else
            executorService.execute(() -> processMessage(message));
    }

    private void processMessage(PrintShopDataResponse message)
    {
        log.debug("message objectType:{}", message.objectType().toString());
        switch (message.objectType())
        {
        //BILLING
        case BILL:
            billingPrintShop.processResponse(message.data());
            break;
        default:
            log.warn("Unsupported message with objectType {}", message.objectType());
        }
    }

}
