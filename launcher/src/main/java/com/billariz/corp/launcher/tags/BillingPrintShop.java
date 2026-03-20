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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import static java.util.stream.Collectors.*;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.database.model.Document;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.enumeration.ActivityStatus;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.model.enumeration.EventStatus;
import com.billariz.corp.database.model.enumeration.JournalAction;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.model.light.messageCode;
import com.billariz.corp.database.repository.BillRepository;
import com.billariz.corp.database.repository.DocumentRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.launcher.Launcher;
import com.billariz.corp.launcher.exception.LauncherException;
import com.billariz.corp.launcher.exception.LauncherFatalException;
import com.billariz.corp.launcher.queue.PrintShopDataDetailMessage;
import com.billariz.corp.launcher.queue.PrintShopDataDetailResponse;
import com.billariz.corp.launcher.queue.PrintShopDataMessage;
import com.billariz.corp.launcher.queue.PrintShopQueueProducer;
import com.billariz.corp.launcher.utils.EventUtils;
import com.billariz.corp.launcher.utils.JournalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingPrintShop implements Launcher
{
    private static final String ACTIVITY_BILLING_RUN="ACTIVITY_BILLING_RUN";

    private static final String ACTIVITY_INVOICE="ACTIVITY_INVOICE";

    private final EventRepository        eventRepository;

    private final JournalRepository      journalRepository;

    private final RelationRepository     relationRepository;

    private final EventUtils             eventUtils;

    private final JournalUtils                 journalUtils;

    private final BillRepository         billRepository;

    private final PrintShopQueueProducer printShopQueueProducer;

    private final DocumentRepository documentRepository;

    ///private final PrintShopResponseQueueProducer printShopResponseQueueProducer;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Iterable<Long> eventIds, EventExecutionMode executionMode)
    {
        var events = eventRepository.findAllById(eventIds);
        events.forEach(e -> process(e, executionMode));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processResponse(Set<PrintShopDataDetailResponse> data)
    {
        log.info("Starting process response from printshop: [{}]", data);
        data.forEach(e ->  processResponseDetail(e));
    }

    private void process(Event event, EventExecutionMode executionMode)
    {
        log.info("Starting process event : [{}]", event.getId());
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
        log.info("Event [{}] processed, strating next process", event.getId());
        eventUtils.triggerOnUpdateEventStatus(event);
        journal.setNewStatus(Objects.toString(event.getStatus()));
        journal.setMessageCodes(messages);
        journalRepository.save(journal);
    }

    private void handle(Event event, Journal journal, List<messageCode> messages) throws LauncherException
    {
        var processingBase = (event.getAction()!= null) ? event.getAction() : "UNITARY";
        messages.add(new messageCode("EVENT_USE_CASE", new Object[]{processingBase}));

        Set<Long> billsIds = new HashSet<>();

        if(processingBase.equals("UNITARY")){
            log.info("Processing Base [{}]", processingBase);
            var relation = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_INVOICE, event.getActivityId());
            if (relation.isEmpty())
            {
                log.info("No invoice relation found for Event [{}] ", event);
                throw new LauncherFatalException("MISSING_INVOICE", new Object[]{event.getId()});
            }
            else 
                billsIds.add(relation.get().getSecondObjectId());
        } 
        if(processingBase.equals("IN_MASS")){
                log.info("Processing Base [{}]", processingBase);
                var relation = relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_BILLING_RUN, event.getActivityId());
                if (relation.isEmpty()) {
                        log.info("No billing run relation found for Event [{}] ", event);
                        throw new LauncherFatalException("MISSING_BILLINRUN", new Object[]{event.getId()});
                    }
                else
                    billsIds = billRepository.findAllBillIdByBillingRunId(relation.get().getSecondObjectId());
            }
        log.info("billsIds to process [{}]", billsIds);
        sendMessageToPrintShopQueue(billsIds, event, messages);
        journal.setMessageCodes(messages);
    }

    private void sendMessageToPrintShopQueue(Set<Long> billsIds, Event event,  List<messageCode> messages)
    {
        boolean paginationCondition = true;
        int offset = 0;
        int packetSize = 10;

        PrintShopDataMessage printShopData = new PrintShopDataMessage( ObjectType.BILL);

        while (paginationCondition)
        {
            Set<Long> bIds = billsIds.stream().skip(offset).limit(packetSize).collect(toSet());
            bIds.forEach(b -> printShopData.data().add(new PrintShopDataDetailMessage(event.getId(), b)));

            paginationCondition = (bIds.size() == packetSize);
            offset += packetSize;
            log.info("Data sent to BILL PRINTSHOP => " + printShopData);
            messages.add(new messageCode("PRINTSHOP_DATA", new Object[]{printShopData}));
            printShopData.data().forEach(dt -> journalUtils.addJournal(ObjectType.BILL, dt.objectId(),
                                            "PRINTSHOP_BILL", 
                                            new Object[]{dt.objectId(), event.getId()},
                                            event,
                                            JournalAction.LOG
                                            ));
            printShopQueueProducer.publish(printShopData);
            printShopData.data().clear();
        }

        //testReceivingPrintshopMessage(1L, event);
    }


    // private void testReceivingPrintshopMessage(Long billId, Event event)
    // {
    //     PrintShopDataDetailResponse detail = new PrintShopDataDetailResponse(event.getId(),billId, "1", "OK", "OK");
    //     Set<PrintShopDataDetailResponse> dt = new HashSet<>();
    //     dt.add(detail);
    //     PrintShopDataResponse response = new PrintShopDataResponse(ObjectType.BILL, dt);

    //     printShopResponseQueueProducer.publish(response);

    // }

    public void processResponseDetail(PrintShopDataDetailResponse detail)
    {
        log.info("Processing detail from printshop for event [{}] and object type [{}] of id [{}] ", detail.eventId(),detail.objectType(),detail.objectId());
        var event = eventRepository.findById(detail.eventId()).orElse(null);
        if (detail.code().equals("1"))
        {
            var document = new Document();
            document.setObjectId(detail.objectId());
            document.setObjectType(detail.objectType());
            document.setType(detail.objectType());
            document.setDescription(detail.msg());
            document.setPath(detail.path());
            documentRepository.save(document);

            journalUtils.addJournal(detail.objectType(), detail.objectId(),
                                        "BILL_PRINTSHOP_INFO", 
                                        new Object[]{event.getId()},
                                        event,
                                        JournalAction.LOG
                                        );
            journalUtils.addJournal(ObjectType.EVENT, event.getId(),
                                        "BILL_PRINTSHOP_INFO", 
                                        new Object[]{event.getId()},
                                        event,
                                        JournalAction.LOG
                                        );

            event.setCascaded(true);
            event.setStatus(EventStatus.COMPLETED);
            eventUtils.triggerOnUpdateEventStatus(event);

        }
        else
        {
            log.info(detail.objectId().toString() + "printshop error on eventId: " + detail.eventId().toString() + "with message:" + detail.msg());
            
            journalUtils.addJournal(detail.objectType() !=null ? detail.objectType() : ObjectType.BILL,  detail.objectId(),
                                        "BILL_PRINTSHOP_ERROR", 
                                        new Object[]{detail.objectId(), event.getId(),detail.msg()},
                                        event,
                                        JournalAction.ERROR
                                        );
            journalUtils.addJournal(ObjectType.EVENT, event.getId(),
                                        "BILL_PRINTSHOP_ERROR", 
                                        new Object[]{detail.objectId(), event.getId(),detail.msg()},
                                        event,
                                        JournalAction.ERROR
                                        );
            
            event.setCascaded(true);
            event.setStatus(EventStatus.IN_FAILURE);
            eventUtils.triggerOnUpdateEventStatus(event);
        }
    }
}
