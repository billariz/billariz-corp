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
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.ContractPerimeter;

@RepositoryRestResource
public interface ContractPerimeterRepository extends RepositoryReadWriteDelete<ContractPerimeter, Long>
{
    Optional<ContractPerimeter> findByContractId(Long contractId);

    @Query("SELECT cp.perimeterId FROM ContractPerimeter cp, Contract c WHERE cp.perimeterId=:perimeterId AND cp.contractId=c.id AND c.status IN (:contractStatus)")
    List<Long> findWithContractStatusInAndPerimeterId(@Param("contractStatus") List<String> contractStatus, @Param("perimeterId") Long perimeterId);
}