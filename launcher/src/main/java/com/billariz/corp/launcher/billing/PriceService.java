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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import com.billariz.corp.database.model.PointOfService;
import com.billariz.corp.database.model.PointOfServiceCapacity;
import com.billariz.corp.database.model.PointOfServiceConfiguration;
import com.billariz.corp.database.model.PostalCode;
import com.billariz.corp.database.model.Rate;
import com.billariz.corp.database.model.ServiceElement;
import com.billariz.corp.database.repository.GeoFactorRepository;
import com.billariz.corp.database.repository.PostalCodeRepository;
import com.billariz.corp.database.repository.RateRepository;
import com.billariz.corp.launcher.exception.LauncherException;
import com.billariz.corp.launcher.utils.BillingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceService
{
    private final BillFactorCalculationService billFactorCalculationService;

    private final GeoFactorRepository          geoFactorRepository;

    private final PostalCodeRepository         postalCodeRepository;

    private final RateRepository               rateRepository;

    private final String                       CONSTANT    = "constant";

    private final String                       PERCENTAGE  = "percent";

    private final String                       RATE_FACTOR = "rateFactor";

    private final BillingUtils                     billingUtils;

    public List<PriceObject> getPrice(QuantityObject quantity, ServiceElement serviceElement, ContractAndCustomerInfo ctfAndCuInfo) throws LauncherException
    {
        List<Rate> rates = getRates(quantity, serviceElement, ctfAndCuInfo);
        List<PriceObject> prices = new ArrayList<>();

        log.debug("Selected Rates for service element {}: {}", serviceElement, rates);
        if (rates.isEmpty())
            return Collections.emptyList();
        for (Rate rate : rates)
        {
            if (rate.getUnit().equals("EUR_PER_KVA_PER_YEAR"))
                prices.addAll(defineEurPerKvaPerYearPrices(quantity, ctfAndCuInfo, rate, serviceElement));
            else
                prices.add(mapIndex(rate, quantity, serviceElement, ctfAndCuInfo));
        }
        return prices.stream().sorted(Comparator.comparing(PriceObject::getStartDate)).toList();
    }

    private List<PriceObject> defineEurPerKvaPerYearPrices(QuantityObject quantity, ContractAndCustomerInfo ctfAndCuInfo, Rate rate, ServiceElement serviceElement) throws LauncherException
    {
        List<PriceObject> priceObjects = new ArrayList<>();
        for (PointOfServiceCapacity capacity : ctfAndCuInfo.getCapacities())
        {
            billingUtils.checkPosCapacityEndDate(capacity, quantity);
            if (billingUtils.overlappingDatesCondition(rate.getStartDate(), rate.getEndDate(), capacity.getStartDate(), capacity.getEndDate()))
            {
                priceObjects.add(mapIndex(rate, quantity, serviceElement, ctfAndCuInfo, billingUtils.maxDate(rate.getStartDate(), capacity.getStartDate()),
                billingUtils.minDate(rate.getEndDate(), capacity.getEndDate()), rate.getPrice().multiply(capacity.getValue())));
            }
        }
        return priceObjects;
    }

    private BigDecimal getOperand(ServiceElement serviceElement)
    {
        if (CONSTANT.equalsIgnoreCase(serviceElement.getOperandType()))
        {
            return billingUtils.stringToBigDecimal(serviceElement.getOperand());
        }
        else if (PERCENTAGE.equalsIgnoreCase(serviceElement.getOperandType()))
        {
            return billingUtils.stringToBigDecimal(serviceElement.getOperand()).divide(billingUtils.getPercentage(), billingUtils.getMathContext());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal getFactor(ServiceElement serviceElement, QuantityObject quantity, ContractAndCustomerInfo ctfAndCuInfo) throws LauncherException
    {
        if (CONSTANT.equalsIgnoreCase(serviceElement.getFactorType()))
            return billingUtils.stringToBigDecimal(serviceElement.getFactor());
        else if (PERCENTAGE.equalsIgnoreCase(serviceElement.getFactorType()))
            return billingUtils.stringToBigDecimal(serviceElement.getFactor()).divide(billingUtils.getPercentage(), billingUtils.getMathContext());
        else if (RATE_FACTOR.equalsIgnoreCase(serviceElement.getFactorType()))
            return billFactorCalculationService.calculateBillFactor(quantity, serviceElement, ctfAndCuInfo);
        else if (serviceElement.getFactorType() != null && !serviceElement.getFactorType().isEmpty())
            return BigDecimal.ONE;
        return BigDecimal.ZERO;
    }

    private List<Rate> getRates(QuantityObject quantity, ServiceElement serviceElement, ContractAndCustomerInfo ctfAndCuInfo)
    {
        List<Rate> rates = new ArrayList<>();
        var rateLog = new StringBuilder();

        for (Rate rate : rateRepository.findAllByType(serviceElement.getRateType()))
        {
            rateLog.append("\nRate -> ").append(rate.toString());
            if (rateFilter(quantity, rate, ctfAndCuInfo, rateLog))
            {
                rates.add(rate);
            }
        }
        log.debug("{}", rateLog);
        return rates;
    }

    private boolean rateFilter(QuantityObject quantity, Rate rate, ContractAndCustomerInfo ctfAndCuInfo, StringBuilder rateLog)
    {
        return (checkTouAndTouGroup(quantity, rate) && filter(rate.getChannel(), ctfAndCuInfo.getContract().getChannel())
                && filter(rate.getServiceCategory(), ctfAndCuInfo.getContract().getServiceCategory())
                && filter(rate.getCustomerCategory(), ctfAndCuInfo.getCustomerCategory()) && filter(rate.getCustomerType(), ctfAndCuInfo.getCustomerType())
                && filter(rate.getInstallmentFrequency(), ctfAndCuInfo.getContract().getInstallPeriodicity())
                && checkPosAndPosCategoryInfo(rate, ctfAndCuInfo.getPosConfigurations(), ctfAndCuInfo.getPos(), rateLog)
                && handleThreshold(quantity, rate, ctfAndCuInfo, rateLog) && checkRateAndQuantityDates(quantity, rate, rateLog));
    }

    private boolean checkFilter(boolean filter, String param, StringBuilder rateLog)
    {
        if (!filter)
        {
            rateLog.append("\n").append(param).append("'s filter returned false.");
            return false;
        }
        return true;
    }

    private boolean checkRateAndQuantityDates(QuantityObject quantity, Rate rate, StringBuilder rateLog)
    {
        if (!billingUtils.overlappingDatesCondition(quantity.getStartDate(), quantity.getEndDate(), rate.getStartDate(),
                rate.getEndDate() == null ? quantity.getEndDate() : rate.getEndDate()))
        {
            rateLog.append("\nThe Quantity and the rate's dates weren't overlapping.");
            return false;
        }
        return true;
    }

    private boolean checkTouAndTouGroup(QuantityObject quantity, Rate rate)
    {
        if (rate.getTou() != null && rate.getTouGroup() != null)
            return filter(rate.getTou(), quantity.getTou()) && filter(rate.getTouGroup(), quantity.getTouGroup());
        return false;
    }

    private boolean handleThreshold(QuantityObject quantity, Rate rate, ContractAndCustomerInfo ctfAndCuInfo, StringBuilder rateLog)
    {
        String absolute = "absolute";

        if ("capacity".equalsIgnoreCase(rate.getThresholdBase()))
        {
            if (!ctfAndCuInfo.getCapacities().isEmpty())
            {
                if (absolute.equalsIgnoreCase(rate.getThresholdType()))
                {
                    return thresholdCapacityAbsolute(quantity, rate, ctfAndCuInfo, rateLog);
                }
            }
            else
            {
                rateLog.append("\nNo PosCapacity Found.");
                return false;
            }
        }
        else if ("rateLevel".equalsIgnoreCase(rate.getThresholdBase()) && "absolute".equalsIgnoreCase(rate.getThresholdType())) {
            return thresholdRateLevelAbsolute(quantity, rate, ctfAndCuInfo, rateLog);
        }
        return true;
    }

    private boolean thresholdCapacityAbsolute(QuantityObject quantity, Rate rate, ContractAndCustomerInfo ctfAndCuInfo, StringBuilder rateLog)
    {
        rateLog.append("\nFiltering throw Threshold Capacity Type ABSOLUTE.");
        return handleThresholdCapacityAbsolute(quantity, rate, ctfAndCuInfo, rateLog);
    }

    private boolean thresholdRateLevelAbsolute(QuantityObject quantity, Rate rate, ContractAndCustomerInfo ctfAndCuInfo, StringBuilder rateLog)
    {
        rateLog.append("\nFiltering throw Threshold RateLevel Type Absolute.");
        var areaCode = postalCodeRepository.findByPostalCodeAndCityName(ctfAndCuInfo.getPos().getAddress().getPostalCode(),
                ctfAndCuInfo.getPos().getAddress().getCity()).map(PostalCode::getAreaCode).orElse("");
        rateLog.append("\nAreaCode -> ").append(areaCode);
        var rateLevel = geoFactorRepository.findByAreaCodeAndStartDateIsLessThanEqualAndEndDateIsGreaterThan(areaCode, quantity.getStartDate(),
                quantity.getEndDate());
        rateLog.append("\nRateLevel -> ").append(rateLevel);
        if (rateLevel.isPresent())
        {
            return (rate.getThreshold().compareTo(rateLevel.get().getDepartmentalIncreaseCoef()) >= 0);
        }
        else
        {
            rateLog.append("\nNo RateLevel found.");
            return false;
        }
    }

    private boolean handleThresholdCapacityAbsolute(QuantityObject quantity, Rate rate, ContractAndCustomerInfo contractAndCustomerInfo, StringBuilder rateLog)
    {
        return contractAndCustomerInfo.getCapacities().stream().peek(capacity -> rateLog.append("\nCapacity -> ").append(capacity)).map(
                capacity -> billingUtils.checkPosCapacityEndDate(capacity, quantity)).anyMatch(
                        capacity -> filter(rate.getTou(), capacity.getTou())
                                && billingUtils.overlappingDatesCondition(capacity.getStartDate(), capacity.getEndDate(), quantity.getStartDate(),
                                        quantity.getEndDate(), rateLog)
                                && (capacity.getValue() != null && capacity.getValue().compareTo(rate.getThreshold()) == 0));
    }

    private boolean checkPosAndPosCategoryInfo(Rate rate, List<PointOfServiceConfiguration> posConfigurations, PointOfService pointOfService, StringBuilder rateLog)
    {
        return posConfigurations.stream().anyMatch(info -> checkFilter(filter(rate.getPosCategory(), info.getPosCategory()), "PosCategory", rateLog)
                && checkFilter(filter(rate.getGridRate(), info.getGridRate()), "GridRate", rateLog)
                && checkFilter(filter(rate.getTgoCode(), pointOfService.getTgoCode()), "TgoCode", rateLog)
                && checkFilter(filter(rate.getDgoCode(), pointOfService.getDgoCode()), "DgoCode", rateLog));
    }

    private PriceObject mapIndex(Rate rate, QuantityObject quantity, ServiceElement serviceElement, ContractAndCustomerInfo ctfAndCuInfo) throws LauncherException
    {
        var priceObject = new PriceObject();

        priceObject.setTou(rate.getTou());
        priceObject.setTouGroup(rate.getTouGroup());
        priceObject.setStartDate(rate.getStartDate());
        priceObject.setEndDate(rate.getEndDate());
        priceObject.setUnit(rate.getUnit());
        priceObject.setPrice(billingUtils.calculatePrice(getOperand(serviceElement), getFactor(serviceElement, quantity, ctfAndCuInfo), rate.getPrice()));
        priceObject.setThreshold(rate.getThreshold());
        priceObject.setThresholdBase(rate.getThresholdBase());
        return priceObject;
    }

    private PriceObject mapIndex(Rate rate, QuantityObject quantity, ServiceElement serviceElement, ContractAndCustomerInfo ctfAndCuInfo, LocalDate startDate, LocalDate endDate, BigDecimal price) throws LauncherException
    {
        var priceObject = new PriceObject();

        priceObject.setTou(rate.getTou());
        priceObject.setTouGroup(rate.getTouGroup());
        priceObject.setStartDate(startDate);
        priceObject.setEndDate(endDate);
        priceObject.setUnit("EURO_BY_YEAR");
        priceObject.setPrice(billingUtils.calculatePrice(getOperand(serviceElement), getFactor(serviceElement, quantity, ctfAndCuInfo), price));
        priceObject.setThreshold(rate.getThreshold());
        priceObject.setThresholdBase(rate.getThresholdBase());
        return priceObject;
    }
}
