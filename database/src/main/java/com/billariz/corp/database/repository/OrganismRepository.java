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

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.Organism;
import com.billariz.corp.database.projection.inOrganism;

@RepositoryRestResource(excerptProjection = inOrganism.class)
public interface OrganismRepository extends RepositoryReadWriteDelete<Organism, Long>
{
    List<Organism> findAllByMasterOrganismId(Long masterOrganismId);

    @Query("SELECT s FROM Organism s WHERE (:id IS NULL OR s.id=:id) AND (:master IS NULL OR s.master=:master) AND (:masterOrganismId IS NULL OR s.masterOrganismId=:masterOrganismId) AND (:subCategory IS NULL OR s.subCategory=:subCategory)  AND (:category IS NULL OR s.category=:category) AND (:companyName IS NULL OR lower(s.company.companyName) LIKE lower(CONCAT('%', :companyName, '%')))")
    Page<Organism> findOrganism(@Param("id") Long id, @Param("master") Boolean master, @Param("masterOrganismId") Long masterOrganismId, @Param("subCategory") String subCategory, @Param("category") String category, @Param("companyName") String companyName, Pageable pageable);

}
