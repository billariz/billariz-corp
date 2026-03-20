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

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteFull;
import com.billariz.corp.database.model.Relation;

@RepositoryRestResource
public interface RelationRepository extends RepositoryReadWriteFull<Relation, Long>
{
    Optional<Relation> findFirstByRelationTypeAndFirstObjectIdOrderByIdDesc(String relationType, Long firstObjectId);

    boolean existsByRelationTypeAndFirstObjectIdAndSecondObjectId(String relationType, Long firstObjectId, Long secondObjectId);

    boolean existsByFirstObjectIdAndSecondObjectIdAndRelationType(Long firstObjectId, Long secondObjectId, String relationType);

    Optional<Relation> findAllByRelationTypeAndFirstObjectId(String relationType, Long firstObjectId);

    Optional<Relation> findFirstByRelationTypeAndSecondObjectId(String relationType, Long secondObjectId);

    @Query("SELECT e FROM Relation e "+
                "WHERE (:firstObjectId IS NULL OR e.firstObjectId=:firstObjectId) "+
                "AND (:secondObjectId IS NULL OR e.secondObjectId=:secondObjectId) "+
                "AND (:relationType IS NULL OR e.relationType=:relationType) ")
    Page<Relation> findRelation(Long firstObjectId, Long secondObjectId, String relationType, Pageable pageable);

}
