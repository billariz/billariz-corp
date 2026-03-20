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
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.billariz.corp.database.model.DocumentInvoice;

@Repository
public interface DocumentInvoiceRepository extends CrudRepository<DocumentInvoice, String>
{
    @Query("SELECT d FROM DocumentInvoice d WHERE d.contractId=:contractId AND d.id IN (SELECT p.invoiceId from Process p WHERE p.invoiceId=d.id and p.nature=:nature)")
    List<DocumentInvoice> searchByContractIdAndProcessNature(@Param("contractId") Long contractId, @Param("nature") String nature);
}
