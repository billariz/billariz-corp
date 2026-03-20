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
public enum MonitoringObject
{
    TRANSACTION("TRANSACTION"), PROCESS("PROCESS"), ALLOCATION("ALLOCATION"), PAYMENT_ISSUES("PAYMENT-ISSUES"), BANK_FINANCIAL_INFORMATION_TRACKING(
            "BankFinancialInformationTracking"), BANK_FINANCIAL_TRANSACTION("BankFinancialTransaction"), BANK_ACCOUNT("bankAccount"), CUSTOMER(
                    "CUSTOMER"), INVOICE("INVOICE"), PREPAYMENT("PREPAYMENT"), CONTRACT(
                            "CONTRACT"), COLLECTION_CLASS_CODE("collectionClassCode"), THIRD_RECIPIENT("Third Destinataire"), ACTOR_RECIPIENT(
                                    "Actor Destinataire"), THIRD_PAYER("Third Payeur"), ACTOR_PAYER("Actor Payeur"), UPDATE_PROCESS_TRANSACTION(
                                            "UPDATE_PROCESS_TRANSACTION"), INSERT_PROCESS_TRANSACTION("INSERT_PROCESS_TRANSACTION"), IMPORT_IN_PP(
                                                    "IMPORT_IN_PP"), IMPORT_PAYMENT_REJECTION(
                                                            "IMPORT_PAYMENT_REJECTION"), LAUNCH_PAYIN_PAYOUT("LAUNCH_PAYIN_PAYOUT"), LOG("LOG"), ADHOC(
                                                                    "ADHOC"), EVENT("EVENT"), BILLING_VALUATION("BILLING_VALUATION"), BILLING_ELIGIBILITY(
                                                                            "BILLING_ELIGIBILITY"), BILL_SEGMENT("BILL_SEGMENT"), SERVICE_ELEMENT(
                                                                                    "SERVICE_ELEMENT"), METER_READ("METER_READ"), PERIMETER("PERIMETER"), BILL(
                                                                                            "BILL"), USER("USER"),ACTIVITY("ACTIVITY"), BILLINGRUN("BILLINGRUN");

    private final String value;

    @Override
    public String toString()
    {
        return value;
    }

    public static MonitoringObject fromValue(String argValue)
    {
        return Arrays.stream(values()).filter(type -> type.value.equals(argValue)).findFirst().orElse(null);
    }

    @Converter(autoApply = true)
    public static class CustomerTypeConverter implements AttributeConverter<MonitoringObject, String>
    {
        @Override
        public String convertToDatabaseColumn(MonitoringObject attribute)
        {
            return (attribute == null ? null : attribute.value);
        }

        @Override
        public MonitoringObject convertToEntityAttribute(String dbData)
        {
            return fromValue(dbData);
        }
    }
}
