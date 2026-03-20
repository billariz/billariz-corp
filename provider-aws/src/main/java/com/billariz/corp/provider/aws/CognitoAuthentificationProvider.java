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

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.billariz.corp.database.model.User;
import com.billariz.corp.provider.AuthentificationProvider;
import com.billariz.corp.provider.exception.ProviderException;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeliveryMediumType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserStatusType;

@RequiredArgsConstructor
@Profile(Constants.PROVIDER_NAME)
@Component
public class CognitoAuthentificationProvider implements AuthentificationProvider
{
    private final Cognito cognito;

    @Override
    public void createUser(User user, String userPassword) throws ProviderException
    {
        try
        {
            var cognitoRequest = cognito.adminCreateUserRequest().username(user.getContact().getEmail()).userAttributes(
                    AttributeType.builder().name("name").value(user.getUserName()).build(),
                    AttributeType.builder().name("email").value(user.getContact().getEmail()).build(),
                    AttributeType.builder().name("email_verified").value("true").build(),
                    AttributeType.builder().name("locale").value(user.getDefaultLanguage()).build(),
                    AttributeType.builder().name("picture").value(user.getPicture()).build(),
                    AttributeType.builder().name("profile").value(user.getUserRole()).build(),
                    AttributeType.builder().name("phone_number").value(user.getContact().getPhone1()).build(),
                    AttributeType.builder().name("phone_number_verified").value("true").build(),
                    AttributeType.builder().name("given_name").value(user.getIndividual().getFirstName()).build(),
                    AttributeType.builder().name("family_name").value(user.getIndividual().getLastName()).build())
                                            .temporaryPassword(userPassword).messageAction("SUPPRESS")
                                            .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                                            .forceAliasCreation(Boolean.FALSE).build();
                                            
            var createUserWithTempPassword = cognito.getIdentifyProvider().adminCreateUser(cognitoRequest);
            if (createUserWithTempPassword.user().userStatus() != UserStatusType.FORCE_CHANGE_PASSWORD)
                throw new IllegalArgumentException("Unexpected userStatus: " + createUserWithTempPassword.user().userStatus());
        }
        catch (Exception e)
        {
            throw new ProviderException("Unable to create user " + user, e);
        }
    }

    @Override
    public void updateUser(User user) throws ProviderException
    {
        try
        {
            var cognitoRequest = cognito.adminUpdateUserAttributesRequest().username(user.getContact().getEmail()).userAttributes(
                    AttributeType.builder().name("name").value(user.getContact().getEmail()).build(),
                    AttributeType.builder().name("locale").value(user.getDefaultLanguage()).build(),
                    AttributeType.builder().name("picture").value(user.getPicture()).build(),
                    AttributeType.builder().name("profile").value(user.getUserRole()).build(),
                    AttributeType.builder().name("phone_number").value(user.getContact().getPhone1()).build(),
                    AttributeType.builder().name("phone_number_verified").value("true").build(),
                    AttributeType.builder().name("given_name").value(user.getIndividual().getFirstName()).build(),
                    AttributeType.builder().name("family_name").value(user.getIndividual().getLastName()).build()).build();

            cognito.getIdentifyProvider().adminUpdateUserAttributes(cognitoRequest);

        }
        catch (Exception e)
        {
            throw new ProviderException("Unable to create user " + user, e);
        }
    }

    @Override
    public void deleteUser(User user) throws ProviderException
    {
        try
        {
            var request = cognito.adminDeleteUserRequest().username(user.getUserName()).build();
            cognito.getIdentifyProvider().adminDeleteUser(request);
        }
        catch (Exception e)
        {
            throw new ProviderException("Unable to delete user " + user, e);
        }
    }

    @Override
    public void disableUser(User user) throws ProviderException
    {
        try
        {
            var request = cognito.adminDisableUserRequest().username(user.getUserName()).build();
            cognito.getIdentifyProvider().adminDisableUser(request);
        }
        catch (Exception e)
        {
            throw new ProviderException("Unable to disable user " + user, e);
        }
    }

    @Override
    public void enableUser(User user) throws ProviderException
    {
        try
        {
            var request = cognito.adminEnableUserRequest().username(user.getUserName()).build();
            cognito.getIdentifyProvider().adminEnableUser(request);
        }
        catch (Exception e)
        {
            throw new ProviderException("Unable to anable user " + user, e);
        }
    }
}
