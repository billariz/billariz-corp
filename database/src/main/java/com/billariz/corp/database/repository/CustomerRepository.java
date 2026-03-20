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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.database.model.Customer;
import com.billariz.corp.database.projection.inCustomer;

@RepositoryRestResource(excerptProjection = inCustomer.class)
public interface CustomerRepository extends RepositoryReadWriteDelete<Customer, Long>
{
        @Query("SELECT a.status FROM Customer a WHERE a.id=:id")
        String findStatusById(Long id);

        @Query("SELECT c FROM Customer c "+
                        "LEFT JOIN Perimeter p ON c.id = p.customerId " +
                        "LEFT JOIN Individual i ON c.individual.id = i.id " +
                        "LEFT JOIN Contact ct ON c.contact.id = ct.id " +
                        "LEFT JOIN Company co ON c.company.id = co.id " +
                        "WHERE " +
                        "(:contractId IS NULL OR c.id IN (SELECT cp.perimeter.customerId FROM ContractPerimeter cp WHERE cp.contractId=:contractId))  " +
                        "AND (:perimeterId IS NULL OR p.id = :perimeterId) " +
                        "AND (:perimeterRef IS NULL OR lower(p.reference) = lower(:perimeterRef)) " +
                        "AND (:customerRef IS NULL OR lower(c.reference)=lower(:customerRef)) " +
                        "AND (:firstName IS NULL OR lower(i.firstName) LIKE lower(CONCAT('%', :firstName, '%'))) " +
                        "AND (:lastName IS NULL OR lower(i.lastName) LIKE lower(CONCAT('%', :lastName, '%'))) " +
                        "AND (:status IS NULL OR c.status=:status)  " +
                        "AND (:type IS NULL OR c.type=:type)  " +
                        "AND (:category IS NULL OR c.category=:category)  " +
                        "AND (:id IS NULL OR c.id=:id)  " +
                        "AND (:email IS NULL OR lower(ct.email) = lower(:email)) " +
                        "AND (:phone IS NULL OR lower(ct.phone1) = lower(:phone) OR lower(ct.phone2) = lower(:phone) OR lower(ct.phone3) = lower(:phone)) " +     "AND (:companyName IS NULL OR lower(co.companyName) LIKE lower(CONCAT('%', :companyName, '%'))) " +
                        "AND (:identificationId IS NULL OR co.identificationId = :identificationId) " +
                        "AND (:vatId IS NULL OR co.vatId = :vatId)")
        Page<Customer> findCustomer(Long id, 
                                Long perimeterId, 
                                Long contractId, 
                                String category, 
                                String customerRef, 
                                String firstName, 
                                String lastName, 
                                String type, 
                                String status, 
                                String phone, 
                                String email, 
                                String companyName, 
                                String identificationId, 
                                String vatId, 
                                String perimeterRef,
                                Pageable pageable);

        @Query("SELECT COUNT(CASE WHEN c.status='CLIENT' THEN 1.0 END),AVG(CASE WHEN c.status='CLIENT' THEN 1.0 ELSE 0 END),count(*) FROM Customer c")
        Object customersConvertedChart();

        @Query("SELECT COUNT(CASE WHEN c.status='PROSPECT' THEN 1.0 END),AVG(CASE WHEN c.status='PROSPECT' THEN 1.0 ELSE 0 END),count(*) FROM Customer c")
        Object customersApplicationChart();

        @Query("SELECT COUNT(CASE WHEN c.status='LEAD' THEN 1.0 END),AVG(CASE WHEN c.status='LEAD' THEN 1.0 ELSE 0 END),count(*) FROM Customer c")
        Object customersLeadChart();

}
