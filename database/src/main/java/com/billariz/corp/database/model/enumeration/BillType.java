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
public enum BillType
{
    CYCLICAL("CYCLICAL"), EVENT("EVENT"), FINAL("FINAL"), GROUP_CYCLICAL("GROUP_CYCLICAL"), GROUP_EVENT("GROUP_EVENT"), GROUP_FINAL("GROUP_FINAL");

    private final String value;

    @Override
    public String toString()
    {
        return value;
    }

    public static BillType fromValue(String argValue)
    {
        return Arrays.stream(values()).filter(type -> type.value.equals(argValue)).findFirst().orElse(null);
    }

    @Converter(autoApply = true)
    public static class CustomerTypeConverter implements AttributeConverter<BillType, String>
    {
        @Override
        public String convertToDatabaseColumn(BillType attribute)
        {
            return (attribute == null ? null : attribute.value);
        }

        @Override
        public BillType convertToEntityAttribute(String dbData)
        {
            return fromValue(dbData);
        }
    }
}
