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
import java.util.Objects;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.BillingRun;
import com.billariz.corp.database.model.ObjectProcessRule;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.repository.BillingRunRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.ObjectProcessRuleRepository;
import com.billariz.corp.notifier.billingRun.BillingRunCaptor;
import com.billariz.corp.launcher.queue.LauncherQueue;
import com.billariz.corp.launcher.utils.JournalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingRunCaptorImpl implements BillingRunCaptor
{
    @PersistenceContext
    private EntityManager                             entityManager;

    private ThreadLocal<Optional<ObjectProcessRule>> ruleActive = new ThreadLocal<>();

    private final ObjectProcessRuleRepository        objectProcessRulesRepository;

    private final BillingRunRepository                billingRunRepository;

    private final EventRepository                     eventRepository;

    private final LauncherQueue                       launcherQueue;

    private final JournalUtils                 journalUtils;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPrePersistOrUpdate(BillingRun billingRun)
    {
        var oldStatus = getOldValue(billingRun);
        var newStatus = billingRun.getStatus();
        var rules = objectProcessRulesRepository.findAllByNewStatusAndInitialStatusAndObjectType(newStatus, oldStatus, ObjectType.BILLING_RUN);
        if (!rules.isEmpty())
        {
            var rule = rules.stream().filter(billProcessRules -> checkConditions(billProcessRules, billingRun)).findFirst();
            var finalStatus = rule.map(ObjectProcessRule::getFinalStatus);

            if (finalStatus.isPresent())
            {
                billingRun.setStatus(finalStatus.get());
            }
            ruleActive.set(rule);
        }
        else
            ruleActive.set(Optional.empty());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPostPersistOrUpdate(BillingRun billingRun)
    {
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        var rule = ruleActive.get();
        if (rule.isPresent() && rule.get().getActivityType() != null)
        {
            var activity = getAction(rule.get().getActivityType());
            if (activity.equals(rule.get().getActivityType()))
            {
                launcherQueue.createActivityEvent(rule.get().getActivityType(), billingRun.getId(), 
                                         ObjectType.BILLING_RUN, "systemAgent");
                journalUtils.addJournal(ObjectType.BILLING_RUN, billingRun.getId(),
                            "OBJECT_UPDATED_WITH_RULE", 
                            new Object[]{"BILLING_RUN", rule.toString()},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
            }
            else
            {
                launchEvent(billingRun, activity);
                journalUtils.addJournal(ObjectType.BILLING_RUN, billingRun.getId(),
                            "LAUNCH_ACTIVITY_ON_OBJECT", 
                            new Object[]{activity, "BILLING_RUN", billingRun.getId()},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
            }
        }
        else
            journalUtils.addJournal(ObjectType.BILLING_RUN, billingRun.getId(),
                            "OBJECT_UPDATED_WITH_NO_RULE", 
                            new Object[]{"BILLING_RUN"},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );

        ruleActive.remove();
    }

    private String getAction(String actType)
    {
        int indexDeuxPoints = actType.indexOf(":");
        if (indexDeuxPoints != -1)
        {
            return actType.substring(indexDeuxPoints + 1);
        }
        else
        {
            return actType;
        }
    }

    private void launchEvent(BillingRun billingRun, String action)
    {
        eventRepository.updateAllByBillingRunAndStatusAndSubCategory(billingRun.getId(), action, EventExecutionMode.EVENT_MANAGER, LocalDate.now());
    }

    private boolean checkConditions(ObjectProcessRule streamRule, BillingRun eventDatabase)
    {
        return true;
    }

    private String getOldValue(BillingRun billingRun)
    {
        String oldValue = null;
        var id = billingRun.getId();

        if (id != null)
        {
            oldValue = billingRunRepository.findById(id).map(BillingRun::getStatus).orElse(null);
        }
        log.info("[BILL]getOldValue: id={} oldStatus={} newStatus={}", billingRun.getId(), oldValue, billingRun.getStatus());
        return Objects.toString(oldValue, "");
    }
}
