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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import com.billariz.corp.provider.exception.ProviderException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ActiveProfiles(Constants.PROVIDER_NAME)
@ContextConfiguration(classes = TestConfig.class)
@SpringBootTest
public class S3StorageProviderTest
{
    @Autowired
    private S3StorageProvider storageProvider;

    @MockBean
    private S3Client          awsClient;

    @Test
    void testGenerateLink() throws ProviderException
    {
        var ret = storageProvider.generateDirectDownloadLink("path1", "path2");

        assertNotNull(ret);
        assertFalse(ret.isEmpty());
    }

    @Test
    void testReadFailed() throws ProviderException
    {
        when(awsClient.getObject(any(GetObjectRequest.class))).thenThrow(S3Exception.builder().build());
        assertThrows(ProviderException.class, () -> storageProvider.read("path1", "path2"));
    }

    @Test
    void testReadOk() throws ProviderException
    {
        var input = "input".getBytes();
        var response = new ResponseInputStream<GetObjectResponse>(GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream(input)));
        when(awsClient.getObject(any(GetObjectRequest.class))).thenReturn(response);

        var ret = storageProvider.read("path1", "path2");

        assertNotNull(ret);
        assertArrayEquals(input, ret);
    }

    @Test
    void testSaveFailed() throws ProviderException
    {
        when(awsClient.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(S3Exception.builder().build());
        assertThrows(ProviderException.class, () -> storageProvider.save("test".getBytes(), "path1", "path2"));
    }

    @Test
    void testSaveOk() throws ProviderException
    {
        when(awsClient.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(PutObjectResponse.builder().eTag("eTag").build());
        assertDoesNotThrow(() -> storageProvider.save("test".getBytes(), "path1", "path2"));
    }
}
