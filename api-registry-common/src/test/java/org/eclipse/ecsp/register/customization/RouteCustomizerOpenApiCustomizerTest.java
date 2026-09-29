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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteCustomizerOpenApiCustomizerTest {

    private RouteCustomizerProperties properties;
    private RouteCustomizerOpenApiCustomizer customizer;

    @BeforeEach
    void setUp() {
        properties = new RouteCustomizerProperties();
        customizer = new RouteCustomizerOpenApiCustomizer(properties);
        customizer.setEmbeddedValueResolver(value -> value.replace("${spring.application.servicename}",
                "demo-service"));
    }

    private OpenAPI openApiWithOperation(String path, String tag, String operationId, boolean secured) {
        Operation operation = new Operation();
        operation.setTags(List.of(tag));
        operation.setOperationId(operationId);
        if (secured) {
            operation.setSecurity(List.of(new SecurityRequirement().addList("JwtAuthValidator", "SelfManage")));
        }
        Paths paths = new Paths();
        paths.addPathItem(path, new PathItem().get(operation));
        OpenAPI openApi = new OpenAPI();
        openApi.setPaths(paths);
        return openApi;
    }

    private RouteCustomizerProperties.RouteMapping mapping(String routeId, boolean replaceOriginal,
                                                            String... targets) {
        RouteCustomizerProperties.RouteMapping mapping = new RouteCustomizerProperties.RouteMapping();
        mapping.setRouteId(routeId);
        mapping.setReplaceOriginal(replaceOriginal);
        mapping.setExposeAs(List.of(targets));
        return mapping;
    }

    @Test
    void renamesRouteWhenReplaceOriginalIsTrue() {
        OpenAPI openApi = openApiWithOperation("/v1/api/dothis", "Demo", "doThis", true);
        properties.setMappings(List.of(mapping("Demo-doThis", true, "/api/dothis")));

        customizer.customise(openApi);

        Paths paths = openApi.getPaths();
        assertFalse(paths.containsKey("/v1/api/dothis"));
        assertTrue(paths.containsKey("/api/dothis"));
        Operation clone = paths.get("/api/dothis").getGet();
        assertEquals("Demo-doThis", clone.getExtensions().get(RouteCustomizerOpenApiCustomizer.ALIAS_OF_EXTENSION));
        assertEquals("/v1/api/dothis",
                clone.getExtensions().get(RouteCustomizerOpenApiCustomizer.ORIGINAL_PATH_EXTENSION));
        assertEquals(1, clone.getSecurity().size());
    }

    @Test
    void fansOutWithoutRemovingOriginalWhenReplaceOriginalIsFalse() {
        OpenAPI openApi = openApiWithOperation("/v1/api/dothis", "Demo", "doThis", false);
        properties.setMappings(List.of(mapping("Demo-doThis", false,
                "/api/dothis", "/${spring.application.servicename}/v1/api/perform")));

        customizer.customise(openApi);

        Paths paths = openApi.getPaths();
        assertTrue(paths.containsKey("/v1/api/dothis"));
        assertTrue(paths.containsKey("/api/dothis"));
        assertTrue(paths.containsKey("/demo-service/v1/api/perform"));
        assertEquals(3, paths.size());
        String clone1Id = paths.get("/api/dothis").getGet().getOperationId();
        String clone2Id = paths.get("/demo-service/v1/api/perform").getGet().getOperationId();
        assertNotEquals(clone1Id, clone2Id);
    }

    @Test
    void cloneRetagsSummaryAndBakedRouteIdInDescription() {
        OpenAPI openApi = openApiWithOperation("/v1/api/dothis", "Demo", "doThis", false);
        Operation original = openApi.getPaths().get("/v1/api/dothis").getGet();
        // Mirrors what ScopeTagger (an OperationCustomizer, not a GlobalOpenApiCustomizer) bakes
        // into the source operation before this customizer ever runs; it never sees the clone.
        original.setSummary("GET /v1/api/dothis");
        original.setDescription("Does this<br>Demo-doThis<p style='color:red;'>SCOPE: []</p>");
        properties.setMappings(List.of(mapping("Demo-doThis", false, "/api/dothis")));

        customizer.customise(openApi);

        Operation clone = openApi.getPaths().get("/api/dothis").getGet();
        assertEquals("GET /api/dothis", clone.getSummary());
        String expectedAliasRouteId = "Demo-" + clone.getOperationId();
        assertEquals("Does this<br>" + expectedAliasRouteId + "<p style='color:red;'>SCOPE: []</p>",
                clone.getDescription());
        // Original operation's own docs are untouched.
        assertEquals("GET /v1/api/dothis", original.getSummary());
        assertTrue(original.getDescription().contains("Demo-doThis"));
    }

    @Test
    void cloneRetagsSummaryWrittenAsMethodPlusClassLevelPrefixStrippedPath() {
        // Some controllers hand-author @Operation(summary = "GET " + <method-mapping-only path>)
        // omitting the class-level @RequestMapping prefix, so it never literally equals the full
        // registered path (e.g. real path is /v1.0/findVehicleById/{id}, summary just says
        // "GET /findVehicleById/{id}"). The literal full-path replace can't catch that; the
        // leading "<METHOD> /" shape is what identifies it as this convention instead.
        OpenAPI openApi = openApiWithOperation("/v1.0/findVehicleById/{id}", "Demo", "findVehicleById", false);
        Operation original = openApi.getPaths().get("/v1.0/findVehicleById/{id}").getGet();
        original.setSummary("GET /findVehicleById/{id}");
        properties.setMappings(List.of(mapping("Demo-findVehicleById", false, "/v1/vehicles/{id}")));

        customizer.customise(openApi);

        Operation clone = openApi.getPaths().get("/v1/vehicles/{id}").getGet();
        assertEquals("GET /v1/vehicles/{id}", clone.getSummary());
        assertEquals("GET /findVehicleById/{id}", original.getSummary());
    }

    @Test
    void noMatchLeavesPathsUnchanged() {
        OpenAPI openApi = openApiWithOperation("/v1/api/dothis", "Demo", "doThis", false);
        properties.setMappings(List.of(mapping("Other-op", false, "/api/other")));

        customizer.customise(openApi);

        assertEquals(1, openApi.getPaths().size());
    }

    @Test
    void unresolvedPlaceholderSkipsOnlyThatTarget() {
        customizer.setEmbeddedValueResolver(value -> {
            throw new IllegalArgumentException("unresolved: " + value);
        });
        OpenAPI openApi = openApiWithOperation("/v1/api/dothis", "Demo", "doThis", false);
        properties.setMappings(List.of(mapping("Demo-doThis", false, "/${unknown.property}/v1/api/perform")));

        customizer.customise(openApi);

        assertEquals(1, openApi.getPaths().size());
    }

    @Test
    void reservedPathTargetIsSkipped() {
        OpenAPI openApi = openApiWithOperation("/v1/api/dothis", "Demo", "doThis", false);
        properties.setMappings(List.of(mapping("Demo-doThis", false, "/actuator/health")));

        customizer.customise(openApi);

        assertEquals(1, openApi.getPaths().size());
    }

    @Test
    void missingPathVariableTargetIsSkipped() {
        OpenAPI openApi = openApiWithOperation("/v1/api/dothis/{id}", "Demo", "doThis", false);
        properties.setMappings(List.of(mapping("Demo-doThis", false, "/api/dothis")));

        customizer.customise(openApi);

        assertEquals(1, openApi.getPaths().size());
    }

    @Test
    void allTargetsFailingLeavesOriginalWhenReplaceOriginalTrue() {
        OpenAPI openApi = openApiWithOperation("/v1/api/dothis/{id}", "Demo", "doThis", false);
        properties.setMappings(List.of(mapping("Demo-doThis", true, "/api/dothis")));

        customizer.customise(openApi);

        assertTrue(openApi.getPaths().containsKey("/v1/api/dothis/{id}"));
        assertEquals(1, openApi.getPaths().size());
    }

    @Test
    void customiseIsNoOpWithoutMappings() {
        OpenAPI openApi = openApiWithOperation("/v1/api/dothis", "Demo", "doThis", false);
        customizer.customise(openApi);
        assertEquals(1, openApi.getPaths().size());
    }
}
