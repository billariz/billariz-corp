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

package com.billariz.corp.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import com.billariz.corp.database.repository.BillRepository;
import com.billariz.corp.provider.BaseConstants;
import com.billariz.corp.provider.StorageProvider;
import com.billariz.corp.provider.exception.ProviderException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class DocumentController
{
    @Autowired
    private BillRepository billRepository;

    @Autowired
    @Qualifier(BaseConstants.BEAN_STORAGE_INVOICE)
    public StorageProvider storageDocument;

    @GetMapping("/download/bill/{billId}")
    public RedirectView downloadBill(@PathVariable long billId) throws ProviderException
    {
        log.debug("Want to download bill: {}", billId);

        var bill = billRepository.findById(billId).orElseThrow();
        var link = storageDocument.generateDirectDownloadLink(bill.getPath());

        return new RedirectView(link);
    }
}
