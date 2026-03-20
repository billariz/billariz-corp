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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.BillingRun;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.ContractPointOfService;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.Parameter;
import com.billariz.corp.database.model.PointOfService;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.BillSegmentStatus;
import com.billariz.corp.database.model.enumeration.BillableChargeStatus;
import com.billariz.corp.database.model.enumeration.BillingValuationBase;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.MeterReadStatus;
import com.billariz.corp.database.model.enumeration.SeStatus;
import com.billariz.corp.database.model.enumeration.ServiceElementTypeCategory;
import com.billariz.corp.database.model.light.EligibleServiceElement;
import com.billariz.corp.database.model.light.messageCode;
import com.billariz.corp.database.repository.BillSegmentRepository;
import com.billariz.corp.database.repository.BillableChargeRepository;
import com.billariz.corp.database.repository.BillingRunRepository;
import com.billariz.corp.database.repository.ContractPointOfServiceRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.MeterReadRepository;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.database.repository.ServiceElementRepository;
import com.billariz.corp.database.validator.BaseValidator;
import com.billariz.corp.launcher.Launcher;
import com.billariz.corp.launcher.billing.BillingService;
import com.billariz.corp.launcher.exception.LauncherException;
import com.billariz.corp.launcher.exception.LauncherFatalException;
import com.billariz.corp.launcher.utils.BillingUtils;
import com.billariz.corp.launcher.utils.EventUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BillingValuation implements Launcher
{
    private static final String ACTIVITY_CONTRACT="ACTIVITY_CONTRACT";

    private static final String ACTIVITY_BILLING_RUN="ACTIVITY_BILLING_RUN";

    private final BillingService                   billingService;

    private final ParameterRepository              parameterRepository;

    private final ServiceElementRepository         serviceElementRepository;

    private final EventRepository                  eventRepository;

    private final RelationRepository               relationRepository;

    private final JournalRepository                journalRepository;

    private final EventUtils                       eventUtils;

    private final BillSegmentRepository            billSegmentRepository;

    private final BillingRunRepository             billingRunRepository;

    private final MeterReadRepository              meterReadRepository;

    private final BillableChargeRepository         billableChargeRepository;

    private final ContractPointOfServiceRepository contractPointOfServiceRepository;

    private final BillingUtils billingUtils;

    private List<SeStatus> seListStatus;

    private List<String> windowOffset;


    @Override
   @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Iterable<Long> eventIds, EventExecutionMode executionMode)
    {
        var events = eventRepository.findAllById(eventIds);

        LocalDate now = LocalDate.now();
        seListStatus = parameterRepository
                                .findAllByTypeAndNameAndStartDateBefore("BILLABLE_STATUS", 
                                            BaseValidator.SERVICE_ELEMENT_STATUS, now)
                                .stream()
                                .map(Parameter::getValue)
                                .map(SeStatus::valueOf)
                                .toList();

        windowOffset = parameterRepository
                                .findAllByTypeAndNameAndStartDateBefore("BILLING_CYCLE", "windowOffset", now)
                                .stream()
                                .map(Parameter::getValue)
                                .toList();

        events.forEach(e -> process(e, executionMode));
    }

    public void process(Event event, EventExecutionMode executionMode)
    {
        // Prepare
        List<messageCode> messages = new ArrayList<>();
        event.setCascaded(true);
        event.setStatus(EventStatus.IN_PROGRESS);
        if (event.getRank() == 1)
                    event.getActivity().setStatus(ActivityStatus.IN_PROGRESS);
        event.setExecutionDate(LocalDateTime.now());
        event.setExecutionMode(executionMode);
        var journal = eventUtils.addJournal(event);

        // Execute
        try
        {
            var wait = process(event, journal, messages);
            if(wait)
                event.setStatus( EventStatus.COMPLETED);
            else{
                event.setStatus(EventStatus.PENDING);
                event.setExecutionMode(EventExecutionMode.EVENT_MANAGER);
            }
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

    private boolean process(Event event, Journal journal, List<messageCode> messages) throws LauncherException
    {
        var valuationBase = event.getAction()!= null ? BillingValuationBase.valueOf(event.getAction()) : BillingValuationBase.FULL;
        messages.add(new messageCode("EVENT_USE_CASE", new Object[]{event.getAction()}));

        var relationContract = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_CONTRACT,
                    event.getActivityId()).orElseThrow(
                        () -> new LauncherFatalException("MISSING_CONTRACT", new Object[]{event.getId()}));
        log.info("relation[{}] found for Event[{}] ", relationContract, event);
        var ctfAndCuInfo = billingService.fetchContractAndCustomerInfo(relationContract.getSecondObjectId());
        var seList = serviceElementRepository.findWithStatusInAndMasterIsTrueAndTosContractStatusIn(seListStatus,
                    relationContract.getSecondObjectId());
        if (seList.isEmpty())
                throw new LauncherFatalException("MISSING_BILLABLE_SERVICES", new Object[]{relationContract.getSecondObjectId(),event.getId()});

        var relationBillRun = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_BILLING_RUN,
                    event.getActivityId());
        
        LocalDate limitDate = billingUtils.isCutOffDateBased(valuationBase) ? event.getTriggerDate() : LocalDate.now().plusYears(10);

        // checker l'existance d'un SE master compatible avec le valuationBase
        if(!valuationBase.equals(BillingValuationBase.FULL) && !valuationBase.equals(BillingValuationBase.FINAL_FULL)
                        && !valuationBase.equals(BillingValuationBase.CUTOFF_DATE_FULL)){
            if(!checkValuationBaseCompatibility(valuationBase, seList)) {
                if(billingUtils.isCutOffDateBased(valuationBase))
                    throw new LauncherFatalException("MISSING_ELIGIBLE_SE_MASTERS", new Object[]{valuationBase});
                else {
                    messages.add(new messageCode("MISSING_ELIGIBLE_SE_MASTERS", new Object[]{valuationBase}));
                    journal.setMessageCodes(messages);
                    return true;
                }
            }

        }

        if (!relationBillRun.isEmpty())
        {
            var billingRun = billingRunRepository.findById(relationBillRun.get().getSecondObjectId());
            if (billingUtils.checkIfBillSegmentExist(seList, billingRun.get(), valuationBase))
            {
                messages.add(new messageCode("ALL_SE_BILLED", null));
                journal.setMessageCodes(messages);
                return true;
            }
            if (!checkEligibility(ctfAndCuInfo.get().getContract(), billingRun.get(), seList, valuationBase))
            {
                messages.add(new messageCode("NO_ELEMENTS_TO_BILL", null));
                journal.setMessageCodes(messages);
                return false;
            }
        }
        else  {

            if (billingUtils.checkIfBillSegmentExist(seList, limitDate, valuationBase))
            {
                messages.add(new messageCode("ALL_SE_BILLED", null));
                journal.setMessageCodes(messages);
                return true;
            }
        }

        billingService.handleValuation(ctfAndCuInfo.get(), seList, valuationBase, limitDate);
        log.debug("** END Valuation for contract", ctfAndCuInfo.get().getContract().getId());

        journal.setMessageCodes(messages);
        return true;
    }

    private Boolean checkValuationBaseCompatibility(BillingValuationBase base, List<EligibleServiceElement> seList) throws LauncherException
    {
        switch (base) {
            case BILLABLE_CHARGE, CUTOFF_DATE_BC:
                return seList.stream().anyMatch(e -> e.getCategory().equals(ServiceElementTypeCategory.BILLABLE_CHARGE));
            case CONSUMPTION, CUTOFF_DATE_CONSUMPTION:
                return seList.stream().anyMatch(e -> e.getCategory().equals(ServiceElementTypeCategory.CONSUMPTION));
            case SUBSCRIPTION, CUTOFF_DATE_SUBSCRIPTION:
                return seList.stream().anyMatch(e -> e.getCategory().equals(ServiceElementTypeCategory.SUBSCRIPTION));
            default:
                return false;
        }
    }
    
    private Boolean checkEligibility(Contract ctr, BillingRun billingRun, List<EligibleServiceElement> serviceElementList, BillingValuationBase valuationBase) throws LauncherException
    {
        deleteIneligibleSe(serviceElementList, ctr, billingRun, valuationBase);
        if (!serviceElementList.isEmpty())
                return true;
        else
                log.warn("No Eligible Service Element found for the contract: {} ", ctr.getId());

        return false;
    }

    private void deleteIneligibleSe(List<EligibleServiceElement> seInfoList, Contract contract, BillingRun billingRun, BillingValuationBase valuationBase) throws LauncherException
    {
        for (EligibleServiceElement seInfo : new ArrayList<>(seInfoList))
            if (!checkConsumptionConditions(seInfo, contract, billingRun, valuationBase))
                seInfoList.remove(seInfo);
    }

    private boolean checkConsumptionConditions(EligibleServiceElement seInfo, Contract contract, 
                                    BillingRun billingRun, BillingValuationBase valuationBase) throws LauncherException
    {
        // Check metered SEs
        if (seInfo.isMetered())
        { 
            LocalDate referenceStartDate = valuationBase.equals(BillingValuationBase.FINAL_FULL)
                                            ? seInfo.getEndDate().minusDays(Long.parseLong(windowOffset.get(0)))
                                            : billingRun.getStartDate();
            LocalDate referenceEndDate = valuationBase.equals(BillingValuationBase.FINAL_FULL)
                                            ? seInfo.getEndDate().plusDays(Long.parseLong(windowOffset.get(0)))
                                            : billingRun.getEndDate();
            if (meterReadRepository.existsByStatusAndContractIdAndIsWithinDateRange(
                                MeterReadStatus.AVAILABLE, 
                                contract.getId(), 
                                referenceStartDate,
                                referenceEndDate))
                return true;
            else
                return false;
        }

        // Check BILLABLE_CHARGE SEs
        if (ServiceElementTypeCategory.BILLABLE_CHARGE.equals(seInfo.getType().getSeTypeCategory()))
        {
            var posRefList = contractPointOfServiceRepository.findAllByContractId(contract.getId())
                                                            .stream()
                                                            .map(ContractPointOfService::getPointOfService)
                                                            .map(PointOfService::getReference)
                                                            .collect(Collectors.toSet());
            return billableChargeRepository.existsByPosRefInAndStatus(posRefList, BillableChargeStatus.AVAILABLE);
        }
        // Check PAYMENT_PLAN SEs
        if (ServiceElementTypeCategory.PAYMENT_PLAN.equals(seInfo.getType().getSeTypeCategory()))
        {
            List<BillSegmentStatus> bsStatusExcluded = new ArrayList<>(Arrays.asList(BillSegmentStatus.CANCELLED, BillSegmentStatus.ERROR));
            var billSegment = billSegmentRepository.findFirstBySeIdAndStatusNotInOrderByEndDateDesc(seInfo.getId(), bsStatusExcluded);
            if (billSegment.isPresent() && billSegment.get().getEndDate() != null)
            {
                var billSegmentEndDate = billSegment.get().getEndDate();
                return (billSegmentEndDate.isBefore(LocalDate.now().plusDays(Long.parseLong(windowOffset.get(0)))));
            }
        }
        return false;
    }
}
