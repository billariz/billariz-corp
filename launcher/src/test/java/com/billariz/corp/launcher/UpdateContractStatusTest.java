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

package com.billariz.corp.launcher;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.ContractPerimeter;
import com.billariz.corp.database.model.Customer;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.ObjectProcessRule;
import com.billariz.corp.database.model.Perimeter;
import com.billariz.corp.database.model.Relation;
import com.billariz.corp.database.repository.BillableChargeRepository;
import com.billariz.corp.database.repository.BillingRunRepository;
import com.billariz.corp.database.repository.ContractPointOfServiceRepository;
import com.billariz.corp.database.repository.ContractRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.MonitoringRepository;
import com.billariz.corp.database.repository.ObjectProcessRuleRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.database.repository.ServiceRepository;
import com.billariz.corp.database.repository.TermOfServiceRepository;
import com.billariz.corp.launcher.queue.LauncherQueue;
import com.billariz.corp.launcher.tags.UpdateContractStatus;
import com.billariz.corp.launcher.utils.EventUtils;

@SpringBootTest(classes = { UpdateContractStatus.class })
@ContextConfiguration
public class UpdateContractStatusTest
{
        private static final String ACTIVITY_CONTRACT="ACTIVITY_CONTRACT";

        @Autowired
        private UpdateContractStatus             updateContractStatus;

        @MockBean
        private ContractRepository               contractRepository;

        @MockBean
        private ObjectProcessRuleRepository     objectProcessRulesRepository;

        @MockBean
        private EventRepository                  eventRepository;

        @MockBean
        private JournalRepository                journalRepository;

        @MockBean
        private LauncherQueue                    launcherQueue;

        @MockBean
        private MonitoringRepository             monitoringRepository;

        @MockBean
        private RelationRepository               relationRepository;

        @Captor
        private ArgumentCaptor<Journal>          captorJournal;

        @MockBean
        private EventUtils                       eventUtils;

        @MockBean
        private TermOfServiceRepository         termOfServicesRepository;

        @MockBean
        private ContractPointOfServiceRepository contractPointOfServiceRepository;

        @MockBean
        private BillableChargeRepository         billableChargeRepository;

        @MockBean
        private BillingRunRepository             billingRunRepository;

        @MockBean
        private ServiceRepository                serviceRepository;

        @Test
        public void testWithoutContractzzzzzzz()
        {
                var customer = new Customer().setId(30L);
                var contract = new Contract().setId(20L).setContractPerimeter(new ContractPerimeter().setPerimeter(
                                new Perimeter().setCustomer(customer).setCustomerId(customer.getId()))).setContractualStartDate(LocalDate.of(2023, 1, 1));
                var cpr1 = new ObjectProcessRule().setChannel("*").setCustomerCategory("*").setNewStatus("effective").setInitialStatus("*").setSeller(
                                "*").setServiceSubCategory("*");
                var events = Collections.singleton(new Event().setId(1L).setActivityId(10L));
                var eventsIds = events.stream().map(Event::getId).toList();
                var relationContract = new Relation().setSecondObjectId(contract.getId());

                when(objectProcessRulesRepository.findAll()).thenReturn(Collections.singletonList(cpr1));
                when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
                when(eventRepository.findAllById(eventsIds)).thenReturn(events);
                events.forEach(e -> {
                        when(eventRepository.save(e)).thenReturn(e);
                        when(relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_CONTRACT,
                                        e.getId())).thenReturn(Optional.of(relationContract));
                });
                // updateContractStatus.process(eventsIds,
                // EventExecutionMode.AUTO);

                // verify(contractRepository, atLeast(1)).save(any());
                // verify(eventRepository, atLeast(1)).save(any());
                // verify(journalRepository,
                // atLeast(1)).save(captorJournal.capture());
                // assertEquals(Objects.toString(EventStatus.COMPLETED),
                // captorJournal.getValue().getForStatus());
                verify(launcherQueue, never()).sendEventToQueue(any());
        }

        @Test
        public void testWithoutContract()
        {
                var events = Collections.singleton(new Event().setId(1L).setActivityId(10L));
                var eventsIds = events.stream().map(Event::getId).toList();

                when(eventRepository.findAllById(eventsIds)).thenReturn(events);
                events.forEach(e -> when(eventRepository.save(e)).thenReturn(e));
                // updateContractStatus.process(eventsIds,
                // EventExecutionMode.AUTO);

                verify(contractRepository, never()).save(any());
                // verify(eventRepository, atLeast(1)).save(any());
                // verify(journalRepository,
                // atLeast(1)).save(captorJournal.capture());
                // assertEquals(Objects.toString(EventStatus.COMPLETED),
                // captorJournal.getValue().getForStatus());
                verify(launcherQueue, never()).sendEventToQueue(any());
        }
}
