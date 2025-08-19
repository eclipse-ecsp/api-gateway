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
import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.eclipse.ecsp.gateway.service.RouteUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.mockito.Mockito.when;

/**
 * Tests for ApiRegistryClient.
 * This class uses WireMock to mock the API registry responses and validates
 * proper error handling and timeout scenarios.
 *
 * @author Abhishek Kumar
 */
class ApiRegistryClientTest {
    private static final int EXPECTED_ROUTE_COUNT = 2;
    private static final int EXPECTED_SINGLE_ROUTE = 1;
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(15);

    private WireMockServer wireMockServer;
    private ApiRegistryClient client;

    @Mock
    private RouteUtils mockRouteUtils;

    private AutoCloseable mockitoCloseable;

    @BeforeEach
    void setUp() {
        mockitoCloseable = MockitoAnnotations.openMocks(this);
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        // Setup mock RouteUtils to return a dummy route
        IgniteRouteDefinition dummyRoute = new IgniteRouteDefinition();
        dummyRoute.setId("DUMMY");
        when(mockRouteUtils.getDummyRoute()).thenReturn(dummyRoute);

        client = new ApiRegistryClient("http://localhost:" + wireMockServer.port(), WebClient.builder(), mockRouteUtils);
        ReflectionTestUtils.setField(client, "routesEndpoint", "/api/v1/routes");
        ReflectionTestUtils.setField(client, "routeScopes", "SYSTEM_READ");
        ReflectionTestUtils.setField(client, "routeUserId", "1");
    }

    @AfterEach
    void tearDown() throws Exception {
        wireMockServer.stop();
        if (mockitoCloseable != null) {
            mockitoCloseable.close();
        }
    }

    @Test
    @DisplayName("getRoutes should return routes when API registry responds successfully")
    void getRoutes_whenApiRegistryReturnsRoutes_thenReturnFluxOfRoutes() {
        // Given
        String validRoutesJson = """
                [{
                    "id": "route1",
                    "uri": "http://service1:8080",
                    "predicates": [{"name": "Path", "args": {"_genkey_0": "/api/v1/**"}}],
                    "filters": [],
                    "order": 0
                },
                {
                    "id": "route2", 
                    "uri": "http://service2:8080",
                    "predicates": [{"name": "Path", "args": {"_genkey_0": "/api/v2/**"}}],
                    "filters": [],
                    "order": 1
                }]""";

        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/routes"))
                .withHeader("Accept", WireMock.equalTo("application/json"))
                .withHeader("user-id", WireMock.equalTo("1"))
                .withHeader("scope", WireMock.equalTo("SYSTEM_READ"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(validRoutesJson)));

        // When & Then
        StepVerifier.create(client.getRoutes())
                .expectNextCount(EXPECTED_ROUTE_COUNT)
                .expectComplete()
                .verify(TEST_TIMEOUT);
    }

    @Test
    @DisplayName("getRoutes should return dummy route when API registry returns server error")
    void getRoutes_whenApiRegistryReturnsServerError_thenReturnDummyRoute() {
        // Given
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/routes"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withBody("Internal Server Error")));

        // When & Then
        StepVerifier.create(client.getRoutes())
                .expectNextCount(EXPECTED_SINGLE_ROUTE)
                .expectComplete()
                .verify(TEST_TIMEOUT);
    }

    @Test
    @DisplayName("getRoutes should return dummy route when API registry connection times out")
    void getRoutes_whenApiRegistryConnectionTimesOut_thenReturnDummyRoute() {
        // Given - Stop the WireMock server to simulate connection failure
        wireMockServer.stop();

        try {
            // When & Then
            StepVerifier.create(client.getRoutes())
                    .expectNextCount(EXPECTED_SINGLE_ROUTE)
                    .expectComplete()
                    .verify(TEST_TIMEOUT);
        } finally {
            // Restart the server for subsequent tests
            wireMockServer.start();
            WireMock.configureFor("localhost", wireMockServer.port());
        }
    }

    @Test
    @DisplayName("getRoutes should return empty flux when API registry returns empty array")
    void getRoutes_whenApiRegistryReturnsEmptyArray_thenReturnEmptyFlux() {
        // Given
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/routes"))
                .withHeader("Accept", WireMock.equalTo("application/json"))
                .withHeader("user-id", WireMock.equalTo("1"))
                .withHeader("scope", WireMock.equalTo("SYSTEM_READ"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        // When & Then
        StepVerifier.create(client.getRoutes())
                .expectComplete()
                .verify(TEST_TIMEOUT);
    }

    @Test
    @DisplayName("getRoutes should return dummy route when API registry returns malformed JSON")
    void getRoutes_whenApiRegistryReturnsMalformedJson_thenReturnDummyRoute() {
        // Given
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/routes"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("invalid json")));

        // When & Then
        StepVerifier.create(client.getRoutes())
                .expectNextCount(EXPECTED_SINGLE_ROUTE)
                .expectComplete()
                .verify(TEST_TIMEOUT);
    }

    @Test
    @DisplayName("getRoutes should include correct headers in request")
    void getRoutes_whenCalled_thenIncludeRequiredHeaders() {
        // Given
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/routes"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        // When
        StepVerifier.create(client.getRoutes())
                .expectComplete()
                .verify(TEST_TIMEOUT);

        // Then
        WireMock.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/api/v1/routes"))
                .withHeader("Accept", WireMock.equalTo("application/json"))
                .withHeader("user-id", WireMock.equalTo("1"))
                .withHeader("scope", WireMock.equalTo("SYSTEM_READ")));
    }

    @Test
    @DisplayName("getRoutes should handle 404 not found gracefully")
    void getRoutes_whenApiRegistryReturnsNotFound_thenReturnDummyRoute() {
        // Given
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/routes"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withBody("Not Found")));

        // When & Then
        StepVerifier.create(client.getRoutes())
                .expectNextCount(EXPECTED_SINGLE_ROUTE)
                .expectComplete()
                .verify(TEST_TIMEOUT);
    }

    @Test
    @DisplayName("getRoutes should handle client errors gracefully")
    void getRoutes_whenApiRegistryReturnsBadRequest_thenReturnDummyRoute() {
        // Given
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/routes"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())
                        .withBody("Bad Request")));

        // When & Then
        StepVerifier.create(client.getRoutes())
                .expectNextCount(EXPECTED_SINGLE_ROUTE)
                .expectComplete()
                .verify(TEST_TIMEOUT);
    }
}
