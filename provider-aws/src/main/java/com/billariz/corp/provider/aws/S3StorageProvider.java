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

package com.billariz.corp.provider.aws;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import com.billariz.corp.provider.StorageProvider;
import com.billariz.corp.provider.exception.ProviderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.utils.IoUtils;

@RequiredArgsConstructor
@Slf4j
public class S3StorageProvider implements StorageProvider
{
    private final S3Client    s3;

    private final S3Presigner presigner;

    private final String      bucketName;

    @Override
    public String generateDirectDownloadLink(String... path) throws ProviderException
    {
        var key = toPath(path);
        log.debug("Generating presigned URL for key: {} of path : {}", key, path);
        var objectRequest = GetObjectRequest.builder().bucket(bucketName).key(key).build();
        var presignRequest = GetObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(10)).getObjectRequest(objectRequest).build();
        var presignedRequest = presigner.presignGetObject(presignRequest);

        log.info("Presigned URL: [{}]", presignedRequest.url().toString());
        log.info("HTTP method: [{}]", presignedRequest.httpRequest().method());
        return presignedRequest.url().toExternalForm();
    }

    @Override
    public byte[] read(String... path) throws ProviderException
    {
        var key = toPath(path);

        try
        {
            var baos = new ByteArrayOutputStream();
            var operation = GetObjectRequest.builder().bucket(bucketName).key(key).build();

            IoUtils.copy(s3.getObject(operation), baos);

            log.debug("load {} bytes from {}", baos.size(), key);
            return baos.toByteArray();

        }
        catch (Exception e)
        {
            throw new ProviderException("Unable to upload file: " + path, e);
        }
    }

    @Override
    public void save(byte[] content, String... path) throws ProviderException
    {
        var key = toPath(path);

        try
        {
            var operation = PutObjectRequest.builder().bucket(bucketName).key(key).build();
            var response = s3.putObject(operation, RequestBody.fromBytes(content));

            log.debug("save {} bytes to {} : response={}", content.length, key, response);

        }
        catch (Exception e)
        {
            throw new ProviderException("Unable to upload file: " + path, e);
        }
    }

    private String toPath(String... path)
    {
        return String.join("/", path);
    }
}
