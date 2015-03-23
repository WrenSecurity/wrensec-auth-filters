/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.jaspi.runtime.context;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.jaspi.utils.MessageInfoUtils;
import org.forgerock.json.resource.ResourceException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ContextHandlerTest {

    private ContextHandler contextHandler;

    private MessageInfoUtils messageInfoUtils;

    @BeforeMethod
    public void setUp() {

        messageInfoUtils = mock(MessageInfoUtils.class);

        contextHandler = new ContextHandler(messageInfoUtils);
    }

    @Test
    public void shouldValidateServerAuthModulesWhenAuthModulesNull() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = null;

        //When
        contextHandler.validateServerAuthModuleConformToHttpServletProfile(authModules);

        //Then
    }

    @SuppressWarnings("rawtypes")
    @Test  (expectedExceptions = AuthenticationException.class)
    public void validateServerAuthModulesShouldThrowJaspiAuthExceptionWhenDoesNotSupportEither() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        ServerAuthModule authModule = mock(ServerAuthModule.class);
        authModules.add(authModule);

        given(authModule.getSupportedMessageTypes()).willReturn(new Class[0]);

        //When
        contextHandler.validateServerAuthModuleConformToHttpServletProfile(authModules);

        //Then
        fail();
    }

    @SuppressWarnings("rawtypes")
    @Test  (expectedExceptions = AuthenticationException.class)
    public void validateServerAuthModulesShouldThrowJaspiAuthExceptionWhenOnlySupportsHttpServletRequest()
            throws AuthException {

        //Given
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        ServerAuthModule authModule = mock(ServerAuthModule.class);
        authModules.add(authModule);

        given(authModule.getSupportedMessageTypes()).willReturn(new Class[]{HttpServletRequest.class});

        //When
        contextHandler.validateServerAuthModuleConformToHttpServletProfile(authModules);

        //Then
        fail();
    }

    @SuppressWarnings("rawtypes")
    @Test (expectedExceptions = AuthenticationException.class)
    public void validateServerAuthModulesShouldThrowJaspiAuthExceptionWhenOnlySupportsHttpServletResponse()
            throws AuthException {

        //Given
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        ServerAuthModule authModule = mock(ServerAuthModule.class);
        authModules.add(authModule);

        given(authModule.getSupportedMessageTypes()).willReturn(new Class[]{HttpServletResponse.class});

        //When
        contextHandler.validateServerAuthModuleConformToHttpServletProfile(authModules);

        //Then
        fail();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void shouldValidateServerAuthModules() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        ServerAuthModule authModule = mock(ServerAuthModule.class);
        authModules.add(authModule);

        given(authModule.getSupportedMessageTypes())
                .willReturn(new Class[]{HttpServletRequest.class, HttpServletResponse.class});

        //When
        contextHandler.validateServerAuthModuleConformToHttpServletProfile(authModules);

        //Then
    }

    @Test
    public void shouldValidateServerAuthModulesWhenAuthModuleListContainsNullAuthModule() throws AuthException {

        //Given
        List<ServerAuthModule> authModules = new ArrayList<ServerAuthModule>();
        authModules.add(null);

        //When
        contextHandler.validateServerAuthModuleConformToHttpServletProfile(authModules);

        //Then
    }

    @Test
    public void shouldHandleCompletionWhenAuthStatusIsNull() throws AuthException, IOException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        AuthStatus authStatus = null;

        try {
            //When
            contextHandler.handleCompletion(messageInfo, clientSubject, authStatus);

            //Then
            fail("Expect exception");
        } catch (AuthException e) {
            assertTrue(e.getCause() instanceof ResourceException);
            assertEquals(((ResourceException) e.getCause()).getCode(), 401);
        }
    }

    @Test
    public void shouldHandleCompletionWhenAuthStatusIsNotNullAndPrincipalNameNotSet() throws AuthException,
            IOException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        AuthStatus authStatus = AuthStatus.SUCCESS;
        HttpServletRequest request = mock(HttpServletRequest.class);
        Map<String, Object> contextMap = new HashMap<String, Object>();

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfoUtils.getMap(messageInfo, JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT)).willReturn(contextMap);

        //When
        contextHandler.handleCompletion(messageInfo, clientSubject, authStatus);

        //Then
        verify(request).setAttribute(JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT, contextMap);
        verify(messageInfo, never()).setRequestMessage(anyObject());
    }

    @Test
    public void shouldHandleCompletionWhenAuthStatusIsNotNull() throws AuthException, IOException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        AuthStatus authStatus = AuthStatus.SUCCESS;
        HttpServletRequest request = mock(HttpServletRequest.class);
        Map<String, Object> contextMap = new HashMap<String, Object>();
        Principal principalOne = mock(Principal.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(messageInfoUtils.getMap(messageInfo, JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT)).willReturn(contextMap);
        clientSubject.getPrincipals().add(principalOne);
        given(principalOne.getName()).willReturn("PRN_ONE");

        //When
        contextHandler.handleCompletion(messageInfo, clientSubject, authStatus);

        //Then
        verify(request).setAttribute(JaspiRuntime.ATTRIBUTE_AUTH_PRINCIPAL, "PRN_ONE");
        verify(request).setAttribute(JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT, contextMap);
    }
}