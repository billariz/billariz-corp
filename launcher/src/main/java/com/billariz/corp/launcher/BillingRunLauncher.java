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
import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.billariz.corp.database.model.BillingRun;
import com.billariz.corp.database.model.BillingWindow;
import com.billariz.corp.database.model.Parameter;
import com.billariz.corp.database.model.enumeration.BillType;
import com.billariz.corp.database.model.enumeration.BillingFrequencyEnum;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.repository.BillingRunRepository;
import com.billariz.corp.database.repository.BillingWindowRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.database.validator.BaseValidator;
import com.billariz.corp.launcher.utils.BillingRunUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingRunLauncher
{

    private final BillingWindowRepository     billingWindowRepository;

    private final BillingRunRepository        billingRunRepository;

    private final ParameterRepository         parameterRepository;

    private List<Parameter>                   windowOffset;

    private List<String>                 finalBillOffset;

    private final EventRepository             eventRepository;

    private final BillingRunUtils               billingRunUtils;


    @Scheduled(cron = "0 30 7 * * ?")
    @Scheduled(initialDelay = 10_000, fixedRate = Long.MAX_VALUE)
    void process()
    {
        log.info("[BILLING RUN LAUNCHER 🚀] ** START 🚀**");
        var bws = billingWindowRepository.findAll();
        if (bws != null)
        {
            windowOffset = parameterRepository.findAllByTypeAndNameAndStartDateBefore("BILLING_CYCLE", "windowOffset", LocalDate.now());
            finalBillOffset = parameterRepository.findAllByTypeAndNameAndStartDateBefore("BILLING_CYCLE", "finalBillOffset", LocalDate.now())
                                            .stream().map(Parameter::getValue).toList();
            var ebws = composeBws(bws);
            if (ebws.isEmpty())
                log.debug("Nothing to do, no eligible billig windows were found");
            else
                log.debug("Eligible billig windows processed: {}", ebws);
        }
        else
            log.warn("No billing window found, check configuration");

        var billingRuns = billingRunRepository.findAllByStatusAndEndDateIsBefore("OPEN", LocalDate.now());
        billingRuns.forEach(br -> checkClosingCondition(br));
        log.info("[BILLING RUN LAUNCHER ✅] ** END ✅**");
    }

    private void checkClosingCondition(BillingRun billingRun)
    {
        if (billingRun.getEndDate().isBefore(LocalDate.now()))
        {
            LaunchBillingSituationCheck(billingRun);
            billingRun.setCascaded(true);
            billingRun.setStatus("CLOSED");
            billingRunRepository.save(billingRun);
        }
    }

    private void LaunchBillingSituationCheck(BillingRun billingRun)
    {
        eventRepository.updateAllByBillingRunAndStatusAndSubCategory(billingRun.getId(), "BAD_CHECK", EventExecutionMode.EVENT_MANAGER, LocalDate.now());
    }

    private List<BillingWindow> composeBws(List<BillingWindow> bws)
    {
        var contractStatusBillableCyclic = parameterRepository.findAllByTypeAndNameAndStartDateBefore("BILLABLE_STATUS", BaseValidator.CONTRACT_STATUS,
                LocalDate.now()).stream().map(Parameter::getValue).toList();
        var contractStatusBillableFinal = parameterRepository.findAllByTypeAndNameAndStartDateBefore("BILLABLE_STATUS", BaseValidator.CONTRACT_STATUS_TERMINATION,
                LocalDate.now()).stream().map(Parameter::getValue).toList();
        List<BillingWindow> eligibleBillingWindows = new ArrayList<>();

        bws.parallelStream()
            .filter(bw ->   {
                                var sd = billWindowStartDate(bw);
                                return isBwEligible(bw, sd, windowOffset.get(0).getValue()) && !isRunnedBillingWindow(bw);
                            })
            .forEach(bw -> {
                                eligibleBillingWindows.add(bw);
                                createBillingRun(bw, billWindowStartDate(bw), contractStatusBillableCyclic, contractStatusBillableFinal);
                            });
        
        return eligibleBillingWindows;
    }

    private void createBillingRun(BillingWindow bw, LocalDate sd, List<String> contractStatusBillableCyclic, List<String> contractStatusBillableFinal)
    {
        List<BillingRun> brs = new ArrayList<>();
        BillingRun br = new BillingRun();
        br.setBillType(BillType.CYCLICAL);
        br.setStatus("OPEN");
        br.setBillingCycle(bw.getBillingCycle());
        br.setBillingWindow(bw);
        br.setRunDate(LocalDate.now());
        br.setStartDate(sd);
        br.setEndDate(sd.plusDays(Long.parseLong(windowOffset.get(0).getValue())));
        br.setCascaded(true);
        brs.add(br);

        if(bw.getBillingFrequency().equals(BillingFrequencyEnum.MONTHLY.getValue())){
            //Add a billingRun for a final bills
            var brFinal =  billingRunUtils.createBillingRun(br);
            brFinal.setBillType(BillType.FINAL);
            brs.add(brFinal);

            //Add a billingRun for a Event bills
            var brEvent = billingRunUtils.createBillingRun(br);
            brEvent.setBillType(BillType.EVENT);
            brs.add(brEvent);
        }
        billingRunRepository.saveAll(brs);
        brs.stream().forEach(e -> billingRunUtils.createBillingRunLink(e, contractStatusBillableCyclic, contractStatusBillableFinal,finalBillOffset));
    }

    private Boolean isRunnedBillingWindow(BillingWindow eligibleBillingWindow)
    {
        var runnedBw = billingRunRepository.findAllByBillingWindowAndStartDate(eligibleBillingWindow, billWindowStartDate(eligibleBillingWindow));
        log.debug("Runned billng window: {}", runnedBw);
        if (runnedBw.isEmpty())
            return false;
        else
            return true;
    }

    private Boolean isBwEligible(BillingWindow billWindow, LocalDate startDate, String bwOffet)
    {
        return (LocalDate.now().isBefore(startDate.plusDays(Long.parseLong(bwOffet)))) && (LocalDate.now().isAfter(startDate));

    }

    private LocalDate billWindowStartDate(BillingWindow billWindow)
    {
        log.debug("billWindowStartDate for billing window: {}", billWindow);
        var startDate = LocalDate.now();
        switch (billWindow.getBillingFrequency())
        {
        case "MONTHLY":
            var day = billWindow.getStartDate();
            startDate = startDate.withDayOfMonth(Integer.parseInt(day));
            break;
        default:
            var dayb = billWindow.getStartDate().substring(0, 2);
            var monthb = billWindow.getStartDate().substring(3, 5);
            startDate = LocalDate.of(startDate.getYear(), Integer.parseInt(monthb), Integer.parseInt(dayb));
            break;
        }
        return startDate;
    }

}
