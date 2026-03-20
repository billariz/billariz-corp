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

import java.util.Objects;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.ObjectProcessRule;
import com.billariz.corp.database.model.Service;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.model.enumeration.ServiceStatus;
import com.billariz.corp.database.repository.ObjectProcessRuleRepository;
import com.billariz.corp.database.repository.ServiceRepository;
import com.billariz.corp.notifier.service.ServiceCaptor;
import com.billariz.corp.launcher.queue.LauncherQueue;
import com.billariz.corp.launcher.utils.JournalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceCaptorImpl implements ServiceCaptor
{
    @PersistenceContext
    private EntityManager                             entityManager;

    private ThreadLocal<Optional<ObjectProcessRule>> ruleActive = new ThreadLocal<>();

    private final ObjectProcessRuleRepository        objectProcessRulesRepository;

    private final ServiceRepository                      serviceRepository;

    private final LauncherQueue                       launcherQueue;

    private final JournalUtils                 journalUtils;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPrePersistOrUpdate(Service service)
    {
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        var oldStatus = getOldValue(service);
        var newStatus = Objects.toString(service.getStatus(), "");
        var rules = objectProcessRulesRepository.findAllByNewStatusAndInitialStatusAndObjectType(newStatus, oldStatus, ObjectType.SERVICE);
        if (!rules.isEmpty())
        {
            var rule = rules.stream().filter(rl -> checkConditions(rl, service)).findFirst();
            var finalStatus = rule.map(ObjectProcessRule::getFinalStatus);

            if (finalStatus.isPresent())
            {
                service.setStatus(ServiceStatus.valueOf(finalStatus.get()));
                journalUtils.addJournal(ObjectType.SERVICE, service.getId(),
                            "OBJECT_UPDATED_WITH_RULE", 
                            new Object[]{"SERVICE",rule.toString()},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );

            }
            ruleActive.set(rule);
        }
        else
            ruleActive.set(Optional.empty());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPostPersistOrUpdate(Service service) {
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        var rule = ruleActive.get();
        if (rule == null || !rule.isPresent()) {
            journalUtils.addJournal(ObjectType.SERVICE, service.getId(),
                            "OBJECT_UPDATED_WITH_NO_RULE", 
                            new Object[]{"SERVICE"},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
            return;
        }
        if (rule == null || rule.get().getActivityType() == null) {
            journalUtils.addJournal(ObjectType.SERVICE, service.getId(),
                            "OBJECT_UPDATED_WITH_NO_RULE", 
                            new Object[]{"SERVICE"},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
            return;
        }
    
        var activity = getAction(rule.get().getActivityType());
        if (activity.equals(rule.get().getActivityType())) {
            launcherQueue.createActivityEvent( rule.get().getActivityType(),"systemAgent",
                                        service.getId(), ObjectType.SERVICE,
                                        service.getContractId(), ObjectType.CONTRACT);
            journalUtils.addJournal(ObjectType.SERVICE, service.getId(),
                            "OBJECT_UPDATED_WITH_RULE", 
                            new Object[]{"SERVICE", rule.toString()},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
        } else {
            launchEvent(service, activity);
            journalUtils.addJournal(ObjectType.SERVICE, service.getId(),
                            "LAUNCH_ACTIVITY_ON_OBJECT", 
                            new Object[]{activity, "SERVICE", service.getId()},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
        }
        ruleActive.remove();
    }

    private String getAction(String actType)
    {
        int indexDeuxPoints = actType.indexOf(":");
        if (indexDeuxPoints != -1)
            return actType.substring(indexDeuxPoints + 1);
        else
            return actType;
    }

    private void launchEvent(Service service, String action)
    {
        switch (action)
        {
        default:
            break;
        }
    }

    private boolean checkConditions(ObjectProcessRule streamRule, Service eventDatabase)
    {
        return true;
    }

    private String getOldValue(Service service) {
        return service != null && service.getId() != null
                ? serviceRepository.findById(service.getId())
                                   .map(Service::getStatus)
                                   .map(Object::toString)
                                   .orElse(null)
                : null;
    }
}
