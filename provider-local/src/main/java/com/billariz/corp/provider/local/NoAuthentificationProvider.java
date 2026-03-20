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

package com.billariz.corp.provider.local;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.billariz.corp.database.model.User;
import com.billariz.corp.provider.AuthentificationProvider;
import com.billariz.corp.provider.exception.ProviderException;
import lombok.extern.slf4j.Slf4j;

@Profile(Constants.PROVIDER_NAME)
@Component
@Slf4j
public class NoAuthentificationProvider implements AuthentificationProvider
{
    @Override
    public void createUser(User user, String userPassword) throws ProviderException
    {
        log.info("Create user: userName={} userEmail={}", user.getContact().getEmail(), user.getContact().getEmail());
    }

    @Override
    public void deleteUser(User user) throws ProviderException
    {
        log.info("Create user: userName={} userEmail={}", user.getContact().getEmail(), user.getContact().getEmail());
    }

    @Override
    public void updateUser(User user) throws ProviderException
    {
        log.info("Create user: userName={} userEmail={}", user.getContact().getEmail(), user.getContact().getEmail());
    }

    @Override
    public void disableUser(User user) throws ProviderException
    {
        log.info("Create user: userName={} userEmail={}", user.getContact().getEmail(), user.getContact().getEmail());
    }

    @Override
    public void enableUser(User user) throws ProviderException
    {
        log.info("Create user: userName={} userEmail={}", user.getContact().getEmail(), user.getContact().getEmail());
    }

    // @Override
    // public void changePassword(String accessToken, String oldPassword, String
    // newPassword) throws ProviderException
    // {
    // log.info("Change password: accessToken={}", accessToken);
    // }

    // @Override
    // public void deleteUser(String userName) throws ProviderException
    // {
    // log.info("Delete user: userName={}", userName);
    // }

    // @Override
    // public OAuthTokens loginUser(String userName, String userPassword) throws
    // ProviderException
    // {
    // log.info("Login user: userName={}", userName);
    // return new OAuthTokens("tokenType", "idToken", "accessTocken",
    // "refreshToken", Integer.MAX_VALUE);
    // }

    // @Override
    // public void resetPassword(String userName, String newPassword) throws
    // ProviderException
    // {
    // log.info("Reset password: userName={}", userName);
    // }
}
