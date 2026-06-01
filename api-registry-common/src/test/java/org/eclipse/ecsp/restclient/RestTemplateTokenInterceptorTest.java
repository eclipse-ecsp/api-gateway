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

package org.eclipse.ecsp.restclient;

import org.eclipse.ecsp.security.SecurityContext;
import org.eclipse.ecsp.security.ValidationConfigProperties;
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
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link RestTemplateTokenInterceptor}.
 */
@ExtendWith(MockitoExtension.class)
class RestTemplateTokenInterceptorTest {

    @Mock
    private HttpRequest httpRequest;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private ClientHttpResponse httpResponse;

    private HttpHeaders headers;
    private ValidationConfigProperties config;
    private RestTemplateTokenInterceptor underTest;

    @BeforeEach
    void beforeEach() {
        config = new ValidationConfigProperties();
        underTest = new RestTemplateTokenInterceptor(config);
        headers = new HttpHeaders();
        Mockito.when(httpRequest.getHeaders()).thenReturn(headers);
    }

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    @Test
    void shouldAddAuthHeaderWhenTokenPresent() throws Exception {
        setValidToken("valid-token");
        Mockito.when(httpRequest.getURI()).thenReturn(URI.create("http://internal-svc/api"));
        Mockito.when(execution.execute(Mockito.any(), Mockito.any())).thenReturn(httpResponse);

        ClientHttpResponse result = underTest.intercept(httpRequest, new byte[0], execution);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("Bearer valid-token", headers.getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void shouldSkipWhenAuthHeaderAlreadyPresent() throws Exception {
        setValidToken("valid-token");
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer existing-token");
        Mockito.when(execution.execute(Mockito.any(), Mockito.any())).thenReturn(httpResponse);

        underTest.intercept(httpRequest, new byte[0], execution);

        // Should not have overwritten the header
        Assertions.assertEquals("Bearer existing-token", headers.getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void shouldSkipWhenTokenExpired() throws Exception {
        setExpiredToken("expired-token");
        Mockito.when(httpRequest.getURI()).thenReturn(URI.create("http://internal-svc/api"));
        Mockito.when(execution.execute(Mockito.any(), Mockito.any())).thenReturn(httpResponse);

        underTest.intercept(httpRequest, new byte[0], execution);

        Assertions.assertNull(headers.getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void shouldSkipWhenHostIsExcluded() throws Exception {
        setValidToken("valid-token");
        config.getTokenPropagation().setExcludeHosts(Collections.singletonList("external-api.com"));
        Mockito.when(httpRequest.getURI()).thenReturn(URI.create("http://external-api.com/resource"));
        Mockito.when(execution.execute(Mockito.any(), Mockito.any())).thenReturn(httpResponse);

        underTest.intercept(httpRequest, new byte[0], execution);

        Assertions.assertNull(headers.getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void shouldSkipWhenHostIsExternalAndExternalDisabled() throws Exception {
        setValidToken("valid-token");
        config.getTokenPropagation().setAllowExternalHosts(false);
        config.getTokenPropagation().setIncludeHosts(Collections.singletonList("internal.svc"));
        Mockito.when(httpRequest.getURI()).thenReturn(URI.create("http://unknown-external.com/resource"));
        Mockito.when(execution.execute(Mockito.any(), Mockito.any())).thenReturn(httpResponse);

        underTest.intercept(httpRequest, new byte[0], execution);

        Assertions.assertNull(headers.getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void shouldForwardWhenHostIsInIncludeList() throws Exception {
        setValidToken("valid-token");
        config.getTokenPropagation().setAllowExternalHosts(false);
        config.getTokenPropagation().setIncludeHosts(Collections.singletonList("internal.svc"));
        Mockito.when(httpRequest.getURI()).thenReturn(URI.create("http://internal.svc/resource"));
        Mockito.when(execution.execute(Mockito.any(), Mockito.any())).thenReturn(httpResponse);

        underTest.intercept(httpRequest, new byte[0], execution);

        Assertions.assertEquals("Bearer valid-token", headers.getFirst(HttpHeaders.AUTHORIZATION));
    }

    private void setValidToken(String token) {
        long futureEpoch = Instant.now().plusSeconds(3600).getEpochSecond();
        List<TokenClaim> claims = Arrays.asList(
            new TokenClaim("sub", "user-1"),
            new TokenClaim("exp", futureEpoch)
        );
        SecurityContext.set(token, claims);
    }

    private void setExpiredToken(String token) {
        long pastEpoch = Instant.now().minusSeconds(3600).getEpochSecond();
        List<TokenClaim> claims = Collections.singletonList(new TokenClaim("exp", pastEpoch));
        SecurityContext.set(token, claims);
    }
}
