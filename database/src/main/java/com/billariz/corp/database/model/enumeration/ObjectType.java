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

package com.billariz.corp.database.model.enumeration;

import java.util.Arrays;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ObjectType
{

    BANK_FINANCIAL_INFORMATION_TRACKING(
     "BankFinancialInformationTracking"), BANK_FINANCIAL_TRANSACTION("BankFinancialTransaction"), BANK_ACCOUNT("bankAccount"),
     INVOICE("INVOICE"), PREPAYMENT("PREPAYMENT"), COLLECTION_CLASS_CODE("collectionClassCode"), THIRD_RECIPIENT("Third Destinataire"), ACTOR_RECIPIENT(
     "Actor Destinataire"), THIRD_PAYER("Third Payeur"), ACTOR_PAYER("Actor Payeur"), LOG("LOG"), ADHOC(
     "ADHOC"), BILLING_VALUATION("BILLING_VALUATION"), BILLING_ELIGIBILITY(
     "BILLING_ELIGIBILITY"), THIRD("THIRD"),
    CONTRACT("CONTRACT"),ACTOR("ACTOR"),SERVICE_ELEMENT("SERVICE_ELEMENT"), CONTRACT_POS("CONTRACT_POS"), SERVICE("SERVICE"), TERM_OF_SERVICE("TERM_OF_SERVICE"), CALCUL("CALCUL"), CUSTOMER("CUSTOMER"), PERIMETER("PERIMETER"), ACTIVITY("ACTIVITY"), EVENT("EVENT"), BILLING_RUN("BILLING_RUN"), BILL(
            "BILL"), PARAMETER("PARAMETER"), POINT_OF_SERVICE("POINT_OF_SERVICE"), CHART("CHART"),
            ARTICLE("ARTICLE"), METER_READ_DETAIL("METER_READ_DETAIL"), METER_READ("METER_READ"), 
            USER("USER"), BILLABLE_CHARGE("BILLABLE_CHARGE"), BILL_SEGMENT("BILL_SEGMENT"), OBJECT_PROCESS_RULE("OBJECT_PROCESS_RULE"), 
            LAUNGAGE_MAPPING("LAUNGUAGE_MAPPING"), EVENT_MANAGER("EVENT_MANAGER");

    private final String value;

    @Override
    public String toString()
    {
        return value;
    }

    public static ObjectType fromValue(String argValue)
    {
        return Arrays.stream(values()).filter(type -> type.value.equals(argValue)).findFirst().orElse(null);
    }

    @Converter(autoApply = true)
    public static class CustomerTypeConverter implements AttributeConverter<ObjectType, String>
    {
        @Override
        public String convertToDatabaseColumn(ObjectType attribute)
        {
            return (attribute == null ? null : attribute.value);
        }

        @Override
        public ObjectType convertToEntityAttribute(String dbData)
        {
            return fromValue(dbData);
        }
    }
}
