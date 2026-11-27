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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.eclipse.ecsp.config.RouteCustomizerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RouteCustomizerOpenApiCustomizer}.
 */
class RouteCustomizerOpenApiCustomizerTest {

    private RouteCustomizerProperties properties;
    private RouteCustomizerOpenApiCustomizer customizer;

    @BeforeEach
    void setUp() {
        properties = new RouteCustomizerProperties();
        properties.setEnabled(true);
        customizer = new RouteCustomizerOpenApiCustomizer(properties);
    }

    private static RouteCustomizerProperties.FilterOverride filter(String name, Map<String, String> args) {
        RouteCustomizerProperties.FilterOverride filter = new RouteCustomizerProperties.FilterOverride();
        filter.setName(name);
        filter.setArgs(new HashMap<>(args));
        return filter;
    }

    private static Operation operation(String tag, String operationId, String description) {
        Operation operation = new Operation();
        if (tag != null) {
            operation.setTags(List.of(tag));
        }
        operation.setOperationId(operationId);
        operation.setDescription(description);
        return operation;
    }

    @Test
    void addsFilterExtensionsAndDescriptionMatchingCustomGatewayFilterFormat() {
        RouteCustomizerProperties.RouteMapping byRouteId = new RouteCustomizerProperties.RouteMapping();
        byRouteId.setRouteId("Demo-do-this");
        byRouteId.setFilters(List.of(
                filter("VehicleIdValidation", Map.of("source", "header", "name", "vehicle-id")),
                filter("AuditFilter", Map.of())));
        properties.setMappings(List.of(byRouteId));

        Operation matched = operation("Demo", "do_this", "Find vehicle");
        Operation untouched = operation("Demo", "otherOp", "Other");
        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/v1/vehicles", new PathItem().get(matched).post(untouched)));

        customizer.customise(openApi);
        customizer.customise(openApi); // idempotent on repeat invocation

        assertEquals("Find vehicle<br><b>Custom Gateway Filters:</b> "
                + "<span style='color:blue;'>VehicleIdValidation</span>, "
                + "<span style='color:blue;'>AuditFilter</span>", matched.getDescription());
        assertEquals(Map.of("source", "header", "name", "vehicle-id"),
                matched.getExtensions().get(CustomGatewayFilterCustomizer.FILTERS_EXTENSION + "-VehicleIdValidation"));
        assertEquals(Map.of(),
                matched.getExtensions().get(CustomGatewayFilterCustomizer.FILTERS_EXTENSION + "-AuditFilter"));
        assertTrue(matched.getExtensions().get(CustomGatewayFilterCustomizer.FILTERS_EXTENSION + "-AuditFilter")
                instanceof RouteCustomizerOpenApiCustomizer.ConfiguredFilterArgs);
        assertEquals("Other", untouched.getDescription());
        assertNull(untouched.getExtensions());
    }

    @Test
    void appendsToExistingAnnotationDescriptionAndPreservesAnnotationExtensionArgs() {
        RouteCustomizerProperties.RouteMapping byPath = new RouteCustomizerProperties.RouteMapping();
        byPath.setPath("/v1/vehicles");
        byPath.setMethod("GET");
        byPath.setFilters(List.of(
                filter("ValidateApiKey", Map.of("header-name", "x-override")),
                filter("ConfigFilter", Map.of("mode", "strict"))));
        properties.setMappings(List.of(byPath));

        Operation op = operation(null, null,
                "Desc<br><b>Custom Gateway Filters:</b> <span style='color:blue;'>ValidateApiKey</span>");
        op.addExtension("x-filter-ValidateApiKey", new HashMap<>(Map.of("header-name", "x-api-key")));
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/v1/vehicles", new PathItem().get(op)));

        customizer.customise(openApi);

        assertEquals("Desc<br><b>Custom Gateway Filters:</b> <span style='color:blue;'>ValidateApiKey</span>, "
                + "<span style='color:blue;'>ValidateApiKey</span>, <span style='color:blue;'>ConfigFilter</span>",
                op.getDescription());
        assertEquals(Map.of("header-name", "x-api-key"), op.getExtensions().get("x-filter-ValidateApiKey"));
        assertEquals(Map.of("mode", "strict"), op.getExtensions().get("x-filter-ConfigFilter"));
    }

    @Test
    void doesNothingWhenDisabledOrEmpty() {
        RouteCustomizerProperties.RouteMapping mapping = new RouteCustomizerProperties.RouteMapping();
        mapping.setRouteId("Demo-doThis");
        mapping.setFilters(List.of(filter("AuditFilter", Map.of())));
        properties.setMappings(List.of(mapping));
        properties.setEnabled(false);

        Operation op = operation("Demo", "doThis", null);
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/v1/demo", new PathItem().get(op)));

        customizer.customise(openApi);

        assertNull(op.getDescription());
        assertNull(op.getExtensions());
    }
}
