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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class GenericRsqlSpecification<T> implements Specification<T>
{
    private static final String           NULL             = "null";

    private static final long             serialVersionUID = 5546046598898481992L;

    private String                        property;

    private transient RsqlSearchOperation operator;

    private List<String>                  arguments;

    public GenericRsqlSpecification(ComparisonNode comparisonNode)
    {
        property = comparisonNode.getSelector();
        operator = RsqlSearchOperation.getSimpleOperator(comparisonNode.getOperator());
        arguments = comparisonNode.getArguments();
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder)
    {
        return toPredicate(root, query, builder, property, operator, arguments);
    }

    private static Predicate toPredicate(Root<?> root, CriteriaQuery<?> query, CriteriaBuilder builder, String property, RsqlSearchOperation operator, List<String> arguments)
    {
        switch (operator)
        {
        case EQUAL:
        {
            var propertyPath = resolvePath(root, property);
            var argument = castArguments(propertyPath, arguments).get(0);

            if (argument == null)
                return builder.isNull(propertyPath);
            else if (argument instanceof String)
                return builder.like(builder.lower(propertyPath), argument.toString().toLowerCase().replace('*', '%'));
            return builder.equal(propertyPath, argument);
        }
        case GREATER_THAN_OR_EQUAL:
        {
            var propertyPath = resolvePath(root, property, Comparable.class);
            var argument = (Comparable<Object>) castArguments(propertyPath, arguments).get(0);

            return builder.greaterThanOrEqualTo(propertyPath, argument);
        }
        case LESS_THAN_OR_EQUAL:
        {
            var propertyPath = resolvePath(root, property, Comparable.class);
            var argument = (Comparable<Object>) castArguments(propertyPath, arguments).get(0);

            return builder.lessThanOrEqualTo(propertyPath, argument);
        }
        // case IN:
        // return getAbsolutePath(root, property, String.class).in(args);
        // case NOT_IN:
        // return builder.not(getAbsolutePath(root, property,
        // String.class).in(args));
        case SUBQUERY_IN:
            return subQueryIn(query, builder, resolvePath(root, property), arguments);
        default:
            throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }

    private static Path<String> resolvePath(final Path<?> path, final String property)
    {
        return resolvePath(path, property, String.class);
    }

    private static <R> Path<R> resolvePath(final Path<?> path, final String property, Class<R> className)
    {
        if (property.contains("."))
        {
            return resolvePath(path.get(property.substring(0, property.indexOf('.'))), property.substring(property.indexOf('.') + 1), className);
        }
        return path.get(property);
    }

    private static List<Object> castArguments(Path<?> propertyPath, List<String> arguments)
    {
        Class<? extends Object> type = propertyPath.getJavaType();

        return arguments.stream().map(argument -> castArgument(argument, type)).toList();
    }

    private static Object castArgument(String argument, Class<?> type)
    {
        if (NULL.equalsIgnoreCase(argument))
            return null;
        if (type.isEnum())
            return retreiveEnumClass(type, argument);
        if (type.equals(LocalDate.class))
            return LocalDate.parse(argument);
        if (type.equals(LocalDateTime.class))
            return LocalDateTime.parse(argument);
        if (type.equals(OffsetDateTime.class))
            return OffsetDateTime.parse(argument);
        if (type.equals(Integer.class))
            return Integer.parseInt(argument);
        if (type.equals(Long.class) || type.equals(long.class))
            return Long.parseLong(argument);
        if (type.equals(Boolean.class) || type.equals(boolean.class))
            return Boolean.valueOf(argument);
        return argument;
    }

    private static Object retreiveEnumClass(final Class<?> type, final String argument)
    {
        try
        {
            var method = type.getMethod("fromValue", String.class);

            return method.invoke(null, argument);
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Enum missing fromValue method: " + type, e);
        }
    }

    private static Predicate subQueryIn(final CriteriaQuery<?> query, final CriteriaBuilder builder, final Path<?> path, List<String> arguments)
    {
        if (arguments.size() != 3)
            throw new IllegalArgumentException("Number of arguments is invalid: " + arguments);

        var className = arguments.get(0);
        var fieldId = arguments.get(1);
        var fieldExpression = arguments.get(2);

        try
        {
            var classTable = Class.forName(className);
            var subQuery = query.subquery(Long.class);
            var subRoot = subQuery.from(classTable);
            var rootNode = new RsqlParser<>().parse(fieldExpression);
            var subQueryVisitor = new SubQueryVisitor(subRoot, query, builder);

            rootNode.accept(subQueryVisitor);
            subQuery.select(resolvePath(subRoot, fieldId, Long.class));
            subQuery.where(subQueryVisitor.predicates.toArray(new Predicate[subQueryVisitor.predicates.size()]));
            return path.in(subQuery);
        }
        catch (ClassNotFoundException e)
        {
            throw new IllegalArgumentException("Unsupported class for subQuery: " + className, e);
        }
    }

    @RequiredArgsConstructor
    private static class SubQueryVisitor implements RSQLVisitor<Void, Void>
    {
        private final Root<?>          subRoot;

        private final CriteriaQuery<?> query;

        private final CriteriaBuilder  builder;

        private List<Predicate>        predicates = new ArrayList<>();

        @Override
        public Void visit(AndNode node, Void param)
        {
            createSpecification(node);
            return null;
        }

        @Override
        public Void visit(OrNode node, Void param)
        {
            createSpecification(node);
            return null;
        }

        private Predicate createSpecification(Node node)
        {
            if (node instanceof LogicalNode)
                return createSpecification((LogicalNode) node);
            if (node instanceof ComparisonNode)
                return createSpecification((ComparisonNode) node);
            return null;
        }

        private Predicate createSpecification(LogicalNode logicalNode)
        {
            logicalNode.getChildren().stream().map(this::createSpecification).filter(Objects::nonNull).forEach(predicates::add);
            return null;
        }

        private Predicate createSpecification(ComparisonNode node)
        {
            var property = node.getSelector();
            var operator = RsqlSearchOperation.getSimpleOperator(node.getOperator());
            var arguments = node.getArguments();

            return toPredicate(subRoot, query, builder, property, operator, arguments);
        }

        @Override
        public Void visit(ComparisonNode node, Void param)
        {
            var property = node.getSelector();
            var operator = RsqlSearchOperation.getSimpleOperator(node.getOperator());
            var arguments = node.getArguments();

            predicates.add(toPredicate(subRoot, query, builder, property, operator, arguments));
            return null;
        }
    }
}