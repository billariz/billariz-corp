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

package com.billariz.corp.provider.local.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.billariz.corp.provider.BaseConstants;
import com.billariz.corp.provider.local.Constants;
import com.billariz.corp.provider.local.JavaQueueProvider;
import com.billariz.corp.provider.local.LocalStorageProvider;

@Profile(Constants.PROVIDER_NAME)
@Configuration
public class LocalConfig
{
    @Bean(BaseConstants.BEAN_QUEUE_BILLING)
    public JavaQueueProvider queueBilling()
    {
        return new JavaQueueProvider(BaseConstants.BEAN_QUEUE_BILLING);
    }

    @Bean(BaseConstants.BEAN_QUEUE_EVENT_MANAGER)
    public JavaQueueProvider queueEventManger()
    {
        return new JavaQueueProvider(BaseConstants.BEAN_QUEUE_EVENT_MANAGER);
    }

    @Bean(BaseConstants.BEAN_QUEUE_INVOICE_REQUEST)
    public JavaQueueProvider queueInvoiceRequest()
    {
        return new JavaQueueProvider(BaseConstants.BEAN_QUEUE_INVOICE_REQUEST);
    }

    @Bean(BaseConstants.BEAN_QUEUE_INVOICE_RESPONSE)
    public JavaQueueProvider queueInvoiceResponse()
    {
        return new JavaQueueProvider(BaseConstants.BEAN_QUEUE_INVOICE_RESPONSE);
    }

    @Bean(BaseConstants.BEAN_STORAGE_DOCUMENT)
    public LocalStorageProvider storageDocument()
    {
        return new LocalStorageProvider(BaseConstants.BEAN_STORAGE_DOCUMENT);
    }

    @Bean(BaseConstants.BEAN_STORAGE_INVOICE)
    public LocalStorageProvider storageInvoice()
    {
        return new LocalStorageProvider(BaseConstants.BEAN_STORAGE_INVOICE);
    }
}
