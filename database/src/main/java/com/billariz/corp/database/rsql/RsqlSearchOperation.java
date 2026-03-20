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

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum RsqlSearchOperation
{
    EQUAL(RSQLOperators.EQUAL, false),

    // NOT_EQUAL(RSQLOperators.NOT_EQUAL, false),

    // GREATER_THAN(RSQLOperators.GREATER_THAN, false),

    GREATER_THAN_OR_EQUAL(RSQLOperators.GREATER_THAN_OR_EQUAL, false),

    // LESS_THAN(RSQLOperators.LESS_THAN, false),

    LESS_THAN_OR_EQUAL(RSQLOperators.LESS_THAN_OR_EQUAL, false),

    // IN(RSQLOperators.IN, false),

    // NOT_IN(RSQLOperators.NOT_IN, false),

    SUBQUERY_IN(new ComparisonOperator("=insub=", true), true);

    private final ComparisonOperator operator;

    private final boolean            subQuery;

    public static RsqlSearchOperation getSimpleOperator(ComparisonOperator operator)
    {
        for (RsqlSearchOperation operation : values())
            if (operation.operator.equals(operator))
                return operation;
        throw new IllegalArgumentException("Unknown operator: " + operator);
    }

    public static Set<ComparisonOperator> operators()
    {
        return Stream.of(values()).map(RsqlSearchOperation::getOperator).collect(Collectors.toSet());
    }
}