/********************************************************************************
 * Copyright (c) 2023-24 Harman International
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.security.validator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.ecsp.interceptors.SecurityRequirementCache;
import org.eclipse.ecsp.interceptors.TokenValidationInterceptor;
import org.eclipse.ecsp.security.ScopeOverrideProperties;
import org.eclipse.ecsp.security.SecurityContext;
import org.eclipse.ecsp.security.ValidationConfigProperties;
import org.eclipse.ecsp.tokenvalidator.ScopeMatchMode;
import org.eclipse.ecsp.tokenvalidator.TokenValidator;
import org.eclipse.ecsp.tokenvalidator.exception.InvalidSignatureException;
import org.eclipse.ecsp.tokenvalidator.exception.TokenExpiredException;
import org.eclipse.ecsp.tokenvalidator.impl.DefaultScopeValidator;
import org.eclipse.ecsp.tokenvalidator.model.TokenClaim;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;
import tools.jackson.databind.ObjectMapper;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link TokenValidationInterceptor}.
 */
@ExtendWith(MockitoExtension.class)
class TokenValidationInterceptorTest {

    @Mock
    private TokenValidator tokenValidator;

    @Mock
    private SecurityRequirementCache securityRequirementCache;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HandlerMethod handlerMethod;

    private ScopeOverrideProperties scopeOverrideProperties;

    private ValidationConfigProperties config;
    private TokenValidationInterceptor underTest;
    private StringWriter responseWriter;

    @BeforeEach
    void beforeEach() throws Exception {
        config = new ValidationConfigProperties();
        config.getSecurity().setEnabled(true);
        scopeOverrideProperties = new ScopeOverrideProperties();
        underTest = new TokenValidationInterceptor(tokenValidator, config, securityRequirementCache, new ObjectMapper(),
                new DefaultScopeValidator(Set.of(), ScopeMatchMode.ANY), scopeOverrideProperties);
        responseWriter = new StringWriter();
        Mockito.lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    @Test
    void shouldReturnTrueWhenSecurityDisabled() throws Exception {
        config.getSecurity().setEnabled(false);

        boolean result = underTest.preHandle(request, response, handlerMethod);

        Assertions.assertTrue(result);
        Mockito.verifyNoInteractions(tokenValidator, securityRequirementCache);
    }

    @Test
    void shouldReturnTrueWhenHandlerIsNotHandlerMethod() throws Exception {
        boolean result = underTest.preHandle(request, response, new Object());

        Assertions.assertTrue(result);
        Mockito.verifyNoInteractions(tokenValidator, securityRequirementCache);
    }

    @Test
    void shouldReturnTrueWhenCacheReportsNotSecured() throws Exception {
        Mockito.when(securityRequirementCache.isSecured(handlerMethod)).thenReturn(false);

        boolean result = underTest.preHandle(request, response, handlerMethod);

        Assertions.assertTrue(result);
        Mockito.verifyNoInteractions(tokenValidator);
    }

    @Test
    void shouldReturn401WhenAuthorizationHeaderMissing() throws Exception {
        Mockito.when(securityRequirementCache.isSecured(handlerMethod)).thenReturn(true);
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        boolean result = underTest.preHandle(request, response, handlerMethod);

        Assertions.assertFalse(result);
        Mockito.verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void shouldReturn401WhenAuthorizationHeaderMalformed() throws Exception {
        Mockito.when(securityRequirementCache.isSecured(handlerMethod)).thenReturn(true);
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcjpwYXNz");

        boolean result = underTest.preHandle(request, response, handlerMethod);

        Assertions.assertFalse(result);
        Mockito.verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void shouldReturn401WhenTokenValidationFails() throws Exception {
        Mockito.when(securityRequirementCache.isSecured(handlerMethod)).thenReturn(true);
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer bad-token");
        Mockito.when(tokenValidator.validate("bad-token"))
            .thenThrow(new InvalidSignatureException("invalid signature"));

        boolean result = underTest.preHandle(request, response, handlerMethod);

        Assertions.assertFalse(result);
        Mockito.verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void shouldReturn401WhenTokenExpired() throws Exception {
        Mockito.when(securityRequirementCache.isSecured(handlerMethod)).thenReturn(true);
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer expired-token");
        Mockito.when(tokenValidator.validate("expired-token"))
            .thenThrow(new TokenExpiredException("token is expired"));

        boolean result = underTest.preHandle(request, response, handlerMethod);

        Assertions.assertFalse(result);
        Mockito.verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void shouldStoreClaimsInSecurityContextWhenTokenIsValid() throws Exception {
        List<TokenClaim> claims = Collections.singletonList(new TokenClaim("sub", "user-42"));
        Mockito.when(securityRequirementCache.isSecured(handlerMethod)).thenReturn(true);
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid-token");
        Mockito.when(tokenValidator.validate("valid-token")).thenReturn(claims);

        boolean result = underTest.preHandle(request, response, handlerMethod);

        Assertions.assertTrue(result);
        Assertions.assertTrue(SecurityContext.getToken().isPresent());
        Assertions.assertEquals("valid-token", SecurityContext.getToken().get());
        Assertions.assertEquals("user-42", SecurityContext.getUserId().orElse(null));
    }

    @Test
    void shouldClearSecurityContextInAfterCompletion() {
        List<TokenClaim> claims = Collections.emptyList();
        SecurityContext.set("some-token", claims);

        underTest.afterCompletion(request, response, handlerMethod, null);

        Assertions.assertFalse(SecurityContext.getToken().isPresent());
    }

    @Test
    void shouldClearSecurityContextWhenExceptionOccurs() {
        List<TokenClaim> claims = Collections.emptyList();
        SecurityContext.set("some-token", claims);

        underTest.afterCompletion(request, response, handlerMethod, new RuntimeException("oops"));

        Assertions.assertFalse(SecurityContext.getToken().isPresent());
    }
}
