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

import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.BillableCharge;
import com.billariz.corp.database.model.ObjectProcessRule;
import com.billariz.corp.database.model.enumeration.BillableChargeStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.repository.BillableChargeRepository;
import com.billariz.corp.database.repository.ObjectProcessRuleRepository;
import com.billariz.corp.notifier.billableCharge.BillableChargeCaptor;
import com.billariz.corp.launcher.queue.LauncherQueue;
import com.billariz.corp.launcher.utils.JournalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillableChargeCaptorImpl implements BillableChargeCaptor
{
    @PersistenceContext
    private EntityManager                             entityManager;

    private ThreadLocal<Optional<ObjectProcessRule>> ruleActive = new ThreadLocal<>();

    private final ObjectProcessRuleRepository        objectProcessRulesRepository;

    private final BillableChargeRepository            billableChargeRepository;

    private final JournalUtils                 journalUtils;

    private final LauncherQueue                       launcherQueue;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPrePersistOrUpdate(BillableCharge billableCharge)
    {
        var oldStatus = getOldValue(billableCharge);
        var newStatus = billableCharge.getStatus();
        var rules = objectProcessRulesRepository.findAllByNewStatusAndInitialStatusAndObjectType(newStatus.toString(),
                oldStatus != null ? oldStatus.toString() : null, ObjectType.BILLABLE_CHARGE);

        if (!rules.isEmpty())
        {
            var rule = rules.stream().filter(meterReadProcessRules -> checkConditions(meterReadProcessRules, billableCharge)).findFirst();
            var finalStatus = rule.map(ObjectProcessRule::getFinalStatus);

            if (finalStatus.isPresent())
            {
                billableCharge.setStatus(BillableChargeStatus.valueOf(finalStatus.get()));
            }
            ruleActive.set(rule);
        }
        else
            ruleActive.set(Optional.empty());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPostPersistOrUpdate(BillableCharge billableCharge)
    {
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        var rule = ruleActive.get();
        log.info("[BILLABLE_CHARGE] handleAfterWriteInDataBase: id={} updatedStatus={} rule={}", billableCharge.getId(), billableCharge.getStatus(), rule);
        if (rule.isPresent() && rule.get().getActivityType() != null)
        {
            launcherQueue.createActivityEvent(rule.get().getActivityType(), 
                                                billableCharge.getId(), ObjectType.BILLABLE_CHARGE, "systemAgent");
            journalUtils.addJournal(ObjectType.BILLABLE_CHARGE, billableCharge.getId(),
                                    "BC_IMPORTED_WITH_RULE", 
                                    new Object[]{rule.toString()},
                                    null,
                                    JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                                    );
        }
        else
            journalUtils.addJournal(ObjectType.BILLABLE_CHARGE, billableCharge.getId(),
                                    "BC_IMPORTED_WITH_NO_RULE", 
                                    null,
                                    null,
                                    JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                                    );

        ruleActive.remove();
    }

    private boolean checkConditions(ObjectProcessRule streamRule, BillableCharge billableCharge)
    {
        return (rulesNormalized(streamRule.getMarket(), billableCharge.getMarket()) && rulesNormalized(streamRule.getDirection(), billableCharge.getDirection()));
    }

    private boolean rulesNormalized(String search, String item)
    {
        if ("*".equals(search))
            return true;
        return search.equals(item == null ? "" : item);
    }

    private BillableChargeStatus getOldValue(BillableCharge billableCharge)
    {
        BillableChargeStatus oldValue = null;
        var id = billableCharge.getId();

        if (id != null)
        {
            oldValue = billableChargeRepository.findById(id).map(BillableCharge::getStatus).orElse(null);
        }
        log.info("[BILLABLE_CHARGE] getOldValue: id={} oldStatus={} newStatus={}", billableCharge.getId(), oldValue, billableCharge.getStatus());
        return oldValue;
    }
}
