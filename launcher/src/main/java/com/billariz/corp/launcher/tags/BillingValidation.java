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

import static java.util.stream.Collectors.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.Article;
import com.billariz.corp.database.model.Bill;
import com.billariz.corp.database.model.BillDetail;
import com.billariz.corp.database.model.BillSegment;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.Parameter;
import com.billariz.corp.database.model.VatDetail;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.BillNature;
import com.billariz.corp.database.model.enumeration.BillSegmentStatus;
import com.billariz.corp.database.model.enumeration.BillStatusEnum;
import com.billariz.corp.database.model.enumeration.BillableChargeStatus;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.MeterReadStatus;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.model.light.messageCode;
import com.billariz.corp.database.repository.BillRepository;
import com.billariz.corp.database.repository.BillSegmentRepository;
import com.billariz.corp.database.repository.BillableChargeRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.MeterReadRepository;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.launcher.billing.BillingService;
import com.billariz.corp.launcher.Launcher;
import com.billariz.corp.launcher.exception.LauncherException;
import com.billariz.corp.launcher.exception.LauncherFatalException;
import com.billariz.corp.launcher.utils.EventUtils;
import com.billariz.corp.utils.EntityReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BillingValidation implements Launcher
{
    private static final String ACTIVITY_CONTRACT="ACTIVITY_CONTRACT";

    private static final String ACTIVITY_PERIMETER="ACTIVITY_PERIMETER";

    private static final String ACTIVITY_INVOICE="ACTIVITY_INVOICE";

    private final EventRepository       eventRepository;

    private final JournalRepository     journalRepository;

    private final EventUtils            eventUtils;

    private final RelationRepository    relationRepository;

    private final BillRepository        billRepository;

    private final BillSegmentRepository billSegmentRepository;

    private final MeterReadRepository   meterReadRepository;

    private final BillableChargeRepository  billableChargeRepository;

    private final ParameterRepository           parameterRepository;

    private final EntityReferenceGenerator referenceUtils;

    private final BillingService      billingService;

    @Override
   @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Iterable<Long> eventIds, EventExecutionMode executionMode)
    {
        var events = eventRepository.findAllById(eventIds);

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
            process(event, journal, messages);
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
        log.debug("Event[{}] processed, strating next process", event.getId());
        eventUtils.triggerOnUpdateEventStatus(event);
        journal.setNewStatus(Objects.toString(event.getStatus()));
        journal.setMessageCodes(messages);
        journalRepository.save(journal);

    }

    private void process(Event event, Journal journal, List<messageCode> messages) throws LauncherException
    {
        if(event.getAction()!=null)
            messages.add(new messageCode("EVENT_USE_CASE", new Object[]{event.getAction()}));

        var relationInv = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_INVOICE, 
                            event.getActivityId());
        log.info("relation[{}] found for Event[{}] ", relationInv, event);
        
        List<Bill> bills = new ArrayList<>();
        if(!relationInv.isPresent())
        {
            var contractRelation = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_CONTRACT,
                    event.getActivityId());
            var perimeterRelation = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_PERIMETER,
                    event.getActivityId());
            bills = (!perimeterRelation.isPresent())
                    ? billRepository.findAllByStatusAndContractId(BillStatusEnum.CALCULATED, contractRelation.get().getSecondObjectId())
                    : billRepository.findAllByStatusAndPerimeterId(BillStatusEnum.CALCULATED, perimeterRelation.get().getSecondObjectId());
            bills.stream().forEach(b -> b.setCascaded(true)); // petmet d'hiniber le listner
        }
        else {
                var bl= billRepository.findById(relationInv.get().getSecondObjectId()).orElseThrow(
                () -> new LauncherFatalException("MISSING_BILL_IN_DB", new Object[]{
                            relationInv.get().getSecondObjectId()}));
                bl.setCascaded(true); // petmet d'hiniber le listner
                bills.add(bl);
            }
        
        if (bills.isEmpty())
            new LauncherFatalException("MISSING_BILLS_TO_VALIDATE", new Object[]{event.getId()});
        
        //Récupérer les paramètres d'entrée
        var amountMax= parameterRepository.findAllByTypeAndNameAndStartDateBefore("BILLING",
                                        "maxAmountValidationAuto",LocalDate.now())
                                    .stream().map(Parameter::getValue)
                                    .findFirst()
                                    .orElseThrow(() -> new LauncherFatalException("MISSING_PARAMETER",  new Object[]{"maxAmountValidationAuto"} 
                                    ));
        messages.add(new messageCode("VALUE_FOR_PARAMETER", new Object[]{"maxAmountValidationAuto", amountMax}));
        for (Bill bill : bills) {
                if(!bill.getGroup()){
                    //Prepare simple bill
                    var bss = billSegmentRepository.findAllByBillId(bill.getId());
                    Map<String, BigDecimal> bssTotalByVatMap = bss.stream()
                            .collect(Collectors.groupingBy(
                                BillSegment::getVatRate, 
                                Collectors.mapping(
                                    BillSegment::getAmount,
                                    Collectors.reducing(
                                        BigDecimal.ZERO,
                                        amount -> bill.getNature().equals(BillNature.CREDIT_NOTE) ? amount.negate() : amount,
                                        BigDecimal::add 
                                    )
                                )
                            ));
                    Map<String, BigDecimal> billLineTotalByVatMap = bill.getDetails().stream().collect(Collectors.groupingBy(
                                BillDetail::getVatRate, 
                                Collectors.mapping(
                                    BillDetail::getTotalWithoutVat,
                                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                                )
                            ));
                    var sumLine = billLineTotalByVatMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                    var sumTva = bill.getVatDetails().stream().map(VatDetail::getTotalVat).reduce(BigDecimal.ZERO, BigDecimal::add);

                    //#1// Vérifie la cohérence entre les totaux BS (Billing Summary) et les lignes de facturation par taux de TVA.
                        // Les paramètres bssTotalByVatMap et billLineTotalByVatMap contiennent respectivement les totaux par taux de TVA
                        // pour le résumé de facturation et les lignes de facture.
                    billCheckBsVsLinesByVatRate(bssTotalByVatMap, billLineTotalByVatMap);
                    messages.add(new messageCode("BILL_CHECK_BSVSLINES", new Object[]{bssTotalByVatMap, billLineTotalByVatMap}));

                    //#2// somme des lignes = total hors TVA
                    // Checks the consistency between the sum of the invoice lines (sumLine) and the overall bill totals (bill).
                    billCheckLinesVsTotals( sumLine, bill);
                    messages.add(new messageCode("BILL_CHECK_LINESVSTOTAL", new Object[]{sumLine, bill.getTotalAmount()}));
                    //#3// somme total hors TVA + total TVa = total general
                    // Checks the consistency between the sum of the invoice lines (sumLine), the total VAT (sumTva),
                    // and the overall bill totals (bill).
                    billCheckSumVatTotVsTotals( sumLine, sumTva, bill);
                    messages.add(new messageCode("BILL_CHECK_OVERAL", new Object[]{sumLine, sumTva, bill.getTotalAmount()}));
                }
                else { //GroupBill
                    //Prepare GroupBill
                    var childBills = billRepository.findAllByGroupBillId(
                            (bill.getNature().equals(BillNature.CREDIT_NOTE))?bill.getCancelledBillId():bill.getId());
                    Map<String, BigDecimal> childBillsTotalVatByRateMap = childBills.stream()
                                .flatMap(bl -> bl.getVatDetails().stream())
                                .collect(Collectors.groupingBy(
                                    VatDetail::getVatRate,
                                    Collectors.mapping(
                                        VatDetail::getTotalVat,
                                        Collectors.reducing(
                                            BigDecimal.ZERO, 
                                            totalVat -> bill.getNature().equals(BillNature.CREDIT_NOTE) ? totalVat.negate() : totalVat,
                                            BigDecimal::add) // Faire la somme
                                )
                            ));
                    Map<String, BigDecimal> totalVatPerVatRate = bill.getVatDetails().stream().collect(Collectors.groupingBy(
                                VatDetail::getVatRate, // Regrouper par vatRate
                                Collectors.mapping(
                                    VatDetail::getTotalVat, // Extraire amount
                                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add) // Faire la somme
                                )
                            ));
                    //#4// somme TVA par vat rate est égale à tva par vat rate du group
                    // Checks the consistency between the VAT totals grouped by rate from child bills (childBillsTotalVatByRateMap) 
                    // and the overall VAT totals grouped by rate (totalVatPerVatRate).
                    groupBillCheckSumVatByRateVsTotals(childBillsTotalVatByRateMap, totalVatPerVatRate);
                    messages.add(new messageCode("GROUP_BILL_CHECK_BSVSLINES", new Object[]{childBillsTotalVatByRateMap, totalVatPerVatRate}));

                    //#5// somme total hors TVA du group + total TVa du group = total general
                    // Checks the consistency between the total VAT grouped by rate (totalVatPerVatRate) 
                    // and the overall bill totals (bill).
                    groupBillCheckSumVatTotVsTotals(totalVatPerVatRate, bill);
                    messages.add(new messageCode("GROUP_BILL_CHECK_SUMVATVSTOT", new Object[]{totalVatPerVatRate, bill.getTotalAmount()}));
                }

                //#6// Montant ne dépassant pas une borne pour validation auto
                // Checks if the total amount of the bill (bill) exceeds the maximum allowed threshold (amountMax) for an auto validation.
                // Triggers an event (event) if the threshold is violated.
                totalAmountThresholdCheck(bill, amountMax, event);
                messages.add(new messageCode("GROUP_BILL_CHECK_SUMVATVSTOT", new Object[]{bill.getTotalAmount(), amountMax}));

                //Valider la facture
                validateBill(bill, event.getTriggerDate());
        }

        journal.setMessageCodes(messages);
    }

    private boolean totalAmountThresholdCheck(Bill bill, String amountMax, Event event) throws LauncherException
    {
        if(bill.getTotalAmount().compareTo(BigDecimal.valueOf(Integer.parseInt(amountMax))) > 0){
            event.setExecutionMode(EventExecutionMode.MANUAL);
            throw new LauncherFatalException(String.format(
                    "AMOUNT_OUT_OF_BOUND",  new Object[]{bill.getTotalAmount(), amountMax}));
        }
        return true;
    }

    private boolean groupBillCheckSumVatTotVsTotals(Map<String, BigDecimal> totalVatPerVatRate, Bill bill) throws LauncherException
    {
        var totalVat = totalVatPerVatRate.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        var tot = bill.getTotalWithoutVat().add(totalVat);
        if(tot.compareTo(bill.getTotalAmount()) !=0)
            throw new LauncherFatalException("VALIDATION_TOTAL_MISMATCH",new Object[]{
                bill.getTotalWithoutVat(),
                totalVat,
                bill.getTotalAmount()
            });

        return true;
    }

    private boolean groupBillCheckSumVatByRateVsTotals(Map<String, BigDecimal> childBillsTotalVatByRateMap, Map<String, BigDecimal> totalVatPerVatRate) throws LauncherException
    {
        for (String vatRate : childBillsTotalVatByRateMap.keySet()){
            BigDecimal childBillsTotal = childBillsTotalVatByRateMap.getOrDefault(vatRate, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalVat = totalVatPerVatRate.getOrDefault(vatRate, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            if (childBillsTotal.compareTo(totalVat) != 0) {
                throw new LauncherFatalException(String.format(
                    "VALIDATION_VAT_MISMATCH", new Object[]{
                    vatRate, childBillsTotal, totalVat}
                ));
            }

        }
        return true;
    }

    private boolean billCheckLinesVsTotals(BigDecimal sumLine, Bill bill) throws LauncherException
    {
        if(sumLine.compareTo(bill.getTotalWithoutVat()) != 0)
                    throw new LauncherFatalException(String.format(
                            "VALIDATION_LINES_MISMATCH", new Object[]{
                            sumLine,
                            bill.getTotalWithoutVat()}
                    ));
        return true;
    }

    private boolean billCheckSumVatTotVsTotals(BigDecimal sumLine, BigDecimal sumTva, Bill bill) throws LauncherException
    {
        var calcTot = sumLine.add(sumTva);
        if(calcTot.compareTo(bill.getTotalAmount()) != 0)
            throw new LauncherFatalException(String.format(
                "VALIDATION_TOTAL_CONSISTANCY_MISMATCH", new Object[]{
                sumLine,
                sumTva,
                bill.getTotalAmount()}
            ));
        return true;
    }

    private boolean billCheckBsVsLinesByVatRate(Map<String, BigDecimal> bssTotalByVatMap, Map<String, BigDecimal> billLineTotalByVatMap) throws LauncherException {
        for (String vatRate : bssTotalByVatMap.keySet()) {
            BigDecimal bssTotal = bssTotalByVatMap.getOrDefault(vatRate, BigDecimal.ZERO);
            BigDecimal billLineTotal = billLineTotalByVatMap.getOrDefault(vatRate, BigDecimal.ZERO);
    
            if (bssTotal.compareTo(billLineTotal) != 0) {
                throw new LauncherFatalException(String.format(
                    "VALIDATION_VAT_LINES_MISMATCH", new Object[]{
                    vatRate, bssTotal, billLineTotal}
                ));
            }
        }
        return true;
    }

    private void validateBill(Bill bill, LocalDate acountingDate) throws LauncherException
    {
        bill.setAccountingDate(acountingDate);
        bill.setStatus(BillStatusEnum.VALIDATED);
        bill.setReference(referenceUtils.referenceGenerator(ObjectType.BILL));

        if(!bill.getGroup())   
            processBillSegments(bill, false, null);
        else {
            List<Bill> childBills;
            childBills = billRepository.findAllByGroupBillId(
                   bill.getNature().equals(BillNature.CREDIT_NOTE) ? bill.getCancelledBillId():bill.getId());
            if (childBills.isEmpty())
                    throw new LauncherFatalException("MISSING_CHILD_BILLS", new Object[]{bill.getId()});
            childBills.forEach(bl -> updateChildbill(bl,bill.getNature()));
        }
    }

    private void updateChildbill(Bill bill, BillNature groupBillNature)
    {
        if(groupBillNature.equals(BillNature.CREDIT_NOTE)){
            bill.setStatus(BillStatusEnum.CALCULATED);
            bill.setGroupBillId(null);
        }
        else
            bill.setStatus(BillStatusEnum.BILLED);
        
        processBillSegments(bill, true, groupBillNature);
    }

    private void processBillSegments(Bill bill, boolean isGroup, BillNature groupBillNature)
    {
        List<BillSegment> billSegments = new ArrayList<>();
        billSegments = billSegmentRepository.findAllByBillId(bill.getId());
        List<Long> bs = billSegments.stream().map(BillSegment::getId).distinct().collect(toList());
        List<Long> mrs = billSegments.stream().map(BillSegment::getMeterReadId).distinct().collect(toList());
        List<Long> bcs = billSegments.stream().map(billSegment -> {
                                Article article = billSegment.getArticle();
                                return article != null ? article.getBillableChargeId() : null;
                            }).distinct().collect(toList());
        if(!isGroup){
            billSegmentRepository.updateBillSegmentStatusAndBillIdById((bill.getNature().equals(BillNature.CREDIT_NOTE)) ?  
                            BillSegmentStatus.CANCELLED:BillSegmentStatus.BILLED, bill.getId(), bs);
            meterReadRepository.updateStatus(mrs, (bill.getNature().equals(BillNature.CREDIT_NOTE)) ? 
                            MeterReadStatus.AVAILABLE : MeterReadStatus.BILLED);
            billableChargeRepository.updateStatus(bcs, (bill.getNature().equals(BillNature.CREDIT_NOTE)) ? 
                            BillableChargeStatus.AVAILABLE : BillableChargeStatus.BILLED);
        }
        else{
            billSegmentRepository.updateBillSegmentStatusAndBillIdById((groupBillNature.equals(BillNature.CREDIT_NOTE)) ?  
                            BillSegmentStatus.COMPUTED:BillSegmentStatus.BILLED, bill.getId(), bs);
            meterReadRepository.updateStatus(mrs, (groupBillNature.equals(BillNature.CREDIT_NOTE)) ? 
                            MeterReadStatus.COMPUTED : MeterReadStatus.BILLED);
            billableChargeRepository.updateStatus(bcs, (groupBillNature.equals(BillNature.CREDIT_NOTE)) ? 
                            BillableChargeStatus.COMPUTED : BillableChargeStatus.BILLED);
        }
        billSegments.stream().forEach(bseg -> {
                                                billingService.stopServices(bseg);
                                                });
    }
}
