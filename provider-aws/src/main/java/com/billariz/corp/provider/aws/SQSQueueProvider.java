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

import java.util.List;

import com.billariz.corp.provider.QueueProvider;
import com.billariz.corp.provider.exception.ProviderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@RequiredArgsConstructor
@Slf4j
public class SQSQueueProvider implements QueueProvider
{
    private final SqsClient sqsClient;

    private final String    queueUrl;

    private final Integer   maxNumberOfMessages;

    @Override
    public List<String> consume() throws ProviderException
    {
        try
        {
            var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(maxNumberOfMessages).build());
            var messages = response.messages();

            log.debug("receiveMessage {} from {}", messages, queueUrl);

            if (!messages.isEmpty())
            {
                var entries = messages.stream().map(
                        m -> DeleteMessageBatchRequestEntry.builder().id(m.messageId()).receiptHandle(m.receiptHandle()).build()).toList();

                sqsClient.deleteMessageBatch(DeleteMessageBatchRequest.builder().queueUrl(queueUrl).entries(entries).build());
            }
            return messages.stream().map(Message::body).toList();
        }
        catch (Exception e)
        {
            throw new ProviderException("Unable to consume message", e);
        }
    }

    @Override
    public void publish(String message, boolean isFifo) throws ProviderException
    {
        try
        {
            if (!isFifo)
                sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(message).build());
            else
                sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageGroupId("ALL").messageBody(message).build());
            log.debug("sendMessage {} to {}", message, queueUrl);
        }
        catch (Exception e)
        {
            throw new ProviderException("Unable to send message: " + message, e);
        }
    }
}
