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
import com.billariz.corp.database.model.MeterReadDetail;
import com.billariz.corp.database.model.enumeration.MeterReadSource;

@RepositoryRestResource
public interface MeterReadDetailRepository extends RepositoryReadWriteDelete<MeterReadDetail, Long>
{
        List<MeterReadDetail> findAllByMeterReadIdAndMeasureType(Long meterReadId, String measureType);

        @Query("SELECT m FROM MeterReadDetail m WHERE "+
            "(:type IS NULL OR m.measureType =:type) "+
            //"AND (:source IS NULL OR m.meterRead.source =:source) "+
            "AND (:meterReadId IS NULL OR m.meterReadId =:meterReadId) "+
            "AND (:tou IS NULL OR m.tou =:tou)")
        Page<MeterReadDetail> findMeterReadDetail(Long meterReadId, 
                                            String tou, 
                                            String type, 
                                            //MeterReadSource source, 
                                            Pageable pageable);

        @Query("""
                SELECT DISTINCT m
                FROM MeterReadDetail m
                JOIN BillSegment b ON b.meterReadId = m.meterReadId
                WHERE b.billId = :billId
                """)
        List<MeterReadDetail> findMeterReadDetailByBillId(@Param("billId") Long billId);

}