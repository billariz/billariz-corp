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

package com.billariz.corp.launcher.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import com.billariz.corp.database.model.BillSegment;
import com.billariz.corp.database.model.BillingRun;
import com.billariz.corp.database.model.PointOfServiceCapacity;
import com.billariz.corp.database.model.PointOfServiceConfiguration;
import com.billariz.corp.database.model.ServiceElementType;
import com.billariz.corp.database.model.enumeration.BillSegmentStatus;
import com.billariz.corp.database.model.enumeration.BillingValuationBase;
import com.billariz.corp.database.model.enumeration.ServiceElementTypeCategory;
import com.billariz.corp.database.model.light.EligibleServiceElement;
import com.billariz.corp.database.repository.BillSegmentRepository;
import com.billariz.corp.database.repository.ParameterRepository;
import com.billariz.corp.launcher.billing.PriceObject;
import com.billariz.corp.launcher.billing.QuantityObject;
import com.billariz.corp.launcher.exception.LauncherException;
import com.billariz.corp.launcher.exception.LauncherFatalException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingUtils
{
    private final BillSegmentRepository    billSegmentRepository;

    private final ParameterRepository parameterRepository;

    @Getter
    private final MathContext mathContext = new MathContext(10);

    @Getter
    private final BigDecimal  percentage  = new BigDecimal("100");

    public BigDecimal calculateAmount(BigDecimal quantity, BigDecimal price)
    {
        return quantity.multiply(price);
    }

    public BigDecimal calculatePrice(BigDecimal operand, BigDecimal factor, BigDecimal price)
    {
        return operand.add(factor.multiply(price, mathContext));
    }

    public boolean overlappingDatesCondition(LocalDate firstStartDate, LocalDate firstEndDate, LocalDate secondStartDate, LocalDate secondEndDate, StringBuilder rateLog)
    {
        if (!(firstStartDate.isBefore(secondEndDate) && secondStartDate.isBefore(firstEndDate)))
        {
            rateLog.append("\nThe given dates are not overlapping.");
            return false;
        }
        return true;
    }

    public boolean overlappingDatesCondition(LocalDate firstStartDate, LocalDate firstEndDate, LocalDate secondStartDate, LocalDate secondEndDate)
    {
        if (!(firstStartDate.isBefore(secondEndDate) && secondStartDate.isBefore(firstEndDate)))
        {
            return false;
        }
        return true;
    }

    public LocalDate minDate(LocalDate first, LocalDate second)
    {
        return (first.isBefore(second) ? first : second);
    }

    public LocalDate maxDate(LocalDate first, LocalDate second)
    {
        return (first.isAfter(second) ? first : second);
    }

    public MonthDay minDate(MonthDay first, MonthDay second)
    {
        return (first.isBefore(second) ? first : second);
    }

    public MonthDay maxDate(MonthDay first, MonthDay second)
    {
        return (first.isAfter(second) ? first : second);
    }

    public String setQuantityAndPriceUnit(String quantityUnit, PriceObject priceObject)
    {
        try
        {
            log.debug("Quantity Unit -> {}", quantityUnit);
            ChronoUnit chronoUnit = ChronoUnit.valueOf(quantityUnit.toUpperCase());
            switch (chronoUnit)
            {
            case DAYS:
                setPriceValue(priceObject, quantityTimeUnitIntoYearTimeUnit(ChronoUnit.DAYS));
                return "DAY";
            case YEARS:
                setPriceValue(priceObject, BigDecimal.ONE);
                return "YEAR";
            case WEEKS:
                setPriceValue(priceObject, quantityTimeUnitIntoYearTimeUnit(ChronoUnit.WEEKS));
                return "WEEK";
            case MONTHS:
                setPriceValue(priceObject, quantityTimeUnitIntoYearTimeUnit(ChronoUnit.MONTHS));
                return "MONTH";
            case HOURS:
                setPriceValue(priceObject, quantityTimeUnitIntoYearTimeUnit(ChronoUnit.HOURS));
                return "HOUR";
            default:
                return quantityUnit;
            }

        }
        catch (Exception ignoreException)
        {
            return quantityUnit;
        }
    }

    public BigDecimal quantityTimeUnitIntoYearTimeUnit(ChronoUnit chronoUnit)
    {
        switch (chronoUnit)
        {
        case DAYS:
            return new BigDecimal("365");
        case YEARS:
            return BigDecimal.ONE;
        case WEEKS:
            return new BigDecimal("52.1429");
        case MONTHS:
            return new BigDecimal("11.999996713319");
        case HOURS:
            return new BigDecimal("8760");
        default:
            return new BigDecimal(1);
        }
    }

    public void setPriceValue(PriceObject priceObject, BigDecimal divisor)
    {
        if (priceObject.getUnit().equalsIgnoreCase("EURO_BY_YEAR")) {
            priceObject.setPrice(priceObject.getPrice().divide(divisor, mathContext));
            log.debug("Final Price Value For PriceUnit[{}] -> {}", priceObject.getUnit(), priceObject.getPrice());
        }
    }

    public PointOfServiceCapacity checkPosCapacityEndDate(PointOfServiceCapacity posCapacity, QuantityObject quantity)
    {
        if (posCapacity.getEndDate() == null)
            posCapacity.setEndDate(quantity.getEndDate());
        return posCapacity;
    }

    public PointOfServiceConfiguration checkPosConfEndDate(PointOfServiceConfiguration posConfiguration, QuantityObject quantity)
    {
        if (posConfiguration.getEndDate() == null)
            posConfiguration.setEndDate(quantity.getEndDate());
        return posConfiguration;
    }

    public BigDecimal calculatePercentage(BigDecimal nbr, BigDecimal percentageRate)
    {
        return nbr.multiply(percentageRate).divide(new BigDecimal("100"), getMathContext());
    }

    public BigDecimal stringToBigDecimal(String value)
    {
        if (value == null || value.isEmpty())
        {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.replace(',', '.'));
    }

    public boolean checkIfBillSegmentExist(List<EligibleServiceElement> seList, BillingRun billingRun, BillingValuationBase valuationBase) throws LauncherException
    {
        var windowOffset = parameterRepository.findAllByTypeAndNameAndStartDateBefore("BILLING_CYCLE", 
                                        "windowOffset", LocalDate.now());
        List<BillSegmentStatus> bsStatusExcluded = new ArrayList<>(Arrays.asList(BillSegmentStatus.CANCELLED, BillSegmentStatus.ERROR));
        if(windowOffset.isEmpty())
                throw new LauncherFatalException("MISSING_PARAMETER",new Object[]{"windowOffset"});
        var offset= Long.parseLong(windowOffset.get(0).getValue());
        var check = false;
        
        for (EligibleServiceElement se : seList) {
            check = checkBillSegment(se, valuationBase, billingRun.getStartDate(), billingRun.getEndDate() , offset, bsStatusExcluded);
        }
        return check;
    }

    public boolean checkIfBillSegmentExist(List<EligibleServiceElement> seList, LocalDate cutOffDate, 
                                                    BillingValuationBase valuationBase) throws LauncherException
    {
        List<BillSegmentStatus> bsStatusExcluded = new ArrayList<>(Arrays.asList(BillSegmentStatus.CANCELLED, BillSegmentStatus.ERROR));
        var check = false;
        for (EligibleServiceElement se : seList) {
            check = checkBillSegment(se, valuationBase, se.getStartDate(), cutOffDate, 1L, bsStatusExcluded);
        }
        return check;
    }

    private boolean checkBillSegment(EligibleServiceElement se, BillingValuationBase valuationBase, LocalDate sd, LocalDate ed, 
                                        Long offset, List<BillSegmentStatus> bsStatusExcluded) {
        return billSegmentRepository.findFirstBySeIdAndStatusNotInOrderByEndDateDesc(se.getId(), bsStatusExcluded)
            .stream()
            .anyMatch(bs -> isWithinDateRange(bs, valuationBase, sd, ed, offset));
    }

    private boolean isWithinDateRange(BillSegment bs, BillingValuationBase valuationBase, LocalDate sd, LocalDate ed, Long offset) {

        LocalDate referenceStartDate = valuationBase.equals(BillingValuationBase.FINAL_FULL)
            ? bs.getSe().getEndDate()
            : sd;
        LocalDate referenceEndDate = valuationBase.equals(BillingValuationBase.FINAL_FULL)
            ? bs.getSe().getEndDate()
            : ed;
        
        return bs.getEndDate().isAfter(referenceStartDate.minusDays(offset)) 
                            && bs.getEndDate().isBefore(referenceEndDate.plusDays(offset));

    }

    public Boolean isCutOffDateBased(BillingValuationBase bv)
    {
        if(bv.equals(BillingValuationBase.CUTOFF_DATE_BC) || bv.equals(BillingValuationBase.CUTOFF_DATE_FULL) 
                    || bv.equals(BillingValuationBase.CUTOFF_DATE_CONSUMPTION) || bv.equals(BillingValuationBase.CUTOFF_DATE_SUBSCRIPTION))
            return true;
        else return false;
    }

    // Conditions pour le traitement des différents cas
    public boolean shouldProcessForBillingCharge(ServiceElementType seType, BillingValuationBase valuationBase) {
        return seType.getSeTypeCategory().equals(ServiceElementTypeCategory.BILLABLE_CHARGE) &&
            (valuationBase.equals(BillingValuationBase.BILLABLE_CHARGE) 
            || valuationBase.equals(BillingValuationBase.CUTOFF_DATE_BC)
            || valuationBase.equals(BillingValuationBase.CUTOFF_DATE_FULL) 
            || valuationBase.equals(BillingValuationBase.FULL) 
            || valuationBase.equals(BillingValuationBase.FINAL_FULL));
    }

    public boolean shouldProcessForConsumption(ServiceElementType seType, BillingValuationBase valuationBase) {
        return seType.isMetered() &&
            (valuationBase.equals(BillingValuationBase.CONSUMPTION) 
            || valuationBase.equals(BillingValuationBase.CUTOFF_DATE_CONSUMPTION) 
            || valuationBase.equals(BillingValuationBase.CUTOFF_DATE_FULL) 
            || valuationBase.equals(BillingValuationBase.FULL) 
            || valuationBase.equals(BillingValuationBase.FINAL_FULL));
    }

    public boolean shouldProcessForSubscription(ServiceElementType seType, BillingValuationBase valuationBase) {
        return seType.getSeTypeCategory().equals(ServiceElementTypeCategory.SUBSCRIPTION) &&
            (valuationBase.equals(BillingValuationBase.SUBSCRIPTION) 
            || valuationBase.equals(BillingValuationBase.CUTOFF_DATE_SUBSCRIPTION) 
            || valuationBase.equals(BillingValuationBase.FULL) 
            || valuationBase.equals(BillingValuationBase.CUTOFF_DATE_FULL) 
            || valuationBase.equals(BillingValuationBase.FINAL_FULL));
    }

}
