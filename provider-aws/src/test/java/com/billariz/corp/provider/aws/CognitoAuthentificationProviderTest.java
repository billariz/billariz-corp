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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import com.billariz.corp.database.model.Contact;
import com.billariz.corp.database.model.User;
import com.billariz.corp.provider.exception.ProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceNotFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserStatusType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;
import software.amazon.awssdk.services.s3.S3Client;

@ActiveProfiles(Constants.PROVIDER_NAME)
@ContextConfiguration(classes = TestConfig.class)
@SpringBootTest(classes = { CognitoAuthentificationProvider.class, Cognito.class })
public class CognitoAuthentificationProviderTest
{
    @Autowired
    private CognitoAuthentificationProvider cognitoProvider;

    @MockBean
    private CognitoIdentityProviderClient   awsClient;

    @MockBean
    private S3Client                        s3Client;

    @MockBean
    private User                            user;

    @Test
    void testErrorCognito() throws ProviderException
    {
        User user = new User();
        Contact contact = new Contact();
        contact.setEmail("userEmail");
        user.setContact(contact);
        Mockito.when(awsClient.adminCreateUser(any(AdminCreateUserRequest.class))).thenThrow(ResourceNotFoundException.builder().build());
        assertThrows(ProviderException.class, () -> cognitoProvider.createUser(user, "userPassword"));
    }

    @Test
    void testUserNotCreated() throws ProviderException
    {
        User user = new User();
        Contact contact = new Contact();
        contact.setEmail("userEmail");
        user.setContact(contact);
        var createUser = AdminCreateUserResponse.builder().user(UserType.builder().userStatus(UserStatusType.UNKNOWN).build()).build();

        Mockito.when(awsClient.adminCreateUser(any(AdminCreateUserRequest.class))).thenReturn(createUser);
        assertThrows(ProviderException.class, () -> cognitoProvider.createUser(user, "userPassword"));
    }

    // @Test
    // void testUserCreatedButWithInvalidChallenge() throws ProviderException
    // {
    // var createUser =
    // AdminCreateUserResponse.builder().user(UserType.builder().userStatus(UserStatusType.FORCE_CHANGE_PASSWORD).build()).build();
    // var initiateAuth =
    // AdminInitiateAuthResponse.builder().challengeName(ChallengeNameType.MFA_SETUP).build();

    // Mockito.when(awsClient.adminCreateUser(any(AdminCreateUserRequest.class))).thenReturn(createUser);
    // Mockito.when(awsClient.adminInitiateAuth(any(AdminInitiateAuthRequest.class))).thenReturn(initiateAuth);
    // assertThrows(ProviderException.class, () ->
    // cognitoProvider.createUser("userName", "userEmail", "userPassword"));
    // }

    // @Test
    // void
    // testUserCreatedLogguedButCantChangePasswordBecauseChallengeUnexpected()
    // throws ProviderException
    // {
    // var createUser =
    // AdminCreateUserResponse.builder().user(UserType.builder().userStatus(UserStatusType.FORCE_CHANGE_PASSWORD).build()).build();
    // var initiateAuth =
    // AdminInitiateAuthResponse.builder().challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED).build();
    // var changePassword =
    // AdminRespondToAuthChallengeResponse.builder().challengeName(ChallengeNameType.ADMIN_NO_SRP_AUTH).build();

    // Mockito.when(awsClient.adminCreateUser(any(AdminCreateUserRequest.class))).thenReturn(createUser);
    // Mockito.when(awsClient.adminInitiateAuth(any(AdminInitiateAuthRequest.class))).thenReturn(initiateAuth);
    // Mockito.when(awsClient.adminRespondToAuthChallenge(any(AdminRespondToAuthChallengeRequest.class))).thenReturn(changePassword);
    // assertThrows(ProviderException.class, () ->
    // cognitoProvider.createUser("userName", "userEmail", "userPassword"));
    // }

    // @Test
    // void testUserCreatedLogguedButCantChangePasswordBecauseEmptyResponse()
    // throws ProviderException
    // {
    // var createUser =
    // AdminCreateUserResponse.builder().user(UserType.builder().userStatus(UserStatusType.FORCE_CHANGE_PASSWORD).build()).build();
    // var initiateAuth =
    // AdminInitiateAuthResponse.builder().challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED).build();
    // var changePassword =
    // AdminRespondToAuthChallengeResponse.builder().build();

    // Mockito.when(awsClient.adminCreateUser(any(AdminCreateUserRequest.class))).thenReturn(createUser);
    // Mockito.when(awsClient.adminInitiateAuth(any(AdminInitiateAuthRequest.class))).thenReturn(initiateAuth);
    // Mockito.when(awsClient.adminRespondToAuthChallenge(any(AdminRespondToAuthChallengeRequest.class))).thenReturn(changePassword);
    // assertThrows(ProviderException.class, () ->
    // cognitoProvider.createUser("userName", "userEmail", "userPassword"));
    // }

    // @Test
    // void testUserCreatedFullOk() throws ProviderException
    // {
    // var createUser =
    // AdminCreateUserResponse.builder().user(UserType.builder().userStatus(UserStatusType.FORCE_CHANGE_PASSWORD).build()).build();
    // var initiateAuth =
    // AdminInitiateAuthResponse.builder().challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED).build();
    // var changePassword =
    // AdminRespondToAuthChallengeResponse.builder().authenticationResult(AuthenticationResultType.builder().build()).build();

    // Mockito.when(awsClient.adminCreateUser(any(AdminCreateUserRequest.class))).thenReturn(createUser);
    // Mockito.when(awsClient.adminInitiateAuth(any(AdminInitiateAuthRequest.class))).thenReturn(initiateAuth);
    // Mockito.when(awsClient.adminRespondToAuthChallenge(any(AdminRespondToAuthChallengeRequest.class))).thenReturn(changePassword);
    // assertDoesNotThrow(() -> cognitoProvider.createUser("userName",
    // "userEmail", "userPassword"));
    // }

    // @Test
    // void testChangePasswordButFailed() throws ProviderException
    // {
    // Mockito.when(awsClient.changePassword(any(ChangePasswordRequest.class))).thenThrow(UserNotFoundException.builder().build());
    // assertThrows(ProviderException.class, () ->
    // cognitoProvider.changePassword("accessToken", "oldPassword",
    // "newPassword"));
    // }

    // @Test
    // void testChangePasswordOk() throws ProviderException
    // {
    // var changePassword = ChangePasswordResponse.builder().build();

    // Mockito.when(awsClient.changePassword(any(ChangePasswordRequest.class))).thenReturn(changePassword);
    // assertDoesNotThrow(() -> cognitoProvider.changePassword("accessToken",
    // "oldPassword", "newPassword"));
    // }

    // @Test
    // void testUserLoginButUnknownUser() throws ProviderException
    // {
    // Mockito.when(awsClient.adminInitiateAuth(any(AdminInitiateAuthRequest.class))).thenThrow(UserNotFoundException.builder().build());
    // assertThrows(ProviderException.class, () ->
    // cognitoProvider.loginUser("userName", "userPassword"));
    // }

    // @Test
    // void testDeleteUserButFailed() throws ProviderException
    // {
    // Mockito.when(awsClient.adminDeleteUser(any(AdminDeleteUserRequest.class))).thenThrow(UserNotFoundException.builder().build());
    // assertThrows(ProviderException.class, () ->
    // cognitoProvider.deleteUser("userName"));
    // }

    // @Test
    // void testDeleteUserOk() throws ProviderException
    // {
    // var resetPassword = AdminDeleteUserResponse.builder().build();

    // Mockito.when(awsClient.adminDeleteUser(any(AdminDeleteUserRequest.class))).thenReturn(resetPassword);
    // assertDoesNotThrow(() -> cognitoProvider.deleteUser("userName"));
    // }

    // @Test
    // void testUserLoginOk() throws ProviderException
    // {
    // var initiateAuth =
    // AdminInitiateAuthResponse.builder().authenticationResult(
    // AuthenticationResultType.builder().accessToken("accessToken").expiresIn(1).idToken("idToken").refreshToken("refreshToken").tokenType(
    // "tokenType").build()).build();

    // Mockito.when(awsClient.adminInitiateAuth(any(AdminInitiateAuthRequest.class))).thenReturn(initiateAuth);

    // var auth = cognitoProvider.loginUser("userName", "userPassword");

    // assertNotNull(auth);
    // assertEquals(Integer.valueOf(1), auth.expiresIn());
    // assertEquals("accessToken", auth.accessToken());
    // assertEquals("idToken", auth.idToken());
    // assertEquals("refreshToken", auth.refreshToken());
    // assertEquals("tokenType", auth.tokenType());
    // }

    // @Test
    // void testResetPasswordButFailed() throws ProviderException
    // {
    // Mockito.when(awsClient.adminSetUserPassword(any(AdminSetUserPasswordRequest.class))).thenThrow(UserNotFoundException.builder().build());
    // assertThrows(ProviderException.class, () ->
    // cognitoProvider.resetPassword("userName", "password"));
    // }

    // @Test
    // void testResetPasswordOk() throws ProviderException
    // {
    // var resetPassword = AdminSetUserPasswordResponse.builder().build();

    // Mockito.when(awsClient.adminSetUserPassword(any(AdminSetUserPasswordRequest.class))).thenReturn(resetPassword);
    // assertDoesNotThrow(() -> cognitoProvider.resetPassword("userName",
    // "password"));
    // }
}
