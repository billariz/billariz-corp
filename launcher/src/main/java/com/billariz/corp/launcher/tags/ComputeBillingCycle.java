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

package com.billariz.corp.launcher.tags;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.BillingCycle;
import com.billariz.corp.database.model.BillingWindow;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.Perimeter;
import com.billariz.corp.database.model.ReadingCycle;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.light.messageCode;
import com.billariz.corp.database.repository.BillingWindowRepository;
import com.billariz.corp.database.repository.ContractRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.database.repository.PointOfServiceConfigurationRepository;
import com.billariz.corp.database.repository.ReadingCycleBillingCycleRepository;
import com.billariz.corp.database.repository.ReadingPeriodReadingCycleRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.launcher.Launcher;
import com.billariz.corp.launcher.exception.LauncherException;
import com.billariz.corp.launcher.exception.LauncherFatalException;
import com.billariz.corp.launcher.utils.EventUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ComputeBillingCycle implements Launcher
{
    private static final String ACTIVITY_CONTRACT="ACTIVITY_CONTRACT";

    private final ContractRepository                    contractRepository;

    private final ParameterRepository                   parameterRepository;

    private final EventRepository                       eventRepository;

    private final JournalRepository                     journalRepository;

    private final RelationRepository                    relationRepository;

    private final PointOfServiceConfigurationRepository pointOfServiceConfigurationRepository;

    private final BillingWindowRepository               billingWindowRepository;

    private final ReadingPeriodReadingCycleRepository   readingPeriodReadingCycleRepository;

    private final ReadingCycleBillingCycleRepository    readingCycleBillingCycleRepository;

    private final EventUtils                            eventUtils;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Iterable<Long> eventIds, EventExecutionMode executionMode)
    {
        var events = eventRepository.findAllById(eventIds);

        events.forEach(e -> process(e, executionMode));
    }

    private void process(Event event, EventExecutionMode executionMode)
    {
        // Prepare
        List<messageCode> messages = new ArrayList<>();
        event.setCascaded(true);
        event.setStatus(EventStatus.IN_PROGRESS);
        event.setExecutionDate(LocalDateTime.now());
        event.setExecutionMode(executionMode);
        if (event.getRank() == 1)
                    event.getActivity().setStatus(ActivityStatus.IN_PROGRESS);
        var journal = eventUtils.addJournal(event);

        // Execute
        try
        {
            handle(event, journal, messages);
            event.setStatus(EventStatus.COMPLETED);
        }
        catch (LauncherException e)
        {
            log.error("handle: " + e.getMessage(), e);
            event.setStatus(EventStatus.IN_FAILURE);
            journal.setMethod(JournalAction.ERROR.getValue());
            messages.add(new messageCode(e.getMessage(), e.getArgs()));
            journal.setComment(e.getMessage());
            event.getActivity().setStatus(ActivityStatus.BLOCKED);
        }

        // Finish
        log.info("Event[{}] processed, strating next process", event);
        eventUtils.triggerOnUpdateEventStatus(event);
        journal.setNewStatus(Objects.toString(event.getStatus()));
        journal.setMessageCodes(messages);
        journalRepository.save(journal);
    }

   
    private void handle(Event event, Journal journal, List<messageCode> messages) throws LauncherException
    {
        var relation = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_CONTRACT, event.getActivityId())
                                        .orElseThrow(() -> new LauncherFatalException("MISSING_CONTRACT", new Object[]{event.getId()}));
        log.info("relation[{}] found for Event[{}] ", relation, event);

        var contract = contractRepository.findById(relation.getSecondObjectId())
                        .orElseThrow(() -> new LauncherFatalException("MISSING_CONTRACT_IN_DB",  new Object[]{relation.getSecondObjectId()}));
        var posConfiguration = pointOfServiceConfigurationRepository.findFirstByContractIdAndEndDateIsNullOrderByStartDateDesc(contract.getId())
                        .orElseThrow(() -> new LauncherFatalException("MISSING_POS_CONF", new Object[]{contract.getId()}));
        var pointOfService = posConfiguration.getPointOfService();
        var readingCycle = computeReadingCycle(posConfiguration.getReadingPeriode(), posConfiguration.getReadingFrequency(), pointOfService.getMarket());
        pointOfService.setReadingCycleId(readingCycle.getId());
        var billingCycle = computeContractBillingCycle(readingCycle, contract);
        messages.add(new messageCode("BILLING_CYCLE", new Object[]{billingCycle.getId()}));
        contract.setCascaded(true);
        contract.setBillingCycleId(billingCycle.getId());
        contract.setBillAfterDate(computeBillAfterDate(billingCycle));
        messages.add(new messageCode("BAD", new Object[]{contract.getBillAfterDate().toString()}));

        //Case multisite
        if (contract.getContractPerimeter().getPerimeter().getType().isBillable() 
                    && contract.getContractPerimeter().getPerimeter().getBillingCycle() == null)
        {
            contract.getContractPerimeter().getPerimeter().setBillingCycleId(
                    computePerimeterBillingCycle(contract.getContractPerimeter().getPerimeter()).getId());
            messages.add(new messageCode("BILLING_CYCLE", new Object[]{contract.getContractPerimeter().getPerimeter().getBillingCycle()}));
            contract.getContractPerimeter().getPerimeter().setBillAfterDate(computeBillAfterDate(billingCycle));
            messages.add(new messageCode("BAD", new Object[]{contract.getContractPerimeter().getPerimeter().getBillAfterDate().toString()}));
        }
    }

    private ReadingCycle computeReadingCycle(String readingPeriod, String readingFrequency, String market) throws LauncherException
    {
        log.debug("** computeReadingCycle");
        var readingCycle = readingPeriodReadingCycleRepository.findFirstByMarketAndReadingPeriodAndReadingFrequency(market, readingPeriod,
                readingFrequency).orElseThrow(() -> new LauncherFatalException("MISSING_READING_CYCLE", new Object[]{market, readingFrequency, readingPeriod}));
        return readingCycle.getReadingCycle();
    }

    private BillingCycle computeContractBillingCycle(ReadingCycle readingCycle, Contract contract) throws LauncherException
    {

        log.debug("** computeBillingCycle");
        log.debug("** readingCycle: {}", readingCycle.getId());
        log.debug("** billingFrequency: {}", contract.getBillingFrequency());
        var billingCycle = readingCycleBillingCycleRepository.findFirstByReadingCycleAndBillingFrequency(readingCycle,
                contract.getBillingFrequency()).orElseThrow(
                        () -> new LauncherFatalException("MISSING_BILLING_CYCLE", new Object[]{contract.getBillingFrequency(), readingCycle.getId()}));

        return billingCycle.getBillingCycle();
    }

    private BillingCycle computePerimeterBillingCycle(Perimeter perimeter) throws LauncherException
    {
        log.debug("** compute Billing Cycle for perimeter : {}", perimeter.getId());
        var billingPivotDate = parameterRepository.findAllByTypeAndNameAndStartDateBefore("BILLING_CYCLE", "perimeterBillingDay", LocalDate.now()).get(
                0).getValue();
        var readingCycle = computeReadingCycle(billingPivotDate, perimeter.getBillingFrequency(), perimeter.getMarket());
        var billingCycle = readingCycleBillingCycleRepository.findFirstByReadingCycleAndBillingFrequency(readingCycle,
                perimeter.getBillingFrequency()).orElseThrow(
                        () -> new LauncherFatalException("MISSING_BILLING_CYCLE", new Object[]{perimeter.getBillingFrequency(), readingCycle.getId()}));

        return billingCycle.getBillingCycle();
    }

    private LocalDate computeBillAfterDate(BillingCycle billingCycle) throws LauncherException
    {

        var badOffset = parameterRepository.findAllByTypeAndNameAndStartDateBefore("BILLING_CYCLE", "badOffset", LocalDate.now()).get(0).getValue();
        var windowOffset = parameterRepository.findAllByTypeAndNameAndStartDateBefore("BILLING_CYCLE", "windowOffset", LocalDate.now()).get(0).getValue();
        var minDay = parameterRepository.findAllByTypeAndNameAndStartDateBefore("BILLING_CYCLE", "minDayOpenWindow", LocalDate.now()).get(0).getValue();
        var billWindow = billingWindowRepository.findFirstByBillingCycle(billingCycle);

        var billAfterDate = findNextBillingWindowStartDate(billWindow, minDay).plusDays(Long.parseLong(windowOffset) + Long.parseLong(badOffset));
        return billAfterDate;
    }

    private LocalDate findNextBillingWindowStartDate(BillingWindow billWindow, String minDay) throws LauncherException
    {

        var startDate = LocalDate.now();
        switch (billWindow.getBillingFrequency())
        {
            case "MONTHLY":
                int day = Integer.parseInt(billWindow.getStartDate());
                int currentDay = startDate.getDayOfMonth();
                if (day - currentDay < Integer.parseInt(minDay)) { // Passer au mois suivant si l'écart est inférieur à minDay
                    startDate = startDate.plusMonths(1);
                }
                startDate = startDate.withDayOfMonth(day);  // Ajuster le jour du mois
                break;
            case "BIMONTHLY":
                var dayp = billWindow.getStartDate().substring(0, 2);
                var monthp = billWindow.getStartDate().substring(3, 5);
                startDate = LocalDate.of(startDate.getYear(), Integer.valueOf(monthp), Integer.valueOf(dayp));
                if (LocalDate.now().plusDays(Long.parseLong(minDay)).isAfter(startDate))
                    startDate = startDate.plusMonths(2);
                break;
            case "BIANNUAL":
                var dayb = billWindow.getStartDate().substring(0, 2);
                var monthb = billWindow.getStartDate().substring(3, 5);
                startDate = LocalDate.of(startDate.getYear(), Integer.valueOf(monthb), Integer.valueOf(dayb));
                if (LocalDate.now().plusDays(Long.parseLong(minDay)).isAfter(startDate))
                    startDate = startDate.plusMonths(6);
                break;
            case "ANNUAL":
                var daya = billWindow.getStartDate().substring(0, 2);
                var montha = billWindow.getStartDate().substring(3, 5);
                startDate = LocalDate.of(startDate.getYear(), Integer.valueOf(montha), Integer.valueOf(daya));
                if (LocalDate.now().plusDays(Long.parseLong(minDay)).isAfter(startDate))
                    startDate = startDate.plusMonths(12);
                break;
        }

        return startDate;
    }

}