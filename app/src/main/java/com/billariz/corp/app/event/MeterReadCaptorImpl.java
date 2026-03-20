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
import com.billariz.corp.database.model.MeterRead;
import com.billariz.corp.database.model.ObjectProcessRule;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.MeterReadStatus;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.repository.MeterReadRepository;
import com.billariz.corp.database.repository.ObjectProcessRuleRepository;
import com.billariz.corp.notifier.meterRead.MeterReadCaptor;
import com.billariz.corp.launcher.queue.LauncherQueue;
import com.billariz.corp.launcher.utils.JournalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MeterReadCaptorImpl implements MeterReadCaptor
{
    @PersistenceContext
    private EntityManager                             entityManager;

    private ThreadLocal<Optional<ObjectProcessRule>> ruleActive = new ThreadLocal<>();

    private final ObjectProcessRuleRepository        objectProcessRulesRepository;

    private final MeterReadRepository                 meterReadRepository;

    private final LauncherQueue                       launcherQueue;

    private final JournalUtils                 journalUtils;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPrePersistOrUpdate(MeterRead meterRead)
    {
        var oldStatus = getOldValue(meterRead);
        var newStatus = meterRead.getStatus();
        var rules = objectProcessRulesRepository.findAllByNewStatusAndInitialStatusAndObjectType(newStatus.toString(),
                oldStatus != null ? oldStatus.toString() : null, ObjectType.METER_READ);

        if (!rules.isEmpty())
        {
            var rule = rules.stream().filter(meterReadProcessRules -> checkConditions(meterReadProcessRules, meterRead)).findFirst();
            var finalStatus = rule.map(ObjectProcessRule::getFinalStatus);

            if (finalStatus.isPresent())
            {
                meterRead.setStatus(MeterReadStatus.valueOf(finalStatus.get()));
            }
            ruleActive.set(rule);
        }
        else
            ruleActive.set(Optional.empty());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPostPersistOrUpdate(MeterRead meterRead)
    {
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        var rule = ruleActive.get();
        log.info("[METER_READ] handleAfterWriteInDataBase: id={} updatedStatus={} rule={}", meterRead.getId(), meterRead.getStatus(), rule);
        if (rule.isPresent() && rule.get().getActivityType() != null)
        {
            launcherQueue.createActivityEvent(rule.get().getActivityType(), 
                                                meterRead.getId(), ObjectType.METER_READ, "systemAgent");
            journalUtils.addJournal(ObjectType.METER_READ, meterRead.getId(),
                            "OBJECT_UPDATED_WITH_RULE", 
                            new Object[]{"METER_READ",rule.toString()},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
        }
        else
            journalUtils.addJournal(ObjectType.METER_READ, meterRead.getId(),
                            "OBJECT_UPDATED_WITH_NO_RULE", 
                            new Object[]{"METER_READ"},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );


        ruleActive.remove();
    }

    private boolean checkConditions(ObjectProcessRule streamRule, MeterRead meterRead)
    {
        return (rulesNormalized(streamRule.getMarket(), meterRead.getMarket()) && rulesNormalized(streamRule.getDirection(), meterRead.getDirection()));
    }

    private boolean rulesNormalized(String search, String item)
    {
        if ("*".equals(search))
            return true;
        return search.equals(item == null ? "" : item);
    }

    private MeterReadStatus getOldValue(MeterRead meterRead) {
        var id = meterRead.getId();
        return (id != null) 
            ? meterReadRepository.findById(id).map(MeterRead::getStatus).orElse(null) 
            : null;
    }
    
}
