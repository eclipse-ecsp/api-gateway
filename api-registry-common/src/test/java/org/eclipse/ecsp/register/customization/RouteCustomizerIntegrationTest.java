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

package org.eclipse.ecsp.register.customization;

import org.eclipse.ecsp.controller.CustomGatewayFilterInvalidTestController;
import org.eclipse.ecsp.register.ApiRouteRegistrationService;
import org.eclipse.ecsp.register.ApiRoutesLoader;
import org.eclipse.ecsp.register.model.FilterDefinition;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.utils.RegistryCommonConstants;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads the real springdoc + route-customizer pipeline against a fixture controller and checks
 * the route definitions that would be sent to api-registry.
 */
@SpringBootTest(classes = RouteCustomizerIntegrationTest.RouteCustomizerTestConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "api.registry.enabled=true",
    "api.registry.security.enabled=false",
    "api.registry.service_name=http://registry",
    "openapi.path.include=/**",
    "spring.application.name=registry-test-service",
    "spring.application.version=1.0",
    "spring.application.servicename=registry-test-service",
    "server.port=8080",
    "api.context-path=/svc",
    "api.registry.registration-mode=reconcile",
    "api.registry.registration-generation=1",
    "api.registry.route-customizer.enabled=true",
    "api.registry.route-customizer.mappings[0].route-id=route-customizer-test-controller-findVehicle",
    "api.registry.route-customizer.mappings[0].filters[0].name=RouteWideFilter",
    "api.registry.route-customizer.mappings[1].route-id=route-customizer-test-controller-findVehicle",
    "api.registry.route-customizer.mappings[1].expose-as[0]=/api/vehicles/{vehicleId}",
    "api.registry.route-customizer.mappings[1].filters[0].name=AliasOnlyFilter",
    "api.registry.route-customizer.mappings[1].filters[0].args.limit=10"
})
class RouteCustomizerIntegrationTest {

    private static final String ORIGINAL_ROUTE_ID = "route-customizer-test-controller-findVehicle";

    @Autowired
    private ApiRoutesLoader apiRoutesLoader;

    @MockitoBean
    private ApiRouteRegistrationService apiRouteRegistrationService;

    @Test
    void originalKeepsItsPathAndGetsOnlyRouteWideFilters() throws URISyntaxException {
        RouteDefinition original = route(apiRoutesLoader.getApiRoutes(), ORIGINAL_ROUTE_ID);

        assertEquals("/route-customizer/vehicles/{vehicleId}", path(original));
        List<String> filters = filterNames(original);
        assertTrue(filters.contains("JwtAuthValidator"), filters.toString());
        assertTrue(filters.contains("RouteWideFilter"), filters.toString());
        assertFalse(filters.contains("AliasOnlyFilter"), filters.toString());
    }

    @Test
    void aliasGetsTargetedRewriteInheritedSecurityAndBothFilterScopes() throws URISyntaxException {
        RouteDefinition alias = apiRoutesLoader.getApiRoutes().stream()
                .filter(route -> route.getId().startsWith(ORIGINAL_ROUTE_ID + "-alias-"))
                .findFirst().orElseThrow();

        assertEquals("/api/vehicles/{vehicleId}", path(alias));
        FilterDefinition rewrite = filter(alias, RegistryCommonConstants.REWRITE_PATH_FILTER);
        assertEquals("^\\Q/api/vehicles/\\E(?<v0>[^/]+)$", rewrite.getArgs().get(RegistryCommonConstants.REGEX));
        assertEquals("/svc/route-customizer/vehicles/${v0}",
                rewrite.getArgs().get(RegistryCommonConstants.REPLACEMENT));
        assertEquals("SelfManage", filter(alias, "JwtAuthValidator").getArgs().get("scope"));
        assertTrue(filterNames(alias).contains("RouteWideFilter"));
        assertEquals("10", filter(alias, "AliasOnlyFilter").getArgs().get("limit"));
    }

    private static RouteDefinition route(List<RouteDefinition> routes, String id) {
        return routes.stream().filter(route -> id.equals(route.getId())).findFirst().orElseThrow();
    }

    private static String path(RouteDefinition route) {
        return route.getPredicates().stream()
                .filter(predicate -> RegistryCommonConstants.PATH.equals(predicate.getName()))
                .map(predicate -> predicate.getArgs().get(RegistryCommonConstants.KEY_0))
                .findFirst().orElseThrow();
    }

    private static FilterDefinition filter(RouteDefinition route, String name) {
        return route.getFilters().stream().filter(filter -> name.equals(filter.getName())).findFirst().orElseThrow();
    }

    private static List<String> filterNames(RouteDefinition route) {
        return route.getFilters().stream().map(FilterDefinition::getName).toList();
    }

    @SpringBootApplication
    @ComponentScan(basePackages = {"org.eclipse.ecsp", "org.springdoc"},
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = CustomGatewayFilterInvalidTestController.class))
    static class RouteCustomizerTestConfiguration {
    }
}
