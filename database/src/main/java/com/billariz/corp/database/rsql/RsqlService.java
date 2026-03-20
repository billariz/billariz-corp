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
import org.springframework.stereotype.Component;

/**
 * Exemple d'utilisation:
 * 
 * <pre>{@code
 * var request = new RsqlBuilder();
 *
 * if (id.isPresent())
 *     request.and("id", "==", id.get());
 * if (processId.isPresent())
 *     request.and("id", "=insub=", Relation.class, "firstObjectId",
 *             "relationType==" + RelationType.ACTIVITY_PROCESS.getValue() + " and secondObjectId==" + processId.get());
 * if (statusList.isPresent())
 *     request.and("status", "=in=", statusList.get());
 * if (fromDate.isPresent())
 *     request.and("startDate", "=ge=", fromDate.get());
 * if (toDate.isPresent())
 *     request.and("endDate", "=le=", toDate.get());
 *
 * var response = new SearchActivitiesResponseDto();
 * var spec = rsql.toSpec(request.toString(), Activity.class);
 * }</pre>
 */
@Component
public class RsqlService
{
    public <T> Specification<T> toSpec(String search, Class<T> clazz)
    {
        if (search == null || search.isEmpty())
            return null;
        return new RsqlParser<T>().toSpecification(search);
    }
}
