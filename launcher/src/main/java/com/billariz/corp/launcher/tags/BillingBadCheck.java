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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.MeterRead;
import com.billariz.corp.database.model.MeterReadDetail;
import com.billariz.corp.database.model.Parameter;
import com.billariz.corp.database.model.PointOfService;
import com.billariz.corp.database.model.PointOfServiceConfiguration;
import com.billariz.corp.database.model.PointOfServiceEstimate;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.BillSegmentStatus;
import com.billariz.corp.database.model.enumeration.BillingValuationBase;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.MeterReadContext;
import com.billariz.corp.database.model.enumeration.MeterReadQuality;
import com.billariz.corp.database.model.enumeration.MeterReadSource;
import com.billariz.corp.database.model.enumeration.MeterReadStatus;
import com.billariz.corp.database.model.enumeration.MeterReadType;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.model.enumeration.PointOfServiceDataStatus;
import com.billariz.corp.database.model.enumeration.SeStatus;
import com.billariz.corp.database.model.light.EligibleServiceElement;
import com.billariz.corp.database.model.light.messageCode;
import com.billariz.corp.database.repository.BillSegmentRepository;
import com.billariz.corp.database.repository.BillingRunRepository;
import com.billariz.corp.database.repository.ContractRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.MeterReadDetailRepository;
import com.billariz.corp.database.repository.MeterReadRepository;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.database.repository.ServiceElementRepository;
import com.billariz.corp.database.validator.BaseValidator;
import com.billariz.corp.launcher.Launcher;
import com.billariz.corp.launcher.exception.LauncherException;
import com.billariz.corp.launcher.exception.LauncherFatalException;
import com.billariz.corp.launcher.utils.BillingUtils;
import com.billariz.corp.launcher.utils.EventUtils;
import com.billariz.corp.launcher.utils.JournalUtils;
import com.billariz.corp.launcher.utils.MeterReadUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingBadCheck implements Launcher
{

    private static final String ACTIVITY_CONTRACT="ACTIVITY_CONTRACT";

    private static final String ACTIVITY_BILLING_RUN="ACTIVITY_BILLING_RUN";

    private static final String ESTIMATE = "ESTIMATE";

    private final EventRepository          eventRepository;

    private final JournalRepository        journalRepository;

    private final RelationRepository       relationRepository;

    private final EventUtils               eventUtils;

    private final ServiceElementRepository serviceElementRepository;

    private final ContractRepository       contractRepository;

    private final BillingRunRepository     billingRunRepository;

    private final BillSegmentRepository    billSegmentRepository;

    private final BillingUtils              billingUtils;

    private final ParameterRepository              parameterRepository;

    private final MeterReadRepository           meterReadRepository;

    private final MeterReadDetailRepository meterReadDetailRepository;

    private final JournalUtils                 journalUtils;

    private List<SeStatus> seListStatus;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Iterable<Long> eventIds, EventExecutionMode executionMode)
    {
        LocalDate now = LocalDate.now();
        seListStatus = parameterRepository
                                .findAllByTypeAndNameAndStartDateBefore("BILLABLE_STATUS", 
                                            BaseValidator.SERVICE_ELEMENT_STATUS, now)
                                .stream()
                                .map(Parameter::getValue)
                                .map(SeStatus::valueOf)
                                .toList();

        var events = eventRepository.findAllById(eventIds);
        events.forEach(e -> process(e, executionMode));
    }

    private void process(Event event, EventExecutionMode executionMode)
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
            if(wait){
                event.setExecutionMode(EventExecutionMode.AUTO);
                event.setStatus(EventStatus.PENDING);
                var firstEvent = eventRepository.findFirstByActivityId(event.getActivityId());
                firstEvent.setExecutionDate(null);
                messages.add(new messageCode("BAD_CHECK_ITERRATION",null));
            }
            else {
                event.setStatus(EventStatus.COMPLETED);
                eventUtils.triggerOnUpdateEventStatus(event);
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
        log.debug("Event[{}] processed, strating next process", event.getId());
        journal.setNewStatus(Objects.toString(event.getStatus()));
        journal.setMessageCodes(messages);
        journalRepository.save(journal);
    }

    private boolean process(Event event, Journal journal, List<messageCode> messages) throws LauncherException
    {
        if(event.getAction()!=null)
            messages.add(new messageCode("EVENT_USE_CASE", new Object[]{event.getAction()}));
        
        var valuationBase = BillingValuationBase.valueOf(event.getAction());

        var contractRelation = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_CONTRACT,
                event.getActivityId()).orElseThrow(
                    () -> new LauncherFatalException("MISSING_CONTRACT", new Object[]{event.getId()}));
        var contract = contractRepository.findById(contractRelation.getSecondObjectId()).orElseThrow(
                () -> new LauncherFatalException("MISSING_CONTRACT_IN_DB",  new Object[]{contractRelation.getSecondObjectId()}));
        var relationBrun = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_BILLING_RUN,
                event.getActivityId()).orElseThrow(
                    () -> new LauncherFatalException("MISSING_BILLING_RUN", new Object[]{event.getId()}));
        var billingRun = billingRunRepository.findById(relationBrun.getSecondObjectId()).orElseThrow(
                () -> new LauncherFatalException("MISSING_BILLING_RUN_IN_DB", new Object[]{relationBrun.getSecondObjectId()}));

        var serviceElementList = serviceElementRepository.findWithStatusInAndMasterIsTrueAndTosContractStatusIn(seListStatus,
                    contract.getId());
        if (serviceElementList.isEmpty())
                throw new LauncherFatalException("MISSING_BILLABLE_SERVICES", new Object[]{contract.getId(), event.getId()});

        if (!billingUtils.checkIfBillSegmentExist(serviceElementList, billingRun, valuationBase))
        {
            Map<String, List<EligibleServiceElement>> groupedByTouGroup = serviceElementList.stream()
                .collect(Collectors.groupingBy(EligibleServiceElement::getTouGroup));
            
            for (Map.Entry<String, List<EligibleServiceElement>> entry : groupedByTouGroup.entrySet()) {
                List<EligibleServiceElement> serviceElments = entry.getValue();
                MeterRead mr = new MeterRead();
                createEstimatedMr(mr, contract, serviceElments.get(0));
                List<MeterReadDetail> mrdList = new ArrayList<>();
                
                for (EligibleServiceElement serviceElement : serviceElments) {
                    if(serviceElement.isEstimateAuthorized() && MeterReadUtils.checkMinimumNbrOfDay(serviceElement, mr.getStartDate(), mr.getEndDate()))
                        createMeterReadDetail(mrdList, mr, contract, serviceElement);
                    else 
                        throw new LauncherFatalException("NO_ELEMENT_TO_BILL", new Object[]{serviceElement.getId(), contract.getId(), billingRun.getId()});
                }
                //Finaliser la MR
                if(mrdList.size()>0) {
                    mr.setMeterReadDetails(mrdList);
                    BigDecimal totalQuantity = mrdList.stream()
                                .map(MeterReadDetail::getQuantity)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                    mr.setTotalQuantity(totalQuantity);
                    meterReadRepository.save(mr);
                    journalUtils.addJournal(ObjectType.METER_READ, mr.getId(),
                                    "MR_ESTIMATED_CREATED", 
                                    new Object[]{event.getId()},
                                    event,
                                    JournalAction.LOG
                                    );
                    
                    messages.add(new messageCode("MR_ESTIMATED_CREATED_EVENT", new Object[]{mr.getId(), event.getId()}));
                }
            }
            return true; //Pending attente d'une nouvelle itération de l'event pour validation auto
        }
        // Retourner FALSE car les conditions de facturation sont OK, cloturer l'evenement
        return false; 
    }

    private void createEstimatedMr(MeterRead meterRead, Contract contract, EligibleServiceElement seMaster) throws LauncherException
    {
        var estimateEndDate = seMaster.getEndDate()==null ? contract.getBillAfterDate() : MeterReadUtils.getMinDate(contract.getBillAfterDate(), seMaster.getEndDate());
        var pivotdate = getPivotdate(seMaster);
        var estimateStartDate = pivotdate == null ? seMaster.getStartDate() : pivotdate;
        meterRead.setStartDate(estimateStartDate);
        meterRead.setEndDate(estimateEndDate);
        var pos = contract.getContractPointOfServices().get(0).getPointOfService();
        meterRead.setStartDate(estimateStartDate);
        meterRead.setEndDate(estimateEndDate);
        meterRead.setContext(MeterReadContext.ESTIMATE);
        meterRead.setDirection(pos.getDirection());
        meterRead.setMarket(pos.getMarket());
        meterRead.setPosRef(pos.getReference());
        meterRead.setQuality(MeterReadQuality.ESTIMATED);
        meterRead.setReceptionDate(LocalDate.now());
        meterRead.setSource(MeterReadSource.SYSTEM);
        meterRead.setStatus(MeterReadStatus.INITIALIZED);
        meterRead.setTouGroup(seMaster.getTouGroup());
        meterRead.setType(MeterReadType.INITIAL);
        meterRead.setUnit("kWh");
    }

    private void createMeterReadDetail(List<MeterReadDetail> mrdList, MeterRead mr, Contract contract, EligibleServiceElement seMaster) throws LauncherException 
    {
        if(seMaster.isMetered()) {
            
            List<PointOfServiceConfiguration> posConfig = contract.getContractPointOfServices().get(0).getPointOfService().getConfigurations().stream().filter(
                    es -> es.getStatus().equals(PointOfServiceDataStatus.VALIDATED)
                    && es.getStartDate().isBefore(mr.getEndDate())
                    && (es.getEndDate() == null || es.getEndDate().isAfter(mr.getStartDate())))
                    .collect(Collectors.toList());

            List<PointOfServiceEstimate> posEstimate = contract.getContractPointOfServices().get(0).getPointOfService().getEstimates().stream().filter(
                    es -> es.getStatus().equals(PointOfServiceDataStatus.VALIDATED) 
                    && es.getStartDate().isBefore(mr.getEndDate()) 
                    && (seMaster.getTou().equals("*") || es.getTou().equals(seMaster.getTou()))
                    && (es.getEndDate() == null || es.getEndDate().isAfter(mr.getStartDate())))
                    .collect(Collectors.toList());

            for(PointOfServiceConfiguration posConf : posConfig)
                    addMeterReadDetail(mrdList, seMaster.getTou(), contract.getContractPointOfServices().get(0).getPointOfService(), 
                                        posConf, posEstimate, mr.getStartDate(), mr.getEndDate());
        } 
                //Si SE non metered ou pas assez de jours alors ne rien faire, estimation non nécessaire
        else
            log.debug("SE[{}] processed, no estimate to calculate for this Se ", seMaster.getId());

    }

    private void addMeterReadDetail(List<MeterReadDetail> mrdL, String tou, PointOfService pos, PointOfServiceConfiguration posConf, 
                List<PointOfServiceEstimate> posEstim, LocalDate sd, LocalDate ed) throws LauncherException 
    {
        LocalDate subSd= MeterReadUtils.getMaxDate(sd, posConf.getStartDate());
        LocalDate subEd= MeterReadUtils.getMinDate(ed, posConf.getEndDate());
        var quantity = handleEstimate(tou, posEstim, posConf, subSd, subEd);
        var mrd = new MeterReadDetail();
        mrd.setMeasureType("EA");
        mrd.setStartDate(subSd);
        mrd.setEndDate(subEd);
        // mrd.setQuality(MeterReadQuality.ESTIMATED);
        // mrd.setSource(MeterReadSource.SYSTEM);
        mrd.setQuantity(quantity);
        mrd.setUnit("kWh");
        mrd.setTou(tou.equals("*") ? "TOTAL_HOUR":tou);
        mrdL.add(mrd);
    }

    private LocalDate getPivotdate(EligibleServiceElement serviceElement)
    {
        List<BillSegmentStatus> bsStatusExcluded = new ArrayList<>(Arrays.asList(BillSegmentStatus.CANCELLED, BillSegmentStatus.ERROR));
        var bs = billSegmentRepository.findFirstBySeIdAndStatusNotInOrderByEndDateDesc(serviceElement.getId(), bsStatusExcluded);
        if (bs.isPresent())
                return bs.get().getEndDate();
        return null;
    }

    private BigDecimal handleEstimate(String tou, List<PointOfServiceEstimate> posEstim, PointOfServiceConfiguration posConf, 
                                LocalDate sd, LocalDate ed)throws LauncherException 
    {
        List<MeterReadDetail> mrDetails = meterReadRepository.findWithStatusInAndContractAndSourceAndTouGroup(
                    Arrays.asList(MeterReadStatus.BILLED, 
                      MeterReadStatus.COMPUTED, 
                      MeterReadStatus.INITIALIZED, 
                      MeterReadStatus.VALUATED, 
                      MeterReadStatus.AVAILABLE),
                    posConf.getPointOfService().getContractPointOfService().getContractId(),
                    Arrays.asList(MeterReadSource.MARKET, MeterReadSource.USER),
                    posConf.getTouGroup()
                    ).stream()
                    .flatMap(mr -> meterReadDetailRepository.findAllByMeterReadIdAndMeasureType(mr.getId(), "EA").stream())
                    .filter(mrd -> (tou.equals("*")|| mrd.getTou().equals(tou)))
                    .collect(Collectors.toList());
        
        if(MeterReadUtils.hasContinuous12MonthHistory(mrDetails))
                   return MeterReadUtils.calculateProRataQuantity(mrDetails,sd,ed);
        else {
            if(posEstim.isEmpty())
                throw new LauncherFatalException("MISSING_ESTIMATE", new Object[]{posConf.getPosId(), tou});
            
            var esimate = posEstim.stream().filter(es -> es.getEstimateType().equals("CAR") 
                        || es.getEstimateType().equals("SUPPLIER_ESTIMATE")).findFirst().orElseThrow(
                            () -> new LauncherFatalException("MISSING_ESTIMATE_ELEGIBLE", new Object[]{posConf.getPosId(), tou})
                        );
            return esimate.getValue().multiply(BigDecimal.valueOf(MeterReadUtils.calculateDaysBetween(sd,ed)))
                        .divide(BigDecimal.valueOf(360), RoundingMode.HALF_UP);
        }
        // Voir si utilisation du TETA, intégrer ici
    }
}
