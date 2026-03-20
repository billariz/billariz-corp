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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import com.billariz.corp.database.model.BillSegment;
import com.billariz.corp.database.model.Contract;
import com.billariz.corp.database.model.ContractPerimeter;
import com.billariz.corp.database.model.Customer;
import com.billariz.corp.database.model.Perimeter;
import com.billariz.corp.database.model.PointOfService;
import com.billariz.corp.database.model.PointOfServiceCapacity;
import com.billariz.corp.database.model.PointOfServiceConfiguration;
import com.billariz.corp.database.model.ServiceElement;
import com.billariz.corp.database.model.enumeration.EventExecutionMode;

import com.billariz.corp.database.model.enumeration.PointOfServiceDataStatus;
import com.billariz.corp.database.repository.BillSegmentRepository;
import com.billariz.corp.database.repository.BillableChargeRepository;
import com.billariz.corp.database.repository.BillingRunRepository;
import com.billariz.corp.database.repository.ClimaticRefRepository;
import com.billariz.corp.database.repository.CoefARepository;
import com.billariz.corp.database.repository.ContractPointOfServiceRepository;
import com.billariz.corp.database.repository.ContractRepository;
import com.billariz.corp.database.repository.EventRepository;
import com.billariz.corp.database.repository.GeoFactorRepository;
import com.billariz.corp.database.repository.JournalRepository;
import com.billariz.corp.database.repository.MarketGeoRefRepository;
import com.billariz.corp.database.repository.MeterReadDetailRepository;
import com.billariz.corp.database.repository.MeterReadRepository;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.database.repository.PointOfServiceCapacityRepository;
import com.billariz.corp.database.repository.PointOfServiceConfigurationRepository;
import com.billariz.corp.database.repository.PointOfServiceEstimateRepository;
import com.billariz.corp.database.repository.PostalCodeRepository;
import com.billariz.corp.database.repository.RateRepository;
import com.billariz.corp.database.repository.RelationRepository;
import com.billariz.corp.database.repository.ServiceElementRepository;
import com.billariz.corp.database.repository.ServiceQuantityTypeRepository;
import com.billariz.corp.database.repository.ServiceRepository;
import com.billariz.corp.database.repository.TermOfServiceRepository;
import com.billariz.corp.database.repository.TouRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.billariz.corp.launcher.billing.BillFactorCalculationService;
import com.billariz.corp.launcher.billing.BillingService;
import com.billariz.corp.launcher.billing.PriceService;
import com.billariz.corp.launcher.billing.QuantityService;
import com.billariz.corp.launcher.tags.BillingValuation;
import com.billariz.corp.launcher.utils.EventUtils;
import com.billariz.corp.provider.BaseConstants;
import com.billariz.corp.provider.QueueProvider;

@SpringBootTest(classes = { BillingValuation.class, BillFactorCalculationService.class, BillingService.class, QuantityService.class, PriceService.class,
                BillingValuationTest.Config.class })
@ContextConfiguration
public class BillingValuationTest
{
        @Autowired
        private BillingValuation                      billingValuation;

        @Autowired
        private QuantityService                       quantityService;

        @MockBean
        private BillSegmentRepository                 billSegmentRepository;

        @MockBean
        private ClimaticRefRepository                 climaticRefRepository;

        @MockBean
        private CoefARepository                       coefARepository;

        @MockBean
        private GeoFactorRepository                   geoFactorRepository;

        @MockBean
        private MeterReadDetailRepository             consumptionDetailRepository;

        @MockBean
        private ContractRepository                    contractRepository;

        @MockBean
        private JournalRepository                     journalRepository;

        @MockBean
        private MarketGeoRefRepository                marketGeoRefRepository;

        @MockBean
        private MeterReadRepository                   meterReadRepository;

        @MockBean
        private PointOfServiceCapacityRepository      pointOfServiceCapacityRepository;

        @MockBean
        private PointOfServiceConfigurationRepository pointOfServiceConfigurationRepository;

        @MockBean
        private PointOfServiceEstimateRepository      pointOfServiceEstimateRepository;

        @MockBean
        private PostalCodeRepository                  postalCodeRepository;

        @MockBean
        private RateRepository                        rateRepository;

        @MockBean
        private ServiceElementRepository              serviceElementRepository;

        @MockBean
        private ServiceQuantityTypeRepository         serviceQuantityTypeRepository;

        @MockBean
        private TouRepository                         touRepository;

        @MockBean
        @Qualifier(BaseConstants.BEAN_QUEUE_BILLING)
        private QueueProvider                         queueProvider;

        @Captor
        private ArgumentCaptor<BillSegment>           captorBillSegment;

        @MockBean
        private ParameterRepository                   parameterRepository;

        @MockBean
        private TermOfServiceRepository              termOfServicesRepository;

        @MockBean
        private ContractPointOfServiceRepository      contractPointOfServiceRepository;

        @MockBean
        private BillableChargeRepository              billableChargeRepository;

        @MockBean
        private BillingRunRepository                  billingRunRepository;

        @MockBean
        private ServiceRepository                     serviceRepository;

        @MockBean
        private EventRepository                       eventRepository;

        @MockBean
        private RelationRepository                    relationRepository;

        @MockBean
        private EventUtils                            eventUtils;

        @Test
        void testNoQuantity() throws Exception
        {
                var customer = new Customer().setType("RESIDENTIAL").setCategory("B2C");
                var contract = new Contract().setId(72L).setContractPerimeter(new ContractPerimeter().setPerimeter(new Perimeter().setCustomer(customer)));
                var pos = new PointOfService().setId(11L);
                var posConf = new PointOfServiceConfiguration().setPointOfService(pos);
                var posCapa = new PointOfServiceCapacity().setPosId(pos.getId());
                var se = new ServiceElement().setId(118L).setMetered(true);
                List<Long> ids = Arrays.asList(1L);
                when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
                when(pointOfServiceConfigurationRepository.findAllByContractIdAndStatusIn(contract.getId(),
                                List.of(PointOfServiceDataStatus.VALIDATED))).thenReturn(Collections.singletonList(posConf));
                when(pointOfServiceCapacityRepository.findWithContractIdAndStatus(contract.getId(), PointOfServiceDataStatus.VALIDATED)).thenReturn(
                                Collections.singletonList(posCapa));
                when(queueProvider.consume()).thenReturn(Collections.singletonList(
                                "[{\"id\":118,\"metered\":true,\"sqType\":\"KWH_PER_TOU\",\"type\":\"CONSUMPTION\",\"category\":\"CONSUMPTION\",\"contractId\":72}]"));
                when(serviceElementRepository.findById(se.getId())).thenReturn(Optional.of(se));

                quantityService.postConstruct();
                billingValuation.process(ids, EventExecutionMode.AUTO);
                verify(meterReadRepository, never()).updateStatus(any(), any());
        }

        /*
         * @Test void testQuantityKwhPerTou() throws Exception { var startDate =
         * LocalDate.of(2022, 1, 1); var endDate = LocalDate.of(2030, 1, 1); var
         * customer = new Customer().setType("RESIDENTIAL").setCategory("B2C");
         * var contract = new Contract().setId(72L).setContractPerimeter( new
         * ContractPerimeter().setPerimeter(new
         * Perimeter().setCustomer(customer))); var meterRead1 = new
         * MeterRead().setId(10L).setEndDate(endDate).setSource(MeterSource.
         * MARKET) .setTouGroup(""); var meterRead2 = new
         * MeterRead().setId(11L).setEndDate(endDate).setSource(MeterSource.
         * MARKET) .setTouGroup("TG"); var pos = new
         * PointOfService().setId("pos-id"); var posConf = new
         * PointOfServiceConfiguration().setPointOfService(pos); var posCapa =
         * new PointOfServiceCapacity().setPosId(pos.getId()); List<Long> ids =
         * Arrays.asList(1L); var rateConsum = new
         * Rate().setType("CONSUM").setPrice(new
         * BigDecimal("0.1")).setStartDate(startDate)
         * .setChannel("*").setContractServiceSubCategory( "*")
         * .setCustomerCategory("*").setCustomerType("*").
         * setInstallmentFrequency("*")
         * .setPosCategory("*").setGridRate("*").setTgoCode("*").setDgoCode(
         * "*") .setTou(TouEnum.ALL).setTouGroup("*"); var se = new
         * ServiceElement().setId(118L).setMetered(true).setOperandType(
         * "CONSTANT"). setOperand("0") .setFactorType("CONSTANT").setFactor(
         * "1")
         * .setSqType(ServiceQuantityEnum.KWH_PER_TOU).setTou(TouEnum.TOTAL_HOUR
         * ). setTouGroup("TG")
         * .setRateType(rateConsum.getType()).setVatRate("NR"); var seqt = new
         * ServiceQuantityType().setId(ServiceQuantityEnum.KWH_PER_TOU.getValue(
         * )) .setMeasureType("measureType"); var consoDetail = new
         * ConsumptionDetail().setQuantity(BigDecimal.TEN).setStartDate(
         * startDate) .setEndDate(endDate).setTou(se.getTou());
         * when(consumptionDetailRepository.findAllByMeterReadIdAndMeasureType(
         * meterRead2.getId(), seqt.getMeasureType())).thenReturn(
         * Collections.singletonList(consoDetail));
         * when(contractRepository.findById(contract.getId())).thenReturn(
         * Optional.of( contract));
         * when(meterReadRepository.findWithStatusAndContractAndSource(
         * MeterReadStatus. AVAILABLE, contract.getId(),
         * Arrays.asList(MeterSource.MARKET, MeterSource.USER)))
         * .thenReturn(Arrays.asList(meterRead1, meterRead2));
         * when(pointOfServiceConfigurationRepository.
         * findAllByContractIdAndStatus( contract.getId(), "VALIDATED"))
         * .thenReturn(Collections.singletonList(posConf));
         * when(pointOfServiceCapacityRepository.findWithContractIdAndStatus(
         * contract. getId(), "VALIDATED"))
         * .thenReturn(Collections.singletonList(posCapa));
         * when(queueProvider.consume()).thenReturn(Collections.singletonList(
         * "[{\"id\":118,\"metered\":true,\"sqType\":\"KWH_PER_TOU\",\"type\":\"CONSUMPTION\",\"category\":\"CONSUMPTION\",\"contractId\":72}]"
         * )); when(rateRepository.findAllByType(se.getRateType())).thenReturn(
         * Collections. singletonList(rateConsum));
         * when(serviceElementRepository.findById(se.getId())).thenReturn(
         * Optional.of(se ));
         * when(serviceQuantityTypeRepository.findAll()).thenReturn(Collections.
         * singletonList(seqt)); quantityService.postConstruct();
         * billingValuation.process(ids, EventExecutionMode.EVENT_MANAGER);
         * verify(billSegmentRepository,
         * atLeastOnce()).save(captorBillSegment.capture());
         * verify(meterReadRepository,
         * times(1)).updateStatus(Collections.singleton(meterRead2.getId()),
         * MeterReadStatus.BILLED); var billSegments =
         * captorBillSegment.getAllValues(); assertEquals(1,
         * billSegments.size()); assertNotNull(billSegments.get(0));
         * assertNull(billSegments.get(0).getFailureMotive());
         * assertEquals(BigDecimal.TEN, billSegments.get(0).getQuantity());
         * assertEquals(se.getTou(), billSegments.get(0).getTou());
         * assertEquals(se.getTouGroup(), billSegments.get(0).getTouGroup());
         * assertEquals(se.getVatRate(), billSegments.get(0).getVatRate());
         * assertEquals(BillSegmentStatus.CALCULATED,
         * billSegments.get(0).getStatus()); }
         */
        @Configuration
        public static class Config
        {
                @Bean
                public ObjectMapper objectMapper()
                {
                        return new ObjectMapper();
                }
        }
}
