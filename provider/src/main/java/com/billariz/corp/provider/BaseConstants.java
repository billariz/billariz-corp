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

package com.billariz.corp.provider;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BaseConstants
{
    public static final String CONFIG_BASE                 = "provider";

    public static final String BEAN_QUEUE_BILLING          = "queueBilling";

    public static final String BEAN_QUEUE_EVENT_MANAGER    = "queueEventManager";

    public static final String BEAN_QUEUE_INVOICE_REQUEST  = "invoiceRequest";

    public static final String BEAN_QUEUE_INVOICE_RESPONSE = "invoiceResponse";

    public static final String BEAN_STORAGE_DOCUMENT       = "storageDocument";

    public static final String BEAN_STORAGE_INVOICE        = "storageInvoice";

}
