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

package org.eclipse.ecsp.gateway.plugins;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.jetty.JettyHttpServerFactory;
import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.gateway.clients.ApiRegistryClient;
import org.eclipse.ecsp.gateway.plugins.RequestHeaderFilter.Config;
import org.eclipse.ecsp.gateway.plugins.RequestHeaderFilter.GlobalHeaderConfig;
import org.eclipse.ecsp.gateway.rest.ApiGatewayController;
import org.eclipse.ecsp.gateway.service.PublicKeyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import java.util.List;
import java.util.Set;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {RequestHeaderFilterTest.RequestHeaderFilterTestConfig.class}
)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class RequestHeaderFilterTest {

    @MockitoBean
    ApiGatewayController apiGatewayController;

    @MockitoBean
    PublicKeyService publicKeyService;

    @MockitoBean
    ApiRegistryClient apiRegistryClient;

    @Autowired
    WebTestClient webTestClient;
    @Autowired
    WireMockServer wireMockServer;
    @Autowired
    RequestHeaderFilter requestHeaderFilter;


    @BeforeAll
    static void setUp() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @AfterEach
    void afterEach() {
        wireMockServer.resetAll();
    }

    @Test
    void testMandatoryHeaderIsMissing() {
        webTestClient.post().uri("/v1/test/missing-header")
                .bodyValue("""
                        {
                            "test": "test"
                        }
                        """)
                .header("MyCustomHeader", "test")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody().jsonPath("$.message").isEqualTo("Missing required header sessionId in the request");
    }

    @Test
    void testMandatoryHeaderAvailable() {
        wireMockServer.stubFor(WireMock.post("/v1/test/valid-header").willReturn(WireMock.ok()));
        webTestClient.post().uri("/v1/test/valid-header")
                .bodyValue("""
                        {
                            "test": "test"
                        }
                        """)
                .header("sessionid", "test1")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void testOptionalHeaderAvailable() {
        wireMockServer.stubFor(WireMock.post("/v1/test/valid-optional-header").willReturn(WireMock.ok()));
        webTestClient.post().uri("/v1/test/valid-optional-header")
                .bodyValue("""
                        {
                            "test": "test"
                        }
                        """)
                .header("sessionid", "test1")
                .header("requestId", "test2")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void testInvalidHeaderValue() {
        wireMockServer.stubFor(WireMock.post("/v1/test/invalid-header-value").willReturn(WireMock.ok()));
        webTestClient.post().uri("/v1/test/invalid-header-value")
                .bodyValue("""
                        {
                            "test": "test"
                        }
                        """)
                .header("sessionId", "test")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody().jsonPath("$.message").isEqualTo("Invalid sessionId header value: test");
    }

    @Test
    void testInvalidOptionalHeaderValue() {
        wireMockServer.stubFor(WireMock.post("/v1/test/optional-header").willReturn(WireMock.ok()));
        webTestClient.post().uri("/v1/test/optional-header")
                .bodyValue("""
                        {
                            "test": "test"
                        }
                        """)
                .header("sessionId", "test1")
                .header("requestId", "test")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody().jsonPath("$.message").isEqualTo("Invalid requestId header value: test");
    }

    @Test
    void testRemoveUnknownHeader() {
        wireMockServer.stubFor(WireMock.post("/v1/test/remove-unknown-header").willReturn(WireMock.ok()));
        webTestClient.post().uri("/v1/test/remove-unknown-header")
                .bodyValue("""
                        {
                            "test": "test"
                        }
                        """)
                .header("sessionId", "test1")
                .header("requestId", "test1")
                .header("CustomHeader", "header123")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.OK);
        wireMockServer.verify(postRequestedFor(urlEqualTo("/v1/test/remove-unknown-header"))
                .withHeader("sessionId", equalTo("test1"))
                .withHeader("requestId", equalTo("test1"))
                .withHeader("correlationId", matching("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
                .withHeader("x-api-key", matching("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
                .withoutHeader("CustomHeader"));
    }

    @Test
    void testSkipHeader() {
        wireMockServer.stubFor(WireMock.post("/v1/test/skip-header").willReturn(WireMock.ok()));
        webTestClient.post().uri("/v1/test/skip-header")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "test": "test"
                        }
                        """)
                .header("sessionId", "test1")
                .header("requestId", "test1")
                .header("X-Test-Header", "test")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.OK);
        wireMockServer.verify(postRequestedFor(urlEqualTo("/v1/test/skip-header"))
                .withHeader("sessionId", equalTo("test1"))
                .withHeader("requestId", equalTo("test1"))
                .withHeader("X-Test-Header", equalTo("test"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("correlationId", matching("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
                .withHeader("host", equalTo("localhost:" + wireMockServer.port())));
    }

    @Test
    void testSkipValidationForApiHeader() {
        wireMockServer.stubFor(WireMock.post("/v1/test/skip-header-validation").willReturn(WireMock.ok()));
        webTestClient.post().uri("/v1/test/skip-header-validation")
                .bodyValue("""
                        {
                            "test": "test"
                        }
                        """)
                .header("CustomHeader", "CustomHeaderValue")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.OK);
        wireMockServer.verify(postRequestedFor(urlEqualTo("/v1/test/skip-header-validation"))
                .withHeader("CustomHeader", equalTo("CustomHeaderValue")));
    }

    @Test
    void testRouteLevelHeaders() {
        wireMockServer.stubFor(WireMock.post("/v2/test/route-header-validation").willReturn(WireMock.ok()));
        webTestClient.post().uri("/v2/test/route-header-validation")
                .bodyValue("""
                        {
                            "test": "test"
                        }
                        """)
                .header("x-api-key", "MyValidApiKey")
                .header("sessionId", "sessionId1")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.OK);
        wireMockServer.verify(postRequestedFor(urlEqualTo("/v2/test/route-header-validation"))
                .withHeader("correlationId", matching("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
                .withHeader("sessionId", equalTo("sessionId1"))
                .withHeader("x-api-key", equalTo("MyValidApiKey")));
    }

    @Test
    void testRouteLevelOptionalHeaders() {
        wireMockServer.stubFor(WireMock.post("/v2/test/route-header-validation").willReturn(WireMock.ok()));
        webTestClient.post().uri("/v2/test/route-header-validation")
                .bodyValue("""
                        {
                            "test": "test"
                        }
                        """)
                .header("x-api-key", "MyValidApiKey")
                .header("x-tenant-id", "MyValidTenantID")
                .header("sessionId", "sessionId1")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.OK);
        wireMockServer.verify(postRequestedFor(urlEqualTo("/v2/test/route-header-validation"))
                .withHeader("correlationId", matching("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
                .withHeader("sessionId", equalTo("sessionId1"))
                .withHeader("x-tenant-id", equalTo("MyValidTenantID"))
                .withHeader("x-api-key", equalTo("MyValidApiKey")));
    }

    @Test
    void testRouteLevelInvalidMetaDataHeaders() {
        wireMockServer.stubFor(WireMock.post("/v3/test/route-invalid-header-metadata").willReturn(WireMock.ok()));
        webTestClient.post().uri("/v3/test/route-invalid-header-metadata")
                .bodyValue("""
                        {
                            "test": "test"
                        }
                        """)
                .header("x-api-key", "MyValidApiKey")
                .header("x-tenant-id", "MyValidTenantID")
                .header("sessionId", "sessionId1")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.OK);
        wireMockServer.verify(postRequestedFor(urlEqualTo("/v3/test/route-invalid-header-metadata"))
                .withHeader("correlationId", matching("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
                .withHeader("sessionId", equalTo("sessionId1"))
                .withoutHeader("x-tenant-id")
                .withoutHeader("x-api-key"));
    }

    @Test
    void testGetOrDefault() {
        String result = ReflectionTestUtils.invokeMethod(requestHeaderFilter, "getOrDefault", null, "test");
        Assertions.assertEquals("test", result);

        String result2 = ReflectionTestUtils.invokeMethod(requestHeaderFilter, "getOrDefault", "arg1", "arg2");
        Assertions.assertEquals("arg1", result2);
    }

    @Test
    void testInvalidRoute() {
        webTestClient.post().uri("/v4/test")
                .bodyValue("""
                        {
                            "test": "test"
                        }
                        """)
                .header("x-api-key", "MyValidApiKey")
                .header("x-tenant-id", "MyValidTenantID")
                .header("sessionId", "sessionId1")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testServerFailure() {
        wireMockServer.stubFor(WireMock.post("/v2/test").willReturn(WireMock.serverError()));
        webTestClient.post().uri("/v2/test")
                .bodyValue("""
                        {
                            "test": "test"
                        }
                        """)
                .header("x-api-key", "MyValidApiKey")
                .header("x-tenant-id", "MyValidTenantID")
                .header("sessionId", "sessionId1")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(String.class);
    }

    @TestConfiguration()
    static class RequestHeaderFilterTestConfig {

        @Autowired
        RequestHeaderFilter requestHeaderFilter;

        @Bean(destroyMethod = "stop")
        WireMockServer wireMockServer() {
            WireMockConfiguration options = wireMockConfig()
                    .dynamicPort()
                    .httpServerFactory(new JettyHttpServerFactory());
            WireMockServer wireMock = new WireMockServer(options);
            wireMock.start();
            return wireMock;
        }

        @Bean
        RouteLocator testRoutes(RouteLocatorBuilder builder, WireMockServer wireMock) {
            Config config = new Config();
            config.setRemoveUnknownHeaders(true);
            config.setAllowHeaders(Set.of("X-Test-Header", "Content-Type"));
            config.setSkipValidationForApis(Set.of("/v1/test/skip-header-validation"));
            config.setAppendHeadersIfMissing(Set.of("correlationId", "x-api-key"));
            GlobalHeaderConfig sessionHeaderConfig = new GlobalHeaderConfig();
            sessionHeaderConfig.setName("sessionId");
            sessionHeaderConfig.setRequired(true);
            sessionHeaderConfig.setRegex("^[a-zA-Z0-9_]{5,10}$");
            GlobalHeaderConfig requestHeaderConfig = new GlobalHeaderConfig();
            requestHeaderConfig.setName("requestId");
            requestHeaderConfig.setRequired(false);
            requestHeaderConfig.setRegex("^[a-zA-Z0-9_]{5,10}$");
            config.setGlobalHeaders(List.of(sessionHeaderConfig, requestHeaderConfig));

            GatewayFilter gatewayFilter = requestHeaderFilter.apply(config);
            return builder
                    .routes()
                    .route(predicateSpec -> predicateSpec
                            .path("/v1/test/**")
                            .filters(spec -> spec.filter(gatewayFilter))
                            .uri(wireMock.baseUrl()))
                    .route(predicateSpec -> predicateSpec
                            .path("/v2/test/**")
                            .filters(spec -> spec.filter(gatewayFilter))
                            .metadata("headers", "[{\"required\":true,\"name\":\"x-api-key\"}"
                                    + ",{\"required\":false,\"name\":\"x-tenant-id\"}]")
                            .uri(wireMock.baseUrl()))
                    .route(predicateSpec -> predicateSpec
                            .path("/v3/test/**")
                            .filters(spec -> spec.filter(gatewayFilter))
                            .metadata("headers", "[{,\"name\":\"x-api-key\"},"
                                    + "{\"required\":false,\"name\":\"x-tenant-id\"}]")
                            .uri(wireMock.baseUrl()))
                    .build();
        }
    }
}
