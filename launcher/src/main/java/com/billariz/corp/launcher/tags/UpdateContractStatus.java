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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.ObjectProcessRule;
import com.billariz.corp.database.model.Service;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.model.enumeration.ServiceStatus;
import com.billariz.corp.database.model.light.messageCode;
import com.billariz.corp.database.repository.ContractRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.ObjectProcessRuleRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.database.repository.ServiceRepository;
import com.billariz.corp.launcher.Launcher;
import com.billariz.corp.launcher.exception.LauncherException;
import com.billariz.corp.launcher.exception.LauncherFatalException;
import com.billariz.corp.launcher.queue.LauncherQueue;
import com.billariz.corp.launcher.utils.EventUtils;
import com.billariz.corp.launcher.utils.JournalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UpdateContractStatus implements Launcher
{
    private static final String ACTIVITY_CONTRACT="ACTIVITY_CONTRACT";

    private final ContractRepository           contractRepository;

    private final ServiceRepository            servicesRepository;

    private final ObjectProcessRuleRepository objectProcessRulesRepository;

    private final EventRepository              eventRepository;

    private final JournalRepository            journalRepository;

    private final LauncherQueue                launcherQueue;

    private final RelationRepository           relationRepository;

    private final EventUtils                   eventUtils;

    private final JournalUtils                 journalUtils;

    private List<ObjectProcessRule>           rules;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Iterable<Long> eventIds, EventExecutionMode executionMode)
    {
        var events = eventRepository.findAllById(eventIds);

        rules = objectProcessRulesRepository.findAll();
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
        if(event.getAction()!=null)
            messages.add(new messageCode("EVENT_USE_CASE", new Object[]{event.getAction()}));
        var relation = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_CONTRACT,
                event.getActivityId()).orElseThrow(() -> new LauncherFatalException("MISSING_CONTRACT", new Object[]{event.getId()}));
        log.info("relation[{}] found for Event[{}] ", relation, event);
        var contract = contractRepository.findById(relation.getSecondObjectId()).orElseThrow(() 
                            -> new LauncherFatalException("MISSING_CONTRACT_IN_DB",  new Object[]{relation.getSecondObjectId()}));
        contract.setCascaded(true);
        var contractNewStatus = event.getAction();
        if (contractNewStatus == null)
        {
            if (contract.getStatus().equals("INSTALLED"))
                contractNewStatus = checkStartingConditions(contract.getId());
            if (contract.getContractualEndDate() != null)
                contractNewStatus = checkStoppingConditions(contract);
        }
        contractNewStatus = contractNewStatus == null ? "*" : contractNewStatus;
        messages.add(new messageCode("CONTRACT_NEW_STATUS", new Object[]{contractNewStatus}));
        var rule = updateContractStatus(contract, contractNewStatus, event);

        if (rule != null && rule.getActivityType() != null)
            launcherQueue.createActivityEvent(rule.getActivityType(), contract.getId(), ObjectType.CONTRACT, "systemAgent");

        if (rule == null)
            messages.add(new messageCode("CONTRACT_NO_RULE", null));
    
        journal.setMessageCodes(messages);
    }

    private ObjectProcessRule updateContractStatus(Contract contract, String contractNewStatus, Event event) throws LauncherException
    {
        log.debug("Contract comSystem : {} | Contract New : {}", contract.getStatus(), contractNewStatus);
        var customer = contract.getContractPerimeter().getPerimeter().getCustomer();
        var ruleOpt = rules.stream().filter(r -> filterRule(r, contract, contractNewStatus, customer.getCategory())).findFirst();

        if (ruleOpt.isEmpty()) {
            if (!"*".equals(contractNewStatus)) {
                throw new LauncherFatalException("NO_RULE_FOUND", new Object[]{ObjectType.CONTRACT,contract.getStatus(), 
                                                    contractNewStatus});
            }
            return null;
        }
        
        log.debug("Contract Process Rule => {}", ruleOpt);
        ObjectProcessRule rule = ruleOpt.get();
        contract.setStatus(rule.getFinalStatus());
        
        journalUtils.addJournal(ObjectType.CONTRACT,contract.getId(),
                                        "STATUS_UPDATE_BY_RULE", 
                                        new Object[]{contract.getStatus(), event.getId(), rule.getId()},
                                        event,
                                        JournalAction.STATUS_CHANGE
                                        );
        
        // Mise à jour du périmètre si le statut est TERMINATED ou CANCELLED
        if ("TERMINATED".equals(contract.getStatus()) || "CANCELLED".equals(contract.getStatus())) {
            terminatePerimeter(contract);
        }
        return rule;
    }

    private void terminatePerimeter(Contract contract)
    {
        contract.getContractPerimeter().setEndDate(contract.getContractualEndDate());
        if(contract.getContractPerimeter().getPerimeter().getPerimeterType().equals("MONO_SITE"))
            contract.getContractPerimeter().getPerimeter().setEndDate(contract.getContractualEndDate());
            contract.getContractPerimeter().getPerimeter().setStatus(contract.getStatus().equals("CANCELLED") ? "CANCELLED" : "CLOSED");
    }

    private String checkStartingConditions(Long contractId)
    {
        return servicesRepository.existsByContractIdAndStatusIn(contractId, List.of(ServiceStatus.ACTIVE)) 
                ? "EFFECTIVE" 
                : null;
    }

    private String checkStoppingConditions(Contract contract)
    {
        if(contract.getContractualStartDate().isEqual(contract.getContractualEndDate()))
            return "CANCELLED";
        else return checkServices(contract);
    }

    private String checkServices(Contract contract) {
        // Vérifie directement l'existence de services non default avec date de fin egal a df contrat et isDefault false
        List<Service> terminatedServices = servicesRepository.findByContractIdAndStatusAndIsDefault(
                contract.getId(), 
                ServiceStatus.TERMINATED,
                contract.getContractualEndDate()
                );
        if (!terminatedServices.isEmpty())
            return "TERMINATED";
        else {
            return null;
        }
    }
}
