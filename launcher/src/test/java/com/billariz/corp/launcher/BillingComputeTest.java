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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import com.billariz.corp.database.model.Bill;
import com.billariz.corp.database.model.BillSegment;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.ContractPerimeter;
import com.billariz.corp.database.model.Parameter;
import com.billariz.corp.database.model.Perimeter;
import com.billariz.corp.database.model.Rate;
import com.billariz.corp.database.model.ServiceElement;
import com.billariz.corp.database.model.enumeration.BillSegmentStatus;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;
import com.billariz.corp.database.repository.BillRepository;
import com.billariz.corp.database.repository.BillSegmentRepository;
import com.billariz.corp.database.repository.ContractRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.MeterReadRepository;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.database.repository.PerimeterRepository;
import com.billariz.corp.database.repository.RateRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.database.repository.TermOfServiceRepository;
import com.billariz.corp.database.validator.BaseValidator;
import com.billariz.corp.launcher.queue.LauncherQueue;
import com.billariz.corp.launcher.tags.BillingCompute;
import com.billariz.corp.launcher.utils.EventUtils;

@SpringBootTest(classes = { BillingCompute.class })
@ContextConfiguration
public class BillingComputeTest
{
        @Autowired
        private BillingCompute           billingCompute;

        @MockBean
        private BillRepository           billRepository;

        @MockBean
        private BillSegmentRepository    billSegmentRepository;

        @MockBean
        private ContractRepository       contractRepository;

        @MockBean
        private ParameterRepository      parameterRepository;

        @MockBean
        private RateRepository           rateRepository;

        @MockBean
        private TermOfServiceRepository termOfServicesRepository;

        @MockBean
        private EventRepository          eventRepository;

        @MockBean
        private RelationRepository       relationRepository;

        @MockBean
        private JournalRepository        journalRepository;

        @MockBean
        private EventUtils               eventUtils;

        @MockBean
        private PerimeterRepository      perimeterRepository;

        @MockBean
        private MeterReadRepository      meterReadRepository;

        @Captor
        private ArgumentCaptor<Bill>     captorBill;

        @MockBean
        private LauncherQueue            launcherQueue;

        @Test
        void testSimple()
        {
                var dateBefore = LocalDate.of(2020, 1, 1);
                var dateStart = LocalDate.of(2022, 1, 1);
                var dateEnd = LocalDate.of(2022, 1, 31);
                var contract = new Contract().setId(1L).setContractPerimeter(
                                new ContractPerimeter().setPerimeterId(2L).setPerimeter(new Perimeter().setCustomerId(3L)));
                var billSegment1 = new BillSegment().setVatRate("NORMAL_RATE").setPrice(new BigDecimal("10")).setStartDate(dateStart).setEndDate(
                                dateEnd).setSe(new ServiceElement().setCategory("CONSUMPTION").setSubCategory("se1subcat"));
                var billSegment2 = new BillSegment().setVatRate("REDUCED_RATE").setPrice(new BigDecimal("10")).setStartDate(dateStart).setEndDate(
                                dateEnd).setSe(new ServiceElement().setCategory("SUBSCRIPTION").setSubCategory("se2subcat"));
                var billSegments = Arrays.asList(billSegment1, billSegment2);
                var paramBillableStatus = Collections.singletonList(new Parameter().setValue("BILL_IT"));
                var paramBillableStatusStr = paramBillableStatus.stream().map(Parameter::getValue).toList();
                var rateRR = new Rate().setType("RR").setTou("*").setTouGroup("*").setStartDate(dateBefore).setPrice(new BigDecimal("0.055")).setUnit(
                                "EURO_BY_EURO");
                var rateNR = new Rate().setType("NR").setTou("*").setTouGroup("*").setStartDate(dateBefore).setPrice(new BigDecimal("0.20")).setUnit(
                                "EURO_BY_EURO");
                var tosId = Arrays.asList(10L, 11L);

                //when(billSegmentRepository.findWithStatusInAndTosIdIn(paramBillableStatusStr, tosId)).thenReturn(billSegments);
                when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
                when(parameterRepository.findAllByTypeAndNameAndStartDateBefore(eq("BILLABLE_STATUS"), eq(BaseValidator.SERVICE_ELEMENT_STATUS),
                                any())).thenReturn(paramBillableStatus);
                //when(rateRepository.findAllByType(VatRates.REDUCED_RATE.toString())).thenReturn(Collections.singletonList(rateRR));
                //when(rateRepository.findAllByType(VatRates.NORMAL_RATE.toString())).thenReturn(Collections.singletonList(rateNR));
                when(termOfServicesRepository.findAllIds(contract.getId())).thenReturn(tosId);

                billingCompute.process(Collections.singletonList(contract.getId()), EventExecutionMode.AUTO);
                // verify(billRepository).save(captorBill.capture());

                // var bill = captorBill.getValue();
                /*
                 * assertNotNull(bill.getBillDate()); assertEquals(dateStart,
                 * bill.getStartDate()); assertEquals(dateEnd,
                 * bill.getEndDate()); assertEquals(new BigDecimal("0.550"),
                 * bill.getVatRR()); assertEquals(new BigDecimal("2.00"),
                 * bill.getVatNR()); assertEquals(new BigDecimal("2.550"),
                 * bill.getTotalVat()); assertEquals(new BigDecimal("20"),
                 * bill.getTotalWithoutVat()); assertEquals(new
                 * BigDecimal("22.550"), bill.getTotalAmount());
                 * assertNotNull(bill.getDetails());
                 * assertEquals(billSegments.size(), bill.getDetails().size());
                 */
        }

        /*
         * @Test void tesVatNROnly() { var dateBefore = LocalDate.of(2020, 1,
         * 1); var dateStart = LocalDate.of(2022, 1, 1); var dateEnd =
         * LocalDate.of(2022, 1, 31); var contract = new
         * Contract().setId(1L).setContractPerimeter( new
         * ContractPerimeter().setPerimeterId(2L) .setPerimeter(new
         * Perimeter().setCustomerId(3L))); var billSegment1 = new
         * BillSegment().setVatRate("NR").setPrice(new BigDecimal("10"))
         * .setStartDate(dateStart).setEndDate(dateEnd).setSe( new
         * ServiceElement().setCategory("se1cat").setSubCategory("se1subcat"));
         * var billSegments = Collections.singletonList(billSegment1); var
         * paramBillableStatus = Collections.singletonList(new
         * Parameter().setValue("BILL_IT")); var paramBillableStatusStr =
         * paramBillableStatus.stream().map(Parameter::getValue).toList(); var
         * rateNR = new
         * Rate().setType("NR").setTou(TouEnum.ALL).setTouGroup("*").
         * setStartDate( dateBefore) .setPrice(new BigDecimal("0.20")).setUnit(
         * RateUnit.EURO_BY_EURO); var tosId = Arrays.asList(10L, 11L); var
         * relation = new
         * Relation().setId(123L).setFirstObjectId(12L).setSecondObjectId(23L);
         * var relat = Collections.singletonList(relation);
         * when(relationRepository.findById(12L)).thenReturn(relat);
         * when(billSegmentRepository.findWithStatusInAndTosIdIn(
         * paramBillableStatusStr, tosId)) .thenReturn(billSegments);
         * when(contractRepository.findById(contract.getId())).thenReturn(
         * Optional.of( contract));
         * when(parameterRepository.findAllByTypeAndNameAndStartDateBefore(eq(
         * "BILLABLE_STATUS"), eq(BaseValidator.SERVICE_ELEMENT_STATUS),
         * any())).thenReturn( paramBillableStatus);
         * when(rateRepository.findAllByType("NR")).thenReturn(Collections.
         * singletonList (rateNR));
         * when(termOfServicesRepository.findAllIds(contract.getId())).
         * thenReturn(tosId) ;
         * billingCompute.process(Collections.singletonList(contract.getId()),
         * EventExecutionMode.AUTO);
         * verify(billRepository).save(captorBill.capture()); var bill =
         * captorBill.getValue(); assertNotNull(bill.getBillDate());
         * assertEquals(dateStart, bill.getStartDate()); assertEquals(dateEnd,
         * bill.getEndDate()); assertEquals(new BigDecimal("0"),
         * bill.getVatRR()); assertEquals(new BigDecimal("2.00"),
         * bill.getVatNR()); assertEquals(new BigDecimal("2.00"),
         * bill.getTotalVat()); assertEquals(new BigDecimal("10"),
         * bill.getTotalWithoutVat()); assertEquals(new BigDecimal("12.00"),
         * bill.getTotalAmount()); assertNotNull(bill.getDetails());
         * assertEquals(billSegments.size(), bill.getDetails().size()); }
         */
}
