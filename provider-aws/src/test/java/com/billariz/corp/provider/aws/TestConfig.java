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

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.billariz.corp.provider.aws.config.AwsProviderConfig;
import com.billariz.corp.provider.aws.config.CognitoConfig;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class TestConfig
{
    @Bean
    public AwsProviderConfig awsProviderConfig()
    {
        var cognito = new CognitoConfig("HmacSHA256", Region.EU_WEST_3, "userPoolId", "userPoolClientId", "userPoolClientSecret");

        return new AwsProviderConfig(null, cognito, null);
    }

    @Bean
    public SQSQueueProvider sqsQueueProvider(SqsClient sqsClient)
    {
        return new SQSQueueProvider(sqsClient, "http://queue-url.com", 10);
    }

    @Bean
    public S3StorageProvider storageBanking(S3Client s3, S3Presigner presigner)
    {
        return new S3StorageProvider(s3, presigner, "bucket");
    }

    @Bean
    @ConditionalOnMissingBean
    public S3Presigner s3Presigner()
    {
        return S3Presigner.builder().region(Region.EU_WEST_3).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public S3Client s3Client()
    {
        return S3Client.builder().region(Region.EU_WEST_3).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SqsClient sqsClient()
    {
        return SqsClient.builder().region(Region.EU_WEST_3).build();
    }
}
