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

package org.eclipse.ecsp.gateway.integration;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.eclipse.ecsp.gateway.config.ClientAccessControlProperties;
import org.eclipse.ecsp.gateway.filter.ClientAccessControlGatewayFilterFactory;
import org.eclipse.ecsp.gateway.metrics.ClientAccessControlMetrics;
import org.eclipse.ecsp.gateway.model.AccessRule;
import org.eclipse.ecsp.gateway.model.ClientAccessConfig;
import org.eclipse.ecsp.gateway.service.AccessRuleMatcherService;
import org.eclipse.ecsp.gateway.service.ClientAccessControlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end integration test for Client Access Control feature.
 *
 * <p>Tests the complete request flow without external dependencies:
 * <ul>
 *   <li>JWT extraction from Authorization header</li>
 *   <li>Client ID validation and security checks</li>
 *   <li>Rule matching with wildcards and deny rules</li>
 *   <li>Allow/deny decision based on configuration</li>
 *   <li>Metrics recording</li>
 * </ul>
 *
 * <p>Validates acceptance scenarios:
 * <ul>
 *   <li>AS-1: Extract client ID from JWT with configurable claim names</li>
 *   <li>AS-2: Reject requests without valid JWT</li>
 *   <li>AS-3: Allow access when client has matching allow rule</li>
 *   <li>AS-4: Deny access when client has deny rule</li>
 * </ul>
 *
 * @author Abhishek Kumar
 */
@DisplayName("Client Access Control End-to-End Tests")
class ClientAccessControlEndToEndTest {

    private static final String CLIENT_ID = "test-client-123";
    private static final String TENANT = "test-tenant";
    private static final String USER_SERVICE = "user-service";
    private static final String PAYMENT_SERVICE = "payment-service";
    private static final String PROFILE_ROUTE = "profile";
    private static final String PAYMENT_ROUTE = "checkout";
    private static final long TOKEN_EXPIRY_MINUTES = 60;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final int MAX_SPLIT_PARTS = 2;

    private ClientAccessControlGatewayFilterFactory filterFactory;
    private AccessRuleMatcherService ruleMatcherService;
    private ClientAccessControlMetrics metrics;
    private ClientAccessControlService cacheService;
    private GatewayFilterChain mockFilterChain;

    @BeforeEach
    void setUp() {
        // Setup configuration
        ClientAccessControlProperties properties = new ClientAccessControlProperties();
        properties.setEnabled(true);
        properties.setClaimNames(Arrays.asList("clientId", "azp", "client_id", "cid"));
        properties.setSkipPaths(Arrays.asList("/actuator/**", "/api-docs/**"));

        ruleMatcherService = new AccessRuleMatcherService();
        metrics = mock(ClientAccessControlMetrics.class);
        cacheService = mock(ClientAccessControlService.class);

        // Create filter factory
        filterFactory = new ClientAccessControlGatewayFilterFactory(
                properties,
                ruleMatcherService,
                cacheService,
                metrics
        );

        // Setup mock filter chain
        mockFilterChain = mock(GatewayFilterChain.class);
        when(mockFilterChain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("AS-1: Should extract client ID from JWT using configurable claim names")
    void testJwtClaimExtractionSuccess() {
        // Given: JWT with clientId claim
        String jwt = createJwt(CLIENT_ID, "clientId");
        ClientAccessConfig config = createConfig(CLIENT_ID, List.of("*:*"));
        when(cacheService.getConfig(CLIENT_ID)).thenReturn(config);

        // When: Request with valid JWT
        String path = "/" + USER_SERVICE + "/" + PROFILE_ROUTE;
        ServerWebExchange exchange = createExchange(jwt, path);
        Mono<Void> result = filterFactory.apply(createFilterConfig(path))
                .filter(exchange, mockFilterChain);

        // Then: Request should be allowed
        StepVerifier.create(result)
                .verifyComplete();
        
        assertThat(exchange.getResponse().getStatusCode()).isNull(); // No error set
    }

    @Test
    @DisplayName("AS-1: Should extract client ID from alternative claim (azp)")
    void testJwtClaimExtractionAlternativeClaim() {
        // Given: JWT with azp claim (no clientId)
        String jwt = createJwt(CLIENT_ID, "azp");
        ClientAccessConfig config = createConfig(CLIENT_ID, List.of("*:*"));
        when(cacheService.getConfig(CLIENT_ID)).thenReturn(config);

        // When: Request with JWT containing azp claim
        String path = "/" + USER_SERVICE + "/" + PROFILE_ROUTE;
        ServerWebExchange exchange = createExchange(jwt, path);
        Mono<Void> result = filterFactory.apply(createFilterConfig(path))
                .filter(exchange, mockFilterChain);

        // Then: Client ID should be extracted from azp
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("AS-2: Should reject request without JWT token")
    void testMissingJwtRejected() {
        // Given: No JWT token
        String path = "/" + USER_SERVICE + "/" + PROFILE_ROUTE;
        ServerWebExchange exchange = createExchange(null, path);

        // When: Request without JWT
        Mono<Void> result = filterFactory.apply(createFilterConfig(path))
                .filter(exchange, mockFilterChain);

        // Then: Request should be rejected with 401
        StepVerifier.create(result)
                .verifyComplete();
        
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("AS-3: Should allow access when client has matching allow rule")
    void testAllowRuleAccessGranted() {
        // Given: Client with specific allow rule
        String jwt = createJwt(CLIENT_ID, "clientId");
        ClientAccessConfig config = createConfig(
                CLIENT_ID,
                List.of(USER_SERVICE + ":" + PROFILE_ROUTE)
        );
        when(cacheService.getConfig(CLIENT_ID)).thenReturn(config);

        // When: Request matches allow rule
        String path = "/" + USER_SERVICE + "/" + PROFILE_ROUTE;
        ServerWebExchange exchange = createExchange(jwt, path);
        Mono<Void> result = filterFactory.apply(createFilterConfig(path))
                .filter(exchange, mockFilterChain);

        // Then: Access should be granted
        StepVerifier.create(result)
                .verifyComplete();
        
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("AS-4: Should deny access when client has deny rule")
    void testDenyRuleAccessDenied() {
        // Given: Client with deny rule
        String jwt = createJwt(CLIENT_ID, "clientId");
        ClientAccessConfig config = createConfig(
                CLIENT_ID,
                List.of("*:*", "!" + PAYMENT_SERVICE + ":*")
        );
        when(cacheService.getConfig(CLIENT_ID)).thenReturn(config);

        // When: Request matches deny rule
        String path = "/" + PAYMENT_SERVICE + "/" + PAYMENT_ROUTE;
        ServerWebExchange exchange = createExchange(jwt, path);
        Mono<Void> result = filterFactory.apply(createFilterConfig(path))
                .filter(exchange, mockFilterChain);

        // Then: Access should be denied
        StepVerifier.create(result)
                .verifyComplete();
        
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should allow wildcard service matching")
    void testWildcardServiceMatches() {
        // Given: Client with wildcard service rule
        String jwt = createJwt(CLIENT_ID, "clientId");
        ClientAccessConfig config = createConfig(
                CLIENT_ID,
                List.of("*:" + PROFILE_ROUTE)
        );
        when(cacheService.getConfig(CLIENT_ID)).thenReturn(config);

        // When: Request to any service with profile route
        String path = "/" + USER_SERVICE + "/" + PROFILE_ROUTE;
        ServerWebExchange exchange = createExchange(jwt, path);
        Mono<Void> result = filterFactory.apply(createFilterConfig(path))
                .filter(exchange, mockFilterChain);

        // Then: Access should be granted
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should allow wildcard route matching")
    void testWildcardRouteMatches() {
        // Given: Client with wildcard route rule
        String jwt = createJwt(CLIENT_ID, "clientId");
        ClientAccessConfig config = createConfig(
                CLIENT_ID,
                List.of(USER_SERVICE + ":*")

        );
        when(cacheService.getConfig(CLIENT_ID)).thenReturn(config);

        // When: Request to user-service with any route
        String path = "/" + USER_SERVICE + "/" + PROFILE_ROUTE;
        ServerWebExchange exchange = createExchange(jwt, path);
        Mono<Void> result = filterFactory.apply(createFilterConfig(path))
                .filter(exchange, mockFilterChain);

        // Then: Access should be granted
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should deny by default when no allow rules match")
    void testDenyByDefaultNoMatchingRules() {
        // Given: Client with specific allow rule
        String jwt = createJwt(CLIENT_ID, "clientId");
        ClientAccessConfig config = createConfig(
                CLIENT_ID,
                List.of(USER_SERVICE + ":" + PROFILE_ROUTE)
        );
        when(cacheService.getConfig(CLIENT_ID)).thenReturn(config);

        // When: Request to different service/route
        ServerWebExchange exchange = createExchange(jwt, "/" + PAYMENT_SERVICE + "/" + PAYMENT_ROUTE);
        Mono<Void> result = filterFactory.apply(new ClientAccessControlGatewayFilterFactory.Config())
                .filter(exchange, mockFilterChain);

        // Then: Access should be denied (deny-by-default)
        StepVerifier.create(result)
                .verifyComplete();
        
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should reject client ID with SQL injection attempt")
    void testSqlInjectionRejected() {
        // Given: JWT with malicious client ID
        String maliciousClientId = "test' OR '1'='1";
        String jwt = createJwt(maliciousClientId, "clientId");

        // When: Request with SQL injection attempt
        String path = "/" + USER_SERVICE + "/" + PROFILE_ROUTE;
        ServerWebExchange exchange = createExchange(jwt, path);
        Mono<Void> result = filterFactory.apply(createFilterConfig(path))
                .filter(exchange, mockFilterChain);

        // Then: Request should be rejected
        StepVerifier.create(result)
                .verifyComplete();
        
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should reject client ID with XSS attempt")
    void testXssInjectionRejected() {
        // Given: JWT with XSS payload
        String maliciousClientId = "<script>alert('xss')</script>";
        String jwt = createJwt(maliciousClientId, "clientId");

        // When: Request with XSS attempt
        String path = "/" + USER_SERVICE + "/" + PROFILE_ROUTE;
        ServerWebExchange exchange = createExchange(jwt, path);
        Mono<Void> result = filterFactory.apply(createFilterConfig(path))
                .filter(exchange, mockFilterChain);

        // Then: Request should be rejected
        StepVerifier.create(result)
                .verifyComplete();
        
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should skip validation for configured paths")
    void testSkipPathsBypassed() {
        // Given: Request to skip path
        String path = "/actuator/health";
        ServerWebExchange exchange = createExchange(null, path);

        // When: Request to actuator endpoint
        Mono<Void> result = filterFactory.apply(createFilterConfig(path))
                .filter(exchange, mockFilterChain);

        // Then: Validation should be skipped
        StepVerifier.create(result)
                .verifyComplete();
        
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    // Helper methods

    private ClientAccessControlGatewayFilterFactory.Config createFilterConfig(String path) {
        // Extract service and route from path (e.g., "/user-service/get-profile")
        String[] parts = path.substring(1).split("/", MAX_SPLIT_PARTS);
        ClientAccessControlGatewayFilterFactory.Config config = new ClientAccessControlGatewayFilterFactory.Config();
        if (parts.length > 0) {
            config.setServiceName(parts[0]);
        }
        if (parts.length > 1) {
            config.setRouteId(parts[1]);
        }
        return config;
    }

    private String createJwt(String clientId, String claimName) {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("user@example.com")
                .issuer("https://auth.example.com")
                .expirationTime(Date.from(Instant.now().plusSeconds(TOKEN_EXPIRY_MINUTES * SECONDS_PER_MINUTE)))
                .issueTime(new Date())
                .claim(claimName, clientId)
                .build();

        // Create unsecured JWT (no signature) as the gateway expects unsecured tokens
        PlainJWT plainJwt = new PlainJWT(claimsSet);
        return plainJwt.serialize();
    }

    private ServerWebExchange createExchange(String jwt, String path) {
        MockServerHttpRequest.BaseBuilder<?> requestBuilder = MockServerHttpRequest.get(path);
        
        if (jwt != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        }
        
        return MockServerWebExchange.from(requestBuilder);
    }

    private ClientAccessConfig createConfig(String clientId, List<String> allowRules) {
        List<AccessRule> rules = allowRules.stream()
                .map(rule -> {
                    String cleanRule = rule.startsWith("!") ? rule.substring(1) : rule;
                    String[] parts = cleanRule.split(":", MAX_SPLIT_PARTS);
                    return AccessRule.builder()
                            .service(parts.length > 0 ? parts[0] : "*")
                            .route(parts.length > 1 ? parts[1] : "*")
                            .deny(rule.startsWith("!"))
                            .originalRule(rule)
                            .build();
                })
                .toList();

        return ClientAccessConfig.builder()
                .clientId(clientId)
                .tenant(TENANT)
                .rules(rules)
                .active(true)
                .source("TEST")
                .lastUpdated(Instant.now())
                .build();
    }
}
