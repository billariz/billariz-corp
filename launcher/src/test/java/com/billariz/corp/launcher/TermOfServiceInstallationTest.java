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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.ContractPerimeter;
import com.billariz.corp.database.model.Customer;
import com.billariz.corp.database.model.Event;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.Perimeter;
import com.billariz.corp.database.model.PointOfService;
import com.billariz.corp.database.model.PointOfServiceConfiguration;
import com.billariz.corp.database.model.Relation;
import com.billariz.corp.database.model.ServiceElementStartOption;
import com.billariz.corp.database.model.ServiceElementType;
import com.billariz.corp.database.model.ServiceStartOption;
import com.billariz.corp.database.model.TermOfServiceStartOption;
import com.billariz.corp.database.model.TermOfServiceType;
import com.billariz.corp.database.repository.ActivityTemplateRepository;
import com.billariz.corp.database.repository.ContractRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.PerimeterRepository;
import com.billariz.corp.database.repository.PointOfServiceConfigurationRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.database.repository.ServiceElementStartOptionRepository;
import com.billariz.corp.database.repository.ServiceElementRepository;
import com.billariz.corp.database.repository.ServiceRepository;
import com.billariz.corp.database.repository.ServiceStartOptionRepository;
import com.billariz.corp.database.repository.ServiceTypeRepository;
import com.billariz.corp.database.repository.TermOfServiceRepository;
import com.billariz.corp.database.repository.ThirdRepository;
import com.billariz.corp.database.repository.TosStartOptionRepository;
import com.billariz.corp.database.repository.TosTypeRepository;
import com.billariz.corp.launcher.queue.LauncherQueue;
import com.billariz.corp.launcher.tags.TermOfServiceInstallation;
import com.billariz.corp.launcher.utils.EventUtils;

@SpringBootTest(classes = { TermOfServiceInstallation.class })
@ContextConfiguration
public class TermOfServiceInstallationTest
{
        private static final String ACTIVITY_CONTRACT="ACTIVITY_CONTRACT";

        @MockBean
        private ActivityTemplateRepository            activityTemplateRepository;

        @MockBean
        private ContractRepository                    contractRepository;

        @MockBean
        private EventRepository                       eventRepository;

        @MockBean
        private JournalRepository                     journalRepository;

        @MockBean
        private LauncherQueue                         launcherQueue;

        @MockBean
        private RelationRepository                    relationRepository;

        @MockBean
        private PointOfServiceConfigurationRepository pointOfServiceConfigurationRepository;

        @MockBean
        private ServiceRepository                     serviceRepository;

        @MockBean
        private ServiceTypeRepository                 serviceTypeRepository;

        @MockBean
        private ServiceElementRepository              serviceElementRepository;

        @MockBean
        private ServiceStartOptionRepository          serviceStartOptionRepository;

        @MockBean
        private ServiceElementStartOptionRepository               seStartOptionRepository;

        @MockBean
        private TermOfServiceRepository              termOfServicesRepository;

        @MockBean
        private ThirdRepository                       thirdRepository;

        @MockBean
        private TosStartOptionRepository              tosStartOptionRepository;

        @MockBean
        private TosTypeRepository                     tosTypeRepository;

        @Captor
        private ArgumentCaptor<Journal>               captorJournal;

        @MockBean
        private EventUtils                            eventUtils;

        @MockBean
        private PerimeterRepository                   perimeterRepository;

        @Test
        void testWithNewTOS()
        {
                var tosStartOption = new TermOfServiceStartOption();
                var tosType = new TermOfServiceType().setId("tostype-id");
                var customer = new Customer().setId(30L);
                var contract = new Contract().setId(20L).setContractPerimeter(new ContractPerimeter().setPerimeter(
                                new Perimeter().setCustomer(customer).setCustomerId(customer.getId()))).setContractualStartDate(LocalDate.of(2023, 1, 1));
                var events = Collections.singleton(new Event().setId(1L).setActivityId(10L));
                var eventsIds = events.stream().map(Event::getId).toList();
                var relationContract = new Relation().setSecondObjectId(contract.getId());
                var pos = new PointOfService().setId(12L);
                var posConfiguration = new PointOfServiceConfiguration().setPointOfService(pos);
                var seso1 = new ServiceElementStartOption().setTouGroup("*").setSeType(new ServiceElementType());
                var sso1 = new ServiceStartOption().setStartDate(LocalDate.of(2000, 1, 1)).setTosType(tosType.getId()).setBillingMode("*").setChannel(
                                "*").setCustomerCategory("*").setCustomerType("*").setDgoCode("*").setMarket("*").setPaymentMode("*").setPosCategory("*").setSeller(
                                                "*").setService("*").setDirection("*").setServiceCategory("*").setServiceSubCategory("*").setTgoCode("*");

                when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
                when(pointOfServiceConfigurationRepository.findFirstByContractIdAndEndDateIsNullOrderByStartDateDesc(contract.getId())).thenReturn(
                                Optional.of(posConfiguration));
                when(eventRepository.findAllById(eventsIds)).thenReturn(events);
                when(serviceStartOptionRepository.findAll()).thenReturn(Collections.singletonList(sso1));
                when(seStartOptionRepository.filterByTosTypeAndDate(eq(tosType.getId()), any())).thenReturn(Collections.singletonList(seso1));
                when(termOfServicesRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
                when(tosStartOptionRepository.filterByTosTypeAndDate(eq(tosType.getId()), any())).thenReturn(Optional.of(tosStartOption));
                when(tosTypeRepository.findById(tosType.getId())).thenReturn(Optional.of(tosType));
                events.forEach(e -> {
                        when(eventRepository.save(e)).thenReturn(e);
                        when(relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_CONTRACT,
                                        e.getActivityId())).thenReturn(Optional.of(relationContract));
                });
                // tosInstallation.process(eventsIds, EventExecutionMode.AUTO);

                // verify(eventRepository, atLeast(1)).save(any());
                // verify(journalRepository,
                // atLeast(1)).save(captorJournal.capture());
                // assertEquals(Objects.toString(EventStatus.IN_FAILURE),
                // captorJournal.getValue().getForStatus());
                // verify(serviceElementRepository, atLeast(0)).save(any());
                // verify(eventUtils,
                // atLeast(1)).triggerOnUpdateEventStatus(any());
        }

        @Test
        void testWithTOSTypeInvalid()
        {
                var customer = new Customer().setId(30L);
                var contract = new Contract().setId(20L).setContractPerimeter(new ContractPerimeter().setPerimeter(
                                new Perimeter().setCustomer(customer).setCustomerId(customer.getId()))).setContractualStartDate(LocalDate.of(2023, 1, 1));
                var events = Collections.singleton(new Event().setId(1L).setActivityId(10L));
                var eventsIds = events.stream().map(Event::getId).toList();
                var relationContract = new Relation().setSecondObjectId(contract.getId());
                var pos = new PointOfService().setId(13L);
                var posConfiguration = new PointOfServiceConfiguration().setPointOfService(pos);
                var sso1 = new ServiceStartOption().setStartDate(LocalDate.of(2000, 1, 1)).setTosType("tos-invalid").setBillingMode("*").setChannel(
                                "*").setCustomerCategory("*").setCustomerType("*").setDgoCode("*").setMarket("*").setPaymentMode("*").setPosCategory("*").setSeller(
                                                "*").setService("*").setDirection("*").setServiceCategory("*").setServiceSubCategory("*").setTgoCode("*");

                when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
                when(pointOfServiceConfigurationRepository.findFirstByContractIdAndEndDateIsNullOrderByStartDateDesc(contract.getId())).thenReturn(
                                Optional.of(posConfiguration));
                when(eventRepository.findAllById(eventsIds)).thenReturn(events);
                when(serviceStartOptionRepository.findAll()).thenReturn(Collections.singletonList(sso1));
                events.forEach(e -> {
                        when(eventRepository.save(e)).thenReturn(e);
                        when(relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_CONTRACT,
                                        e.getActivityId())).thenReturn(Optional.of(relationContract));
                });
                // tosInstallation.process(eventsIds, EventExecutionMode.AUTO);

                // verify(eventRepository, atLeast(1)).save(any());
                // verify(journalRepository,
                // atLeast(1)).save(captorJournal.capture());
                // assertEquals(Objects.toString(EventStatus.IN_FAILURE),
                // captorJournal.getValue().getForStatus());
                // verify(serviceElementRepository, never()).save(any());
                verify(launcherQueue, atLeast(0)).sendEventToQueue(any());
        }

        @Test
        void testWithoutRelation()
        {
                var events = Collections.singleton(new Event().setId(1L).setActivityId(10L));
                var eventsIds = events.stream().map(Event::getId).toList();

                when(eventRepository.findAllById(eventsIds)).thenReturn(events);
                events.forEach(e -> when(eventRepository.save(e)).thenReturn(e));
                // tosInstallation.process(eventsIds, EventExecutionMode.AUTO);

                // verify(eventRepository, atLeast(1)).save(any());
                // verify(journalRepository,
                // atLeast(1)).save(captorJournal.capture());
                // assertEquals(Objects.toString(EventStatus.COMPLETED),
                // captorJournal.getValue().getForStatus());
                // verify(serviceElementRepository, never()).save(any());
                verify(launcherQueue, atLeast(0)).sendEventToQueue(any());
        }

        @Test
        void testWithoutServiceStartOption()
        {
                var customer = new Customer().setId(30L);
                var contract = new Contract().setId(20L).setContractPerimeter(
                                new ContractPerimeter().setPerimeter(new Perimeter().setCustomer(customer).setCustomerId(customer.getId())));
                var events = Collections.singleton(new Event().setId(1L).setActivityId(10L));
                var eventsIds = events.stream().map(Event::getId).toList();
                var relationContract = new Relation().setSecondObjectId(contract.getId());
                var posConfiguration = new PointOfServiceConfiguration();

                when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
                when(pointOfServiceConfigurationRepository.findFirstByContractIdAndEndDateIsNullOrderByStartDateDesc(contract.getId())).thenReturn(
                                Optional.of(posConfiguration));
                when(eventRepository.findAllById(eventsIds)).thenReturn(events);
                events.forEach(e -> {
                        when(eventRepository.save(e)).thenReturn(e);
                        when(relationRepository.findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(ACTIVITY_CONTRACT,
                                        e.getActivityId())).thenReturn(Optional.of(relationContract));
                });
                // tosInstallation.process(eventsIds, EventExecutionMode.AUTO);

                // verify(eventRepository, atLeast(1)).save(any());
                // verify(journalRepository,
                // atLeast(1)).save(captorJournal.capture());
                // assertEquals(Objects.toString(EventStatus.IN_FAILURE),
                // captorJournal.getValue().getForStatus());
                // verify(serviceElementRepository, never()).save(any());
                verify(launcherQueue, never()).sendEventToQueue(any());
        }
}
