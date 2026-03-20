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

package com.billariz.corp.database.rsql;

import java.util.List;
import java.util.stream.Collectors;

public class RsqlBuilder
{
    private final StringBuilder sb = new StringBuilder();

    public RsqlBuilder and(String com, String operator, Object value)
    {
        if (!sb.isEmpty())
            sb.append(" and ");
        sb.append(com).append(operator).append(objectToString(value));
        return this;
    }

    public RsqlBuilder and(String com, String operator, Object... values)
    {
        int count = 0;

        if (!sb.isEmpty())
            sb.append(" and ");
        sb.append(com).append(operator).append('(');
        for (var value : values)
        {
            if (count > 0)
                sb.append(',');
            sb.append(objectToString(value));
            ++count;
        }
        sb.append(')');
        return this;
    }

    public RsqlBuilder or(String com, String operator, Object value)
    {
        if (!sb.isEmpty())
            sb.append(" or ");
        sb.append(com).append(operator).append(objectToString(value));
        return this;
    }

    public boolean isEmpty()
    {
        return sb.isEmpty();
    }

    private String objectToString(Object value)
    {
        if (value instanceof Class)
            return ((Class<?>) value).getName();
        if (value instanceof String)
            return "\"" + value + "\"";
        if (value instanceof List<?>)
            return "(" + ((List<?>) value).stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
        return value.toString();
    }

    @Override
    public String toString()
    {
        return sb.toString();
    }
}
