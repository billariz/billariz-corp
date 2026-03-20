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

import org.springframework.data.jpa.domain.Specification;
import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;

public class RsqlParser<T>
{
    public Node parse(String query)
    {
        return new RSQLParser(RsqlSearchOperation.operators()).parse(query);
    }

    public Specification<T> toSpecification(String query)
    {
        var rootNode = parse(query);

        return rootNode.accept(new RsqlVisitor<T>());
    }
}
