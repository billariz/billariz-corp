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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.billariz.corp.launcher.tags.BillingBadCheck;
import com.billariz.corp.launcher.tags.BillingCompute;
import com.billariz.corp.launcher.tags.BillingPrintShop;
import com.billariz.corp.launcher.tags.BillingValidation;
import com.billariz.corp.launcher.tags.BillingValuation;
import com.billariz.corp.launcher.tags.CatchMarketMasterData;
import com.billariz.corp.launcher.tags.ComputeBillingCycle;
import com.billariz.corp.launcher.tags.TermOfServiceInstallation;
import com.billariz.corp.launcher.tags.TermOfServiceTermination;
import com.billariz.corp.launcher.tags.UpdateBillableChargeStatus;
import com.billariz.corp.launcher.tags.UpdateContractStatus;
import com.billariz.corp.launcher.tags.UpdateMeterReadStatus;
import com.billariz.corp.launcher.tags.UpdatePosMasterData;
import com.billariz.corp.launcher.tags.ValidateBillableCharge;
import com.billariz.corp.launcher.tags.ValidateMeterRead;
import com.billariz.corp.provider.BaseConstants;
import com.billariz.corp.provider.QueueProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueConsumer
{
    private final BillingCompute            billingCompute;

    private final BillingValuation          billingValuation;

    private final ObjectMapper              objectMapper;

    private final TermOfServiceInstallation termOfServiceInstallation;

    private final UpdateContractStatus      updateContractStatus;

    private final ComputeBillingCycle       computeBillingCycle;

    private final BillingValidation         billingValidation;

    private final ValidateMeterRead         validateMeterRead;

    private final ValidateBillableCharge    validateBillableCharge;

    private final UpdateBillableChargeStatus updateBillableChargeStatus;

    private final UpdateMeterReadStatus     updateMeterReadStatus;

    private final BillingPrintShop          billingPrintShop;

    private final BillingBadCheck           billingBadCheck;

    private final UpdatePosMasterData   updatePosMasterData;

    private final TermOfServiceTermination   termOfServiceTermination;

    private final CatchMarketMasterData         catchMarketMasterData;

    @Autowired
    @Qualifier(BaseConstants.BEAN_QUEUE_EVENT_MANAGER)
    private QueueProvider                   provider;

    private ExecutorService                 executorService;

    @PostConstruct
    void postConstruct()
    {
        executorService = Executors.newSingleThreadExecutor();
    }

    @Scheduled(initialDelay = 30_000, fixedDelay = 1_000)
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
            log.debug("Message:", message);
            if (message != null)
                consumeMessage(objectMapper.readValue(message, EventManagerData.class));
        }
        catch (JsonProcessingException e)
        {
            log.error("ERROR consumeMessage", e);
        }
    }

    private void consumeMessage(EventManagerData event)
    {
        if (event.isSynchronous())
            processEvent(event);
        else
            executorService.execute(() -> processEvent(event));
    }

    private void processEvent(EventManagerData event)
    {
        log.debug("events Ids:{}", event.ids().toString());
        switch (event.launcherTag())
        {
        //BILLING
        case "BILLING_PRINTSHOP":
            billingPrintShop.process(event.ids(), event.executionMode());
            break;
        case "BILLING_BAD_CHECK":
            billingBadCheck.process(event.ids(), event.executionMode());
            break;
        case "BILLING_COMPUTE":
            billingCompute.process(event.ids(), event.executionMode());
            break;
        case "BILLING_VALO":
            billingValuation.process(event.ids(), event.executionMode());
            break;
        case "BILLING_VALIDATION":
            billingValidation.process(event.ids(), event.executionMode());
            break;
        case "COMPUTE_BILLING_CYCLE":
            computeBillingCycle.process(event.ids(), event.executionMode());
            break;
        // CONTRACT
        case "TOS_INSTALLATION":
            termOfServiceInstallation.process(event.ids(), event.executionMode());
            break;
        case "UPDATE_CONTRACT_STATUS":
            updateContractStatus.process(event.ids(), event.executionMode());
            break;
        case "METER_READ_VALIDATION":
            validateMeterRead.process(event.ids(), event.executionMode());
            break;
        case "METER_READ_UPDATE_STATUS":
            updateMeterReadStatus.process(event.ids(), event.executionMode());
            break;
        case "BILLABLE_CHARGE_VALIDATION":
            validateBillableCharge.process(event.ids(), event.executionMode());
            break;
        case "BILLABLE_CHARGE_UPDATE_STATUS":
            updateBillableChargeStatus.process(event.ids(), event.executionMode());
            break;
        case "UPDATE_POS_MASTERDATA":
            updatePosMasterData.process(event.ids(), event.executionMode());
            break;
        case "MARKET_MASTER_DATA_CATCHING":
            catchMarketMasterData.process(event.ids(), event.executionMode());
            break;
        case "TOS_TERMINATION":
            termOfServiceTermination.process(event.ids(), event.executionMode());
            break;
        default:
            log.warn("Unsupported event: event={}", event);
        }
    }
}
