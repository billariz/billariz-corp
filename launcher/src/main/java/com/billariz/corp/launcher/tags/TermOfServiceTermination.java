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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.BillSegment;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.Parameter;
import com.billariz.corp.database.model.Service;
import com.billariz.corp.database.model.TermOfService;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.BillSegmentStatus;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.model.enumeration.SeStatus;
import com.billariz.corp.database.model.enumeration.ServiceStatus;
import com.billariz.corp.database.model.enumeration.TosStatus;
import com.billariz.corp.database.model.enumeration.TosTerminationUseCase;
import com.billariz.corp.database.model.light.messageCode;
import com.billariz.corp.database.repository.BillSegmentRepository;
import com.billariz.corp.database.repository.ContractPointOfServiceRepository;
import com.billariz.corp.database.repository.ContractRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.database.repository.ServiceElementRepository;
import com.billariz.corp.database.repository.ServiceRepository;
import com.billariz.corp.database.repository.TermOfServiceRepository;
import com.billariz.corp.database.validator.BaseValidator;
import com.billariz.corp.launcher.Launcher;
import com.billariz.corp.launcher.exception.LauncherException;
import com.billariz.corp.launcher.exception.LauncherFatalException;
import com.billariz.corp.launcher.utils.EventUtils;
import com.billariz.corp.launcher.utils.JournalUtils;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TermOfServiceTermination implements Launcher 
{
    private static final String ACTIVITY_CONTRACT="ACTIVITY_CONTRACT";

    private static final String ACTIVITY_POS="ACTIVITY_POS";

    private static final String ACTIVITY_SERVICE="ACTIVITY_SERVICE";

    private static final String ACTIVITY_TOS = "ACTIVITY_TOS";

    private final EventRepository                  eventRepository;

    private final RelationRepository               relationRepository;

    private final JournalRepository                journalRepository;

    private final EventUtils                       eventUtils;

    private final ContractPointOfServiceRepository contractPointOfServiceRepository;

    private final TermOfServiceRepository  termOfServicesRepository;

    private final ParameterRepository              parameterRepository;

    private final ServiceElementRepository         serviceElementRepository;

    private final BillSegmentRepository     billSegmentRepository;

    private final ServiceRepository serviceRepository;

    private final ContractRepository contractRepository;

    private final JournalUtils                 journalUtils;


    @Override
   @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Iterable<Long> eventIds, EventExecutionMode executionMode)
    {
        var events = eventRepository.findAllById(eventIds);

        LocalDate now = LocalDate.now();
        List<SeStatus> seListStatus = parameterRepository
                                .findAllByTypeAndNameAndStartDateBefore("BILLABLE_STATUS", 
                                            BaseValidator.SERVICE_ELEMENT_STATUS, now)
                                .stream()
                                .map(Parameter::getValue)
                                .map(SeStatus::valueOf)
                                .toList();

        events.forEach(e -> process(e, executionMode, seListStatus));
    }

    public void process(Event event, EventExecutionMode executionMode,  List<SeStatus> seListStatus)
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
            var wait = process(event, seListStatus, journal, messages);
            event.setStatus(wait ? EventStatus.COMPLETED : EventStatus.PENDING);
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

     private boolean process(Event event, List<SeStatus> seStatusBillable, Journal journal, List<messageCode> messages) throws LauncherException
    {
        var useCase = TosTerminationUseCase.valueOf(event.getAction());
        if(useCase!=null)
            messages.add(new messageCode("EVENT_USE_CASE", new Object[]{useCase}));

        var relationContract = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_CONTRACT,
                        event.getActivityId()).orElseThrow(
                            () -> new LauncherFatalException("MISSING_CONTRACT", new Object[]{event.getId()}));
        log.info("relation[{}] found for Event[{}] ", relationContract, event);
        // Bottom-up : action depuis le POS
        if(useCase.equals(TosTerminationUseCase.BOTTOM_UP))
        {
            var relationPos = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_POS,
            event.getActivityId()).orElseThrow(
                () -> new LauncherFatalException("MISSING_POS",new Object[]{event.getId()}));

            var ctrPos = contractPointOfServiceRepository.findByPosIdAndContractId(relationPos.getSecondObjectId(), 
                                        relationContract.getSecondObjectId());
            var direction = ctrPos.getPointOfService().getDirection();
            messages.add(new messageCode("POS_DIRECTION", new Object[]{direction}));

            //build a list of all Tos with same direction of pos
            var tosList = termOfServicesRepository.findAllByContractIdAndStatusInAndDirectionAndEndDateIsNullOrEndDateIsAfter(
                            ctrPos.getContract().getId(),
                            List.of(TosStatus.ACTIVE,TosStatus.PENDING_START,
                            TosStatus.REACTIVATED,TosStatus.IN_FAILURE),
                            direction,
                            ctrPos.getEndDate());
            if(tosList.isEmpty()){
                messages.add(new messageCode("NO_TOS_FOR_DIRECTION", new Object[]{direction}));
                return true;
            }
            //Terminate each Tos
            messages.add(new messageCode("TOS_LIST", new Object[]{tosList.size()}));
            for(TermOfService tos : tosList){
                 terminateTos(tos, seStatusBillable, ctrPos.getEndDate(), event);
            }
            //Terminate Service
            var serviceList = tosList.stream().map(TermOfService::getService).collect(Collectors.toList());
            serviceList.stream().forEach(s -> terminateService(s, ctrPos.getEndDate(), event));

            //Terminate Contract
            terminateContract(ctrPos.getContract(), ctrPos.getEndDate(), event);
            messages.add(new messageCode("TOS_TERMINATED", new Object[]{direction, ctrPos.getEndDate()}));
        }
        //top-down : action sur le contrat ou service
        else if(useCase.equals(TosTerminationUseCase.TOP_DOWN)){
            var relationService = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_SERVICE,
                                                        event.getActivityId());
            List<Service> serviceList = new ArrayList<>();
            LocalDate endDate = null;
            var contract = contractRepository.findById(relationContract.getSecondObjectId());
            if(relationService.isPresent() && !relationService.isEmpty()){
                messages.add(new messageCode("SERVICE_FOUND", new Object[]{relationService.get().getSecondObjectId()}));
                var service = serviceRepository.findById(relationService.get().getSecondObjectId());
                serviceList.add(service.get());
                endDate = service.get().getEndDate();
            }
            else{
                endDate = contractRepository.findById(relationContract.getSecondObjectId()).get().getContractualEndDate();

                serviceList = serviceRepository.findAllByContractIdAndStatusIn(relationContract.getSecondObjectId(),
                                            List.of(ServiceStatus.ACTIVE,ServiceStatus.INITIALIZED, ServiceStatus.INSTALLED,
                                            ServiceStatus.IN_FAILURE));
                messages.add(new messageCode("SERVICES_FOUND", new Object[]{serviceList.size()}));
            }
            // Préparer les IDs des services
            var serviceIds = serviceList.stream().map(Service::getId).toList();
            // Récupérer tous les TOS liés aux services en une seule requête
            var tosMap = termOfServicesRepository.findAllByServiceIdInAndStatusIn(
                                                    serviceIds,
                                                    List.of(TosStatus.ACTIVE, TosStatus.PENDING_START,
                                                    TosStatus.REACTIVATED, TosStatus.IN_FAILURE))
                                                    .stream()
                                                    .collect(Collectors.groupingBy(TermOfService::getServiceId));
            for(Service serv : serviceList){
                var tosList = tosMap.getOrDefault(serv.getId(), List.of());

                //Terminate ALL TOS of the service
                for(TermOfService tos : tosList){
                    terminateTos(tos,seStatusBillable,endDate, event);
                }
                messages.add(new messageCode("SERVICE_TERMINATED", new Object[]{serv.getId()}));
                //terminate Service
                terminateService(serv, endDate, event);
            }
            messages.add(new messageCode("ALL_SERVICE_TERMINATED", new Object[]{endDate}));
            terminateContract(contract.get(), endDate, event);
        }
        //ONE_SELF : action sur le TOS
        else if(useCase.equals(TosTerminationUseCase.ONE_SELF)){
            var relationTos = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_TOS,
                                event.getActivityId()).orElseThrow(
                                () -> new LauncherFatalException("MISSING_TOS", new Object[]{event.getId()}));
            
            var tos = termOfServicesRepository.findById(relationTos.getSecondObjectId());
            //terminate Tos
            terminateTos(tos.get(),seStatusBillable,tos.get().getEndDate(),event);
            messages.add(new messageCode("TOS_ONESELF_TEMINATED", new Object[]{tos.get().getId(), event.getId()}));

            terminateService(tos.get().getService(), tos.get().getEndDate(), event);
            terminateContract(tos.get().getContract(), tos.get().getEndDate(), event);
        }

        journal.setMessageCodes(messages);
        return true;
    }

    public void terminateTos(TermOfService tos, List<SeStatus> seStatusBillable, LocalDate endDate, Event event) throws LauncherException
    {
        //check if any overlap exist with a valid billSegment
        var overLapBss = getOverlapingValidBs(tos, endDate);
        if (!overLapBss.isEmpty())
            throw new LauncherFatalException("TOS_BS_OVERLAPING", new Object[]{tos.getId(), tos.getContractId(),endDate, overLapBss.toString()});

        //terminate all active SE of the Tos
        serviceElementRepository.closeServiceElement(seStatusBillable, 
                                                    tos.getStartDate().isEqual(endDate) ? SeStatus.CANCELLED : SeStatus.PENDING_STOP, 
                                                    tos.getId(), 
                                                    endDate);
        //terminate the Tos
        tos.setEndDate(endDate);
        tos.setStatus((tos.getStartDate().isEqual(endDate)) ? TosStatus.CANCELLED : TosStatus.PENDING_STOP);

        journalUtils.addJournal(ObjectType.TERM_OF_SERVICE, tos.getId(),
                                    "TOS_CLOSED", 
                                    new Object[]{event.getId()},
                                    event,
                                    JournalAction.LOG
                                    );
    }

    public List<BillSegment> getOverlapingValidBs(TermOfService tos, LocalDate endDate) {
        
        var bss = billSegmentRepository.findByStatusInAndTosIdAndEndDateIsAfter(
            List.of(BillSegmentStatus.BILLED, BillSegmentStatus.CALCULATED, 
                        BillSegmentStatus.COMPUTED, BillSegmentStatus.ERROR),
            tos.getId(),
            endDate
            );
        return bss;
    }

    public void terminateContract(Contract contract, LocalDate endDate, Event event){

        var exist = serviceRepository.existsByContractIdAndStatusNotIn(contract.getId(), 
                                                List.of(ServiceStatus.PENDING_STOP,
                                                ServiceStatus.TERMINATED,
                                                ServiceStatus.CANCELLED)
                                                );
        if(!exist)
        {
            contract.setContractualEndDate(endDate);
            journalUtils.addJournal(ObjectType.CONTRACT, contract.getId(),
                                        "CONTRACT_UPDATED", 
                                        new Object[]{"endDate", event.getId()},
                                        event,
                                        JournalAction.LOG
                                        );
        }
    }

    public void terminateService(Service service, LocalDate endDate, Event event){

        var exist = termOfServicesRepository.existsByServiceIdAndStatusNotIn(service.getId(), 
                                                List.of(TosStatus.PENDING_STOP,
                                                TosStatus.CLOSED,
                                                TosStatus.CANCELLED)
                                                );
        if(!exist)
        {
            service.setEndDate(endDate);
            service.setStatus((service.getStartDate().isEqual(endDate)) ? ServiceStatus.CANCELLED: ServiceStatus.PENDING_STOP);
            journalUtils.addJournal(ObjectType.SERVICE, service.getId(),
                                        "SERVICE_CLOSED", 
                                        new Object[]{event.getId()},
                                        event,
                                        JournalAction.LOG
                                        );
        }
    }
}
