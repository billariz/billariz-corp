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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.billariz.corp.provider.aws.config.AwsProviderConfig;
import com.billariz.corp.provider.exception.ProviderException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminEnableUserRequest;

@RequiredArgsConstructor
@Profile(Constants.PROVIDER_NAME)
@Component
public class Cognito
{
    private final AwsProviderConfig             config;

    @Getter
    private final CognitoIdentityProviderClient identifyProvider;

    public AdminCreateUserRequest.Builder adminCreateUserRequest()
    {
        return AdminCreateUserRequest.builder().userPoolId(config.cognito().userPoolId());
    }

    public AdminDisableUserRequest.Builder adminDisableUserRequest()
    {
        return AdminDisableUserRequest.builder().userPoolId(config.cognito().userPoolId());
    }

    public AdminEnableUserRequest.Builder adminEnableUserRequest()
    {
        return AdminEnableUserRequest.builder().userPoolId(config.cognito().userPoolId());
    }

    public AdminUpdateUserAttributesRequest.Builder adminUpdateUserAttributesRequest()
    {
        return AdminUpdateUserAttributesRequest.builder().userPoolId(config.cognito().userPoolId());
    }

    public AdminDeleteUserRequest.Builder adminDeleteUserRequest()
    {
        return AdminDeleteUserRequest.builder().userPoolId(config.cognito().userPoolId());
    }

    public AdminInitiateAuthRequest.Builder adminInitiateAuthRequest()
    {
        return AdminInitiateAuthRequest.builder().userPoolId(config.cognito().userPoolId()).clientId(config.cognito().userPoolClientId());
    }

    public AdminRespondToAuthChallengeRequest.Builder adminRespondToAuthChallengeRequest()
    {
        return AdminRespondToAuthChallengeRequest.builder().userPoolId(config.cognito().userPoolId()).clientId(config.cognito().userPoolClientId());
    }

    public AdminSetUserPasswordRequest.Builder adminSetUserPasswordRequest()
    {
        return AdminSetUserPasswordRequest.builder().userPoolId(config.cognito().userPoolId());
    }

    public String calculateSecretHash(String userName) throws ProviderException
    {
        try
        {
            var mac = Mac.getInstance(config.cognito().secretHashAlgorithm());
            var signingcom = new SecretKeySpec(config.cognito().userPoolClientSecret().getBytes(StandardCharsets.UTF_8),
                    config.cognito().secretHashAlgorithm());

            mac.init(signingcom);
            mac.update(userName.getBytes(StandardCharsets.UTF_8));

            var rawHmac = mac.doFinal(config.cognito().userPoolClientId().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        }
        catch (Exception e)
        {
            throw new ProviderException("Error while calculating", e);
        }
    }

}
