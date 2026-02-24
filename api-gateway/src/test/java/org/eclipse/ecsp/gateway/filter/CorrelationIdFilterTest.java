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
 * See the License for the specific language governing permissions and\
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.gateway.filter;

import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CorrelationIdFilter.
 */
class CorrelationIdFilterTest {

    private static final String CORRELATION_ID_HEADER = "correlationId";
    private static final String TEST_CORRELATION_ID = "test-correlation-id-123";

    private CorrelationIdFilter filter;
    private WebFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void testExtractExistingCorrelationId() {
        // Given: Request with existing correlation ID header
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header(CORRELATION_ID_HEADER, TEST_CORRELATION_ID)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When: Filter processes the request
        Mono<Void> result = filter.filter(exchange, chain);

        // Then: Existing correlation ID should be used
        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getAttributes())
                .containsEntry(GatewayConstants.CORRELATION_ID, TEST_CORRELATION_ID);
        assertThat(exchange.getResponse().getHeaders().getFirst(CORRELATION_ID_HEADER))
                .isEqualTo(TEST_CORRELATION_ID);
    }

    @Test
    void testGenerateNewCorrelationId() {
        // Given: Request without correlation ID header
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When: Filter processes the request
        Mono<Void> result = filter.filter(exchange, chain);

        // Then: New correlation ID should be generated
        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getAttributes())
                .containsKey(GatewayConstants.CORRELATION_ID);
        String correlationId = (String) exchange.getAttributes().get(GatewayConstants.CORRELATION_ID);
        assertThat(correlationId).isNotNull()
                .isNotBlank()
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

        assertThat(exchange.getResponse().getHeaders().getFirst(CORRELATION_ID_HEADER))
                .isEqualTo(correlationId);
    }

    @Test
    void testEmptyCorrelationIdHeader() {
        // Given: Request with empty correlation ID header
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header(CORRELATION_ID_HEADER, "")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When: Filter processes the request
        Mono<Void> result = filter.filter(exchange, chain);

        // Then: New correlation ID should be generated (empty header ignored)
        StepVerifier.create(result)
                .verifyComplete();

        String correlationId = (String) exchange.getAttributes().get(GatewayConstants.CORRELATION_ID);
        assertThat(correlationId).isNotNull()
                .isNotBlank()
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void testCorrelationIdPropagatedToResponse() {
        // Given: Request without correlation ID
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When: Filter processes the request
        Mono<Void> result = filter.filter(exchange, chain);

        // Then: Response should have correlation ID header
        StepVerifier.create(result)
                .verifyComplete();

        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
        assertThat(responseHeaders.getFirst(CORRELATION_ID_HEADER)).isNotNull();
    }

    @Test
    void testFilterOrder() {
        // Then: Filter should have highest precedence
        assertThat(filter.getOrder()).isEqualTo(Integer.MIN_VALUE);
    }
}
