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

import static java.util.stream.Collectors.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.repository.BillRepository;
import com.billariz.corp.database.repository.BillSegmentRepository;
import com.billariz.corp.database.repository.BillableChargeRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.MeterReadRepository;
import com.billariz.corp.database.repository.ObjectProcessRuleRepository;
import com.billariz.corp.app.exception.ProcessInterruptedException;
import com.billariz.corp.database.model.Article;
import com.billariz.corp.database.model.Bill;
import com.billariz.corp.database.model.BillSegment;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.ObjectProcessRule;
import com.billariz.corp.database.model.enumeration.BillNature;
import com.billariz.corp.database.model.enumeration.BillSegmentStatus;
import com.billariz.corp.database.model.enumeration.BillStatusEnum;
import com.billariz.corp.database.model.enumeration.BillableChargeStatus;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.MeterReadStatus;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.notifier.bill.BillCaptor;
import com.billariz.corp.launcher.queue.LauncherQueue;
import com.billariz.corp.launcher.utils.ActivityUtils;
import com.billariz.corp.launcher.utils.JournalUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillCaptorImpl implements BillCaptor
{
    @PersistenceContext
    private EntityManager                             entityManager;

    private ThreadLocal<Optional<ObjectProcessRule>> ruleActive = new ThreadLocal<>();

    private final ObjectProcessRuleRepository        objectProcessRulesRepository;

    private final BillRepository                      billRepository;

    private final EventRepository                     eventRepository;

    private final LauncherQueue                       launcherQueue;

    private final BillSegmentRepository               billSegmentRepository;

    private final MeterReadRepository                 meterReadRepository;

    private final BillableChargeRepository            billableChargeRepository;

    private final ActivityUtils activityUtils;

    private final JournalUtils                 journalUtils;

    private final Locale locale = LocaleContextHolder.getLocale();


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPreRemove(Bill bill)
    {
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        var oldStatus = getOldValue(bill);
        var newStatus = "SUPPRESSION";
        var rules = objectProcessRulesRepository.findAllByNewStatusAndInitialStatusAndObjectType(newStatus, oldStatus, ObjectType.BILL);
        if (!rules.isEmpty())
            journalUtils.addJournal(ObjectType.BILL, bill.getId(),
                                    "DELETING_BILL", 
                                    new Object[]{rules.toString()},
                                    null,
                                    JournalAction.LOG, authenticated.getName()
                                    );
        else
            throw new IllegalArgumentException (journalUtils.getMessage("NO_RULES_TO_DELETE_BILL", new Object[]{bill,rules}, locale));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ProcessInterruptedException.class)
    @Override
    public void onPrePersist(Bill bill)
    {
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        var oldStatus = getOldValue(bill)==""?null:getOldValue(bill);
        var newStatus = Objects.toString(bill.getStatus(), "");
        var rules = objectProcessRulesRepository.findAllByNewStatusAndInitialStatusAndObjectType(newStatus, oldStatus, ObjectType.BILL);
        if (!rules.isEmpty())
        {
            var rule = rules.stream().filter(billProcessRules -> checkConditions(billProcessRules, bill)).findFirst();
            var finalStatus = rule.map(ObjectProcessRule::getFinalStatus);

            if (finalStatus.isPresent())
            {
                launchCreateEventBill(bill);
                journalUtils.addJournal(ObjectType.CONTRACT, bill.getContractId(),
                            "OBJECT_UPDATED_WITH_RULE", 
                            new Object[]{"CONTRACT",rule.toString()},
                            null,
                            JournalAction.LOG, authenticated.getName()
                            );
            }
            //revoi un 206 pour success sans contenu
            throw new ProcessInterruptedException(HttpStatus.NO_CONTENT,  null);
        }
        else
            throw new IllegalArgumentException (journalUtils
                .getMessage("OBJECT_UPDATED_WITH_NO_RULE", new Object[]{bill.getContractId()}, locale));

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void onPreUpdate(Bill bill)
    {
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        var oldStatus = getOldValue(bill)==""?null:getOldValue(bill);
        var newStatus = Objects.toString(bill.getStatus(), "");
        var rules = objectProcessRulesRepository.findAllByNewStatusAndInitialStatusAndObjectType(newStatus, oldStatus, ObjectType.BILL);
        if (!rules.isEmpty())
        {
            var rule = rules.stream().filter(billProcessRules -> checkConditions(billProcessRules, bill)).findFirst();
            var finalStatus = rule.map(ObjectProcessRule::getFinalStatus);

            if (finalStatus.isPresent())
            {
                bill.setStatus(BillStatusEnum.valueOf(finalStatus.get()));
                journalUtils.addJournal(ObjectType.BILL, bill.getId(),
                            "OBJECT_UPDATED_WITH_RULE", 
                            new Object[]{"BILL",rule.toString()},
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
    public void onPostPersistOrUpdate(Bill bill) {
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        var rule = ruleActive.get();
        if (rule == null || !rule.isPresent()) {
            journalUtils.addJournal(ObjectType.BILL, bill.getId(),
                            "OBJECT_UPDATED_WITH_NO_RULE", 
                            new Object[]{"BILL"},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
            return;
        }
        if (rule == null || rule.get().getActivityType() == null) {
            journalUtils.addJournal(ObjectType.BILL, bill.getId(),
                            "OBJECT_UPDATED_WITH_NO_RULE", 
                            new Object[]{"BILL"},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
            return;
        }
    
        var activity = getAction(rule.get().getActivityType());
        if (activity.equals(rule.get().getActivityType())) {
            if (!bill.getGroup()) {
                launcherQueue.createActivityEvent(rule.get().getActivityType(),  "systemAgent",
                                            bill.getId(), ObjectType.BILL,
                                            bill.getContractId(), ObjectType.CONTRACT);
            } else {
                launcherQueue.createActivityEvent(rule.get().getActivityType(), "systemAgent",
                                                bill.getId(), ObjectType.BILL,
                                               bill.getPerimeterId(), ObjectType.PERIMETER);
            }
            journalUtils.addJournal(ObjectType.BILL, bill.getId(),
                            "OBJECT_UPDATED_WITH_RULE", 
                            new Object[]{"BILL",rule.toString()},
                            null,
                            JournalAction.LOG, authenticated !=null ? authenticated.getName() : null
                            );
        } else {
            launchEvent(bill, activity, authenticated !=null ? authenticated.getName() : null);
            journalUtils.addJournal(ObjectType.BILL, bill.getId(),
                            "LAUNCH_ACTIVITY_ON_OBJECT", 
                            new Object[]{activity, "BILL", bill.getId()},
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

    private void launchEvent(Bill bill, String action, String userName)
    {
        switch (action)
        {
        case "DELETE_BILL":
            prepareDeletingBill(bill);
            break;
        case "PRINTSHOP":
            launchBillingStep(bill, "PRINTSHOP", userName);
            break;
        case "VALIDATION":
            launchBillingStep(bill, "VALIDATION", userName);
            break;
        default:
            break;
        }
    }

    private void launchBillingStep(Bill bill, String step, String userName)
    {
        //chercher l'event de validation
        var event = eventRepository.findByBillIdAndStatusInAndSubCategory(bill.getId(), 
                                            List.of(EventStatus.PENDING, EventStatus.IN_FAILURE, EventStatus.SUSPENDED),step);
        
        if(!event.isPresent()){
            journalUtils.addJournal(ObjectType.BILL, bill.getId(),
                                            "MISSING_ELIGIBLE_EVENT", 
                                            new Object[]{"BILL",bill.getId(),step},
                                            null,
                                            JournalAction.LOG, userName);
            throw new IllegalArgumentException (journalUtils
                                .getMessage("MISSING_ELIGIBLE_EVENT", new Object[]{"BILL",bill.getId()}, locale));
        }
        else{//envoyer l'event dans la queue de traitement
            var evt = event.get();
            evt.setCascaded(true);
            evt.setExecutionMode(EventExecutionMode.AUTO);
            if (evt.getTriggerDate()==null || evt.getTriggerDate().isAfter(LocalDate.now()))
                evt.setTriggerDate(LocalDate.now());
            
            sendEventToQueue(evt);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void sendEventToQueue(Event event){
        
        launcherQueue.sendEventToQueue(event);
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void launchCreateEventBill(Bill bill)
    {
        launcherQueue.createActivityEvent("CREATE_EVENT_BILL", "systemAgent",
                                        bill.getContractId(), ObjectType.CONTRACT, 
                                        null, null, bill.getBillDate(), bill.getAction());
    }

    private void prepareDeletingBill(Bill bill) 
    {
        if(!bill.getGroup())
            processBillSegments(bill, bill.getNature());//Traiter les BS, MR et BC
        else {
            if(!bill.getNature().equals(BillNature.CREDIT_NOTE))
                processChildBills(bill);
        }
        if(bill.getNature().equals(BillNature.CREDIT_NOTE)){ // Re valider la facture iniale
            billRepository.updateStatusById(bill.getCancelledBillId(),BillStatusEnum.VALIDATED);
        }
         activityUtils.suspendActivity(ObjectType.BILL,bill.getId(), 
                            journalUtils.getMessage("SUSPEND_ACTIVITY", new Object[]{"BILL", bill.getId()}, locale));
    }
    
    private void processChildBills(Bill bill) 
    {
        List<Bill> childBills;
        childBills = billRepository.findAllByGroupBillId(bill.getId());
        childBills.forEach(bl -> bl.setGroupBillId(null));
    }

    private void processBillSegments(Bill bill, BillNature billNature)
    {
        var billSegments = billSegmentRepository.findAllByBillId(bill.getId());
        List<Long> bs = billSegments.stream().map(BillSegment::getId).distinct().collect(toList());
        billSegmentRepository.updateBillSegmentStatusAndBillIdById(BillSegmentStatus.CALCULATED, null, bs);
        billSegmentRepository.updateBillSegmentStatusAndBillIdById((billNature.equals(BillNature.CREDIT_NOTE)) ?  
                            BillSegmentStatus.BILLED:BillSegmentStatus.CALCULATED, 
                            (billNature.equals(BillNature.CREDIT_NOTE)) ? bill.getCancelledBillId():null, bs);

        List<Long> mrs = billSegments.stream().map(BillSegment::getMeterReadId).distinct().collect(toList());
        meterReadRepository.updateStatus(mrs, (billNature.equals(BillNature.CREDIT_NOTE)) ? MeterReadStatus.BILLED: MeterReadStatus.VALUATED);

        List<Long> bcs = billSegments.stream().map(billSegment -> {
                                Article article = billSegment.getArticle();
                                return article != null ? article.getBillableChargeId() : null;
                            }).distinct().collect(toList());
        billableChargeRepository.updateStatus(bcs, (billNature.equals(BillNature.CREDIT_NOTE)) ? 
                                BillableChargeStatus.BILLED : BillableChargeStatus.VALUATED);
    }

    private boolean checkConditions(ObjectProcessRule streamRule, Bill eventDatabase)
    {
        return true;
    }

    private String getOldValue(Bill bill)
    {
        BillStatusEnum oldValue = null;
        var id = bill.getId();

        if (id != null)
            oldValue = billRepository.findById(id).map(Bill::getStatus).orElse(null);
        log.info("[BILL]getOldValue: id={} oldStatus={} newStatus={}", bill.getId(), oldValue, bill.getStatus());
        return Objects.toString(oldValue, "");
    }
}
