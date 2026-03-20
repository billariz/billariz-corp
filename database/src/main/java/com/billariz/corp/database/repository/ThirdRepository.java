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
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.Third;
import com.billariz.corp.database.projection.inThird;

@RepositoryRestResource(excerptProjection = inThird.class)
public interface ThirdRepository extends RepositoryReadWriteDelete<Third, Long>
{
    @Query("SELECT a.third FROM Actor a WHERE a.role=:role AND a.perimeterId IN (SELECT p.perimeterId FROM ContractPerimeter p WHERE p.contractId=:contractId)")
    Optional<Third> findByContractInPerimeter(@Param("role") String thirdRole, @Param("contractId") long contractId);

    // @Query("SELECT a FROM Third a WHERE (:perimeterId IS NULL OR a.id IN (SELECT act.perimeterId FROM Actor act WHERE act.perimeterId=:perimeterId)) AND (:firstName IS NULL OR lower(a.individual.firstName)=lower(:firstName)) AND (:lastName IS NULL OR lower(a.individual.lastName)=lower(:lastName)) AND (:type IS NULL OR a.type=:type) AND (:id IS NULL OR a.id=:id) AND (:email IS NULL OR a.contact.email=:email) AND (:phone IS NULL OR a.contact.phone1=:phone OR a.contact.phone2=:phone OR a.contact.phone3=:phone) AND (a.company IS NULL OR ((:companyName IS NULL OR lower(a.company.companyName)=lower(:companyName)) AND (:identificationId IS NULL OR a.company.identificationId=:identificationId) AND (:vatId IS NULL OR a.company.vatId=:vatId)))")
    // Page<Third> findThird(Long id, Long perimeterId, String firstName, String lastName, String type, String phone, String email, String companyName, String identificationId, String vatId, Pageable pageable);

    @Query("SELECT a FROM Third a " +
       "LEFT JOIN a.individual individual " +
       "LEFT JOIN a.contact contact " +
       "LEFT JOIN a.company company " +
       "LEFT JOIN a.financialInformation financialInformation " +
       "WHERE (:perimeterId IS NULL OR a.id IN (SELECT act.thirdId FROM Actor act WHERE act.perimeterId=:perimeterId)) " +
       "AND (:perimeterRef IS NULL OR a.id IN (SELECT act.thirdId FROM Actor act WHERE act.perimeter.reference=:perimeterRef)) " +
       "AND (:firstName IS NULL OR lower(individual.firstName) = lower(:firstName)) " +
       "AND (:lastName IS NULL OR lower(individual.lastName) = lower(:lastName)) " +
       "AND (:type IS NULL OR a.type = :type) " +
       "AND (:id IS NULL OR a.id = :id) " +
       "AND (:email IS NULL OR contact.email = :email) " +
       "AND (:phone IS NULL OR contact.phone1 = :phone OR contact.phone2 = :phone OR contact.phone3 = :phone) " +
       "AND (:paymentMode IS NULL OR financialInformation.paymentMode = :paymentMode) " +
       "AND (:domicilationId IS NULL OR financialInformation.domicilationId = :domicilationId) " +
       "AND (:iban IS NULL OR financialInformation.iban = :iban) " +
       "AND (company IS NULL OR ((:companyName IS NULL OR lower(company.companyName) = lower(:companyName)) " +
       "AND (:identificationId IS NULL OR company.identificationId = :identificationId) " +
       "AND (:vatId IS NULL OR company.vatId = :vatId)))")
    Page<Third> findThird(Long id, 
                            Long perimeterId,
                            String perimeterRef,
                            String firstName, 
                            String lastName, 
                            String type, 
                            String phone, 
                            String email, 
                            String companyName, 
                            String identificationId, 
                            String vatId,
                            String paymentMode,
                            String iban,
                            String domicilationId,
                            Pageable pageable);

}
