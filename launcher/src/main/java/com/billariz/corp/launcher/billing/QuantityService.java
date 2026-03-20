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

package com.billariz.corp.launcher.billing;

import static com.billariz.corp.launcher.utils.FilterUtils.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import com.billariz.corp.database.model.BillSegment;
import com.billariz.corp.database.model.MeterRead;
import com.billariz.corp.database.model.MeterReadDetail;
import com.billariz.corp.database.model.ServiceElement;
import com.billariz.corp.database.model.ServiceQuantityType;
import com.billariz.corp.database.model.Tou;
import com.billariz.corp.database.model.enumeration.MeterReadQuality;
import com.billariz.corp.database.model.enumeration.MeterReadSource;
import com.billariz.corp.database.model.enumeration.MeterReadStatus;
import com.billariz.corp.database.repository.MeterReadDetailRepository;
import com.billariz.corp.database.repository.MeterReadRepository;
import com.billariz.corp.database.repository.ServiceQuantityTypeRepository;
import com.billariz.corp.database.repository.TouRepository;
import com.billariz.corp.database.validator.BaseValidator;
import com.billariz.corp.launcher.exception.LauncherException;
import com.billariz.corp.launcher.exception.LauncherFatalException;
import com.billariz.corp.launcher.utils.BillingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuantityService
{

    private final MeterReadDetailRepository     consumptionDetailRepository;

    private final MeterReadRepository           meterReadRepository;

    private final ServiceQuantityTypeRepository serviceQuantityTypeRepository;

    private final TouRepository                 touRepository;

    private List<ServiceQuantityType>           serviceQuantityTypes;

    private final BillingUtils                     billingUtils;

    @PostConstruct
    public void postConstruct()
    {
        serviceQuantityTypes = serviceQuantityTypeRepository.findAll();
    }

    public List<QuantityObject> getQuantity(List<BillSegment> bss, ServiceElement seMaster, ServiceElement serviceElement, ContractAndCustomerInfo ctfAndCuInfo, LocalDate cutOffDate) throws LauncherException
    {
        List<QuantityObject> quantityObjects = new ArrayList<>();
        setServiceQuantity(bss, seMaster, serviceElement, ctfAndCuInfo, quantityObjects, cutOffDate);
        quantityObjects.sort(Comparator.comparing(QuantityObject::getStartDate));
        log.debug("Quantities: {}", quantityObjects);
        return quantityObjects;
    }

    private void setServiceQuantity(List<BillSegment> bss, ServiceElement seMaster, ServiceElement serviceElement, ContractAndCustomerInfo ctfAndCuInfo, List<QuantityObject> quantityObjects, LocalDate cutOffDate) throws LauncherException
    {
        if (BaseValidator.KWH_PER_TOU.equals(serviceElement.getSqType()))
        {
            getListReadingDetailForKwhPerTou(serviceElement, ctfAndCuInfo, quantityObjects, cutOffDate);
        }
        else if (BaseValidator.EUR_FIX.equals(serviceElement.getSqType()))
        {
            quantityObjects.add(getQuantityForEurFix(serviceElement));
        }
        else if (BaseValidator.NUMBER_OF_DAY.equals(serviceElement.getSqType()) || BaseValidator.NUMBER_OF_MONTH.equals(serviceElement.getSqType()))
        {
            ChronoUnit chronoUnit = (BaseValidator.NUMBER_OF_DAY.equals(serviceElement.getSqType())) ? ChronoUnit.DAYS : ChronoUnit.MONTHS;
            quantityObjects.add(getQuantityPerDayOrMonth(bss, seMaster, serviceElement, chronoUnit));
        }
        else if (BaseValidator.SUM_EUR_FIX_BASED_ON_SA.equals(serviceElement.getSqType()))
        {
            getSumEurFixBasedOnSa(bss, seMaster, serviceElement, quantityObjects, ctfAndCuInfo);
        }
        else if (BaseValidator.KWH_PER_DAY_PER_YEAR.equals(serviceElement.getSqType()))
        {
            quantityObjects.add(getQuantityPerDayOrMonth(bss, seMaster, serviceElement, ChronoUnit.DAYS));
        }
        else if (BaseValidator.KVA_PER_TOU_PER_YEAR.equals(serviceElement.getSqType()))
        {
            quantityObjects.add(getKvaQuantityPerTouPerYear(bss, seMaster, serviceElement, ctfAndCuInfo));
        }
        else
        {
            throw new LauncherFatalException("MISSING_SQ_TYPE", new Object[]{serviceElement.getSqType(),
                            serviceElement.getId()});
        }
    }

    private void getSumEurFixBasedOnSa(List<BillSegment> bss, ServiceElement seMaster, ServiceElement serviceElement, List<QuantityObject> quantityObjects, ContractAndCustomerInfo ctfAndCuInfo) throws LauncherException
    {
        if (serviceElement.getSeListBaseForSq() != null && !serviceElement.getSeListBaseForSq().isEmpty())
        {
            QuantityObject quantity = new QuantityObject();
            LocalDate[] billSegmentDates = new LocalDate[2];
            var seListBaseForSq = Arrays.asList(serviceElement.getSeListBaseForSq().split(","));
            var sum = bss.stream()
                    //.filter(bs -> seListBaseForSq.contains(serviceElementRepository.findById(bs.getSeId()).get().getSubCategory()))
                    .filter(bs -> seListBaseForSq.contains(bs.getSe().getSubCategory()))
                    .filter(bs -> bs.getSchema().equals(BaseValidator.BS_SCHEMA_NORMAL))
                    .map(BillSegment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            quantity.setQuantity(sum);
            quantity.setTou(serviceElement.getTou());
            quantity.setTouGroup(serviceElement.getTouGroup());
            quantity.setUnit("EUR_FIX");
            setBillSegmentDates(bss, billSegmentDates, seMaster, serviceElement);
            if (checkBillSegmentDates(serviceElement, billSegmentDates))
            {
                quantity.setStartDate(billSegmentDates[0]);
                quantity.setEndDate(billSegmentDates[1]);
            }
            quantityObjects.add(quantity);
        }
        else
        {
            throw new LauncherFatalException("MISSING_SE_BASE_FOR_SQ", new Object[]{serviceElement.getId()});
        }
    }

    private QuantityObject getKvaQuantityPerTouPerYear(List<BillSegment> bss, ServiceElement seMaster, ServiceElement serviceElement, ContractAndCustomerInfo ctfAndCuInfo) throws LauncherException
    {
        QuantityObject quantity = new QuantityObject();
        LocalDate[] billSegmentDates = new LocalDate[2];
        ServiceElement currentServiceElementMaster;

        quantity.setTou(serviceElement.getTou());
        quantity.setTouGroup(serviceElement.getTouGroup());
        quantity.setUnit(ChronoUnit.YEARS.toString());
        currentServiceElementMaster = setBillSegmentDates(bss, billSegmentDates, seMaster, serviceElement);
        if (currentServiceElementMaster != null)
        {
            setQuantityAndDates(billSegmentDates, currentServiceElementMaster, quantity, ctfAndCuInfo, ChronoUnit.DAYS);
            quantity.setQuantity(quantity.getQuantity().divide(billingUtils.quantityTimeUnitIntoYearTimeUnit(ChronoUnit.DAYS), billingUtils.getMathContext()));
        }
        else
        {
            quantity.setQuantity(BigDecimal.ZERO);
        }
        return quantity;
    }

    private void setQuantityAndDates(LocalDate[] billSegmentDates, ServiceElement currentServiceElementMaster, QuantityObject quantity, ContractAndCustomerInfo ctfAndCuInfo, ChronoUnit chronoUnit) throws LauncherException
    {
        if (checkBillSegmentDates(currentServiceElementMaster, billSegmentDates))
        {
            // BillSegmentDates -> [0] : StartDate & [1] : endDate
            log.debug("StartDate -> {} || EndDate -> {}", billSegmentDates[0], billSegmentDates[1]);
            quantity.setStartDate(billSegmentDates[0]);
            quantity.setEndDate(billSegmentDates[1]);
            if (ctfAndCuInfo.getCapacities().isEmpty())
            {
                quantity.setQuantity(new BigDecimal(chronoUnit.between(billSegmentDates[0], billSegmentDates[1])));
                log.debug("No PosCapacity Found.");
            }
            else
            {
                calculateQuantityByPosCapacity(quantity, ctfAndCuInfo, chronoUnit);
            }
        }
    }

    private void calculateQuantityByPosCapacity(QuantityObject quantity, ContractAndCustomerInfo ctfAndCuInfo, ChronoUnit chronoUnit) throws LauncherException
    {
        quantity.setQuantity(ctfAndCuInfo.getCapacities().stream().map(capacity -> billingUtils.checkPosCapacityEndDate(capacity, quantity)).filter(
                capacity -> capacity.getTou() != null
                        && ("*".equals(capacity.getTou()) || "*".equals(quantity.getTou()) || capacity.getTou().equals(quantity.getTou()))
                        && billingUtils.overlappingDatesCondition(capacity.getStartDate(), capacity.getEndDate(), quantity.getStartDate(),
                                quantity.getEndDate())).map(
                                        capacity -> new BigDecimal(chronoUnit.between(billingUtils.maxDate(capacity.getStartDate(), quantity.getStartDate()),
                                        billingUtils.minDate(capacity.getEndDate(), quantity.getEndDate()))).multiply(capacity.getValue())).reduce(
                                                        BigDecimal.ZERO, BigDecimal::add));
    }

    private QuantityObject getQuantityForEurFix(ServiceElement serviceElement) throws LauncherException
    {
        QuantityObject quantityObject = new QuantityObject();
        quantityObject.setTouGroup(serviceElement.getTouGroup());
        quantityObject.setTou(serviceElement.getTou());
        quantityObject.setStartDate(serviceElement.getStartDate());
        quantityObject.setEndDate(serviceElement.getEndDate());
        quantityObject.setQuantity(BigDecimal.ONE);
        return quantityObject;
    }

    private QuantityObject getQuantityPerDayOrMonth(List<BillSegment> bss, ServiceElement seMaster, ServiceElement serviceElement, ChronoUnit chronoUnit) throws LauncherException
    {
        QuantityObject quantityObject = new QuantityObject();
        LocalDate[] billSegmentDates = new LocalDate[2];
        ServiceElement currentServiceElementMaster;

        quantityObject.setTou(serviceElement.getTou());
        quantityObject.setTouGroup(serviceElement.getTouGroup());
        quantityObject.setUnit(chronoUnit.toString());
        currentServiceElementMaster = setBillSegmentDates(bss, billSegmentDates, seMaster, serviceElement);
        if (currentServiceElementMaster != null)
        {
            setQuantity(currentServiceElementMaster, chronoUnit, quantityObject, billSegmentDates);
        }
        else
        {
            quantityObject.setQuantity(BigDecimal.ZERO);
        }
        return quantityObject;
    }

    private ServiceElement setBillSegmentDates(List<BillSegment> bss, LocalDate[] billSegmentDates, ServiceElement seMaster, ServiceElement serviceElement) throws LauncherException
    {
        if (serviceElement.isMaster())
        {
            var billSegment = bss.stream()
                                    .filter(bs -> serviceElement.getId().equals(bs.getSeId()))
                                    .max((bs1, bs2) -> bs1.getEndDate().compareTo(bs2.getEndDate()));
            if (billSegment.isPresent())
                billSegmentDates[0] = billSegment.get().getEndDate();
            billSegmentDates[1] = LocalDate.now();
            return serviceElement;
        }
        else
        {
            if (seMaster != null)
            {
                var billSegment = bss.stream()
                                    .filter(bs -> seMaster.getId().equals(bs.getSeId()))
                                    .sorted((bs1, bs2) -> {
                                        int compareEndDate = bs1.getEndDate().compareTo(bs2.getEndDate());
                                        if (compareEndDate == 0) {
                                            return bs2.getStartDate().compareTo(bs1.getStartDate());
                                        }
                                        return compareEndDate;
                                    })
                                    .findFirst()
                                    .orElseThrow(() -> new LauncherFatalException("MISSING_BILL_SEGMENT", new Object[]{serviceElement.getId()}));
                
                billSegmentDates[0] = billSegment.getStartDate();
                billSegmentDates[1] = billSegment.getEndDate();
                
                return seMaster;
            }
            else
            {
                throw new LauncherFatalException ("MISSING_SE_MASTER", new Object[]{serviceElement.getId()});
            }
        }
    }

    private boolean checkBillSegmentDates(ServiceElement serviceElement, LocalDate[] billSegmentDates) throws LauncherException
    {
        if (serviceElement != null)
        {
            if (billSegmentDates[0] != null && billSegmentDates[1] != null)
            {
                return true;
            }
            else 
                if (serviceElement.isMaster() && LocalDate.now().isAfter(serviceElement.getStartDate()))
                {
                    billSegmentDates[0] = serviceElement.getStartDate();
                    billSegmentDates[1] = LocalDate.now();
                    return true;
                }
                else
                    {
                        throw new LauncherFatalException ("MISSING_VALIDE_SE_DATES", new Object[]{serviceElement.getId()});
                    }
        }
        else
        {
            log.debug("ServiceElement object is null.");
            
        }
        return false;
    }

    private void setQuantity(ServiceElement serviceElement, ChronoUnit chronoUnit, QuantityObject quantityObject, LocalDate[] billSegmentDates) throws LauncherException
    {
        if (checkBillSegmentDates(serviceElement, billSegmentDates))
        {
            // BillSegmentDates -> [0] : StartDate & [1] : endDate
            log.debug("StartDate -> {} || EndDate -> {}", billSegmentDates[0], billSegmentDates[1]);
            calculateQuantity(billSegmentDates, chronoUnit, serviceElement, quantityObject);
        }
        else
        {
            quantityObject.setQuantity(BigDecimal.ZERO);
        }
    }

    private void calculateQuantity(LocalDate[] billSegmentDates, ChronoUnit chronoUnit, ServiceElement serviceElement, QuantityObject quantityObject) throws LauncherException
    {
        Tou tou = touRepository.findByTouAndGroup(serviceElement.getTou(), serviceElement.getTouGroup()).orElse(null);

        log.debug("Tou -> {}", tou);
        if (tou != null && tou.getPeriodStart() != null && tou.getPeriodEnd() != null)
        {
            getTouPeriod(tou, billSegmentDates, chronoUnit, quantityObject);
        }
        else
        {
            quantityObject.setStartDate(billSegmentDates[0]);
            quantityObject.setEndDate(billSegmentDates[1]);
            quantityObject.setQuantity(new BigDecimal(chronoUnit.between(billSegmentDates[0], billSegmentDates[1])));
        }
    }

    private void getTouPeriod(Tou tou, LocalDate[] billSegmentDates, ChronoUnit chronoUnit, QuantityObject quantityObject) throws LauncherException
    {
        MonthDay mdStart = billingUtils.maxDate(MonthDay.from(tou.getPeriodStart()), MonthDay.from(billSegmentDates[0]));
        MonthDay mdEnd = billingUtils.minDate(MonthDay.from(tou.getPeriodEnd()), MonthDay.from(billSegmentDates[1]));
        if (mdStart != null && mdEnd != null)
        {
            LocalDate startDate = LocalDate.of(billSegmentDates[0].getYear(), mdStart.getMonth(), mdStart.getDayOfMonth());
            LocalDate endDate = LocalDate.of(billSegmentDates[1].getYear(), mdEnd.getMonth(), mdEnd.getDayOfMonth());
            quantityObject.setStartDate(startDate);
            quantityObject.setEndDate(endDate);
            if (ChronoUnit.DAYS.equals(chronoUnit))
            {
                // The YEAR value is useless in this calculations that's why we
                // gave it a default value of 2000.
                quantityObject.setQuantity(new BigDecimal(chronoUnit.between(startDate, endDate)));
            }
            else if (ChronoUnit.MONTHS.equals(chronoUnit))
            {
                quantityObject.setQuantity(new BigDecimal(chronoUnit.between(startDate, endDate)));
            }
        }
        else
        {
            quantityObject.setQuantity(BigDecimal.ZERO);
        }
    }

    private void getListReadingDetailForKwhPerTou(ServiceElement serviceElement, ContractAndCustomerInfo ctfAndCuInfo, List<QuantityObject> quantityObjects, LocalDate cutOffDate) throws LauncherException
    {
        ServiceQuantityType serviceQuantityType = serviceQuantityTypes.stream().filter(
                sq -> serviceElement.getSqType().equals(sq.getId())).findFirst()
                    .orElseThrow(() -> new LauncherFatalException("MISSING_SQ_TYPE", new Object[]{serviceElement.getSqType(), serviceElement.getId()}));

        if (serviceElement.isMetered())
                quantityObjects.addAll(makeQuantitiesForKwhPerTou(serviceElement, serviceQuantityType, ctfAndCuInfo.getContract().getId(), cutOffDate));
        else
            {
                throw new LauncherFatalException ("SQ_TYPE_CONDITION_MISMATCH", new Object[]{serviceElement.getId(), serviceElement.isMetered(), serviceElement.getSqType()});
            }
    }

    private List<QuantityObject> makeQuantitiesForKwhPerTou(ServiceElement serviceElement, ServiceQuantityType serviceQuantityType, Long contractId, LocalDate cutOffDate)
    {
        List<QuantityObject> quantityObjects = new ArrayList<>();
        var allMeterReadList = meterReadRepository.findWithStatusAndContractAndSource(MeterReadStatus.AVAILABLE, contractId,cutOffDate,
                Arrays.asList(MeterReadSource.MARKET, MeterReadSource.USER, MeterReadSource.SYSTEM)).stream().filter(
                        meterRead -> filter(serviceElement.getTouGroup(), meterRead.getTouGroup())).collect(Collectors.toList());
                        
        log.debug("ALL MeterRead From Database: {}", allMeterReadList);
        List<MeterRead> meterReadListMarket = allMeterReadList.parallelStream().filter(
                meterRead -> MeterReadSource.MARKET.equals(meterRead.getSource())).collect(Collectors.toList());
        log.debug("Market MeterRead: {}", meterReadListMarket);
        LocalDate meterReadListMarketMaxEndDate = getMaxLocalDate(meterReadListMarket);
        log.debug("Market MeterReadList MaxEndDate -> {}", meterReadListMarketMaxEndDate);
        List<MeterRead> meterReadListCustomer = allMeterReadList.parallelStream().filter(meterRead -> MeterReadSource.USER.equals(meterRead.getSource())
                && MeterReadQuality.CUSTOMER_READING.equals(meterRead.getQuality()) && maxDateCondition(meterRead, meterReadListMarketMaxEndDate)).collect(
                        Collectors.toList());
        log.debug("Customer MeterRead: {}", meterReadListCustomer);
        LocalDate meterReadListCustomerMaxEndDate = getMaxLocalDate(meterReadListCustomer);
        log.debug("Customer MeterReadList MaxEndDate -> {}", meterReadListCustomerMaxEndDate);
        List<MeterRead> meterReadListEstimate = allMeterReadList.parallelStream().filter(meterRead -> MeterReadSource.SYSTEM.equals(meterRead.getSource())
                && MeterReadQuality.ESTIMATED.equals(meterRead.getQuality()) && maxDateCondition(meterRead, meterReadListCustomerMaxEndDate)).collect(
                        Collectors.toList());
        log.debug("Estimate MeterRead: {}", meterReadListCustomer);
        filterOverlappingReading(meterReadListEstimate, meterReadListCustomer);
        filterOverlappingReading(meterReadListCustomer, meterReadListMarket);
        meterReadListMarket.sort(Comparator.comparing(MeterRead::getStartDate));

        for (MeterRead meterRead : meterReadListMarket)
        {
            quantityObjects.addAll(
                    consumptionDetailRepository.findAllByMeterReadIdAndMeasureType(meterRead.getId(), serviceQuantityType.getMeasureType()).stream().filter(
                            consumptionDetail -> filter(serviceElement.getTou(), consumptionDetail.getTou())).map(
                                    consumptionDetail -> mapQuantityObject(consumptionDetail, meterRead)).collect(Collectors.toList()));
        }
        return quantityObjects;
    }

    private QuantityObject mapQuantityObject(MeterReadDetail consumptionDetail, MeterRead meterRead)
    {
        var quantityObject = new QuantityObject();

        quantityObject.setQuantity(consumptionDetail.getQuantity());
        quantityObject.setEndDate(consumptionDetail.getEndDate());
        quantityObject.setTou(consumptionDetail.getTou());
        quantityObject.setTouGroup(meterRead.getTouGroup());
        quantityObject.setStartDate(consumptionDetail.getStartDate());
        quantityObject.setUnit(consumptionDetail.getUnit());
        quantityObject.setQuality(meterRead.getQuality());
        quantityObject.setMeterReadId(meterRead.getId());
        return quantityObject;
    }

    private boolean maxDateCondition(MeterRead meterRead, LocalDate maxDate)
    {
        return (maxDate == null || (meterRead.getEndDate() != null && meterRead.getEndDate().isAfter(maxDate)));
    }

    private LocalDate getMaxLocalDate(List<MeterRead> meterReads)
    {
        return meterReads.stream().filter(meterRead -> meterRead.getEndDate() != null).map(MeterRead::getEndDate).max(LocalDate::compareTo).orElse(null);
    }

    private void filterOverlappingReading(List<MeterRead> firstMeterReadsList, List<MeterRead> secondMeterReadsList)
    {
        firstMeterReadsList.forEach(meterRead -> checkOverlapping(meterRead, secondMeterReadsList));
    }

    private void checkOverlapping(MeterRead meterRead, List<MeterRead> meterReadList)
    {
        if (meterReadList.stream().noneMatch(meterReadStream -> overlappingCondition(meterReadStream, meterRead)))
        {
            meterReadList.add(meterRead);
        }
    }

    private boolean overlappingCondition(MeterRead meterReadFirst, MeterRead meterReadSecond)
    {
        return billingUtils.overlappingDatesCondition(meterReadFirst.getStartDate(), meterReadFirst.getEndDate(), meterReadSecond.getStartDate(),
                meterReadSecond.getEndDate());
    }
}
