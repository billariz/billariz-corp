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
public enum BillStatusEnum
{
    CALCULATED("CALCULATED"), CALCULATION_IN_PROGRESS("CALCULATION_IN_PROGRESS"), 
    DROPPED("DROPPED"), CANCELATION_IN_PROGRESS("CANCELATION_IN_PROGRESS"), 
    VALIDATION_IN_PROGRESS("VALIDATION_IN_PROGRESS"),
    CANCELED("CANCELED"), PRINTED("PRINTED"), VALIDATED("VALIDATED"), 
    BILLED("BILLED"), PRINTSHOP_IN_PROGRESS("PRINTSHOP_IN_PROGRESS"), 
    IN_FAILURE("IN_FAILURE"), ;

    private final String value;

    @Override
    public String toString()
    {
        return value;
    }

    public static BillStatusEnum fromValue(String argValue)
    {
        return Arrays.stream(values()).filter(type -> type.value.equals(argValue)).findFirst().orElse(null);
    }

    @Converter(autoApply = true)
    public static class CustomerTypeConverter implements AttributeConverter<BillStatusEnum, String>
    {
        @Override
        public String convertToDatabaseColumn(BillStatusEnum attribute)
        {
            return (attribute == null ? null : attribute.value);
        }

        @Override
        public BillStatusEnum convertToEntityAttribute(String dbData)
        {
            return fromValue(dbData);
        }
    }
}
