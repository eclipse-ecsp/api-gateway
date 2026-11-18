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

package org.eclipse.ecsp.register.customization;

import org.eclipse.ecsp.controller.CustomGatewayFilterInvalidTestController;
import org.eclipse.ecsp.register.ApiRouteRegistrationService;
import org.eclipse.ecsp.register.ApiRoutesLoader;
import org.eclipse.ecsp.register.model.FilterDefinition;
import org.eclipse.ecsp.register.model.RouteDefinition;
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
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads the real springdoc pipeline against a fixture controller, with the route customizer configured as the
 * list-shaped {@code api.registry.route-customizer} property, and checks the routes sent to api-registry.
 */
@SpringBootTest(classes = RouteCustomizerIntegrationTest.RouteCustomizerTestConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "api.registry.enabled=true",
    "api.registry.security.enabled=false",
    "api.registry.service_name=http://registry",
    "openapi.path.include=/route-customizer/**",
    "spring.application.name=registry-test-service",
    "spring.application.version=1.0",
    "spring.application.servicename=registry-test-service",
    "server.port=8080",
    "api.registry.route-customizer[0].route-id=route-customizer-test-controller-findVehicle",
    "api.registry.route-customizer[0].filters[0].name=VehicleIdValidation",
    "api.registry.route-customizer[0].filters[0].args.source=header",
    "api.registry.route-customizer[0].filters[0].args.name=vehicle-id",
    "api.registry.route-customizer[1].path=/route-customizer/vehicles/{vehicleId}",
    "api.registry.route-customizer[1].method=GET",
    "api.registry.route-customizer[1].filters[0].name=AuditFilter",
    "api.registry.route-customizer[2].route-id=no-such-route",
    "api.registry.route-customizer[2].filters[0].name=OrphanFilter"
})
class RouteCustomizerIntegrationTest {

    private static final String ROUTE_ID = "route-customizer-test-controller-findVehicle";

    @Autowired
    private ApiRoutesLoader apiRoutesLoader;

    @MockitoBean
    private ApiRouteRegistrationService apiRouteRegistrationService;

    @Test
    void configuredFiltersFollowTheAnnotationDrivenFilterInMappingOrder() throws URISyntaxException {
        RouteDefinition route = route(apiRoutesLoader.getApiRoutes());

        assertEquals(List.of("JwtAuthValidator", "VehicleIdValidation", "AuditFilter"), filterNames(route));
        assertEquals("SelfManage", route.getFilters().get(0).getArgs().get("scope"));
        assertEquals(Map.of("source", "header", "name", "vehicle-id"), route.getFilters().get(1).getArgs());
    }

    @Test
    void mappingThatSelectsNoRouteChangesNoRoute() throws URISyntaxException {
        List<RouteDefinition> routes = apiRoutesLoader.getApiRoutes();

        assertTrue(routes.stream().anyMatch(route -> ROUTE_ID.equals(route.getId())));
        assertTrue(routes.stream().noneMatch(route -> filterNames(route).contains("OrphanFilter")));
    }

    private static RouteDefinition route(List<RouteDefinition> routes) {
        return routes.stream().filter(route -> ROUTE_ID.equals(route.getId())).findFirst().orElseThrow();
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
