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

package com.billariz.corp.launcher;

import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import com.billariz.corp.database.model.Parameter;
import com.billariz.corp.database.model.enumeration.BillType;
import com.billariz.corp.database.repository.BillingRunRepository;
import com.billariz.corp.database.repository.ContractRepository;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.database.validator.BaseValidator;
import com.billariz.corp.launcher.utils.BillingRunUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingRunMonitor
{
    private static final String BILLINGRUN_FINAL_CONTRACT = "BILLINGRUN_FINAL_CONTRACT";

    private final BillingRunRepository        billingRunRepository;

    private final ParameterRepository         parameterRepository;

    private final ContractRepository        contractRepository;

    private final BillingRunUtils               billingRunUtils;

    // Process all open billingRun to create billing activities and links to contrats and perimeters
    // to schedule once a day early morning
    @Scheduled(cron = "0 30 7 * * ?")
    @Scheduled(initialDelay = 120_000, fixedDelay = 300_000)
    void process()
    {
        log.info("[BILLING RUN MONITOR 🚀] ** START 🚀**");
        var billingRuns = billingRunRepository.findAllByStatusAndEndDateIsBefore("OPEN", LocalDate.now().plusDays(100));
        var contractStatusBillableCyclic = parameterRepository.findAllByTypeAndNameAndStartDateBefore("BILLABLE_STATUS", BaseValidator.CONTRACT_STATUS,
                LocalDate.now()).stream().map(Parameter::getValue).toList();
        var contractStatusBillableFinal = parameterRepository.findAllByTypeAndNameAndStartDateBefore("BILLABLE_STATUS", BaseValidator.CONTRACT_STATUS_TERMINATION,
                LocalDate.now()).stream().map(Parameter::getValue).toList();

        if (contractStatusBillableCyclic.isEmpty() || contractStatusBillableFinal.isEmpty()) {
            log.warn("🔍 Invalid configuration: contractStatusBillableCyclic and or contractStatusBillableFinal");
            return;
        }
        var finalBillOffset = parameterRepository.findAllByTypeAndNameAndStartDateBefore("BILLING_CYCLE", "finalBillOffset", LocalDate.now())
                .stream().map(Parameter::getValue).toList();
        var windowOffset = parameterRepository.findAllByTypeAndNameAndStartDateBefore("BILLING_CYCLE", "windowOffset", LocalDate.now())
                .stream().map(Parameter::getValue).toList();

        //Traitement cas standard
        billingRuns.stream().forEach(billingRun -> { billingRunUtils.createBillingRunLink(billingRun, contractStatusBillableCyclic,
                                                                    contractStatusBillableFinal, 
                                                                    finalBillOffset);});
        
        //recyclage des contrats résilié dans le passé et qui sont hors scope du traitement standard (hos windowOffset)
        var contracts = contractRepository.findAllByStatusInAndEndDateAndBillinRunType(
                                                    contractStatusBillableFinal,
                                                    LocalDate.now().minusDays(Long.parseLong(windowOffset.get(0))),
                                                    BILLINGRUN_FINAL_CONTRACT
                                                    );
        contracts.stream().forEach(ctr -> {
                                    //selectionner le billingRun
                                    var br = billingRunRepository.findFirstByStatusAndBillTypeAndStartDate("OPEN", 
                                                                BillType.FINAL,
                                                                LocalDate.now().minusDays(Long.parseLong(finalBillOffset.get(0)))
                                                                );
                                    //affecter le billingRun
                                    if(br !=null)
                                        billingRunUtils.processContract(ctr, br);
                                    }
                                    );

        log.info("[BILLING RUN MONITOR ✅] ** END ✅**");
    }
}
