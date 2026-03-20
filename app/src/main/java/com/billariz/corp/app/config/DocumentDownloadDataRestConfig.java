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

package com.billariz.corp.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import com.billariz.corp.app.controller.DocumentController;
import com.billariz.corp.database.model.Bill;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class DocumentDownloadDataRestConfig
{
    @Bean
    public RepresentationModelProcessor<EntityModel<Bill>> billProcessor()
    {
        return new RepresentationModelProcessor<EntityModel<Bill>>()
        {
            @Override
            public EntityModel<Bill> process(EntityModel<Bill> model)
            {
                var bill = model.getContent();

                if (bill != null)
                {
                    try
                    {
                        var method = DocumentController.class.getMethod("downloadBill", long.class);
                        var link = WebMvcLinkBuilder.linkTo(method, bill.getId()).withRel("download");

                        model.add(link);
                    }
                    catch (Exception e)
                    {
                        log.error("ERROR", e);
                    }
                }
                return model;
            }
        };
    }
}
