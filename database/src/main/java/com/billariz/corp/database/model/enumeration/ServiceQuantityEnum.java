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
public enum ServiceQuantityEnum
{
    NUMBER_OF_DAY("nbrOfDay"), NUMBER_OF_MONTH("nbrOfMonth"), KWH_PER_TOU("kWhPerTou"), UNIT("unit"), EUR_FIX("eurFix"), SUM_EUR_FIX_BASED_ON_SA(
            "sumEurFixBasedOnSe"), KWH_PER_DAY_PER_YEAR("kWhPerDayPerYear"), KVA_PER_TOU_PER_YEAR("kVAPerTouPerYear"), KVA_PER_TOU("kVAPerTou");

    private final String value;

    @Override
    public String toString()
    {
        return value;
    }

    public static ServiceQuantityEnum fromValue(String argValue)
    {
        return Arrays.stream(values()).filter(type -> type.value.equals(argValue)).findFirst().orElse(null);
    }

    @Converter(autoApply = true)
    public static class CustomerTypeConverter implements AttributeConverter<ServiceQuantityEnum, String>
    {
        @Override
        public String convertToDatabaseColumn(ServiceQuantityEnum attribute)
        {
            return (attribute == null ? null : attribute.value);
        }

        @Override
        public ServiceQuantityEnum convertToEntityAttribute(String dbData)
        {
            return fromValue(dbData);
        }
    }
}
