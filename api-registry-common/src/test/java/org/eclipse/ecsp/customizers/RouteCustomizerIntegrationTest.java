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

package org.eclipse.ecsp.customizers;

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.ecsp.controller.CustomGatewayFilterInvalidTestController;
import org.eclipse.ecsp.controller.CustomGatewayFiltersTestController;
import org.eclipse.ecsp.register.ApiRouteRegistrationService;
import org.eclipse.ecsp.register.ApiRoutesLoader;
import org.eclipse.ecsp.register.model.FilterDefinition;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.utils.ObjectMapperUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Loads the real springdoc pipeline against a fixture controller, with {@code api.registry.route-customizer.enabled}
 * enabled, and checks both the registered routes and the generated OpenAPI document ({@code /v3/api-docs/{group}}).
 */
@SpringBootTest(classes = RouteCustomizerIntegrationTest.RouteCustomizerTestConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "api.registry.enabled=true",
    "api.registry.security.enabled=false",
    "api.registry.custom-gateway-filter.enabled=true",
    "api.registry.route-customizer.enabled=true",
    "api.registry.service_name=http://registry",
    "openapi.path.include=/route-customizer/**",
    "spring.application.name=registry-test-service",
    "spring.application.version=1.0",
    "spring.application.servicename=registry-test-service",
    "server.port=8080",
    "test.property=value1",
    "api.registry.route-customizer.mappings[0].route-id=route-customizer-test-controller-findVehicle",
    "api.registry.route-customizer.mappings[0].filters[0].name=VehicleIdValidation",
    "api.registry.route-customizer.mappings[0].filters[0].args.source=header",
    "api.registry.route-customizer.mappings[0].filters[0].args.name=vehicle-id",
    "api.registry.route-customizer.mappings[1].path=/route-customizer/vehicles/{vehicleId}",
    "api.registry.route-customizer.mappings[1].method=GET",
    "api.registry.route-customizer.mappings[1].filters[0].name=AuditFilter",
    "api.registry.route-customizer.mappings[2].route-id=route-customizer-test-controller-listVehicles",
    "api.registry.route-customizer.mappings[2].filters[0].name=ConfigFilter",
    "api.registry.route-customizer.mappings[2].filters[0].args.mode=strict",
    "api.registry.route-customizer.mappings[3].route-id=no-such-route",
    "api.registry.route-customizer.mappings[3].filters[0].name=OrphanFilter"
})
class RouteCustomizerIntegrationTest {

    private static final String FIND_ROUTE_ID = "route-customizer-test-controller-findVehicle";
    private static final String LIST_ROUTE_ID = "route-customizer-test-controller-listVehicles";

    @Autowired
    private ApiRoutesLoader apiRoutesLoader;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private ApiRouteRegistrationService apiRouteRegistrationService;

    @Test
    void configuredFiltersFollowTheAnnotationDrivenFilterInMappingOrder() throws URISyntaxException {
        List<RouteDefinition> routes = apiRoutesLoader.getApiRoutes();
        RouteDefinition findRoute = route(routes, FIND_ROUTE_ID);

        assertEquals(List.of("JwtAuthValidator", "VehicleIdValidation", "AuditFilter"), filterNames(findRoute));
        assertEquals("SelfManage", findRoute.getFilters().get(0).getArgs().get("scope"));
        assertEquals(Map.of("source", "header", "name", "vehicle-id"), findRoute.getFilters().get(1).getArgs());

        RouteDefinition listRoute = route(routes, LIST_ROUTE_ID);
        assertEquals(List.of("JwtAuthValidator", "AnnotFilter", "ConfigFilter"), filterNames(listRoute));
        assertEquals(Map.of("header-name", "x-annot"), listRoute.getFilters().get(1).getArgs());
        assertEquals(Map.of("mode", "strict"), listRoute.getFilters().get(2).getArgs());
    }

    @Test
    void mappingThatSelectsNoRouteChangesNoRoute() throws URISyntaxException {
        List<RouteDefinition> routes = apiRoutesLoader.getApiRoutes();

        assertTrue(routes.stream().anyMatch(route -> FIND_ROUTE_ID.equals(route.getId())));
        assertTrue(routes.stream().noneMatch(route -> filterNames(route).contains("OrphanFilter")));
    }

    @Test
    void openApiDocumentDisplaysConfiguredFiltersSameAsCustomGatewayFilterAnnotation() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        String body = mockMvc.perform(get("/v3/api-docs/registry-test-service"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode paths = ObjectMapperUtil.getObjectMapper().readTree(body).path("paths");
        JsonNode findGet = paths.path("/route-customizer/vehicles/{vehicleId}").path("get");
        assertTrue(findGet.path("description").asText().contains(
                "<br><b>Custom Gateway Filters:</b> <span style='color:blue;'>VehicleIdValidation</span>, "
                        + "<span style='color:blue;'>AuditFilter</span>"));
        assertEquals("header", findGet.path("x-filter-VehicleIdValidation").path("source").asText());
        assertEquals("vehicle-id", findGet.path("x-filter-VehicleIdValidation").path("name").asText());
        assertTrue(findGet.has("x-filter-AuditFilter"));

        JsonNode listGet = paths.path("/route-customizer/vehicles").path("get");
        assertTrue(listGet.path("description").asText().contains(
                "<br><b>Custom Gateway Filters:</b> <span style='color:blue;'>AnnotFilter</span>, "
                        + "<span style='color:blue;'>ConfigFilter</span>"));
        assertEquals("x-annot", listGet.path("x-filter-AnnotFilter").path("header-name").asText());
        assertEquals("strict", listGet.path("x-filter-ConfigFilter").path("mode").asText());
    }

    private static RouteDefinition route(List<RouteDefinition> routes, String routeId) {
        return routes.stream().filter(route -> routeId.equals(route.getId())).findFirst().orElseThrow();
    }

    private static List<String> filterNames(RouteDefinition route) {
        return route.getFilters().stream().map(FilterDefinition::getName).toList();
    }

    @SpringBootApplication
    @ComponentScan(basePackages = {"org.eclipse.ecsp", "org.springdoc"},
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = {CustomGatewayFilterInvalidTestController.class,
                        CustomGatewayFiltersTestController.class}))
    static class RouteCustomizerTestConfiguration {
    }
}
