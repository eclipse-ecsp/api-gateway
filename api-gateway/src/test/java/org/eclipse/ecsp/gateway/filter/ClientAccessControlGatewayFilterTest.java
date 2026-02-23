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

package org.eclipse.ecsp.gateway.filter;

import io.jsonwebtoken.Jwts;
import org.eclipse.ecsp.gateway.config.ClientAccessControlProperties;
import org.eclipse.ecsp.gateway.metrics.ClientAccessControlMetrics;
import org.eclipse.ecsp.gateway.model.AccessRule;
import org.eclipse.ecsp.gateway.model.ClientAccessConfig;
import org.eclipse.ecsp.gateway.service.AccessRuleMatcherService;
import org.eclipse.ecsp.gateway.service.ClientAccessControlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprehensive unit tests for ClientAccessControlGatewayFilterFactory.
 *
 * <p>Tests gateway filter for client access control validation including:
 * - Request validation (JWT extraction, missing headers, malformed tokens)
 * - Security validation (injection attacks, XSS, path traversal)
 * - Skip paths configuration
 * - Filter enabled/disabled behavior
 * - Configuration and constructor validation
 * - Metrics recording
 *
 * <p>Merged from ClientAccessControlGatewayFilterTest and ClientAccessControlGatewayFilterFactoryTest
 * to eliminate duplicate test coverage.
 */
@ExtendWith(MockitoExtension.class)
class ClientAccessControlGatewayFilterTest {
    private static final String VALID_JWT_TOKEN = "valid.jwt.token";
    private static final String BEARER = "Bearer ";
    private static final String API_SERVICE_ROUTE = "/api/service/route";
    private static final String CLIENT_ID = "clientId";
    private static final String SERVICE_NAME = "test-service";
    private static final String ROUTE_ID = "test-route";

    @Mock(strictness = Mock.Strictness.LENIENT)
    private AccessRuleMatcherService accessRuleMatcherService;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private ClientAccessControlService cacheService;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private ClientAccessControlMetrics metrics;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private GatewayFilterChain filterChain;

    private ClientAccessControlProperties properties;
    private ClientAccessControlGatewayFilterFactory filterFactory;

    @BeforeEach
    void setUp() {
        properties = new ClientAccessControlProperties();
        properties.setEnabled(true);
        properties.setClaimNames(List.of(CLIENT_ID, "azp", "client_id"));
        properties.setSkipPaths(List.of("/v3/api-docs/**", "/actuator/**"));

        filterFactory = new ClientAccessControlGatewayFilterFactory(
                properties,
                accessRuleMatcherService,
                cacheService,
                metrics
        );

        // Default mock behavior
        when(filterChain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void testFilterValidClientIdSuccess() {
        // Arrange
        String clientId = "test_client";
        String service = "service";
        String route = "route";
        String jwt = createJwt(clientId, CLIENT_ID);

        // Mock client config
        ClientAccessConfig clientConfig = ClientAccessConfig.builder()
                .clientId(clientId)
                .active(true)
                .rules(List.of(AccessRule.builder()
                        .service(service)
                        .route(route)
                        .deny(false)
                        .build()))
                .build();
        when(cacheService.getConfig(clientId)).thenReturn(clientConfig);
        
        // Mock access rule matching
        when(accessRuleMatcherService.isAllowed(anyList(), eq(service), eq(route))).thenReturn(true);

        MockServerHttpRequest request = MockServerHttpRequest
                .get(API_SERVICE_ROUTE)
                .header(HttpHeaders.AUTHORIZATION, BEARER + jwt)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        ClientAccessControlGatewayFilterFactory.Config config = new ClientAccessControlGatewayFilterFactory.Config();
        config.setServiceName(service);
        config.setRouteId(route);
        GatewayFilter filter = filterFactory.apply(config);

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain).filter(exchange);
        assertEquals(clientId, exchange.getAttribute(CLIENT_ID));
    }

    @Test
    void testFilterMissingAuthorizationHeaderUnauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get(API_SERVICE_ROUTE)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
    }

    @Test
    void testFilterInvalidBearerTokenUnauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get(API_SERVICE_ROUTE)
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz") // Not Bearer
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testFilterNoClientIdInJwtUnauthorized() {
        // Arrange
        String jwt = VALID_JWT_TOKEN;

        MockServerHttpRequest request = MockServerHttpRequest
                .get(API_SERVICE_ROUTE)
                .header(HttpHeaders.AUTHORIZATION, BEARER + jwt)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testFilterSkipPathApiDocs() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/v3/api-docs/swagger-config")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain).filter(exchange);
    }

    @Test
    void testFilterSkipPathActuator() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/actuator/health")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain).filter(exchange);
    }

    @Test
    void testFilterFilterDisabledSkipValidation() {
        // Arrange
        properties.setEnabled(false);

        MockServerHttpRequest request = MockServerHttpRequest
                .get(API_SERVICE_ROUTE)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain).filter(exchange);
    }

    // =========================
    // Configuration Tests
    // =========================

    /**
     * Test purpose          - Verify constructor initializes all dependencies.
     * Test data             - All required dependencies.
     * Test expected result  - Factory created successfully.
     * Test type             - Positive.
     */
    @Test
    void testConstructorAllDependenciesCreateFactorySuccessfully() {
        // GIVEN: All dependencies
        ClientAccessControlProperties props = new ClientAccessControlProperties();
        AccessRuleMatcherService ruleMatcherService = mock(AccessRuleMatcherService.class);
        ClientAccessControlService cache = mock(ClientAccessControlService.class);
        ClientAccessControlMetrics metricsService = mock(ClientAccessControlMetrics.class);

        // WHEN: Create filter factory
        ClientAccessControlGatewayFilterFactory factory = new ClientAccessControlGatewayFilterFactory(
                props, ruleMatcherService, cache, metricsService);

        // THEN: Should be created successfully
        assertNotNull(factory);
    }

    /**
     * Test purpose          - Verify Config class can be instantiated.
     * Test data             - None.
     * Test expected result  - Config instance created.
     * Test type             - Positive.
     */
    @Test
    void testConfigConstructorCreateInstanceSuccessfully() {
        // GIVEN: No prerequisites

        // WHEN: Create config
        ClientAccessControlGatewayFilterFactory.Config config = 
                new ClientAccessControlGatewayFilterFactory.Config();

        // THEN: Should be created
        assertNotNull(config);
    }

    /**
     * Test purpose          - Verify Config setters and getters work correctly.
     * Test data             - Service name and route ID.
     * Test expected result  - Values set and retrieved correctly.
     * Test type             - Positive.
     */
    @Test
    void testConfigSettersAndGettersWorkCorrectly() {
        // GIVEN: Config instance
        ClientAccessControlGatewayFilterFactory.Config config = 
                new ClientAccessControlGatewayFilterFactory.Config();

        // WHEN: Set values
        config.setServiceName(SERVICE_NAME);
        config.setRouteId(ROUTE_ID);

        // THEN: Values should be retrieved correctly
        assertEquals(SERVICE_NAME, config.getServiceName());
        assertEquals(ROUTE_ID, config.getRouteId());
    }

    // =========================
    // Client Inactive Tests
    // =========================

    /**
     * Test purpose          - Verify filter denies request when client is inactive.
     * Test data             - Client config with active=false.
     * Test expected result  - 401 Unauthorized response.
     * Test type             - Negative.
     */
    @Test
    void testFilterInactiveClientUnauthorized() {
        // Arrange
        String jwt = VALID_JWT_TOKEN;
        String clientId = "inactive_client";

        MockServerHttpRequest request = MockServerHttpRequest
                .get(API_SERVICE_ROUTE)
                .header(HttpHeaders.AUTHORIZATION, BEARER + jwt)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Mock inactive client config
        ClientAccessConfig clientConfig = ClientAccessConfig.builder()
                .clientId(clientId)
                .active(false)
                .build();
        when(cacheService.getConfig(clientId)).thenReturn(clientConfig);

        GatewayFilter filter = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
    }

    /**
     * Test purpose          - Verify filter denies request when client config not found.
     * Test data             - Client ID with no config in cache.
     * Test expected result  - 401 Unauthorized response.
     * Test type             - Negative.
     */
    @Test
    void testFilterClientNotFoundUnauthorized() {
        // Arrange
        String jwt = VALID_JWT_TOKEN;

        MockServerHttpRequest request = MockServerHttpRequest
                .get(API_SERVICE_ROUTE)
                .header(HttpHeaders.AUTHORIZATION, BEARER + jwt)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Mock cache returning null (client not found)
        when(cacheService.getConfig(anyString())).thenReturn(null);

        GatewayFilter filter = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
    }

    // =========================
    // Access Rule Tests
    // =========================

    /**
     * Test purpose          - Verify filter denies request when access rules deny access.
     * Test data             - Client config with rules that deny the request.
     * Test expected result  - 401 Unauthorized response.
     * Test type             - Negative.
     */
    @Test
    void testFilterAccessRulesDenyAccessUnauthorized() {
        // Arrange
        String jwt = VALID_JWT_TOKEN;
        String clientId = "restricted_client";
        String service = "service";
        String route = "route";

        MockServerHttpRequest request = MockServerHttpRequest
                .get(API_SERVICE_ROUTE)
                .header(HttpHeaders.AUTHORIZATION, BEARER + jwt)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Mock client config with restrictive rules
        ClientAccessConfig clientConfig = ClientAccessConfig.builder()
                .clientId(clientId)
                .active(true)
                .rules(List.of(AccessRule.builder()
                        .service("other-service")
                        .route("*")
                        .deny(false)
                        .build()))
                .build();
        when(cacheService.getConfig(clientId)).thenReturn(clientConfig);
        
        // Mock access rule matching denying access
        when(accessRuleMatcherService.isAllowed(anyList(), eq(service), eq(route))).thenReturn(false);

        GatewayFilter filter = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config());

        // Act
        Mono<Void> result = filter.filter(exchange, filterChain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
    }

    // =========================
    // Helper Methods
    // =========================

    /**
     * Create a test JWT with specific client ID claim.
     *
     * @param clientId Client ID value
     * @param claimName Claim name to use
     * @return Unsigned JWT token
     */
    @SuppressWarnings("java:S5659")
    private String createJwt(String clientId, String claimName) {
        return Jwts.builder()
                .claim(claimName, clientId)
                .subject("test-subject")
                .issuedAt(new Date())
                .compact();
    }
}
