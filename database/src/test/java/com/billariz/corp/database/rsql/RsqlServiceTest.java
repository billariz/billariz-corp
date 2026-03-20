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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import com.billariz.corp.database.model.Activity;
import com.billariz.corp.database.model.Relation;
import com.billariz.corp.database.model.ServiceElement;

@SpringBootTest(classes = { RsqlService.class })
@ContextConfiguration
public class RsqlServiceTest
{
    @Autowired
    private RsqlService             rsql;

    @MockBean
    private Subquery<Long>          subquery;

    @MockBean
    private CriteriaQuery<Activity> criteriaQuery;

    @MockBean
    private CriteriaBuilder         criteriaBuilder;

    @Test
    void testBorderCase()
    {
        assertNull(rsql.toSpec(null, Activity.class));
        assertNull(rsql.toSpec("", Activity.class));
    }

    @Test
    void testEquals()
    {
        var root = buildRoot(Activity.class, String.class);
        var spec = rsql.toSpec("id==1 or label==\"l*\" or label==\"null\"", Activity.class);

        assertNotNull(spec);

        spec.toPredicate(root, criteriaQuery, criteriaBuilder);
    }

    @Test
    void testFull()
    {
        var request = new RsqlBuilder();

        assertTrue(request.isEmpty());

        request.and("id", "==", 1L);
        request.and("id", "=insub=", Relation.class, "firstObjectId",
                "relationType==" + "ACTIVITY_PROCESS" + " and secondObjectId==" + 2L);
        request.and("startDate", "=ge=", LocalDate.of(2022, 10, 28));
        request.or("endDate", "=le=", LocalDate.of(2022, 10, 28));
        request.or("label", "==", "l*");

        assertFalse(request.isEmpty());
        assertEquals(
                "id==1 and id=insub=(com.billariz.corp.database.model.Relation,\"firstObjectId\",\"relationType==ACTIVITY_PROCESS and secondObjectId==2\") and startDate=ge=2022-10-28 or endDate=le=2022-10-28 or label==\"l*\"",
                request.toString());

        var root = buildRoot(Activity.class, String.class);
        var spec = rsql.toSpec(request.toString(), Activity.class);

        assertNotNull(spec);

        when(criteriaQuery.subquery(Long.class)).thenReturn(subquery);
        when(subquery.from(any(Class.class))).thenReturn(root);
        spec.toPredicate(root, criteriaQuery, criteriaBuilder);
    }

    @Test
    void testObjectDeep()
    {
        var root = buildRoot(Activity.class, String.class);
        var spec = rsql.toSpec("type.defaultLabel==1", Activity.class);

        assertNotNull(spec);

        spec.toPredicate(root, criteriaQuery, criteriaBuilder);
    }

    @Test
    void testObjectNull()
    {
        var root = buildRoot(Activity.class, String.class);
        var spec = rsql.toSpec("type==null", Activity.class);

        assertNotNull(spec);

        spec.toPredicate(root, criteriaQuery, criteriaBuilder);
    }

    @Test
    void testObjectInteger()
    {
        var root = buildRoot(ServiceElement.class, Integer.class);
        var spec = rsql.toSpec("minDayForEstimate==3", ServiceElement.class);

        assertNotNull(spec);

        spec.toPredicate(root, criteriaQuery, criteriaBuilder);
    }

    @Test
    void testObjectLocalDate()
    {
        var root = buildRoot(Activity.class, String.class);
        var spec = rsql.toSpec("startDate==\"2023-01-01\"", Activity.class);

        assertNotNull(spec);

        spec.toPredicate(root, criteriaQuery, criteriaBuilder);
    }

    private <T> Root<T> buildRoot(Class<T> clazz, Class clazzType)
    {
        return new Root<T>()
        {

            @Override
            public Set<Join<T, ?>> getJoins()
            {
                return null;
            }

            @Override
            public boolean isCorrelated()
            {
                return false;
            }

            @Override
            public From<T, T> getCorrelationParent()
            {
                return null;
            }

            @Override
            public <Y> Join<T, Y> join(SingularAttribute<? super T, Y> attribute)
            {
                return null;
            }

            @Override
            public <Y> Join<T, Y> join(SingularAttribute<? super T, Y> attribute, JoinType jt)
            {
                return null;
            }

            @Override
            public <Y> CollectionJoin<T, Y> join(CollectionAttribute<? super T, Y> collection)
            {
                return null;
            }

            @Override
            public <Y> SetJoin<T, Y> join(SetAttribute<? super T, Y> set)
            {
                return null;
            }

            @Override
            public <Y> ListJoin<T, Y> join(ListAttribute<? super T, Y> list)
            {
                return null;
            }

            @Override
            public <K, V> MapJoin<T, K, V> join(MapAttribute<? super T, K, V> map)
            {
                return null;
            }

            @Override
            public <Y> CollectionJoin<T, Y> join(CollectionAttribute<? super T, Y> collection, JoinType jt)
            {
                return null;
            }

            @Override
            public <Y> SetJoin<T, Y> join(SetAttribute<? super T, Y> set, JoinType jt)
            {
                return null;
            }

            @Override
            public <Y> ListJoin<T, Y> join(ListAttribute<? super T, Y> list, JoinType jt)
            {
                return null;
            }

            @Override
            public <K, V> MapJoin<T, K, V> join(MapAttribute<? super T, K, V> map, JoinType jt)
            {
                return null;
            }

            @Override
            public <X, Y> Join<X, Y> join(String attributeName)
            {
                return null;
            }

            @Override
            public <X, Y> CollectionJoin<X, Y> joinCollection(String attributeName)
            {
                return null;
            }

            @Override
            public <X, Y> SetJoin<X, Y> joinSet(String attributeName)
            {
                return null;
            }

            @Override
            public <X, Y> ListJoin<X, Y> joinList(String attributeName)
            {
                return null;
            }

            @Override
            public <X, K, V> MapJoin<X, K, V> joinMap(String attributeName)
            {
                return null;
            }

            @Override
            public <X, Y> Join<X, Y> join(String attributeName, JoinType jt)
            {
                return null;
            }

            @Override
            public <X, Y> CollectionJoin<X, Y> joinCollection(String attributeName, JoinType jt)
            {
                return null;
            }

            @Override
            public <X, Y> SetJoin<X, Y> joinSet(String attributeName, JoinType jt)
            {
                return null;
            }

            @Override
            public <X, Y> ListJoin<X, Y> joinList(String attributeName, JoinType jt)
            {
                return null;
            }

            @Override
            public <X, K, V> MapJoin<X, K, V> joinMap(String attributeName, JoinType jt)
            {
                return null;
            }

            @Override
            public Path<?> getParentPath()
            {
                return null;
            }

            @Override
            public <Y> Path<Y> get(SingularAttribute<? super T, Y> attribute)
            {
                return null;
            }

            @Override
            public <E, C extends Collection<E>> Expression<C> get(PluralAttribute<T, C, E> collection)
            {
                return null;
            }

            @Override
            public <K, V, M extends Map<K, V>> Expression<M> get(MapAttribute<T, K, V> map)
            {
                return null;
            }

            @Override
            public Expression<Class<? extends T>> type()
            {
                return null;
            }

            @Override
            public Path<Object> get(String attributeName)
            {
                if ("id".equals(attributeName))
                    return buildRoot(Object.class, Long.class);
                if ("minDayForEstimate".equals(attributeName))
                    return buildRoot(Object.class, Integer.class);
                if ("startDate".equals(attributeName))
                    return buildRoot(Object.class, LocalDate.class);
                return buildRoot(Object.class, String.class);
            }

            @Override
            public Predicate isNull()
            {
                return null;
            }

            @Override
            public Predicate isNotNull()
            {
                return null;
            }

            @Override
            public Predicate in(Object... values)
            {
                return null;
            }

            @Override
            public Predicate in(Expression<?>... values)
            {
                return null;
            }

            @Override
            public Predicate in(Collection<?> values)
            {
                return null;
            }

            @Override
            public Predicate in(Expression<Collection<?>> values)
            {
                return null;
            }

            @Override
            public <X> Expression<X> as(Class<X> type)
            {
                return null;
            }

            @Override
            public Selection<T> alias(String name)
            {
                return null;
            }

            @Override
            public boolean isCompoundSelection()
            {
                return false;
            }

            @Override
            public List<Selection<?>> getCompoundSelectionItems()
            {
                return null;
            }

            @Override
            public Class<? extends T> getJavaType()
            {
                return clazzType;
            }

            @Override
            public String getAlias()
            {
                return null;
            }

            @Override
            public Set<Fetch<T, ?>> getFetches()
            {
                return null;
            }

            @Override
            public <Y> Fetch<T, Y> fetch(SingularAttribute<? super T, Y> attribute)
            {
                return null;
            }

            @Override
            public <Y> Fetch<T, Y> fetch(SingularAttribute<? super T, Y> attribute, JoinType jt)
            {
                return null;
            }

            @Override
            public <Y> Fetch<T, Y> fetch(PluralAttribute<? super T, ?, Y> attribute)
            {
                return null;
            }

            @Override
            public <Y> Fetch<T, Y> fetch(PluralAttribute<? super T, ?, Y> attribute, JoinType jt)
            {
                return null;
            }

            @Override
            public <X, Y> Fetch<X, Y> fetch(String attributeName)
            {
                return null;
            }

            @Override
            public <X, Y> Fetch<X, Y> fetch(String attributeName, JoinType jt)
            {
                return null;
            }

            @Override
            public EntityType<T> getModel()
            {
                return null;
            }

        };
    }
}
