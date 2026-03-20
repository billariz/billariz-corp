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

package com.billariz.corp.provider.aws.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.billariz.corp.provider.BaseConstants;
import com.billariz.corp.provider.aws.Constants;
import com.billariz.corp.provider.aws.S3StorageProvider;
import com.billariz.corp.provider.aws.SQSQueueProvider;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;

@Profile(Constants.PROVIDER_NAME)
@Configuration
public class AwsConfig
{
    @Bean
    public CognitoIdentityProviderClient cognitoIdentityProviderClient(AwsProviderConfig config)
    {
        return CognitoIdentityProviderClient.builder().region(config.cognito().userPoolRegion()).build();
    }

    @Bean(BaseConstants.BEAN_QUEUE_BILLING)
    public SQSQueueProvider queueBilling(AwsProviderConfig config, SqsClient client)
    {
        var queue = config.queue().billing();

        return new SQSQueueProvider(client, queue.url(), queue.maxNumberOfMessages());
    }

    @Bean(BaseConstants.BEAN_QUEUE_EVENT_MANAGER)
    public SQSQueueProvider queueEventManger(AwsProviderConfig config, SqsClient client)
    {
        var queue = config.queue().eventManager();

        return new SQSQueueProvider(client, queue.url(), queue.maxNumberOfMessages());
    }

    @Bean(BaseConstants.BEAN_QUEUE_INVOICE_REQUEST)
    public SQSQueueProvider queueInvoiceRequest(AwsProviderConfig config, SqsClient client)
    {
        var queue = config.queue().invoiceRequest();

        return new SQSQueueProvider(client, queue.url(), queue.maxNumberOfMessages());
    }

    @Bean(BaseConstants.BEAN_QUEUE_INVOICE_RESPONSE)
    public SQSQueueProvider queueInvoiceResponse(AwsProviderConfig config, SqsClient client)
    {
        var queue = config.queue().invoiceResponse();

        return new SQSQueueProvider(client, queue.url(), queue.maxNumberOfMessages());
    }

    @Bean
    public SqsClient sqsClient()
    {
        return SqsClient.create();
    }

    @Bean
    public S3Client s3Client()
    {
        return S3Client.create();
    }

    @Bean
    public S3Presigner s3Presigner()
    {
        return S3Presigner.create();
    }

    @Bean(BaseConstants.BEAN_STORAGE_DOCUMENT)
    public S3StorageProvider storageDocument(AwsProviderConfig config, S3Client s3, S3Presigner presigner)
    {
        return new S3StorageProvider(s3, presigner, config.bucket().document());
    }

    @Bean(BaseConstants.BEAN_STORAGE_INVOICE)
    public S3StorageProvider storageInvoice(AwsProviderConfig config, S3Client s3, S3Presigner presigner)
    {
        return new S3StorageProvider(s3, presigner, config.bucket().invoice());
    }
}
