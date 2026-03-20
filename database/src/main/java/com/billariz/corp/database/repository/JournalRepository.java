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
import org.springframework.transaction.annotation.Transactional;
import com.billariz.corp.data.RepositoryReadWriteDelete;
import com.billariz.corp.data.RepositoryReadWriteFull;
import com.billariz.corp.database.model.Journal;
import com.billariz.corp.database.model.enumeration.ObjectType;
import com.billariz.corp.database.projection.inJournal;

@RepositoryRestResource(excerptProjection = inJournal.class)
//@Transactional
public interface JournalRepository extends RepositoryReadWriteDelete<Journal, Long>, RepositoryReadWriteFull<Journal, Long>
{
    @Query("SELECT j FROM Journal j " +
                "LEFT JOIN j.user c " +
                "WHERE (:objectId IS NULL OR j.objectId=:objectId) " +
                "AND (:id IS NULL OR j.id=:id) " +
                "AND (:objectType IS NULL OR j.objectType=:objectType) " +
                "AND (:userName IS NULL OR c.userName=:userName)")
    Page<Journal> findJournal(  Long id,
                                Long objectId, 
                                ObjectType objectType, 
                                String userName,
                                Pageable pageable);

}
