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

import java.util.Objects;
import org.springframework.data.jpa.domain.Specification;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.LogicalOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;

public class RsqlVisitor<T> implements RSQLVisitor<Specification<T>, Void>
{
    @Override
    public Specification<T> visit(AndNode node, Void param)
    {
        return createSpecification(node);
    }

    @Override
    public Specification<T> visit(OrNode node, Void param)
    {
        return createSpecification(node);
    }

    @Override
    public Specification<T> visit(ComparisonNode node, Void params)
    {
        return createSpecification(node);
    }

    private Specification<T> createSpecification(Node node)
    {
        if (node instanceof LogicalNode)
            return createSpecification((LogicalNode) node);
        if (node instanceof ComparisonNode)
            return createSpecification((ComparisonNode) node);
        return null;
    }

    private Specification<T> createSpecification(LogicalNode logicalNode)
    {
        var specs = logicalNode.getChildren().stream().map(this::createSpecification).filter(Objects::nonNull).toList();
        var result = specs.get(0);

        if (logicalNode.getOperator() == LogicalOperator.AND)
        {
            for (int i = 1; i < specs.size(); i++)
                result = Specification.where(result).and(specs.get(i));
        }
        else if (logicalNode.getOperator() == LogicalOperator.OR)
        {
            for (int i = 1; i < specs.size(); i++)
                result = Specification.where(result).or(specs.get(i));
        }

        return result;
    }

    private Specification<T> createSpecification(ComparisonNode comparisonNode)
    {
        return Specification.where(new GenericRsqlSpecification<T>(comparisonNode));
    }
}
