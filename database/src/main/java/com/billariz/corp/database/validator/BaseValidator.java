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

package com.billariz.corp.database.validator;

import java.time.LocalDate;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import com.billariz.corp.database.model.Address;
import com.billariz.corp.database.model.FinancialInformation;
import com.billariz.corp.database.repository.ParameterRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseValidator implements Validator
{
    public static final String PRINTSHOP_AGENT = "printShopAgent";

    public static final String EVENT_MANAGER = "eventManager";

    public static final String SYSTEM_AGENT = "systemAgent";

    public static final String BILL_LINE = "BILL_LINE";

    public static final String UNIT_PERCENTAGE = "PERCENT";

    public static final String INFO_LINE = "INFO_LINE";

    public static final String BS_SCHEMA_NORMAL = "NORMAL";

    public static final String BS_SCHEMA_REVERSE = "REVERSE";

    public static final String        KWH_PER_TOU           = "KWH_PER_TOU";

    public static final String        EUR_FIX           = "EUR_FIX";

    public static final String        NUMBER_OF_DAY           = "NUMBER_OF_DAY";

    public static final String        NUMBER_OF_MONTH           = "NUMBER_OF_MONTH";

    public static final String        SUM_EUR_FIX_BASED_ON_SA           = "SUM_EUR_FIX_BASED_ON_SA";

    public static final String        KWH_PER_DAY_PER_YEAR           = "KWH_PER_DAY_PER_YEAR";

    public static final String        KVA_PER_TOU_PER_YEAR           = "KVA_PER_TOU_PER_YEAR";

    public static final String         ACTOR_PAYER      ="PAYER";

    public static final String        BILLING_MODE           = "billingMode";

    public static final String        CONTRACT_STATUS        = "contractStatus";

    public static final String        CONTRACT_STATUS_TERMINATION        = "contractStatusTermination";

    public static final String        CUSTOMER_CATEGORY      = "customerCategory";

    public static final String        CUSTOMER_STATUS        = "customerStatus";

    public static final String        CUSTOMER_TYPE          = "customerType";

    public static final String        PAYMENT_MODE_CODE        = "paymentModeCode";

    public static final String        LANGUAGE_CODE          = "language";

    public static final String        MARKET                 = "market";

    public static final String        PAYMENT_MODE           = "paymentMode";

    public static final String        SERVICE_ELEMENT_STATUS = "serviceElementStatus";

    public static final String        THIRD_TYPE             = "thirdType";

    public static final String        RELATION_TYPE             = "relationType";

    public static final String        VAT_RATE_INDEX             = "vatRateIndex";

    public static final String        VAT_RATE             = "vatRate";

    public static final String        PRICE_TYPE             = "priceType";

    public static final String        CHANNEL             = "channel";

    public static final String        INSTALLMENT_FREQUENCY   ="installmentFrequency";

    public static final String        SERVICE_CATEGORY   ="serviceCategory";

    public static final String        SERVICE_SUB_CATEGORY   ="serviceSubCategory";

    public static final String        PO_CATEGORY   ="posCategory";

    public static final String        TOU   ="tou";

    public static final String        TOU_GROUP   ="touGroup";

    public static final String        GRID_RATE   ="gridRate";

    public static final String        DGO_CODE   ="dgoCode";

    public static final String        TGO_CODE   ="tgoCode";

    public static final String        PRICE_UNIT   ="priceUnit";

    public static final String        THRESHOLD_TYPE   ="thresholdType";

    public static final String        THRESHOLD_BASE   ="thresholdBase";

    public static final String        DIRECTION   ="direction";

    public static final String        SELLER   ="seller";

    public static final String        DELIVERY_STATE   ="deliveryState";

    public static final String        DELIVERY_STATUS   ="deliveryStatus";

    public static final String        PRICE_MODE   ="priceMode";

    public static final String        REF_DATE_FIXED_PRICE   ="refDateTypeForFixedPrice";

    public static final String        SQ_TYPE   ="sqType";

    public static final String        OPERAND_TYPE   ="operandType";

    public static final String        FACTOR_TYPE   ="factorType";

    public static final String        ACCOUNTING_SCHEME   ="accountingScheme";

    public static final String        BILLING_SCHEME   ="billingScheme";

    public static final String        BILLING_SCHEME_NORMAL   ="BILLABLE";

    public static final String        BILLING_SCHEME_REVERSE   ="NOT_BILLABLE";

    public static final String        NACE_CODE   ="naceCode";

    public static final String        LEGAL_FORME   ="legalForm";

    public static final String        ORGANISM_CATEGORY   ="entityCategory";

    public static final String        ORGANISM_SUB_CATEGORY   ="entitySubCategory";

    public static final String        GROUP_CATEGORY   ="groupCategory";

    public static final String        GROUP_SUB_CATEGORY   ="groupSubCategory";

    public static final String        USER_ROLE   ="userRole";

    public static final String        TITLE_CODE   ="titleCode";

    public static final String        BILLING_FREQUENCY   ="billingFrequency";

    public static final String        PERIMETER_TYPE   ="perimeterType";

    public static final String        PERIMETER_STATUS   ="perimeterStatus";

    public static final String          ROLE ="actorRole";

    public static final String          DELIVERY_MODE_CODE="deliveryModeCode";

    public static final String          SOURCE="posDataSource";

    public static final String          POS_DATA_STATUS="posDataStatus";

    public static final String          CAPACITY_TYPE="capacityType";

    public static final String          READING_FREQUENCY="readingFrequency";

    public static final String          BUSINESS_GRID_CODE="businessGridCode";

    public static final String          MARKET_GRID_CODE="marketGridCode";

    public static final String          PROFILE="profile";

    public static final String          ESTIMATE_TYPE="estimateType";

    public static final String          METER_TYPE="meterType";

    public static final String          SMART_METER_STATUS="smartMeterStatus";

    public static final String          DOMICILATION_STATUS="domicilationStatus";

    public static final String          BC_CONTEXT="billableChargeContext";

    public static final String          BC_STATUS="billableChargeStatus";

    public static final String          BC_SOURCE="billableChargeSource";

    public static final String          BC_TYPE="billableChargeType";

    public static final String          MR_CONTEXT="meterReadContext";

    public static final String          MR_STATUS="meterReadStatus";

    public static final String          MR_SOURCE="meterReadSource";

    public static final String          MR_TYPE="meterReadType";

    public static final String          MEASURE_TYPE="measureType";


    private final ParameterRepository parameterRepository;

    void checkMeasureType(String value, Errors errors)
    {
        if (!check(MEASURE_TYPE, value))
            errors.reject("", "meterReadDetail.measureType=" + value);
    }

    void checkMeterReadType(String value, Errors errors)
    {
        if (!check(MR_TYPE, value))
            errors.reject("", "type=" + value);
    }

    void checkMeterReadContext(String value, Errors errors)
    {
        if (!check(MR_CONTEXT, value))
            errors.reject("", "context=" + value);
    }

    void checkMeterReadSource(String value, Errors errors)
    {
        if (!check(MR_SOURCE, value))
            errors.reject("", "source=" + value);
    }

    void CheckMeterReadStatus(String value, Errors errors)
    {
        if (!check(MR_STATUS, value))
            errors.reject("", "status=" + value);
    }

    void checkBillableChargeType(String value, Errors errors)
    {
        if (!check(BC_TYPE, value))
            errors.reject("", "billableCharge.type=" + value);
    }

    void checkBillableChargeContext(String value, Errors errors)
    {
        if (!check(BC_CONTEXT, value))
            errors.reject("", "billableCharge.context=" + value);
    }

    void checkBillableChargeSource(String value, Errors errors)
    {
        if (!check(BC_SOURCE, value))
            errors.reject("", "billableCharge.source=" + value);
    }

    void CheckBillableChargeStatus(String value, Errors errors)
    {
        if (!check(BC_STATUS, value))
            errors.reject("", "billableCharge.status=" + value);
    }

    void checkMeterType(String value, Errors errors)
    {
        if (!check(METER_TYPE, value))
            errors.reject("", "pos.meter.meterType=" + value);
    }

    void checkSmartMeterStatus(String value, Errors errors)
    {
        if (value !=null &&  !check(SMART_METER_STATUS, value))
            errors.reject("", "pos.meter.smartMeterStatus=" + value);
    }

    void checkEstimateType(String value, Errors errors)
    {
        if (!check(ESTIMATE_TYPE, value))
            errors.reject("", "pos.estimate.estimateType=" + value);
    }
    
    void checkReadingFrequency(String value, Errors errors)
    {
        if (!check(READING_FREQUENCY, value))
            errors.reject("", "pos.configuration.readingFrequency=" + value);
    }
    void checkBusinessGridCode(String value, Errors errors)
    {
        if (value !=null && !check(BUSINESS_GRID_CODE, value))
            errors.reject("", "pos.configuration.businessGridCode=" + value);
    }
    void checkMarketGridCode(String value, Errors errors)
    {
        if (value !=null && !check(MARKET_GRID_CODE, value))
            errors.reject("", "pos.configuration.marketGridCode=" + value);
    }
    void checkProfile(String value, Errors errors)
    {
        if (value !=null && !check(PROFILE, value))
            errors.reject("", "pos.configuration.profile=" + value);
    }

    void checkAddress(Address adress, Errors errors)
    {
        if(adress==null)
            errors.reject("", "address=" + adress);
        else {
            if(adress.getCity()==null)
                errors.reject("", "address.city=" + adress.getCity());
            if(adress.getPostalCode()==null)
                errors.reject("", "address.postalCode=" + adress.getPostalCode());
            if(adress.getStreet()==null)
                errors.reject("", "address.street=" + adress.getStreet());
        } 
    } 
    
    void checkCapacityType(String value, Errors errors)
    {
        if (!check(CAPACITY_TYPE, value))
            errors.reject("", "pos.capacityType=" + value);
    }

    void checkPosDataStatus(String value, Errors errors)
    {
        if (!check(POS_DATA_STATUS, value))
            errors.reject("", "pos.posDataStatus=" + value);
    }

    void checkRole(String value, Errors errors)
    {
        if (!check(ROLE, value))
            errors.reject("", "actorRole=" + value);
    }

    void checkSource(String value, Errors errors)
    {
        if (!check(SOURCE, value))
            errors.reject("", "source=" + value);
    }
    
    void checkBillingFrequency(String value, Errors errors)
    {
        if (!check(BILLING_FREQUENCY, value))
            errors.reject("", "billingFrequency=" + value);
    }

    void checkPerimeterType(String value, Errors errors)
    {
        if (!check(PERIMETER_TYPE, value))
            errors.reject("", "perimeterType=" + value);
    }

    void checkPerimeterStatus(String value, Errors errors)
    {
        if (value !=null && !check(PERIMETER_STATUS, value))
            errors.reject("", "perimeterStatus=" + value);
    }

    void checkTitleCode(String value, Errors errors)
    {
        if (value !=null && !check(TITLE_CODE, value))
            errors.reject("", "titleCode=" + value);
    }
    void checkFirstName(String value, Errors errors)
    {
        if (value ==null)
            errors.reject("", "individual.lastName=" + value);
    }
    void checkLastName(String value, Errors errors)
    {
        if (value ==null)
            errors.reject("", "individual.lastName=" + value);
    }

    void checkCompanyName(String value, Errors errors)
    {
        if (value ==null)
            errors.reject("", "company.companyName=" + value);
    }
    void checkDeliveryModeCode(String value, Errors errors)
    {
        if (value !=null && !check(DELIVERY_MODE_CODE, value))
            errors.reject("", "contact.deliveryModeCode=" + value);
    }

    void checkUserRole(String value, Errors errors)
    {
        if (value !=null && !check(USER_ROLE, value))
            errors.reject("", "userRole=" + value);
    }

    void checkGroupSubCategory(String value, Errors errors)
    {
        if (value !=null && !check(GROUP_SUB_CATEGORY, value))
            errors.reject("", "groupSubCategory=" + value);
    }

    void checkGroupCategory(String value, Errors errors)
    {
        if (value !=null && !check(GROUP_CATEGORY, value))
            errors.reject("", "groupCategory=" + value);
    }

    void checkLegalForm(String value, Errors errors)
    {
        if (!check(LEGAL_FORME, value))
            errors.reject("", "legalForm=" + value);
    }

    void checkNaceCode(String value, Errors errors)
    {
        if (!check(NACE_CODE, value))
            errors.reject("", "naceCode=" + value);
    }

    void checkOrganismCategory(String value, Errors errors)
    {
        if (!check(ORGANISM_CATEGORY, value))
            errors.reject("", "entityCategory=" + value);
    }

    void checkOrganismSubCategory(String value, Errors errors)
    {
        if (!check(ORGANISM_SUB_CATEGORY, value))
            errors.reject("", "entitySubCategory=" + value);
    }

    void checkAccountingScheme(String value, Errors errors)
    {
        if (!check(ACCOUNTING_SCHEME, value))
            errors.reject("", "accountingScheme=" + value);
    }

    void checkBillingScheme(String value, Errors errors)
    {
        if (!check(BILLING_SCHEME, value))
            errors.reject("", "billingScheme=" + value);
    }
    void checkFactorType(String value, Errors errors)
    {
        if (!check(FACTOR_TYPE, value))
            errors.reject("", "factorType=" + value);
    }

    void checkOperandType(String value, Errors errors)
    {
        if (!check(OPERAND_TYPE, value))
            errors.reject("", "operandType=" + value);
    }

    void checkSqType(String value, Errors errors)
    {
        if (!check(SQ_TYPE, value))
            errors.reject("", "unit=" + value);
    }

    void checkRefDateTypeForFixedPrice(String value, Errors errors)
    {
        if (value !=null && !check(REF_DATE_FIXED_PRICE, value))
            errors.reject("", "refDateTypeForFixedPrice=" + value);
    }

    void checkPriceMode(String value, Errors errors)
    {
        if (!check(PRICE_MODE, value))
            errors.reject("", "priceMode=" + value);
    }

    void checkDeliveryStatus(String value, Errors errors)
    {
        if (!check(DELIVERY_STATUS, value))
            errors.reject("", "deliveryStatus=" + value);
    }

    void checkDeliveryState(String value, Errors errors)
    {
        if (!check(DELIVERY_STATE, value))
            errors.reject("", "deliveryState=" + value);
    }

    void checkSeller(String value, Errors errors)
    {
        if (!check(SELLER, value))
            errors.reject("", "seller=" + value);
    }

    void checkDirection(String value, Errors errors)
    {
        if (!check(DIRECTION, value))
            errors.reject("", "direction=" + value);
    }

    void checkThresholdBase(String value, Errors errors)
    {
        if (value !=null && !check(THRESHOLD_BASE, value))
            errors.reject("", "thresholdBase=" + value);
    }

    void checkThresholdType(String value, Errors errors)
    {
        if (value !=null && !check(THRESHOLD_TYPE, value))
            errors.reject("", "thresholdType=" + value);
    }

    void checkPriceUnit(String value, Errors errors)
    {
        if (!check(PRICE_UNIT, value))
            errors.reject("", "priceUnit=" + value);
    }

    void checkTgoCode(String value, Errors errors)
    {
        if (!check(TGO_CODE, value))
            errors.reject("", "tgoCode=" + value);
    }

    void checkDgoCode(String value, Errors errors)
    {
        if (!check(DGO_CODE, value))
            errors.reject("", "dgoCode=" + value);
    }

    void checkGridRate(String value, Errors errors)
    {
        if (!check(GRID_RATE, value))
            errors.reject("", "gridRate=" + value);
    }

    void checkTouGroup(String value, Errors errors)
    {
        if (!check(TOU_GROUP, value))
            errors.reject("", "touGroup=" + value);
    }

    void checkTou(String value, Errors errors)
    {
        if (!check(TOU, value))
            errors.reject("", "tou=" + value);
    }

    void checkPosCategory(String value, Errors errors)
    {
        if (!check(PO_CATEGORY, value))
            errors.reject("", "posCategory=" + value);
    }

    void checkServiceCategory(String value, Errors errors)
    {
        if (!check(SERVICE_CATEGORY, value))
            errors.reject("", "serviceCategory=" + value);
    }

    void checkServiceSubCategory(String value, Errors errors)
    {
        if (!check(SERVICE_SUB_CATEGORY, value))
            errors.reject("", "serviceSubCategory=" + value);
    }

    void checkInstallmentFrequency(String value, Errors errors)
    {
        if (!check(INSTALLMENT_FREQUENCY, value))
            errors.reject("", "installmentFrequency=" + value);
    }

    void checkChannel(String value, Errors errors)
    {
        if (!check(CHANNEL, value))
            errors.reject("", "channel=" + value);
    }

    void checkPriceType(String value, Errors errors)
    {
        if (!check(PRICE_TYPE, value))
            errors.reject("", "priceType=" + value);
    }

    void checkBillingMode(String value, Errors errors)
    {
        if (!check(BILLING_MODE, value))
            errors.reject("", "billingMode=" + value);
    }

    void checkVatRate(String value, Errors errors)
    {
        if (!check(VAT_RATE, value))
            errors.reject("", "vatRate=" + value);
    }

    void checkContractStatus(String value, Errors errors)
    {
        if (!check(CONTRACT_STATUS, value))
            errors.reject("", "contractStatus=" + value);
    }

    void checkRelationType(String value, Errors errors)
    {
        if (!check(RELATION_TYPE, value))
            errors.reject("", "relationType=" + value);
    }

    void checkCustomerCategory(String value, Errors errors)
    {
        if (!check(CUSTOMER_CATEGORY, value))
            errors.reject("", "customerCategory=" + value);
    }

    void checkCustomerStatus(String value, Errors errors)
    {
        if (!check(CUSTOMER_STATUS, value))
            errors.reject("", "customerStatus=" + value);
    }

    void checkCustomerType(String value, Errors errors)
    {
        if (!check(CUSTOMER_TYPE, value))
            errors.reject("", "customerType=" + value);
    }

    void checkFinancialInformation(FinancialInformation value, Errors errors)
    {
        if (!check(PAYMENT_MODE_CODE, value.getPaymentMode()))
            errors.reject("", "financialInformation.paymentModeCode=" + value);
    }

    void checkLanguageCode(String value, Errors errors)
    {
        if (!check(LANGUAGE_CODE, value))
            errors.reject("", "language=" + value);
    }

    void checkMarket(String value, Errors errors)
    {
        if (!check(MARKET, value))
            errors.reject("", "market=" + value);
    }

    void checkPaymentMode(String value, Errors errors)
    {
        if (!check(PAYMENT_MODE, value))
            errors.reject("", "paymentMode=" + value);
    }

    void checkIban(String value, Errors errors)
    { //Ajouetr ici controle forlmat Iban
        if (value==null)
            errors.reject("", "IBAN=" + value);
    }

    void checkDomicilationId(String value, Errors errors)
    { 
        if (value==null)
            errors.reject("", "DomiciliationId=" + value);
    }

    void checkDomicilationStatus(String value, Errors errors)
    { 
        if (value==null || !check(DOMICILATION_STATUS, value))
            errors.reject("", "domiciliationStatus=" + value);
    }

    void checkThirdType(String value, Errors errors)
    {
        if (!check(THIRD_TYPE, value))
            errors.reject("", "type=" + value);
    }

    boolean check(String type, String value)
    {
        return parameterRepository.existsByTypeAndNameAndValueAndStartDateBefore("ENUM", type, value, LocalDate.now());
    }
}
