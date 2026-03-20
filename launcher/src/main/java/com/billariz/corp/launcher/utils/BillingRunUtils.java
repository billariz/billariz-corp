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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.billariz.corp.database.model.BillingRun;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.Perimeter;
import com.billariz.corp.database.model.enumeration.BillType;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.repository.ContractPerimeterRepository;
import com.billariz.corp.database.repository.ContractRepository;
import com.billariz.corp.database.repository.PerimeterRepository;
import com.billariz.corp.database.repository.BillingRunRepository;
import com.billariz.corp.launcher.queue.LauncherQueue;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingRunUtils
{

    private final PerimeterRepository         perimeterRepository;

    private final BillingRunRepository        billingRunRepository;

    private final LauncherQueue               launcherQueue;

    private static final String BILLINGRUN_CONTRACT="BILLINGRUN_CONTRACT";

    private static final String BILLINGRUN_FINAL_CONTRACT = "BILLINGRUN_FINAL_CONTRACT";
    
    private static final String BILLINGRUN_PERIMETER="BILLINGRUN_PERIMETER";

    private static final String ACTIVITY_BILLING_RUN="ACTIVITY_BILLING_RUN";

    private final ContractPerimeterRepository contractPerimeterRepository;

    private final ContractRepository          contractRepository;
    

    public void createBillingRunLink(BillingRun billingRun, List<String> contractStatusBillableCyclic, List<String> contractStatusBillableFinal, List<String> finalBillOffset)
    {
        List<Contract> contracts = new ArrayList<>();
        if(billingRun.getBillType().equals(BillType.CYCLICAL)) { //récupérer les contracts non en cours de résiliation
            contracts.addAll(contractRepository.findWithStatusInAndBillingCycle(contractStatusBillableCyclic, 
                billingRun.getBillingCycle().getId(), BILLINGRUN_CONTRACT, billingRun.getId()));
        }
        else{ //process les contracts en cours de résiliation
            contracts.addAll(contractRepository.findWithStatusInAndEndDate(contractStatusBillableFinal, 
                billingRun.getStartDate().plusDays(Long.parseLong(finalBillOffset.get(0))), 
                    BILLINGRUN_FINAL_CONTRACT, billingRun.getId()));
        }
        
        if (contracts.isEmpty())
            log.debug("🔍 No contract without relation available for billing run: {}", billingRun);
        else
            contracts.forEach(c -> processContract(c, billingRun));

        //Process perimeter uniquement si billinRun est de type cyclique
        if(billingRun.getBillType().equals(BillType.CYCLICAL)) {
            var perimeters = perimeterRepository.findWithStatusInAndBillingCycleAndBillingRun(billingRun.getBillingCycle().getId(),
                    BILLINGRUN_PERIMETER, billingRun.getId(), billingRun.getStartDate());
            if (perimeters.isEmpty())
                log.debug("No perimeter available for billing run: {}", billingRun);
            else
                perimeters.forEach(p -> processPerimeter(p, billingRun, contractStatusBillableCyclic));
        }
    }

    public void processContract(Contract contract, BillingRun billingRun)
    {
        log.debug("create billing activity for contract {} and run :{}", contract.getId(), billingRun.getId());
        var perimeter = perimeterRepository.findById(contract.getContractPerimeter().getPerimeterId());
        log.debug("perimeter type:{}", perimeter.get().getType().isBillable());

        //Cyclique
        if(!contract.getStatus().equals("TERMINATION_IN_PROGRESS") && billingRun.getBillType().equals(BillType.CYCLICAL)){
            launcherQueue.createActivityEvent(perimeter.get().getType().isBillable() ? "CYCLICAL_BILLING_MULTISITE" : "CYCLICAL_BILLING", "systemAgent",
                                        contract.getId(), ObjectType.CONTRACT, 
                                        billingRun.getId(), ObjectType.BILLING_RUN);
            launcherQueue.createRelation(BILLINGRUN_CONTRACT, billingRun.getId(), contract.getId(), ObjectType.CONTRACT);
        }
        else{//Final
            if(contract.getStatus().equals("TERMINATION_IN_PROGRESS") && billingRun.getBillType().equals(BillType.FINAL)){
                launcherQueue.createActivityEvent(perimeter.get().getType().isBillable() ? "FINAL_BILLING_MULTISITE" : "FINAL_BILLING", "systemAgent",
                                        contract.getId(),ObjectType.CONTRACT, 
                                        billingRun.getId(), ObjectType.BILLING_RUN);
                launcherQueue.createRelation(BILLINGRUN_FINAL_CONTRACT, billingRun.getId(), contract.getId(), ObjectType.CONTRACT);
            }
        }
    }

    public void processPerimeter(Perimeter perimeter, BillingRun billingRun, List<String> contractStatusBillable)
    {
        if (checkPerimeterEligibility(perimeter, billingRun, contractStatusBillable))
        {
            log.debug("create billing activity for perimeter {} and run :", perimeter, billingRun);

            launcherQueue.createActivityEvent("CYCLICAL_BILLING_MULTISITE", "systemAgent",
                                            perimeter.getId(), ObjectType.PERIMETER,
                                            billingRun.getId(),ObjectType.BILLING_RUN);
            launcherQueue.createRelation(BILLINGRUN_PERIMETER, billingRun.getId(), perimeter.getId(), ObjectType.PERIMETER);
        }
        else
            log.debug("Perimeter {} not eligible for billingRun {}:", perimeter.getId(), billingRun.getId());
    }

    private Boolean checkPerimeterEligibility(Perimeter perimeter, BillingRun billingRun, List<String> contractStatusBillable)
    {
        var ctrPrm = contractPerimeterRepository.findWithContractStatusInAndPerimeterId(contractStatusBillable, perimeter.getId());
        if (ctrPrm.isEmpty())
            return false;
        return true;
    }

    public BillingRun createBillingRun(BillingRun original) {
        BillingRun brnew = new BillingRun();
        brnew.setCascaded(true);
        brnew.setStatus("OPEN");
        brnew.setBillingCycle(original.getBillingCycle());
        brnew.setBillingWindow(original.getBillingWindow());
        brnew.setRunDate(LocalDate.now());
        brnew.setStartDate(original.getStartDate());
        brnew.setEndDate(original.getEndDate());
        return brnew;
    }

    public Long getBillingRunId(LocalDate date, BillType type){
        var br = billingRunRepository.findFirstByStatusAndBillTypeAndStartDate("OPEN", type, date);
        return br.getId();
    }

}
