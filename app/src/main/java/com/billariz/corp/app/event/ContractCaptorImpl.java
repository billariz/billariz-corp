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

package com.billariz.corp.app.event;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.ContractPerimeter;
import com.billariz.corp.database.model.Customer;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.ObjectProcessRule;
import com.billariz.corp.database.model.Perimeter;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.repository.ContractPerimeterRepository;
import com.billariz.corp.database.repository.ContractRepository;
import com.billariz.corp.database.repository.ObjectProcessRuleRepository;
import com.billariz.corp.database.validator.BaseValidator;
import com.billariz.corp.notifier.contract.ContractCaptor;
import com.billariz.corp.launcher.queue.LauncherQueue;
import com.billariz.corp.launcher.utils.JournalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContractCaptorImpl implements ContractCaptor
{
    @PersistenceContext
    private EntityManager                             entityManager;

    private ThreadLocal<Optional<ObjectProcessRule>> ruleActive = new ThreadLocal<>();

    private final ContractPerimeterRepository         contractPerimeterRepository;

    private final ObjectProcessRuleRepository        objectProcessRulesRepository;

    private final ContractRepository                  contractRepository;

    private final LauncherQueue                       launcherQueue;

    private final JournalUtils                 journalUtils;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPrePersistOrUpdate(Contract contract)
    {
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        var oldStatus = getOldValue(contract);
        var newStatus = Objects.toString(contract.getStatus(), "");
        var contractPerimeter = contractPerimeterRepository.findByContractId(contract.getId());
        var customerCategory = contractPerimeter.map(ContractPerimeter::getPerimeter).map(Perimeter::getCustomer).map(Customer::getCategory).orElse("");
        var rules = objectProcessRulesRepository.findAllByNewStatusAndInitialStatusAndObjectType(newStatus, oldStatus, ObjectType.CONTRACT);

        if (!rules.isEmpty())
        {
            var rule = rules.stream().filter(contractProcessRules -> checkConditions(contractProcessRules, contract, customerCategory)).findFirst();
            var status = rule.map(ObjectProcessRule::getFinalStatus);

            if (status.isPresent())
            {
                contract.setStatus(status.get());
                journalUtils.addJournal(ObjectType.CONTRACT, contract.getId(),
                            "OBJECT_UPDATED_WITH_RULE", 
                            new Object[]{"CONTRACT",rule.get().getId().toString()},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
            }
            ruleActive.set(rule);
        }
        else
        {
            ruleActive.set(Optional.empty());
            journalUtils.addJournal(ObjectType.CONTRACT, contract.getId(),
                            "OBJECT_UPDATED_WITH_NO_RULE", 
                            new Object[]{"CONTRACT"},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPostPersistOrUpdate(Contract contract)
    {
        var rule = ruleActive.get();

        log.info("[CONTRACT]handleAfterWriteInDataBase: id={} updatedStatus={} rule={}", contract.getId(), contract.getStatus(), rule);
        if (rule.isPresent() && rule.get().getActivityType() != null)
            launcherQueue.createActivityEvent(rule.get().getActivityType(), contract.getId(), ObjectType.CONTRACT, "systemAgent");

        ruleActive.remove();
    }

    private boolean checkConditions(ObjectProcessRule streamRule, Contract contractDatabase, String customerCategory)
    {
        return (rulesNormalized(streamRule.getMarket(), contractDatabase.getMarket()) && rulesNormalized(streamRule.getCustomerCategory(), customerCategory)
                && rulesNormalized(streamRule.getChannel(), contractDatabase.getChannel())
                && rulesNormalized(streamRule.getSeller(), contractDatabase.getSeller())
                && rulesNormalized(streamRule.getDirection(), contractDatabase.getDirection())
                && rulesNormalized(streamRule.getServiceCategory(), contractDatabase.getServiceCategory())
                && rulesNormalized(streamRule.getServiceSubCategory(), contractDatabase.getServiceSubCategory()));
    }

    private boolean rulesNormalized(String search, String item)
    {
        if ("*".equals(search))
            return true;
        return search.equals(item == null ? "" : item);
    }

    private String getOldValue(Contract contract)
    {
        String oldValue = null;
        var id = contract.getId();

        if (id != null)
        {
            oldValue = contractRepository.findById(id).map(Contract::getStatus).orElse(null);
        }
        log.info("[CONTRACT]getOldValue: id={} oldStatus={} newStatus={}", contract.getId(), oldValue, contract.getStatus());
        return oldValue;
    }
}
