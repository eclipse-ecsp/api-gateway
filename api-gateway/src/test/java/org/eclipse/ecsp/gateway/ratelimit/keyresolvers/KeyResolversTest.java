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

package org.eclipse.ecsp.gateway.ratelimit.keyresolvers;

import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Test class for all KeyResolver implementations.
 */
@ExtendWith(MockitoExtension.class)
class KeyResolversTest {

    private static final int PORT_9090 = 9090;

    private static final int PORT_8080 = 8080;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private Route route;

    private HttpHeaders headers;
    private Map<String, Object> metadata;

    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
        metadata = new HashMap<>();
        
        lenient().when(exchange.getRequest()).thenReturn(request);
        lenient().when(request.getHeaders()).thenReturn(headers);
    }

    // ========== ClientIpKeyResolver Tests ==========

    @Test
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    void clientIpKeyResolver_WithXForwardedFor_ReturnsFirstIp() {
        // Arrange
        ClientIpKeyResolver resolver = new ClientIpKeyResolver();
        headers.add("X-Forwarded-For", "192.168.1.100, 10.0.0.1");

        // Act
        Mono<String> result = resolver.resolve(exchange);

        // Assert
        StepVerifier.create(result)
                .expectNext("192.168.1.100")
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    void clientIpKeyResolver_WithSingleXForwardedFor_ReturnsTrimmedIp() {
        // Arrange
        ClientIpKeyResolver resolver = new ClientIpKeyResolver();
        headers.add("X-Forwarded-For", "  172.16.0.50  ");

        // Act
        Mono<String> result = resolver.resolve(exchange);

        // Assert
        StepVerifier.create(result)
                .expectNext("172.16.0.50")
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    void clientIpKeyResolver_WithoutXForwardedFor_ReturnsRemoteAddress() {
        // Arrange
        ClientIpKeyResolver resolver = new ClientIpKeyResolver();
        InetSocketAddress remoteAddress = new InetSocketAddress("10.20.30.40", PORT_8080);
        when(request.getRemoteAddress()).thenReturn(remoteAddress);

        // Act
        Mono<String> result = resolver.resolve(exchange);

        // Assert
        StepVerifier.create(result)
                .expectNext("10.20.30.40")
                .verifyComplete();
    }

    @Test
    void clientIpKeyResolver_WithNullRemoteAddress_ReturnsEmpty() {
        // Arrange
        ClientIpKeyResolver resolver = new ClientIpKeyResolver();
        when(request.getRemoteAddress()).thenReturn(null);

        // Act
        Mono<String> result = resolver.resolve(exchange);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    void clientIpKeyResolver_WithEmptyXForwardedFor_UsesRemoteAddress() {
        // Arrange
        ClientIpKeyResolver resolver = new ClientIpKeyResolver();
        headers.add("X-Forwarded-For", "");
        InetSocketAddress remoteAddress = new InetSocketAddress("192.168.50.100", PORT_9090);
        when(request.getRemoteAddress()).thenReturn(remoteAddress);

        // Act
        Mono<String> result = resolver.resolve(exchange);

        // Assert
        StepVerifier.create(result)
                .expectNext("192.168.50.100")
                .verifyComplete();
    }

    // ========== RequestHeaderKeyResolver Tests ==========

    @Test
    void requestHeaderKeyResolver_WithValidHeader_ReturnsHeaderValue() {
        // Arrange
        
        metadata.put(GatewayConstants.RATE_LIMITING_METADATA_PREFIX + "headerName", "X-API-Key");
        headers.add("X-API-Key", "abc123xyz");
        
        when(exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)).thenReturn(route);
        when(route.getMetadata()).thenReturn(metadata);

        // Act
        RequestHeaderKeyResolver resolver = new RequestHeaderKeyResolver();
        Mono<String> result = resolver.resolve(exchange);

        // Assert
        StepVerifier.create(result)
                .expectNext("abc123xyz")
                .verifyComplete();
    }

    @Test
    void requestHeaderKeyResolver_WithMissingHeaderName_ReturnsEmpty() {
        // Arrange
        RequestHeaderKeyResolver resolver = new RequestHeaderKeyResolver();
        
        when(exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)).thenReturn(route);
        when(route.getMetadata()).thenReturn(metadata);

        // Act
        Mono<String> result = resolver.resolve(exchange);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void requestHeaderKeyResolver_WithEmptyHeaderName_ReturnsEmpty() {
        // Arrange
        metadata.put(GatewayConstants.RATE_LIMITING_METADATA_PREFIX + "headerName", "");
        
        when(exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)).thenReturn(route);
        when(route.getMetadata()).thenReturn(metadata);

        // Act
        RequestHeaderKeyResolver resolver = new RequestHeaderKeyResolver();
        Mono<String> result = resolver.resolve(exchange);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void requestHeaderKeyResolver_WithMissingHeaderValue_ReturnsEmpty() {
        // Arrange
        metadata.put(GatewayConstants.RATE_LIMITING_METADATA_PREFIX + "headerName", "X-Custom-Header");
        
        when(exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)).thenReturn(route);
        when(route.getMetadata()).thenReturn(metadata);
        when(route.getId()).thenReturn("test-route");

        // Act
        RequestHeaderKeyResolver resolver = new RequestHeaderKeyResolver();
        Mono<String> result = resolver.resolve(exchange);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void requestHeaderKeyResolver_WithEmptyHeaderValue_ReturnsEmpty() {
        // Arrange
        metadata.put(GatewayConstants.RATE_LIMITING_METADATA_PREFIX + "headerName", "X-Empty-Header");
        headers.add("X-Empty-Header", "");
        
        when(exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)).thenReturn(route);
        when(route.getMetadata()).thenReturn(metadata);
        when(route.getId()).thenReturn("test-route");

        // Act
        RequestHeaderKeyResolver resolver = new RequestHeaderKeyResolver();
        Mono<String> result = resolver.resolve(exchange);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void requestHeaderKeyResolver_WithMultipleMetadata_FiltersCorrectly() {
        // Arrange
        metadata.put("someOtherKey", "value");
        metadata.put(GatewayConstants.RATE_LIMITING_METADATA_PREFIX + "headerName", "Authorization");
        metadata.put(GatewayConstants.RATE_LIMITING_METADATA_PREFIX + "otherConfig", "otherValue");
        headers.add("Authorization", "Bearer token123");
        
        when(exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)).thenReturn(route);
        when(route.getMetadata()).thenReturn(metadata);

        // Act
        RequestHeaderKeyResolver resolver = new RequestHeaderKeyResolver();
        Mono<String> result = resolver.resolve(exchange);

        // Assert
        StepVerifier.create(result)
                .expectNext("Bearer token123")
                .verifyComplete();
    }

    @Test
    void requestHeaderKeyResolver_WithCaseInsensitiveHeaderNameAttribute_ReturnsHeaderValue() {
        // Arrange - test case insensitive attribute key matching (HEADERNAME, HeaderName, etc.)
        metadata.put(GatewayConstants.RATE_LIMITING_METADATA_PREFIX + "HEADERNAME", "X-User-ID");
        headers.add("X-User-ID", "user-12345");
        
        when(exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)).thenReturn(route);
        when(route.getMetadata()).thenReturn(metadata);

        // Act
        RequestHeaderKeyResolver resolver = new RequestHeaderKeyResolver();
        Mono<String> result = resolver.resolve(exchange);

        // Assert
        StepVerifier.create(result)
                .expectNext("user-12345")
                .verifyComplete();
    }

    @Test
    void requestHeaderKeyResolver_WithMixedCaseHeaderNameAttribute_ReturnsHeaderValue() {
        // Arrange - test with mixed case attribute key
        metadata.put(GatewayConstants.RATE_LIMITING_METADATA_PREFIX + "HeaderName", "X-Client-ID");
        headers.add("X-Client-ID", "client-98765");
        
        when(exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)).thenReturn(route);
        when(route.getMetadata()).thenReturn(metadata);

        // Act
        RequestHeaderKeyResolver resolver = new RequestHeaderKeyResolver();
        Mono<String> result = resolver.resolve(exchange);

        // Assert
        StepVerifier.create(result)
                .expectNext("client-98765")
                .verifyComplete();
    }

    // ========== RouteNameKeyResolver Tests ==========

    @Test
    void routeNameKeyResolver_WithValidRoute_ReturnsCombinedKey() {
        // Arrange
        metadata.put(GatewayConstants.SERVICE_NAME, "user-service");
        
        when(exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)).thenReturn(route);
        when(route.getMetadata()).thenReturn(metadata);
        when(route.getId()).thenReturn("user-route-123");

        // Act
        RouteNameKeyResolver resolver = new RouteNameKeyResolver();
        Mono<String> result = resolver.resolve(exchange);

        // Assert
        StepVerifier.create(result)
                .expectNext("user-service:user-route-123")
                .verifyComplete();
    }

    @Test
    void routeNameKeyResolver_WithNullServiceName_ReturnsNull() {
        // Arrange
              
        when(exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)).thenReturn(route);
        when(route.getMetadata()).thenReturn(metadata);

        // Act
        RouteNameKeyResolver resolver = new RouteNameKeyResolver();
        Mono<String> result = resolver.resolve(exchange);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void routeNameKeyResolver_WithComplexServiceName_ReturnsCorrectFormat() {
        // Arrange
        metadata.put(GatewayConstants.SERVICE_NAME, "payment-processing-v2");
        
        when(exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)).thenReturn(route);
        when(route.getMetadata()).thenReturn(metadata);
        when(route.getId()).thenReturn("payment-api-endpoint");

        // Act
        RouteNameKeyResolver resolver = new RouteNameKeyResolver();
        Mono<String> result = resolver.resolve(exchange);

        // Assert
        StepVerifier.create(result)
                .expectNext("payment-processing-v2:payment-api-endpoint")
                .verifyComplete();
    }

    // ========== RoutePathKeyResolver Tests ==========

    @Test
    void routePathKeyResolver_WithSimplePath_ReturnsPath() {
        testRoutePath("http://localhost:8080/api/users", "/api/users");
        testRoutePath("http://api.example.com/v2/products/123/details", "/v2/products/123/details");
        testRoutePath("http://localhost:9000/", "/");
        testRoutePath("http://localhost:8080/search?query=test&page=1", "/search");
        testRoutePath("http://localhost:8080/api/users/john%20doe", "/api/users/john doe");
    }

    private boolean testRoutePath(String requestUri, String expectedPath) {
        // Arrange
        RoutePathKeyResolver resolver = new RoutePathKeyResolver();
        when(request.getURI()).thenReturn(URI.create(requestUri));

        // Act
        Mono<String> result = resolver.resolve(exchange);

        // Assert
        StepVerifier.create(result)
                .expectNext(expectedPath)
                .verifyComplete();
        return true;
    }
}
