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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteFull;
import com.billariz.corp.database.model.ContractPointOfService;

@RepositoryRestResource
public interface ContractPointOfServiceRepository extends RepositoryReadWriteFull<ContractPointOfService, Long>
{
    List<ContractPointOfService> findAllByContractId(Long contractId);
    
    @Query("SELECT cp FROM ContractPointOfService cp " +
       "JOIN PointOfService p ON cp.posId = p.id " +
       "WHERE p.reference = :posRef")
    List<ContractPointOfService> findAllByPosRef(String posRef);

    @Query("SELECT cp FROM ContractPointOfService cp " +
       "JOIN PointOfService p ON cp.posId = p.id " +
       "WHERE p.reference = :reference " +
       "AND cp.endDate IS NULL")
    ContractPointOfService findFirstByReferenceAndEndDateIsNull(@Param("reference") String reference);


    ContractPointOfService findByPosIdAndContractId(Long posId, Long contractId);

    @Query("SELECT cp FROM ContractPointOfService cp " +
            "WHERE (:posId IS NULL OR cp.posId = :posId) " +
            "AND cp.contractId = :contractId")
    List<ContractPointOfService> findAllByPosIdAndContractId(Long posId, Long contractId);
    
}