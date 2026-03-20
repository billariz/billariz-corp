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

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import com.billariz.corp.database.model.ClimaticRef;
import com.billariz.corp.database.model.CoefA;
import com.billariz.corp.database.model.GeoFactor;
import com.billariz.corp.database.model.MarketGeoRef;
import com.billariz.corp.database.model.PointOfServiceEstimate;
import com.billariz.corp.database.model.PostalCode;
import com.billariz.corp.database.model.ServiceElement;
import com.billariz.corp.database.model.enumeration.BillFactorEnum;
import com.billariz.corp.database.repository.ClimaticRefRepository;
import com.billariz.corp.database.repository.CoefARepository;
import com.billariz.corp.database.repository.GeoFactorRepository;
import com.billariz.corp.database.repository.MarketGeoRefRepository;
import com.billariz.corp.database.repository.PointOfServiceEstimateRepository;
import com.billariz.corp.database.repository.PostalCodeRepository;
import com.billariz.corp.launcher.exception.LauncherException;
import com.billariz.corp.launcher.exception.LauncherFatalException;
import com.billariz.corp.launcher.utils.BillingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillFactorCalculationService
{
    private final ClimaticRefRepository            climaticRefRepository;

    private final CoefARepository                  coefARepository;

    private final GeoFactorRepository              geoFactorRepository;

    private final MarketGeoRefRepository           marketGeoRefRepository;

    private final PointOfServiceEstimateRepository pointOfServiceEstimateRepository;

    private final PostalCodeRepository             postalCodeRepository;

    private final BillingUtils                     billingUtils;

    public BigDecimal calculateBillFactor(QuantityObject quantity, ServiceElement serviceElement, ContractAndCustomerInfo ctfCusInfo) throws LauncherException
    {
        BillFactorEnum billFactorEnum = BillFactorEnum.fromValue(serviceElement.getFactor());

        if (billFactorEnum != null)
        {
            switch (billFactorEnum)
            {
            case CJN:
                return calculateCJN(quantity, ctfCusInfo);
            case TCCFE_FACTOR:
                return calculateTccfeFactor(quantity, ctfCusInfo);
            case TDCFE_FACTOR:
                return calculateTdcfeFactor(quantity, ctfCusInfo);
            case CJNR:
                return calculateCJNR(quantity, ctfCusInfo);
            default:
                break;
            }
        }
        log.debug("This BillFactor[{}] isn't handled in com-System.", serviceElement.getFactor());
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateTdcfeFactor(QuantityObject quantity, ContractAndCustomerInfo ctfCusInfo) throws LauncherException
    {
        if (ctfCusInfo.getPos() == null)
            throw new LauncherFatalException("MISSING_POS_OF_CONTRACT", new Object[]{ctfCusInfo.getContract().getId()});
        else if (ctfCusInfo.getPos().getAddress() == null)
            throw new LauncherFatalException("MISSING_ADDRESS", new Object[]{ctfCusInfo.getPos().getId()});
        else
        {
            var areaCode = postalCodeRepository.findByPostalCodeAndCityName(ctfCusInfo.getPos().getAddress().getPostalCode(),
                    ctfCusInfo.getPos().getAddress().getCity())
                    .map(PostalCode::getAreaCode)
                    .orElseThrow(() -> new LauncherFatalException("MISSING_AREACODE", new Object[]{ctfCusInfo.getPos().getId(),ctfCusInfo.getPos().getAddress().toString()}));
            log.debug("AreaCode -> {}", areaCode);
            var departmentalIncreaseCoefficient = geoFactorRepository.findByAreaCodeAndStartDateIsLessThanEqualAndEndDateIsGreaterThan(areaCode,
                    quantity.getStartDate(), quantity.getEndDate())
                    .map(GeoFactor::getDepartmentalIncreaseCoef)
                    .orElseThrow(() -> new LauncherFatalException("MISSING_INCREASE_COEF", new Object[]{"Departemental", areaCode}));
            return departmentalIncreaseCoefficient;
        }
    }

    private BigDecimal calculateTccfeFactor(QuantityObject quantity, ContractAndCustomerInfo ctfCusInfo) throws LauncherException
    {
        if (ctfCusInfo.getPos() == null)
            throw new LauncherFatalException("MISSING_POS_OF_CONTRACT", new Object[]{ctfCusInfo.getContract().getId()});
        else if (ctfCusInfo.getPos().getAddress() == null)
            throw new LauncherFatalException("MISSING_ADDRESS", new Object[]{ctfCusInfo.getPos().getId()});
        else
        {
            var areaCode = postalCodeRepository.findByPostalCodeAndCityName(ctfCusInfo.getPos().getAddress().getPostalCode(),
                    ctfCusInfo.getPos().getAddress().getCity())
                        .map(PostalCode::getAreaCode)
                        .orElseThrow(() -> new LauncherFatalException("MISSING_AREACODE", new Object[]{ctfCusInfo.getPos().getId(),ctfCusInfo.getPos().getAddress().toString()}));
            log.debug("AreaCode -> {}", areaCode);
            var departmentalIncreaseCoefficient = geoFactorRepository.findByAreaCodeAndStartDateIsLessThanEqualAndEndDateIsGreaterThan(areaCode,
                    quantity.getStartDate(), quantity.getEndDate())
                        .map(GeoFactor::getDepartmentalIncreaseCoef)
                        .orElseThrow(() -> new LauncherFatalException("MISSING_INCREASE_COEF", new Object[]{"Municipal", areaCode}));
            log.debug("Municipal Increase Coefficient -> {}", departmentalIncreaseCoefficient);
            return departmentalIncreaseCoefficient;
        }
    }

    private BigDecimal calculateCJN(QuantityObject quantity, ContractAndCustomerInfo ctfCusInfo) throws LauncherException
    {
        if (ctfCusInfo.getPos() == null)
            throw new LauncherFatalException("MISSING_POS_OF_CONTRACT", new Object[]{ctfCusInfo.getContract().getId()});
        else if (ctfCusInfo.getPos().getAddress() == null)
            throw new LauncherFatalException("MISSING_ADDRESS", new Object[]{ctfCusInfo.getPos().getId()});
        else
        {
            var valuePosEstimate = pointOfServiceEstimateRepository.findByContractIdAndPosIdAndStatusAndEstimateType(ctfCusInfo.getContract().getId(),
                    ctfCusInfo.getPos().getId(), "VALIDATED", "CAR")
                        .map(PointOfServiceEstimate::getValue)
                        .orElseThrow(() -> new LauncherFatalException("MISSING_POS_ESTIMATE", new Object[]{ctfCusInfo.getPos().getId(), "CAR", "VALIDATED"}));
            log.debug("PosEstimate Value -> {}", valuePosEstimate);
            
            var areaCode = postalCodeRepository.findByPostalCodeAndCityName(ctfCusInfo.getPos().getAddress().getPostalCode(),
                    ctfCusInfo.getPos().getAddress().getCity())
                        .map(PostalCode::getAreaCode)
                        .orElseThrow(() -> new LauncherFatalException("MISSING_AREACODE", new Object[]{ctfCusInfo.getPos().getId(),ctfCusInfo.getPos().getAddress().toString()}));
            log.debug("AreaCode -> {}", areaCode);
            
            var weatherChannel = marketGeoRefRepository.findByAreaCodeAndStartDateIsLessThanEqualAndEndDateIsGreaterThan(areaCode, quantity.getStartDate(),
                    quantity.getEndDate())
                        .map(MarketGeoRef::getWeatherChannelCode)
                        .orElseThrow(() -> new LauncherFatalException("MISSING_WEATHER_CHANNEL", new Object[]{areaCode}));
            log.debug("Weather Channel -> {}", weatherChannel);
            
            var posConfiguration = ctfCusInfo.getPosConfigurations().stream().map(posConf -> billingUtils.checkPosConfEndDate(posConf, quantity)).filter(
                    posConf -> billingUtils.overlappingDatesCondition(posConf.getStartDate(), posConf.getEndDate(), quantity.getStartDate(),
                            quantity.getEndDate()))
                            .findFirst()
                            .orElseThrow(() -> new LauncherFatalException("MISSING_VALIDE_POS_CONF", new Object[]{ctfCusInfo.getPos().getId()}));
            
            if (posConfiguration != null)
            {
                var zi = climaticRefRepository.findByWeatherChannelCodeAndProfile(weatherChannel, posConfiguration.getProfile())
                        .map(ClimaticRef::getZi)
                        .orElseThrow(() -> new LauncherFatalException("MISSING_VALID_ZI", new Object[]{ctfCusInfo.getPos().getId(),weatherChannel}));
                log.debug("Zi -> {}", zi);
                
                var energyNature = marketGeoRefRepository.findByAreaCodeAndStartDateIsLessThanEqualAndEndDateIsGreaterThan(areaCode, quantity.getStartDate(),
                        quantity.getEndDate())
                            .map(MarketGeoRef::getEnergyNature)
                            .orElseThrow(() -> new LauncherFatalException("MISSING_ENERGY_NATURE", new Object[]{areaCode}));
                log.debug("Energy Nature -> {}", energyNature);
                
                var coefficientA = coefARepository.findByDgoCodeAndTgoCodeAndEnergyNatureAndStartDateIsLessThanEqualAndEndDateIsGreaterThan(
                        ctfCusInfo.getPos().getDgoCode(), ctfCusInfo.getPos().getTgoCode(), energyNature, quantity.getStartDate(), quantity.getEndDate())
                        .map(CoefA::getCoefA)
                        .orElseThrow(() -> new LauncherFatalException("MISSING_COEF_A ", new Object[]{ctfCusInfo.getPos().getDgoCode()}));
                log.debug("Coefficient A -> {}", coefficientA);
                
                return valuePosEstimate.multiply(zi.multiply(coefficientA));
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateCJNR(QuantityObject quantity, ContractAndCustomerInfo ctfCusInfo) throws LauncherException
    {
        var cjn = calculateCJN(quantity, ctfCusInfo);

        log.debug("CJN -> {}", cjn);
        if (cjn != null)
        {
            var areaCode = postalCodeRepository.findByPostalCodeAndCityName(ctfCusInfo.getPos().getAddress().getPostalCode(),
                    ctfCusInfo.getPos().getAddress().getCity())
                        .map(PostalCode::getAreaCode)
                        .orElseThrow(() -> new LauncherFatalException("MISSING_AREACODE", new Object[]{ctfCusInfo.getPos().getId(),ctfCusInfo.getPos().getAddress().toString()}));
            log.debug("AreaCode -> {}", areaCode);
            var regionalRateLevel = marketGeoRefRepository.findByAreaCodeAndStartDateIsLessThanEqualAndEndDateIsGreaterThan(areaCode, quantity.getStartDate(),
                    quantity.getEndDate())
                        .map(MarketGeoRef::getRegionalRateLevel)
                        .orElseThrow(() -> new LauncherFatalException("MISSING_RATE_LEVEL", new Object[]{areaCode}));
            log.debug("Regional Rate Level -> {}", areaCode);
            return regionalRateLevel.multiply(cjn, billingUtils.getMathContext());
        }
        return BigDecimal.ZERO;
    }
}
