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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.ContractPointOfService;
import com.billariz.corp.database.model.ObjectProcessRule;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.repository.ContractPointOfServiceRepository;
import com.billariz.corp.database.repository.ObjectProcessRuleRepository;
import com.billariz.corp.notifier.contractPointOfService.ContractPointOfServiceCaptor;
import com.billariz.corp.launcher.queue.LauncherQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContractPointOfServiceCaptorImpl implements ContractPointOfServiceCaptor
{
    @PersistenceContext
    private EntityManager                             entityManager;

    private ThreadLocal<Optional<ObjectProcessRule>> ruleActive = new ThreadLocal<>();

    private final ObjectProcessRuleRepository        objectProcessRulesRepository;

    private final ContractPointOfServiceRepository    contractPointOfServiceRepository;

    private final LauncherQueue                       launcherQueue;

    private static final String ACTIVITY_CONTRACT="ACTIVITY_CONTRACT";

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPrePersistOrUpdate(ContractPointOfService contractPointOfService)
    {
        var oldEndDate = getOldValue(contractPointOfService);
        var newEndDate = contractPointOfService.getEndDate();
        List<ObjectProcessRule> rules;
        if (oldEndDate == null && newEndDate != null) {
            rules = objectProcessRulesRepository.findAllByNewStatusAndInitialStatusAndObjectType(
                    "TERMINATED", 
                    "ACTIVE", 
                    ObjectType.CONTRACT_POS
            );
        } else {
            rules = Collections.emptyList();
        }
        ruleActive.set(rules.isEmpty() ? Optional.empty() : Optional.of(rules.get(0)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPostPersistOrUpdate(ContractPointOfService contractPointOfService)
    {
        var rule = ruleActive.get();

        if (rule.isPresent() && rule.get().getActivityType() != null)
            launcherQueue.createActivityEvent(rule.get().getActivityType(), "systemAgent",
                            contractPointOfService.getPosId(), ObjectType.POINT_OF_SERVICE,
                            contractPointOfService.getContractId(), ObjectType.CONTRACT);
        ruleActive.remove();
    }

    private LocalDate getOldValue(ContractPointOfService contractPointOfService) {
        return Optional.ofNullable(contractPointOfService.getId())
                       .flatMap(contractPointOfServiceRepository::findById)
                       .map(ContractPointOfService::getEndDate)
                       .orElse(null);
    }
}
