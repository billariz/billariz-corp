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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.BillSegment;
import com.billariz.corp.database.model.ObjectProcessRule;
import com.billariz.corp.database.model.TermOfService;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.model.enumeration.TosStatus;
import com.billariz.corp.database.repository.ObjectProcessRuleRepository;
import com.billariz.corp.database.repository.TermOfServiceRepository;
import com.billariz.corp.notifier.termOfservices.TosCaptor;
import com.billariz.corp.launcher.queue.LauncherQueue;
import com.billariz.corp.launcher.tags.TermOfServiceTermination;
import com.billariz.corp.launcher.utils.JournalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TosCaptorImpl implements TosCaptor
{
    @PersistenceContext
    private EntityManager                             entityManager;

    private final TermOfServiceRepository            termOfServiceRepository;

    private final JournalUtils                 journalUtils;

    private final TermOfServiceTermination termOfServiceTermination;

    private final ObjectProcessRuleRepository        objectProcessRulesRepository;

     private final LauncherQueue                       launcherQueue;

    private ThreadLocal<Boolean> isDateUpdated = new ThreadLocal<>();

    private ThreadLocal<Optional<ObjectProcessRule>> ruleActive = new ThreadLocal<>();

    private final Locale locale = LocaleContextHolder.getLocale();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPrePersist(TermOfService tos) 
    {
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        var oldStatus = getOldValue(tos);
        var newStatus = Objects.toString(tos.getStatus(), "");
        var rules = objectProcessRulesRepository.findAllByNewStatusAndInitialStatusAndObjectType(newStatus, oldStatus, ObjectType.TERM_OF_SERVICE);
        if (!rules.isEmpty())
        {
            var rule = rules.stream().filter(rl -> checkConditions(rl, tos)).findFirst();
            var finalStatus = rule.map(ObjectProcessRule::getFinalStatus);

            if (finalStatus.isPresent())
            {
                tos.setStatus(TosStatus.valueOf(finalStatus.get()));
                journalUtils.addJournal(ObjectType.TERM_OF_SERVICE, tos.getId(),
                            "OBJECT_UPDATED_WITH_RULE", 
                            new Object[]{"TERM_OF_SERVICE", rule.toString()},
                            null,
                            JournalAction.LOG, authenticated.getName()
                            );
            }
            ruleActive.set(rule);
        }
        else
            ruleActive.set(Optional.empty());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPreUpdate(TermOfService tos) 
    {
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        //controle BS oberlaping
        var overlappingBss = getOverlapingBillSegments(tos);
        if (!overlappingBss.isEmpty()) {
            throw new IllegalArgumentException(
                journalUtils.getMessage(
                    "BS_OVERLAPINGFOR_TOS",new Object[]{
                    tos.getId(),
                    tos.getContractId(),
                    tos.getEndDate(),
                    overlappingBss},
                    locale
                )
            );
        }
        // Controle  rule
        var oldStatus = getOldValue(tos);
        var newStatus = Objects.toString(tos.getStatus(), "");
        var rules = objectProcessRulesRepository.findAllByNewStatusAndInitialStatusAndObjectType(newStatus, oldStatus, ObjectType.TERM_OF_SERVICE);
        if (!rules.isEmpty())
        {
            var rule = rules.stream().filter(rl -> checkConditions(rl, tos)).findFirst();
            var finalStatus = rule.map(ObjectProcessRule::getFinalStatus);

            if (finalStatus.isPresent())
            {
                tos.setStatus(TosStatus.valueOf(finalStatus.get()));
                journalUtils.addJournal(ObjectType.TERM_OF_SERVICE, tos.getId(),
                            "OBJECT_UPDATED_WITH_RULE", 
                            new Object[]{"TERM_OF_SERVICE", rule.toString()},
                            null,
                            JournalAction.LOG, authenticated.getName()
                            );
            }
            ruleActive.set(rule);
        }
        else
            ruleActive.set(Optional.empty());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPostPersistOrUpdate(TermOfService tos) {
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        var rule = ruleActive.get();
        if (rule == null || !rule.isPresent()) {
            journalUtils.addJournal(ObjectType.TERM_OF_SERVICE, tos.getId(),
                            "OBJECT_UPDATED_WITH_NO_RULE", 
                            new Object[]{"TERM_OF_SERVICE"},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
            return;
        }
        if (rule == null || rule.get().getActivityType() == null) {
            journalUtils.addJournal(ObjectType.TERM_OF_SERVICE, tos.getId(),
                            "OBJECT_UPDATED_WITH_NO_RULE", 
                            new Object[]{"TERM_OF_SERVICE"},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
            return;
        }
    
        var activity = getAction(rule.get().getActivityType());
        if (activity.equals(rule.get().getActivityType())) {
            launcherQueue.createActivityEvent(rule.get().getActivityType(), "systemAgent",
                                            tos.getId(),ObjectType.TERM_OF_SERVICE,
                                            tos.getContractId(), ObjectType.CONTRACT);

            journalUtils.addJournal(ObjectType.TERM_OF_SERVICE, tos.getId(),
                    "OBJECT_UPDATED_WITH_RULE", 
                    new Object[]{"TERM_OF_SERVICE", rule.toString()},
                    null,
                    JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                    );
        } else {
            launchEvent(tos, activity);
            journalUtils.addJournal(ObjectType.TERM_OF_SERVICE, tos.getId(),
                            "LAUNCH_ACTIVITY_ON_OBJECT", 
                            new Object[]{activity, "TERM_OF_SERVICE", tos.getId()},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
        }
        ruleActive.remove();
    }

    private List<BillSegment> getOverlapingBillSegments(TermOfService tos)
    {
        // Contrôle uniquement la date de fin
        var oldEndDate = getOldEndDate(tos);
        var newEndDate = tos.getEndDate();
        List<BillSegment> overlappingSegments = new ArrayList<>();
        // Vérifie si les dates sont différentes
        if ((oldEndDate == null && newEndDate != null) || 
            (oldEndDate != null && !oldEndDate.equals(newEndDate))) {
            isDateUpdated.set(true);

            // Si la nouvelle date de fin est non nulle, vérifie les segments qui se chevauchent
            if (newEndDate != null) {
                overlappingSegments = termOfServiceTermination.getOverlapingValidBs(tos, newEndDate);
            }
        }
        return overlappingSegments;
    }

    private String getAction(String actType)
    {
        int indexDeuxPoints = actType.indexOf(":");
        if (indexDeuxPoints != -1)
            return actType.substring(indexDeuxPoints + 1);
        else
            return actType;
    }

    private void launchEvent(TermOfService tos, String action)
    {
        switch (action)
        {
        default:
            break;
        }
    }

    private boolean checkConditions(ObjectProcessRule streamRule, TermOfService tos)
    {
        return true;
    }

    private LocalDate getOldEndDate(TermOfService tos) {
        return Optional.ofNullable(tos.getId())
                       .flatMap(termOfServiceRepository::findById)
                       .map(TermOfService::getEndDate)
                       .orElse(null);
    }

    private String getOldValue(TermOfService tos) {
        return tos != null && tos.getId() != null
                ? termOfServiceRepository.findById(tos.getId())
                                   .map(TermOfService::getStatus)
                                   .map(Object::toString)
                                   .orElse(null)
                : null;
    }
}
