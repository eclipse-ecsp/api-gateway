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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
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
    private static final String ROUTE_SCOPES = "SYSTEM_READ";
    private static final String ROUTE_USER_ID = "1";
    private static final int EXPECTED_TWO_ROUTES = 2;

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

}
