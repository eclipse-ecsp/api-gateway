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

package org.eclipse.ecsp.gateway.rest;

import org.eclipse.ecsp.gateway.model.ApiService;
import org.eclipse.ecsp.gateway.model.Health;
import org.eclipse.ecsp.gateway.service.IgniteRouteLocator;
import org.eclipse.ecsp.gateway.service.RegistryRouteLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Collections;
import java.util.List;

/**
 * API GatewayController test class.
 */
@ExtendWith(SpringExtension.class)
class ApiGatewayControllerTest {


    private ApiGatewayController apiGatewayController;

    @Mock
    private IgniteRouteLocator igniteRouteLocator;

    @Mock
    private RegistryRouteLoader registryRouteLoader;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        apiGatewayController = new ApiGatewayController(igniteRouteLocator);
    }

    @Test
    void testHealth() {
        Mono<Health> health = apiGatewayController.health();
        Health h = health.block();
        Assertions.assertNotNull(h, "Health object should not be null");
        Assertions.assertEquals("UP", h.getStatus(), "Health status should be UP");
    }

    @Test
    void testReload() {
        apiGatewayController.reload();
        Mockito.verify(igniteRouteLocator, Mockito.times(1)).refreshRoutes();
    }

    @Test
    void testGetApiDocs() {
        Mockito.when(igniteRouteLocator.getApiDocRoutes()).thenReturn(Collections.emptySet());
        Flux<ApiService> apiDocs = apiGatewayController.getApiDocs();
        List<ApiService> apiServiceList = apiDocs.collectList().block();
        Assertions.assertNotNull(apiServiceList, "ApiService list should not be null");
        Assertions.assertTrue(apiServiceList.isEmpty(), "ApiService list should be empty");
    }

    @Test
    void testGetApiDocsForStatic() {
        ReflectionTestUtils.setField(apiGatewayController, "registeredServices", "device-shadow");
        Mockito.when(igniteRouteLocator.getApiDocRoutes()).thenReturn(null);
        Flux<ApiService> apiDocs = apiGatewayController.getApiDocs();
        List<ApiService> apiServiceList = apiDocs.collectList().block();
        Assertions.assertNotNull(apiServiceList, "ApiService list should not be null");
        Assertions.assertFalse(apiServiceList.isEmpty(), "ApiService list should not be empty");
        Assertions.assertEquals(1, apiServiceList.size(), "ApiService list should contain one element");
        Assertions.assertEquals("/v3/api-docs/device-shadow",
                apiServiceList.get(0).getUrl(),
                "Path should be /v3/api-docs/device-shadow");
    }
}
