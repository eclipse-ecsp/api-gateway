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

package org.eclipse.ecsp.gateway.clients;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.eclipse.ecsp.gateway.model.RateLimit;
import org.eclipse.ecsp.gateway.service.RouteUtils;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for ApiRegistryClient using WireMock.
 * Tests the client logic for fetching routes from the API registry with real HTTP calls.
 */
@ExtendWith(MockitoExtension.class)
class ApiRegistryClientTest {

    private static final String ROUTES_ENDPOINT = "/api/v1/routes";
    private static final String RATE_LIMITS_ENDPOINT = "/v1/config/rate-limits";
    private static final String ROUTE_SCOPES = "SYSTEM_READ";
    private static final String ROUTE_USER_ID = "1";
    private static final int EXPECTED_TWO_ROUTES = 2;
    private static final int EXPECTED_TWO_RATE_LIMITS = 2;
    private static final long REPLENISH_RATE_10 = 10;
    private static final long REPLENISH_RATE_5 = 5;
    private static final long REPLENISH_RATE_15 = 15;
    private static final long REPLENISH_RATE_20 = 20;
    private static final long REPLENISH_RATE_25 = 25;

    private WireMockServer wireMockServer;
    private ApiRegistryClient apiRegistryClient;
    private IgniteRouteDefinition dummyRoute;

    @Mock
    private RouteUtils routeUtils;

    @BeforeEach
    void setUp() throws Exception {
        // Setup WireMock server
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        // Setup dummy route - use lenient to avoid UnnecessaryStubbingException in tests where cache is used
        dummyRoute = new IgniteRouteDefinition();
        dummyRoute.setId("dummy-route");
        lenient().when(routeUtils.getDummyRoute()).thenReturn(dummyRoute);

        // Create real WebClient pointing to WireMock server
        WebClient.Builder webClientBuilder = WebClient.builder();
        String baseUrl = "http://localhost:" + wireMockServer.port();
        
        // Create ApiRegistryClient instance with real WebClient
        apiRegistryClient = new ApiRegistryClient(baseUrl, webClientBuilder, routeUtils);
        
        // Use reflection to set the private fields that normally get injected
        setPrivateField(apiRegistryClient, "routesEndpoint", ROUTES_ENDPOINT);
        setPrivateField(apiRegistryClient, "rateLimitsEndpoint", RATE_LIMITS_ENDPOINT);
        setPrivateField(apiRegistryClient, "routeScopes", ROUTE_SCOPES);
        setPrivateField(apiRegistryClient, "routeUserId", ROUTE_USER_ID);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    /**
     * Helper method to set private fields using reflection.
     */
    private void setPrivateField(Object target, String fieldName, String value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Test successful retrieval of routes from the API registry.
     * Verifies that routes are correctly fetched and returned.
     */
    @Test
    void getRoutes_whenSuccessful_thenReturnsRoutes() {
        // Given
        String responseBody = """
            [
                {
                    "id": "route-1",
                    "uri": "http://example.com/api/v1/test1",
                    "predicates": [
                        {
                            "name": "Path",
                            "args": {
                                "pattern": "/api/v1/test1/**"
                            }
                        }
                    ]
                },
                {
                    "id": "route-2",
                    "uri": "http://example.com/api/v1/test2",
                    "predicates": [
                        {
                            "name": "Path",
                            "args": {
                                "pattern": "/api/v1/test2/**"
                            }
                        }
                    ]
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo(ROUTES_ENDPOINT))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(GatewayConstants.USER_ID, equalTo(ROUTE_USER_ID))
                .withHeader(GatewayConstants.SCOPE, equalTo(ROUTE_SCOPES))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        // When
        Flux<IgniteRouteDefinition> result = apiRegistryClient.getRoutes();

        // Then
        StepVerifier.create(result)
                .expectNextMatches(route -> "route-1".equals(route.getId()))
                .expectNextMatches(route -> "route-2".equals(route.getId()))
                .verifyComplete();

        // Verify the correct request was made
        wireMockServer.verify(1, WireMock.getRequestedFor(urlEqualTo(ROUTES_ENDPOINT))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(GatewayConstants.USER_ID, equalTo(ROUTE_USER_ID))
                .withHeader(GatewayConstants.SCOPE, equalTo(ROUTE_SCOPES)));
    }

    /**
     * Test error handling when API registry is unavailable.
     * Verifies that dummy route is returned when an error occurs.
     */
    @Test
    void getRoutes_whenApiRegistryUnavailable_thenReturnsDummyRoute() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo(ROUTES_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withBody("Internal Server Error")));

        // When
        Flux<IgniteRouteDefinition> result = apiRegistryClient.getRoutes();

        // Then
        StepVerifier.create(result)
                .expectNext(dummyRoute)
                .verifyComplete();

        verify(routeUtils).getDummyRoute();
    }

    /**
     * Test error handling when network timeout occurs.
     * Verifies that dummy route is returned for network-related errors.
     */
    @Test
    void getRoutes_whenNetworkError_thenReturnsDummyRoute() {
        // Given - simulate network error by not stubbing the endpoint (connection refused)
        
        // When
        Flux<IgniteRouteDefinition> result = apiRegistryClient.getRoutes();

        // Then
        StepVerifier.create(result)
                .expectNext(dummyRoute)
                .verifyComplete();

        verify(routeUtils).getDummyRoute();
    }

    /**
     * Test successful retrieval of empty route list.
     * Verifies that empty response is handled correctly.
     */
    @Test
    void getRoutes_whenEmptyResponse_thenReturnsDummyRoute() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo(ROUTES_ENDPOINT))
            .willReturn(aResponse()
                .withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("[]")));

        // When
        Flux<IgniteRouteDefinition> result = apiRegistryClient.getRoutes();

        // Then
        StepVerifier.create(result)
            .expectNext(dummyRoute)
            .verifyComplete();
        verify(routeUtils).getDummyRoute();
    }

    /**
     * Test 404 error handling.
     * Verifies that dummy route is returned when routes endpoint is not found.
     */
    @Test
    void getRoutes_when404Error_thenReturnsDummyRoute() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo(ROUTES_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withBody("Not Found")));

        // When
        Flux<IgniteRouteDefinition> result = apiRegistryClient.getRoutes();

        // Then
        StepVerifier.create(result)
                .expectNext(dummyRoute)
                .verifyComplete();

        verify(routeUtils).getDummyRoute();
    }

    /**
     * Test 401 unauthorized error handling.
     * Verifies that dummy route is returned when authentication fails.
     */
    @Test
    void getRoutes_whenUnauthorized_thenReturnsDummyRoute() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo(ROUTES_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.UNAUTHORIZED.value())
                        .withBody("Unauthorized")));

        // When
        Flux<IgniteRouteDefinition> result = apiRegistryClient.getRoutes();

        // Then
        StepVerifier.create(result)
                .expectNext(dummyRoute)
                .verifyComplete();

        verify(routeUtils).getDummyRoute();
    }

    /**
     * Test that proper HTTP headers are configured.
     * Verifies that all required headers are set correctly.
     */
    @Test
    void getRoutes_whenCalled_thenSetsCorrectHeaders() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo(ROUTES_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")));

        // When
        apiRegistryClient.getRoutes().blockLast();

        // Then
        wireMockServer.verify(1, WireMock.getRequestedFor(urlEqualTo(ROUTES_ENDPOINT))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(GatewayConstants.USER_ID, equalTo(ROUTE_USER_ID))
                .withHeader(GatewayConstants.SCOPE, equalTo(ROUTE_SCOPES)));
    }

    /**
     * Test correct URI configuration.
     * Verifies that the correct endpoint URI is used.
     */
    @Test
    void getRoutes_whenCalled_thenUsesCorrectEndpoint() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo(ROUTES_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")));

        // When
        apiRegistryClient.getRoutes().blockLast();

        // Then
        wireMockServer.verify(1, WireMock.getRequestedFor(urlEqualTo(ROUTES_ENDPOINT)));
    }

    /**
     * Test malformed JSON response handling.
     * Verifies that dummy route is returned when response cannot be parsed.
     */
    @Test
    void getRoutes_whenMalformedJson_thenReturnsDummyRoute() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo(ROUTES_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{ invalid json }")));

        // When
        Flux<IgniteRouteDefinition> result = apiRegistryClient.getRoutes();

        // Then
        StepVerifier.create(result)
                .expectNext(dummyRoute)
                .verifyComplete();

        verify(routeUtils).getDummyRoute();
    }

    /**
     * Test route caching on successful fetch.
     * Verifies that routes are cached after successful retrieval.
     */
    @Test
    void getRoutes_whenSuccessful_thenCachesRoutes() {
        // Given
        String responseBody = """
            [
                {
                    "id": "route-1",
                    "uri": "http://example.com/api/v1/test1",
                    "predicates": [
                        {
                            "name": "Path",
                            "args": {
                                "pattern": "/api/v1/test1/**"
                            }
                        }
                    ]
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo(ROUTES_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        // When
        Flux<IgniteRouteDefinition> result = apiRegistryClient.getRoutes();

        // Then
        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

        // Verify routes are cached
        assert apiRegistryClient.hasCachedRoutes();
        assert apiRegistryClient.getCachedRoutesCount() == 1;
    }

    /**
     * Test that cached routes are returned when API registry is down.
     * Verifies the fallback mechanism to use cached routes.
     */
    @Test
    void getRoutes_whenApiRegistryDownAfterSuccessfulFetch_thenReturnsCachedRoutes() {
        // Given - First, load routes successfully
        String responseBody = """
            [
                {
                    "id": "cached-route-1",
                    "uri": "http://example.com/api/v1/cached",
                    "predicates": [
                        {
                            "name": "Path",
                            "args": {
                                "pattern": "/api/v1/cached/**"
                            }
                        }
                    ]
                },
                {
                    "id": "cached-route-2",
                    "uri": "http://example.com/api/v2/cached",
                    "predicates": [
                        {
                            "name": "Path",
                            "args": {
                                "pattern": "/api/v2/cached/**"
                            }
                        }
                    ]
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo(ROUTES_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        // Load routes successfully and cache them
        StepVerifier.create(apiRegistryClient.getRoutes())
                .expectNextCount(EXPECTED_TWO_ROUTES)
                .verifyComplete();

        // Verify routes are cached
        assert apiRegistryClient.getCachedRoutesCount() == EXPECTED_TWO_ROUTES;

        // Now simulate API registry being down
        wireMockServer.stubFor(get(urlEqualTo(ROUTES_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        // When - Try to fetch routes again
        Flux<IgniteRouteDefinition> result = apiRegistryClient.getRoutes();

        // Then - Should return cached routes, not dummy route
        StepVerifier.create(result)
                .expectNextMatches(route -> "cached-route-1".equals(route.getId()))
                .expectNextMatches(route -> "cached-route-2".equals(route.getId()))
                .verifyComplete();

        // Verify getDummyRoute was NOT called
        verify(routeUtils, never()).getDummyRoute();
    }

    /**
     * Test that cached routes are returned when API registry returns empty list.
     * Verifies the fallback mechanism for empty responses.
     */
    @Test
    void getRoutes_whenEmptyResponseAfterSuccessfulFetch_thenReturnsCachedRoutes() {
        // Given - First, load routes successfully
        String responseBody = """
            [
                {
                    "id": "cached-route",
                    "uri": "http://example.com/api/v1/test",
                    "predicates": [
                        {
                            "name": "Path",
                            "args": {
                                "pattern": "/api/v1/test/**"
                            }
                        }
                    ]
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo(ROUTES_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        // Load routes successfully and cache them
        StepVerifier.create(apiRegistryClient.getRoutes())
                .expectNextCount(1)
                .verifyComplete();

        // Now simulate empty response
        wireMockServer.stubFor(get(urlEqualTo(ROUTES_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")));

        // When - Try to fetch routes again
        Flux<IgniteRouteDefinition> result = apiRegistryClient.getRoutes();

        // Then - Should return cached routes, not dummy route
        StepVerifier.create(result)
                .expectNextMatches(route -> "cached-route".equals(route.getId()))
                .verifyComplete();

        // Verify getDummyRoute was NOT called
        verify(routeUtils, never()).getDummyRoute();
    }

    /**
     * Test cache update when new routes are fetched.
     * Verifies that cache is properly updated with new routes.
     */
    @Test
    void getRoutes_whenNewRoutesFetched_thenUpdatesCache() {
        // Given - First fetch with initial routes
        String initialResponse = createSingleRouteResponse("route-v1", "http://example.com/api/v1", "/api/v1/**");

        wireMockServer.stubFor(get(urlEqualTo(ROUTES_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(initialResponse)));

        // Fetch and cache initial routes
        StepVerifier.create(apiRegistryClient.getRoutes())
                .expectNextCount(1)
                .verifyComplete();

        assert apiRegistryClient.getCachedRoutesCount() == 1;

        // Now fetch with updated routes
        String updatedResponse = createTwoRoutesResponse();

        wireMockServer.stubFor(get(urlEqualTo(ROUTES_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(updatedResponse)));

        // When - Fetch updated routes
        Flux<IgniteRouteDefinition> result = apiRegistryClient.getRoutes();

        // Then - Cache should be updated
        StepVerifier.create(result)
                .expectNextMatches(route -> "route-v2".equals(route.getId()))
                .expectNextMatches(route -> "route-v3".equals(route.getId()))
                .verifyComplete();

        assert apiRegistryClient.getCachedRoutesCount() == EXPECTED_TWO_ROUTES;
    }

    /**
     * Helper method to create a response with two routes.
     */
    private String createTwoRoutesResponse() {
        return """
            [
                {
                    "id": "route-v2",
                    "uri": "http://example.com/api/v2",
                    "predicates": [
                        {
                            "name": "Path",
                            "args": {
                                "pattern": "/api/v2/**"
                            }
                        }
                    ]
                },
                {
                    "id": "route-v3",
                    "uri": "http://example.com/api/v3",
                    "predicates": [
                        {
                            "name": "Path",
                            "args": {
                                "pattern": "/api/v3/**"
                            }
                        }
                    ]
                }
            ]
            """;
    }

    /**
     * Helper method to create a single route response.
     */
    private String createSingleRouteResponse(String id, String uri, String pattern) {
        return String.format("""
            [
                {
                    "id": "%s",
                    "uri": "%s",
                    "predicates": [
                        {
                            "name": "Path",
                            "args": {
                                "pattern": "%s"
                            }
                        }
                    ]
                }
            ]
            """, id, uri, pattern);
    }

    /**
     * Test clearCache method.
     * Verifies that cache can be manually cleared.
     */
    @Test
    void clearCache_whenCalled_thenRemovesCachedRoutes() {
        // Given - Load routes successfully
        String responseBody = """
            [
                {
                    "id": "route-1",
                    "uri": "http://example.com/api/v1",
                    "predicates": [
                        {
                            "name": "Path",
                            "args": {
                                "pattern": "/api/v1/**"
                            }
                        }
                    ]
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo(ROUTES_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        StepVerifier.create(apiRegistryClient.getRoutes())
                .expectNextCount(1)
                .verifyComplete();

        assert apiRegistryClient.hasCachedRoutes();

        // When
        apiRegistryClient.clearCache();

        // Then
        assert !apiRegistryClient.hasCachedRoutes();
        assert apiRegistryClient.getCachedRoutesCount() == 0;
    }

    // ==================== Rate Limit Tests ====================

    /**
     * Test successful retrieval of rate limits from the API registry.
     * Verifies that rate limits are correctly fetched and returned.
     */
    @Test
    void getRateLimits_whenSuccessful_thenReturnsRateLimits() {
        // Given
        String responseBody = """
            [
                {
                    "routeId": "route-1",
                    "service": "service-1",
                    "replenishRate": 10,
                    "burstCapacity": 20,
                    "keyResolver": "ClientIpKeyResolver"
                },
                {
                    "routeId": "route-2",
                    "service": "service-2",
                    "replenishRate": 5,
                    "burstCapacity": 10,
                    "keyResolver": "RouteNameKeyResolver"
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(GatewayConstants.USER_ID, equalTo(ROUTE_USER_ID))
                .withHeader(GatewayConstants.SCOPE, equalTo(ROUTE_SCOPES))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        // When
        List<RateLimit> result = apiRegistryClient.getRateLimits();

        // Then
        assertNotNull(result);
        assert result.size() == EXPECTED_TWO_RATE_LIMITS;
        assert "route-1".equals(result.get(0).getRouteId());
        assert "route-2".equals(result.get(1).getRouteId());
        assert result.get(0).getReplenishRate() == REPLENISH_RATE_10;
        assert result.get(1).getReplenishRate() == REPLENISH_RATE_5;

        // Verify the correct request was made
        wireMockServer.verify(1, WireMock.getRequestedFor(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(GatewayConstants.USER_ID, equalTo(ROUTE_USER_ID))
                .withHeader(GatewayConstants.SCOPE, equalTo(ROUTE_SCOPES)));
    }

    /**
     * Test error handling when API registry is unavailable for rate limits.
     * Verifies that empty list is returned when an error occurs and no cache exists.
     */
    @Test
    void getRateLimits_whenApiRegistryUnavailable_thenReturnsEmptyList() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withBody("Internal Server Error")));

        // When
        List<RateLimit> result = apiRegistryClient.getRateLimits();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Test error handling when network error occurs for rate limits.
     * Verifies that empty list is returned for network-related errors.
     */
    @Test
    void getRateLimits_whenNetworkError_thenReturnsEmptyList() {
        // Given - simulate network error by stopping the server
        wireMockServer.stop();

        // When
        List<RateLimit> result = apiRegistryClient.getRateLimits();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Restart server for other tests
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    /**
     * Test successful retrieval of empty rate limits list.
     * Verifies that empty response is handled correctly.
     */
    @Test
    void getRateLimits_whenEmptyResponse_thenReturnsEmptyList() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")));

        // When
        List<RateLimit> result = apiRegistryClient.getRateLimits();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Test 404 error handling for rate limits.
     * Verifies that empty list is returned when rate limits endpoint is not found.
     */
    @Test
    void getRateLimits_when404Error_thenReturnsEmptyList() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withBody("Not Found")));

        // When
        List<RateLimit> result = apiRegistryClient.getRateLimits();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Test 401 unauthorized error handling for rate limits.
     * Verifies that empty list is returned when authentication fails.
     */
    @Test
    void getRateLimits_whenUnauthorized_thenReturnsEmptyList() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.UNAUTHORIZED.value())
                        .withBody("Unauthorized")));

        // When
        List<RateLimit> result = apiRegistryClient.getRateLimits();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Test that proper HTTP headers are configured for rate limits.
     * Verifies that all required headers are set correctly.
     */
    @Test
    void getRateLimits_whenCalled_thenSetsCorrectHeaders() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")));

        // When
        apiRegistryClient.getRateLimits();

        // Then
        wireMockServer.verify(1, WireMock.getRequestedFor(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(GatewayConstants.USER_ID, equalTo(ROUTE_USER_ID))
                .withHeader(GatewayConstants.SCOPE, equalTo(ROUTE_SCOPES)));
    }

    /**
     * Test correct URI configuration for rate limits.
     * Verifies that the correct endpoint URI is used.
     */
    @Test
    void getRateLimits_whenCalled_thenUsesCorrectEndpoint() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")));

        // When
        apiRegistryClient.getRateLimits();

        // Then
        wireMockServer.verify(1, WireMock.getRequestedFor(urlEqualTo(RATE_LIMITS_ENDPOINT)));
    }

    /**
     * Test malformed JSON response handling for rate limits.
     * Verifies that empty list is returned when response cannot be parsed.
     */
    @Test
    void getRateLimits_whenMalformedJson_thenReturnsEmptyList() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{ invalid json }")));

        // When
        List<RateLimit> result = apiRegistryClient.getRateLimits();

        // Then
        assertNotNull(result);
        assert result.isEmpty();
    }

    /**
     * Test rate limit caching on successful fetch.
     * Verifies that rate limits are cached after successful retrieval.
     */
    @Test
    void getRateLimits_whenSuccessful_thenCachesRateLimits() {
        // Given
        String responseBody = """
            [
                {
                    "routeId": "route-1",
                    "service": "service-1",
                    "replenishRate": 10,
                    "burstCapacity": 20,
                    "keyResolver": "ClientIpKeyResolver"
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        // When
        List<RateLimit> result = apiRegistryClient.getRateLimits();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        // Verify rate limits are cached
        assertTrue(apiRegistryClient.hasCachedRateLimits());
        assertEquals(1, apiRegistryClient.getCachedRateLimitsCount());
    }

    /**
     * Test that cached rate limits are returned when API registry is down.
     * Verifies the fallback mechanism to use cached rate limits.
     */
    @Test
    void getRateLimits_whenApiRegistryDownAfterSuccessfulFetch_thenReturnsCachedRateLimits() {
        // Given - First, load rate limits successfully
        String responseBody = """
            [
                {
                    "routeId": "cached-route-1",
                    "service": "cached-service-1",
                    "replenishRate": 15,
                    "burstCapacity": 30,
                    "keyResolver": "ClientIpKeyResolver"
                },
                {
                    "routeId": "cached-route-2",
                    "service": "cached-service-2",
                    "replenishRate": 20,
                    "burstCapacity": 40,
                    "keyResolver": "RouteNameKeyResolver"
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        // Load rate limits successfully and cache them
        List<RateLimit> initialResult = apiRegistryClient.getRateLimits();
        assertEquals(EXPECTED_TWO_RATE_LIMITS, initialResult.size());

        // Verify rate limits are cached
        assertEquals(EXPECTED_TWO_RATE_LIMITS, apiRegistryClient.getCachedRateLimitsCount());

        // Now simulate API registry being down
        wireMockServer.stubFor(get(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        // When - Try to fetch rate limits again
        List<RateLimit> result = apiRegistryClient.getRateLimits();

        // Then - Should return cached rate limits, not empty list
        assertNotNull(result);
        assertEquals(EXPECTED_TWO_RATE_LIMITS, result.size());
        assertEquals("cached-route-1", result.get(0).getRouteId());
        assertEquals("cached-route-2", result.get(1).getRouteId());
        assertEquals(REPLENISH_RATE_15, result.get(0).getReplenishRate());
        assertEquals(REPLENISH_RATE_20, result.get(1).getReplenishRate());
    }

    /**
     * Test that cached rate limits are returned when API registry returns empty list.
     * Verifies the fallback mechanism for empty responses.
     */
    @Test
    void getRateLimits_whenEmptyResponseAfterSuccessfulFetch_thenReturnsCachedRateLimits() {
        // Given - First, load rate limits successfully
        String responseBody = """
            [
                {
                    "routeId": "cached-route",
                    "service": "cached-service",
                    "replenishRate": 25,
                    "burstCapacity": 50,
                    "keyResolver": "ClientIpKeyResolver"
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        // Load rate limits successfully and cache them
        List<RateLimit> initialResult = apiRegistryClient.getRateLimits();
        assertEquals(1, initialResult.size());

        // Now simulate empty response
        wireMockServer.stubFor(get(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")));

        // When - Try to fetch rate limits again
        List<RateLimit> result = apiRegistryClient.getRateLimits();

        // Then - Should return cached rate limits, not empty list
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("cached-route", result.get(0).getRouteId());
        assertEquals(REPLENISH_RATE_25, result.get(0).getReplenishRate());
    }

    /**
     * Test cache update when new rate limits are fetched.
     * Verifies that cache is properly updated with new rate limits.
     */
    @Test
    void getRateLimits_whenNewRateLimitsFetched_thenUpdatesCache() {
        // Given - First fetch with initial rate limits
        String initialResponse = """
            [
                {
                    "routeId": "route-v1",
                    "service": "service-v1",
                    "replenishRate": 5,
                    "burstCapacity": 10,
                    "keyResolver": "ClientIpKeyResolver"
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(initialResponse)));

        // Fetch and cache initial rate limits
        List<RateLimit> initialResult = apiRegistryClient.getRateLimits();
        assertEquals(1, initialResult.size());
        assertEquals(1, apiRegistryClient.getCachedRateLimitsCount());

        // Now fetch with updated rate limits
        String updatedResponse = """
            [
                {
                    "routeId": "route-v2",
                    "service": "service-v2",
                    "replenishRate": 10,
                    "burstCapacity": 20,
                    "keyResolver": "RouteNameKeyResolver"
                },
                {
                    "routeId": "route-v3",
                    "service": "service-v3",
                    "replenishRate": 15,
                    "burstCapacity": 30,
                    "keyResolver": "ClientIpKeyResolver"
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(updatedResponse)));

        // When - Fetch updated rate limits
        List<RateLimit> result = apiRegistryClient.getRateLimits();

        // Then - Cache should be updated
        assertNotNull(result);
        assertEquals(EXPECTED_TWO_RATE_LIMITS, result.size());
        assertEquals("route-v2", result.get(0).getRouteId());
        assertEquals("route-v3", result.get(1).getRouteId());
        assertEquals(EXPECTED_TWO_RATE_LIMITS, apiRegistryClient.getCachedRateLimitsCount());
    }

    /**
     * Test clearRateLimitCache method.
     * Verifies that rate limit cache can be manually cleared.
     */
    @Test
    void clearRateLimitCache_whenCalled_thenRemovesCachedRateLimits() {
        // Given - Load rate limits successfully
        String responseBody = """
            [
                {
                    "routeId": "route-1",
                    "service": "service-1",
                    "replenishRate": 10,
                    "burstCapacity": 20,
                    "keyResolver": "ClientIpKeyResolver"
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        List<RateLimit> result = apiRegistryClient.getRateLimits();
        assertEquals(1, result.size());
        assertTrue(apiRegistryClient.hasCachedRateLimits());

        // When
        apiRegistryClient.clearRateLimitCache();

        // Then
        assertFalse(apiRegistryClient.hasCachedRateLimits());
        assertEquals(0, apiRegistryClient.getCachedRateLimitsCount());
    }

    /**
     * Test that rate limit cache is independent from route cache.
     * Verifies that clearing one cache doesn't affect the other.
     */
    @Test
    void caches_whenBothPopulated_thenAreIndependent() {
        // Given - Load both routes and rate limits successfully
        String routesResponse = """
            [
                {
                    "id": "route-1",
                    "uri": "http://example.com/api/v1",
                    "predicates": [
                        {
                            "name": "Path",
                            "args": {
                                "pattern": "/api/v1/**"
                            }
                        }
                    ]
                }
            ]
            """;

        String rateLimitsResponse = """
            [
                {
                    "routeId": "route-1",
                    "service": "service-1",
                    "replenishRate": 10,
                    "burstCapacity": 20,
                    "keyResolver": "ClientIpKeyResolver"
                }
            ]
            """;

        wireMockServer.stubFor(get(urlEqualTo(ROUTES_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(routesResponse)));

        wireMockServer.stubFor(get(urlEqualTo(RATE_LIMITS_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(rateLimitsResponse)));

        // Populate both caches
        StepVerifier.create(apiRegistryClient.getRoutes())
                .expectNextCount(1)
                .verifyComplete();
        List<RateLimit> rateLimits = apiRegistryClient.getRateLimits();

        assertTrue(apiRegistryClient.hasCachedRoutes());
        assertTrue(apiRegistryClient.hasCachedRateLimits());
        assertEquals(1, rateLimits.size());

        // When - Clear only rate limit cache
        apiRegistryClient.clearRateLimitCache();

        // Then - Route cache should still exist
        assertTrue(apiRegistryClient.hasCachedRoutes());
        assertFalse(apiRegistryClient.hasCachedRateLimits());

        // When - Clear route cache
        apiRegistryClient.clearCache();

        // Then - Both caches should be empty
        assertFalse(apiRegistryClient.hasCachedRoutes());
        assertFalse(apiRegistryClient.hasCachedRateLimits());
    }

}
