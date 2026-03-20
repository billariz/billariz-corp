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

package com.billariz.corp.database.repository;

import org.hibernate.annotations.Parent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.Actor;
import com.billariz.corp.database.projection.inActor;

@RepositoryRestResource(excerptProjection = inActor.class)
public interface ActorRepository extends RepositoryReadWriteDelete<Actor, Long>,PagingAndSortingRepository<Actor, Long>
{
    
    //@EntityGraph(attributePaths = {"third", "third.address","third.contact"})
    @Query("SELECT a FROM Actor a WHERE (:id IS NULL OR a.id=:id) AND (:perimeterId IS NULL OR a.perimeterId=:perimeterId) AND (:customerId IS NULL OR a.perimeter.customerId=:customerId) AND (:role IS NULL OR a.role=:role) AND (:contractId IS NULL OR a.perimeterId IN (SELECT perimeterId FROM ContractPerimeter cp WHERE cp.contractId=:contractId))")
    Page<Actor> findActor(@Param("id") Long id, @Param("contractId") Long contractId, @Param("perimeterId") Long perimeterId, @Param("customerId") Long customerId, @Param("role") String role, Pageable pageable);

}
