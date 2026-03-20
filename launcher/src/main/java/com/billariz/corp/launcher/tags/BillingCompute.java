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

import static com.billariz.corp.launcher.utils.FilterUtils.*;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.Article;
import com.billariz.corp.database.model.Bill;
import com.billariz.corp.database.model.BillDetail;
import com.billariz.corp.database.model.BillSegment;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.Parameter;
import com.billariz.corp.database.model.Perimeter;
import com.billariz.corp.database.model.VatDetail;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.BillNature;
import com.billariz.corp.database.model.enumeration.BillSegmentStatus;
import com.billariz.corp.database.model.enumeration.BillStatusEnum;
import com.billariz.corp.database.model.enumeration.BillType;
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
import com.billariz.corp.database.repository.ContractRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.MeterReadRepository;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.database.repository.PerimeterRepository;
import com.billariz.corp.database.repository.RateRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.database.repository.TermOfServiceRepository;
import com.billariz.corp.database.validator.BaseValidator;
import com.billariz.corp.launcher.Launcher;
import com.billariz.corp.launcher.exception.LauncherException;
import com.billariz.corp.launcher.exception.LauncherFatalException;
import com.billariz.corp.launcher.queue.LauncherQueue;
import com.billariz.corp.launcher.utils.BillingRunUtils;
import com.billariz.corp.launcher.utils.BillingUtils;
import com.billariz.corp.launcher.utils.EventUtils;
import com.billariz.corp.utils.EntityReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BillingCompute implements Launcher
{
    private static final String ACTIVITY_CONTRACT="ACTIVITY_CONTRACT";

    private static final String ACTIVITY_PERIMETER="ACTIVITY_PERIMETER";

    private static final String ACTIVITY_BILLING_RUN="ACTIVITY_BILLING_RUN";

    private static final String ACTIVITY_INVOICE="ACTIVITY_INVOICE";

    private final BillRepository           billRepository;

    private final BillSegmentRepository    billSegmentRepository;

    private final ContractRepository       contractRepository;

    private final RateRepository           rateRepository;

    private final TermOfServiceRepository termOfServicesRepository;

    private final EventRepository          eventRepository;

    private final RelationRepository       relationRepository;

    private final JournalRepository        journalRepository;

    private final PerimeterRepository      perimeterRepository;

    private final EventUtils               eventUtils;

    private final BillingRunUtils           billingRunUtils;

    private final LauncherQueue            launcherQueue;

    private final MeterReadRepository      meterReadRepository;

    private final BillableChargeRepository  billableChargeRepository;

    private final ParameterRepository parameterRepository;

    private final BillingUtils       billingUtils;

    private final EntityReferenceGenerator referenceUtils;

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

        var contractRelation = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_CONTRACT,
                event.getActivityId());
        log.info("relation[{}] found for Event[{}] ", contractRelation, event);
        
        var perimeterRelation = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_PERIMETER,
                event.getActivityId());
        var relationBrun = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_BILLING_RUN,
                event.getActivityId());
        var bill = new Bill();
        bill.setCascaded(true); // permet d'inhiber le listner sur le bill
        if (contractRelation.isPresent())
        {
            var contract = contractRepository.findById(contractRelation.get().getSecondObjectId()).orElseThrow(
                    () -> new LauncherFatalException("MISSING_CONTRACT_IN_DB",  new Object[]{contractRelation.get().getSecondObjectId()}));
            var tosId = termOfServicesRepository.findAllIds(contract.getId());
            log.info("process contractId={}", contractRelation.get().getSecondObjectId());

            if (event.getAction()!=null && event.getAction().equals("CREDIT_NOTE"))
            {
                var relationInv = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_INVOICE, 
                                        event.getActivityId()).orElseThrow(
                                                () -> new LauncherFatalException("MISSING_ACTIVITY_INVOICE", new Object[]{event.getActivityId()}));
                Bill cancelledBill = billRepository.findById(relationInv.getSecondObjectId()).orElseThrow(
                    () -> new LauncherFatalException("MISSING_BILL_IN_DB", new Object[]{relationInv.getSecondObjectId()}));
                cancelledBill.setCascaded(true); // permet d'inhiber le listner sur le bill
                computeCreditInvoice(bill, cancelledBill);
                billRepository.save(bill);
                processBillSegments(bill.getId(),cancelledBill.getId(), null, BillSegmentStatus.COMPUTED, MeterReadStatus.COMPUTED, BillableChargeStatus.COMPUTED);
                cancelBill(cancelledBill, bill.getId());
            }
            else
                {
                    List<BillSegment> billSegments;
                    billSegments = billSegmentRepository.findByTosIdInAndStatus(tosId, BillSegmentStatus.CALCULATED);
                    log.info("billSegments to be processed:", billSegments);
                    computeNormalInvoice(bill, contract, billSegments, tosId);
                    billRepository.save(bill);
                    processBillSegments(bill.getId(),null, billSegments, BillSegmentStatus.COMPUTED, MeterReadStatus.COMPUTED, BillableChargeStatus.COMPUTED);
                }

            setBillType(bill, contract, relationBrun.isPresent());
            bill.setBillingRunId(relationBrun.map(r -> r.getSecondObjectId())
                     .orElseGet(() -> billingRunUtils.getBillingRunId(LocalDate.now().minusDays(1), bill.getType())));
            bill.setReference(referenceUtils.referenceGenerator(ObjectType.CALCUL));
            launcherQueue.createRelation(ACTIVITY_INVOICE, event.getActivityId(), bill.getId(), ObjectType.BILL);
            if(!relationBrun.isPresent())
                launcherQueue.createRelation(ACTIVITY_BILLING_RUN, event.getActivityId(), bill.getBillingRunId(), ObjectType.BILLING_RUN);
        }
        if (perimeterRelation.isPresent())
        {
            var perimeter = perimeterRepository.findById(perimeterRelation.get().getSecondObjectId()).orElseThrow(
                () -> new LauncherFatalException("MISSING_PERIMETER_IN_DB",  new Object[]{perimeterRelation.get().getSecondObjectId()}));
            if (event.getAction() !=null && event.getAction().equals("CREDIT_NOTE"))
            {
                var relationInv = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_INVOICE,
                        event.getActivityId()).orElseThrow(
                            () -> new LauncherFatalException("MISSING_ACTIVITY_INVOICE", new Object[]{event.getActivityId()}));
                var cancelledBill = billRepository.findById(relationInv.getSecondObjectId()).orElseThrow(
                        () -> new LauncherFatalException("MISSING_BILL_TO_CANCEL", new Object[]{relationInv.getSecondObjectId()}));

                List<Bill> childBills;
                childBills = billRepository.findAllByGroupBillId(cancelledBill.getId());
                if (childBills.isEmpty())
                    throw new LauncherFatalException("MISSING_CHILD_BILLS", new Object[]{cancelledBill.getId()});
                computeCreditGroupInvoice(bill, cancelledBill);
                billRepository.save(bill);
                cancelledBill.setStatus(BillStatusEnum.CANCELED);
            }
            else
                {
                    List<Bill> bills;
                    bills = billRepository.findAllByStatusAndPerimeterId(BillStatusEnum.CALCULATED, perimeter.getId());
                    if (bills.isEmpty())
                        throw new LauncherFatalException("MISSING_BILLS_FOR_PERIMETER", new Object[]{perimeter.getId(),BillStatusEnum.CALCULATED});
                    computeNormalGroupInvoice(bill, perimeter, bills);
                    billRepository.save(bill);
                    List<Long> billsIds = bills.stream()
                                                .map(Bill::getId)
                                                .collect(Collectors.toList());
                    billRepository.updateGroupBillId(bill.getId(),billsIds);
                }

            bill.setBillingRunId(relationBrun.isPresent() ? relationBrun.get().getSecondObjectId() : null);
            setGroupBillType(bill, perimeter.getEndDate(), relationBrun.isPresent());
            bill.setReference(referenceUtils.referenceGenerator(ObjectType.CALCUL));
            launcherQueue.createRelation(ACTIVITY_INVOICE, event.getActivityId(), bill.getId(), ObjectType.BILL);
        }
        journal.setMessageCodes(messages);
    }

    private void processBillSegments(Long billId, Long billToCancelId, List<BillSegment> billSegments, BillSegmentStatus bsStatus, MeterReadStatus mrStatus, BillableChargeStatus bcStatus) throws LauncherException
    {
        if(billSegments==null)
                billSegments = billSegmentRepository.findAllByBillId(billToCancelId);

        List<Long> bs = billSegments.stream().map(BillSegment::getId).distinct().collect(toList());
        billSegmentRepository.updateBillSegmentStatusAndBillIdById(bsStatus, billId, bs);

        // traiter les MR
        List<Long> mrs = billSegments.stream()
                        .map(BillSegment::getMeterReadId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
        meterReadRepository.updateStatus(mrs, mrStatus);

        // traiter les BC
        List<Long> bcs = billSegments.stream()
                        .map(billSegment -> {
                                Article article = billSegment.getArticle();
                                return article != null ? article.getBillableChargeId() : null;
                            })
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
        billableChargeRepository.updateStatus(bcs, bcStatus);
    }

    private void setBillType(Bill bill, Contract contract, boolean isCyclique) throws LauncherException
    {
        //TODO gérer getEffectiveEndDate VS getContractualEndDate
        if (bill.getEndDate().equals(contract.getContractualEndDate()))
            bill.setType(BillType.FINAL);
        else if (isCyclique)
            bill.setType(BillType.CYCLICAL);
        else
            bill.setType(BillType.EVENT);
    }

    private void setGroupBillType(Bill bill, LocalDate objectEndDate, boolean isCyclique) throws LauncherException
    {
        if (objectEndDate!=null && bill.getEndDate().equals(objectEndDate))
            bill.setType(BillType.FINAL);
        else if (isCyclique)
            bill.setType(BillType.CYCLICAL);
            else
                bill.setType(BillType.EVENT);
    }

    private void computeNormalGroupInvoice(Bill groupBill, Perimeter perimeter, List<Bill> bills) throws LauncherException
    {
        log.info("***Compute Normal Group Bill***");
        var now = LocalDate.now();
        var vatRate = parameterRepository.findAllByTypeAndNameAndStartDateBefore("BILLING", BaseValidator.VAT_RATE_INDEX,
                now).stream().map(Parameter::getValue).findFirst().orElseThrow(
                    () -> new LauncherFatalException("MISSING_PARAMETER ", new Object[]{BaseValidator.VAT_RATE_INDEX}));
        List<BillDetail> billsDetails = new ArrayList<>();
        bills.stream().flatMap(b -> b.getDetails().stream()).forEach(billsDetails::add);
        var detailsByRate = billsDetails.stream().collect(groupingBy(BillDetail::getVatRate));
        Set<String> vatRates = detailsByRate.keySet();
        groupBill.setNature(BillNature.NORMAL);
        groupBill.setStatus(BillStatusEnum.CALCULATED);
        groupBill.setPerimeterId(perimeter.getId());
        groupBill.setCustomerId(perimeter.getCustomerId());
        List<VatDetail> vatDetails = new ArrayList<>();
        for (String rate : vatRates) {
            vatDetails.add(computeVAT(vatRate, rate, detailsByRate.get(rate)));
        }
        groupBill.setVatDetails(vatDetails);
        groupBill.setTotalVat(groupBill.getVatDetails().stream().map(VatDetail::getTotalVat).reduce(BigDecimal.ZERO, BigDecimal::add));
        groupBill.setTotalWithoutVat(bills.stream().map(Bill::getTotalWithoutVat).reduce(BigDecimal.ZERO, BigDecimal::add));
        groupBill.setTotalAmount(groupBill.getTotalWithoutVat().add(groupBill.getTotalVat()));
        groupBill.setBillDate(LocalDate.now());
        groupBill.setStartDate(bills.stream().map(Bill::getStartDate).sorted().findFirst().get());
        groupBill.setEndDate(bills.stream().map(Bill::getEndDate).sorted(reverseOrder()).findFirst().get());
        groupBill.setGroup(true);
    }

    private void computeCreditGroupInvoice(Bill bill, Bill billCancel) throws LauncherException
    {
        log.info("***Compute Credit Group Bill***");
        bill.setContractId(billCancel.getContractId());
        bill.setPerimeterId(billCancel.getPerimeterId());
        bill.setCustomerId(billCancel.getCustomerId());
        bill.setNature(BillNature.CREDIT_NOTE);
        bill.setCancelledBill(billCancel);
        bill.setStatus(BillStatusEnum.CALCULATED);
        bill.setBillDate(LocalDate.now());
        bill.setStartDate(billCancel.getStartDate());
        bill.setEndDate(billCancel.getEndDate());
        bill.setTotalVat(billCancel.getTotalVat().multiply(new BigDecimal(-1)));
        bill.setTotalWithoutVat(billCancel.getTotalWithoutVat().multiply(new BigDecimal(-1)));
        bill.setTotalAmount(billCancel.getTotalAmount().multiply(new BigDecimal(-1)));
        bill.setCancelledBill(billCancel);
        bill.setGroup(true);
        List<VatDetail> updatedVatDetails = billCancel.getVatDetails().stream()
        .map(v -> {
            VatDetail newVatDetail = new VatDetail();
            newVatDetail.setTotalVat(v.getTotalVat().negate());
            newVatDetail.setTotalWithoutVat(v.getTotalWithoutVat().negate());
            newVatDetail.setVatRate(v.getVatRate());
            return newVatDetail;
        })
        .collect(Collectors.toList());
        bill.setVatDetails(updatedVatDetails);
    }

    private void computeNormalInvoice(Bill bill, Contract contract, List<BillSegment> billSegments, List<Long> tosId) throws LauncherException
    {
        log.info("***Compute Normal Bill***");
        if(billSegments.isEmpty())
                throw new LauncherFatalException("MISSING_BS_TO_COMPUTE", null);
        var now = LocalDate.now();
        var vatRate = parameterRepository.findAllByTypeAndNameAndStartDateBefore("BILLING", BaseValidator.VAT_RATE_INDEX,
                now).stream().map(Parameter::getValue).findFirst().orElseThrow(
                    () -> new LauncherFatalException("MISSING_PARAMETER ", new Object[]{BaseValidator.VAT_RATE_INDEX}));
        List<BillDetail> details = new ArrayList<>();
        for (BillSegment bs : billSegments) {
            details.add(buildFromSegment(bill, bs));
        }
        var detailsByRate = details.stream().collect(groupingBy(BillDetail::getVatRate));
        Set<String> vatRates = detailsByRate.keySet();
        log.debug("process: billSegments={}", billSegments);
        bill.setNature(BillNature.NORMAL);
        bill.setStatus(
                billSegmentRepository.countingByStatusAndTosIdIn(BillSegmentStatus.ERROR, tosId) == 0L ? BillStatusEnum.CALCULATED : BillStatusEnum.IN_FAILURE);
        bill.setContractId(contract.getId());
        bill.setPerimeterId(contract.getContractPerimeter().getPerimeterId());
        bill.setCustomerId(contract.getContractPerimeter().getPerimeter().getCustomerId());
        List<VatDetail> vatDetails = new ArrayList<>();
        for (String rate : vatRates) {
            vatDetails.add(computeVAT(vatRate, rate, detailsByRate.get(rate)));
        }
        bill.setVatDetails(vatDetails);
        bill.setTotalVat(bill.getVatDetails().stream().map(VatDetail::getTotalVat).reduce(BigDecimal.ZERO, BigDecimal::add));
        bill.setTotalWithoutVat(details.stream().map(BillDetail::getTotalWithoutVat).reduce(BigDecimal.ZERO, BigDecimal::add));
        bill.setTotalAmount(bill.getTotalWithoutVat().add(bill.getTotalVat()));
        bill.setBillDate(LocalDate.now());
        bill.setStartDate(details.stream().map(BillDetail::getStartDate).sorted().findFirst().get());
        bill.setEndDate(details.stream().map(BillDetail::getEndDate).sorted(reverseOrder()).findFirst().get());
        bill.setGroup(false);
        bill.setDetails(details);
    }

    private void computeCreditInvoice(Bill bill, Bill billToCancel) throws LauncherException
    {
        List<BillDetail> details = new ArrayList<>();
        bill.setContractId(billToCancel.getContractId());
        bill.setPerimeterId(billToCancel.getPerimeterId());
        bill.setCustomerId(billToCancel.getCustomerId());
        bill.setNature(BillNature.CREDIT_NOTE);
        bill.setCancelledBill(billToCancel);
        bill.setStatus(BillStatusEnum.CALCULATED);
        bill.setBillDate(LocalDate.now());
        bill.setStartDate(billToCancel.getStartDate());
        bill.setEndDate(billToCancel.getEndDate());
        bill.setTotalVat(billToCancel.getTotalVat().multiply(new BigDecimal(-1)));
        bill.setTotalWithoutVat(billToCancel.getTotalWithoutVat().multiply(new BigDecimal(-1)));
        bill.setTotalAmount(billToCancel.getTotalAmount().multiply(new BigDecimal(-1)));
        bill.setGroup(false);
        for (BillDetail dt : billToCancel.getDetails()) {
            details.add(setBillDetail(dt, bill));
        }
        bill.setDetails(details);
        List<VatDetail> updatedVatDetails = billToCancel.getVatDetails().stream()
        .map(v -> {
            VatDetail newVatDetail = new VatDetail();
            newVatDetail.setTotalVat(v.getTotalVat().negate());
            newVatDetail.setTotalWithoutVat(v.getTotalWithoutVat().negate());
            newVatDetail.setVatRate(v.getVatRate());
            return newVatDetail;
        })
        .collect(Collectors.toList());
        bill.setVatDetails(updatedVatDetails);
    }

    private void cancelBill(Bill billToCancel, Long creditNoteId) throws LauncherException
    {
        billToCancel.setStatus(BillStatusEnum.CANCELED);
        billToCancel.setCancelledByBillId(creditNoteId);
    }

    private BillDetail setBillDetail(BillDetail cancelledBillDetail, Bill bill) throws LauncherException
    {
        BillDetail billDetail = new BillDetail();
        billDetail.setQuantity(cancelledBillDetail.getQuantity().multiply(new BigDecimal(-1)));
        billDetail.setTotalWithoutVat(cancelledBillDetail.getTotalWithoutVat().multiply(new BigDecimal(-1)));
        billDetail.setBill(bill);
        billDetail.setBillLineCategory(cancelledBillDetail.getBillLineCategory());
        billDetail.setBillLineSubCategory(cancelledBillDetail.getBillLineSubCategory());
        billDetail.setStartDate(cancelledBillDetail.getStartDate());
        billDetail.setEndDate(cancelledBillDetail.getEndDate());
        billDetail.setQuantityUnit(cancelledBillDetail.getQuantityUnit());
        billDetail.setPrice(cancelledBillDetail.getPrice());
        billDetail.setPriceUnit(cancelledBillDetail.getPriceUnit());
        billDetail.setVatRate(cancelledBillDetail.getVatRate());
        billDetail.setTou(cancelledBillDetail.getTou());
        billDetail.setLineType(cancelledBillDetail.getLineType());
        return billDetail;
    }

    private VatDetail computeVAT(String vatRateIndex, String vatRate, List<BillDetail> details) throws LauncherException
    {   
        VatDetail vatDetail = new VatDetail();
        if (details == null)
            return null;
        var rates = rateRepository.findAllByTypeAndThresholdBase(vatRateIndex,vatRate);
        if (rates.isEmpty())
                throw new LauncherFatalException("MISSING_VAT_RATE", new Object[]{vatRateIndex,vatRate});
        var rate = rates.stream().filter(r -> filterRate(r, details.get(0))).findFirst().orElseThrow(
            () -> new LauncherFatalException("MISSING_VAT_RATE_VALUES", new Object[]{vatRateIndex, details.get(0).toString()}));
        var totalWithoutVat = details.stream().map(BillDetail::getTotalWithoutVat).reduce(BigDecimal.ZERO, BigDecimal::add);
        vatDetail.setBillId(details.get(0).getBill().getId());
        vatDetail.setVatRate(vatRate);
        vatDetail.setTotalWithoutVat(totalWithoutVat);
        vatDetail.setTotalVat(rate.getUnit().equalsIgnoreCase(BaseValidator.UNIT_PERCENTAGE) ?
                                        billingUtils.calculatePercentage(totalWithoutVat, rate.getPrice()) :
                                        billingUtils.calculateAmount(totalWithoutVat, rate.getPrice())
                            );
        return vatDetail;
    }

    private BillDetail buildFromSegment(Bill bill, BillSegment billSegment) throws LauncherException
    {
        var billDetail = new BillDetail();

        billDetail.setBill(bill);
        if(billSegment.getArticleId()==null){
            billDetail.setBillLineCategory(billSegment.getSe().getCategory());
            billDetail.setBillLineSubCategory(billSegment.getSe().getSubCategory());
        } else {
            billDetail.setBillLineCategory(billSegment.getArticle().getArticleType().getCategory());
            billDetail.setBillLineSubCategory(billSegment.getArticle().getArticleType().getSubCategory());
        }
        billDetail.setStartDate(billSegment.getStartDate());
        billDetail.setEndDate(billSegment.getEndDate());
        billDetail.setQuantity(billSegment.getQuantity());
        billDetail.setQuantityUnit(billSegment.getQuantityUnit());
        billDetail.setPrice(billSegment.getPrice());
        billDetail.setPriceUnit(billSegment.getPriceUnit());
        billDetail.setVatRate(billSegment.getVatRate());
        billDetail.setTotalWithoutVat(billSegment.getAmount());
        billDetail.setTou(billSegment.getTou());
        billDetail.setLineType(billSegment.getSe().getBillingScheme()
                    .equals(BaseValidator.BILLING_SCHEME_NORMAL) ? BaseValidator.BILL_LINE : BaseValidator.INFO_LINE
                    );
        return billDetail;
    }
}
