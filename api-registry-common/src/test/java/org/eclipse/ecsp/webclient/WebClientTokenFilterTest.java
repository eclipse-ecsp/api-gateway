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

package org.eclipse.ecsp.webclient;

import org.eclipse.ecsp.security.SecurityContext;
import org.eclipse.ecsp.security.ValidationConfigProperties;
import org.eclipse.ecsp.tokenvalidator.model.TokenClaim;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link WebClientTokenFilter}.
 */
@ExtendWith(MockitoExtension.class)
class WebClientTokenFilterTest {

    @Mock
    private ExchangeFunction exchangeFunction;

    @Mock
    private ClientResponse clientResponse;

    private ValidationConfigProperties config;
    private WebClientTokenFilter underTest;

    @BeforeEach
    void beforeEach() {
        config = new ValidationConfigProperties();
        underTest = new WebClientTokenFilter(config);
        Mockito.when(exchangeFunction.exchange(Mockito.any())).thenReturn(Mono.just(clientResponse));
    }

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    @Test
    void shouldAddAuthHeaderWhenTokenPresent() {
        setValidToken("valid-token");
        ClientRequest request = ClientRequest.create(
            org.springframework.http.HttpMethod.GET, URI.create("http://internal-svc/api"))
            .build();

        underTest.filter(request, exchangeFunction).block();

        ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
        Mockito.verify(exchangeFunction).exchange(captor.capture());
        Assertions.assertEquals("Bearer valid-token",
            captor.getValue().headers().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void shouldSkipWhenAuthHeaderAlreadyPresent() {
        setValidToken("valid-token");
        ClientRequest request = ClientRequest.create(
            org.springframework.http.HttpMethod.GET, URI.create("http://internal-svc/api"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer existing-token")
            .build();

        underTest.filter(request, exchangeFunction).block();

        ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
        Mockito.verify(exchangeFunction).exchange(captor.capture());
        Assertions.assertEquals("Bearer existing-token",
            captor.getValue().headers().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void shouldSkipWhenTokenExpired() {
        setExpiredToken("expired-token");
        ClientRequest request = ClientRequest.create(
            org.springframework.http.HttpMethod.GET, URI.create("http://internal-svc/api"))
            .build();

        underTest.filter(request, exchangeFunction).block();

        ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
        Mockito.verify(exchangeFunction).exchange(captor.capture());
        Assertions.assertNull(captor.getValue().headers().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void shouldSkipWhenHostIsExcluded() {
        setValidToken("valid-token");
        config.getTokenPropagation().setExcludeHosts(Collections.singletonList("external-api.com"));
        ClientRequest request = ClientRequest.create(
            org.springframework.http.HttpMethod.GET, URI.create("http://external-api.com/resource"))
            .build();

        underTest.filter(request, exchangeFunction).block();

        ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
        Mockito.verify(exchangeFunction).exchange(captor.capture());
        Assertions.assertNull(captor.getValue().headers().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void shouldSkipWhenHostIsExternalAndExternalDisabled() {
        setValidToken("valid-token");
        config.getTokenPropagation().setAllowExternalHosts(false);
        config.getTokenPropagation().setIncludeHosts(Collections.singletonList("internal.svc"));
        ClientRequest request = ClientRequest.create(
            org.springframework.http.HttpMethod.GET, URI.create("http://unknown-external.com/resource"))
            .build();

        underTest.filter(request, exchangeFunction).block();

        ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
        Mockito.verify(exchangeFunction).exchange(captor.capture());
        Assertions.assertNull(captor.getValue().headers().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void shouldForwardWhenHostIsInIncludeList() {
        setValidToken("valid-token");
        config.getTokenPropagation().setAllowExternalHosts(false);
        config.getTokenPropagation().setIncludeHosts(Collections.singletonList("internal.svc"));
        ClientRequest request = ClientRequest.create(
            org.springframework.http.HttpMethod.GET, URI.create("http://internal.svc/resource"))
            .build();

        underTest.filter(request, exchangeFunction).block();

        ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
        Mockito.verify(exchangeFunction).exchange(captor.capture());
        Assertions.assertEquals("Bearer valid-token",
            captor.getValue().headers().getFirst(HttpHeaders.AUTHORIZATION));
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
