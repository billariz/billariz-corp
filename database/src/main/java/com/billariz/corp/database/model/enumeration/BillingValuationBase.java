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
public enum BillingValuationBase
{
    CONSUMPTION("CONSUMPTION"), 
    SUBSCRIPTION("SUBSCRIPTION"), 
    BILLABLE_CHARGE("BILLABLE_CHARGE"),
    FULL("FULL"), 
    FINAL_FULL("FINAL_FULL"),
    CUTOFF_DATE_CONSUMPTION("CUTOFF_DATE_CONSUMPTION"), 
    CUTOFF_DATE_SUBSCRIPTION("CUTOFF_DATE_SUBSCRIPTION"), 
    CUTOFF_DATE_BC("CUTOFF_DATE_BC"),
    CUTOFF_DATE_FULL("CUTOFF_DATE_FULL");

    private final String value;

    @Override
    public String toString()
    {
        return value;
    }

    public static BillingValuationBase fromValue(String argValue)
    {
        return Arrays.stream(values()).filter(type -> type.value.equals(argValue)).findFirst().orElse(null);
    }

    @Converter(autoApply = true)
    public static class CustomerTypeConverter implements AttributeConverter<BillingValuationBase, String>
    {
        @Override
        public String convertToDatabaseColumn(BillingValuationBase attribute)
        {
            return (attribute == null ? null : attribute.value);
        }

        @Override
        public BillingValuationBase convertToEntityAttribute(String dbData)
        {
            return fromValue(dbData);
        }
    }
}
