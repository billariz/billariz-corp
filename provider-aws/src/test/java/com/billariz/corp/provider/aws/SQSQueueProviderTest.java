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

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.billariz.corp.provider.exception.ProviderException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

@ActiveProfiles(Constants.PROVIDER_NAME)
@ContextConfiguration(classes = TestConfig.class)
@SpringBootTest
public class SQSQueueProviderTest
{
    @Autowired
    private SQSQueueProvider                          queueProvider;

    @MockBean
    private SqsClient                                 awsClient;

    @Captor
    private ArgumentCaptor<DeleteMessageBatchRequest> captorDeleteMessageBatch;

    @Test
    void testConsumeFailed() throws ProviderException
    {
        when(awsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenThrow(SqsException.builder().build());
        assertThrows(ProviderException.class, () -> queueProvider.consume());
        verify(awsClient, never()).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
    }

    @Test
    void testConsumeOk() throws ProviderException
    {
        var m1 = Message.builder().body("m1").receiptHandle("r1").build();
        var m2 = Message.builder().body("m2").receiptHandle("r2").build();
        var m3 = Message.builder().body("m3").receiptHandle("r3").build();

        when(awsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder().messages(m1, m2, m3).build());
        var ret = queueProvider.consume();
        assertEquals(Arrays.asList(m1.body(), m2.body(), m3.body()), ret);
        verify(awsClient, times(1)).deleteMessageBatch(captorDeleteMessageBatch.capture());
        assertEquals(3, captorDeleteMessageBatch.getValue().entries().size());
        assertEquals(m1.receiptHandle(), captorDeleteMessageBatch.getValue().entries().get(0).receiptHandle());
        assertEquals(m2.receiptHandle(), captorDeleteMessageBatch.getValue().entries().get(1).receiptHandle());
        assertEquals(m3.receiptHandle(), captorDeleteMessageBatch.getValue().entries().get(2).receiptHandle());
    }

    @Test
    void testConsumeOkEmpty() throws ProviderException
    {
        when(awsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder().messages(emptyList()).build());
        var ret = queueProvider.consume();
        assertTrue(ret.isEmpty());
        verify(awsClient, never()).deleteMessageBatch(any(DeleteMessageBatchRequest.class));
    }

    @Test
    void testPublishFailed() throws ProviderException
    {
        when(awsClient.sendMessage(any(SendMessageRequest.class))).thenThrow(SqsException.builder().build());
        assertThrows(ProviderException.class, () -> queueProvider.publish("ok", true));
    }

    @Test
    void testPublishOk() throws ProviderException
    {
        queueProvider.publish("ok", true);
        verify(awsClient).sendMessage(any(SendMessageRequest.class));
    }
}
