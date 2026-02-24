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

package org.eclipse.ecsp.customizers;

import org.eclipse.ecsp.controller.CustomGatewayFilterInvalidTestController;
import org.eclipse.ecsp.customizers.CustomGatewayFilterCustomizerIntegrationTest.RegistryTestConfiguration;
import org.eclipse.ecsp.register.ApiRouteRegistrationService;
import org.eclipse.ecsp.register.ApiRoutesLoader;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@SpringBootTest(classes = RegistryTestConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "api.registry.enabled=true",
    "api.registry.service_name=http://registry",
    "openapi.path.include=/v1/test/**",
    "openapi.path.exclude=/v2/test/**",
    "spring.application.name=registry-test-service",
    "spring.application.version=1.0",
    "spring.application.servicename=registry-test-service",
    "server.port=8080"
})
class CustomGatewayFilterCustomizerIntegrationTest {

    @Autowired
    ApiRoutesLoader apiRoutesLoader;

    @MockitoBean
    ApiRouteRegistrationService apiRouteRegistrationService;

    @Test
    void testGatewayFilterCustomizer() throws URISyntaxException {
        List<RouteDefinition> routes = apiRoutesLoader.getApiRoutes();
        Assertions.assertFalse(routes.isEmpty(), "No routes found in the API registry");
        Optional<RouteDefinition> route = routes.stream()
                .filter(r ->
                        r.getId().equals("custom-gateway-filters-test-controller-testWithMultiFilters"))
                .findFirst();
        Assertions.assertTrue(route.isPresent());
        RouteDefinition routeDefinition = route.get();
        Assertions.assertFalse(routeDefinition.getFilters().isEmpty());
        Assertions.assertTrue(routeDefinition.getFilters().stream()
                .anyMatch(filter ->
                        filter.getName().equals("testFilter3")
                                && filter.getArgs().get("key1").equals("value1")));

        Optional<RouteDefinition> route2 = routes.stream()
                .filter(r ->
                        r.getId().equals("custom-gateway-filters-test-controller-testWithFilters"))
                .findFirst();
        Assertions.assertTrue(route2.isPresent());
        RouteDefinition route2Definition = route2.get();
        Assertions.assertFalse(route2Definition.getFilters().isEmpty());
        Assertions.assertTrue(route2Definition.getFilters().stream()
                .anyMatch(filter ->
                        filter.getName().equals("ValidateApiKey")
                                && filter.getArgs().get("header-name").equals("x-api-key")));
    }

    @SpringBootApplication
    @ComponentScan(basePackages = {"org.eclipse.ecsp", "org.springdoc"},
            excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                            classes = CustomGatewayFilterInvalidTestController.class)
            })
    static class RegistryTestConfiguration {
    }

}