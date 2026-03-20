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
public enum ActivityStatus
{
    IN_FAILURE("IN_FAILURE"), IN_PROGRESS("IN_PROGRESS"), PENDING("PENDING"), COMPLETED("COMPLETED"), BLOCKED("BLOCKED"), CANCELLED("CANCELLED"), SUSPENDED("SUSPENDED");

    private final String value;

    @Override
    public String toString()
    {
        return value;
    }

    public static ActivityStatus fromValue(String argValue)
    {
        return Arrays.stream(values()).filter(type -> type.value.equals(argValue)).findFirst().orElse(null);
    }

    @Converter(autoApply = true)
    public static class CustomerTypeConverter implements AttributeConverter<ActivityStatus, String>
    {
        @Override
        public String convertToDatabaseColumn(ActivityStatus attribute)
        {
            return (attribute == null ? null : attribute.value);
        }

        @Override
        public ActivityStatus convertToEntityAttribute(String dbData)
        {
            return fromValue(dbData);
        }
    }
}
