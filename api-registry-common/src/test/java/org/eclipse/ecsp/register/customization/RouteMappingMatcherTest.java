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

import io.swagger.v3.oas.models.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteMappingMatcherTest {

    private RouteCustomizerProperties.RouteMapping mappingByRouteId(String routeId) {
        RouteCustomizerProperties.RouteMapping mapping = new RouteCustomizerProperties.RouteMapping();
        mapping.setRouteId(routeId);
        return mapping;
    }

    private RouteCustomizerProperties.RouteMapping mappingByPath(String path, String method) {
        RouteCustomizerProperties.RouteMapping mapping = new RouteCustomizerProperties.RouteMapping();
        mapping.setPath(path);
        mapping.setMethod(method);
        return mapping;
    }

    private Operation originalOperation() {
        Operation operation = new Operation();
        operation.setTags(List.of("Demo"));
        operation.setOperationId("doThis");
        return operation;
    }

    private Operation cloneOperation(String aliasOf, String originalPath, int producedByMappingIndex) {
        Operation operation = new Operation();
        operation.setTags(List.of("Demo"));
        operation.setOperationId("doThis-alias-abc123456789");
        operation.setExtensions(Map.of(
                RouteCustomizerOpenApiCustomizer.ALIAS_OF_EXTENSION, aliasOf,
                RouteCustomizerOpenApiCustomizer.ORIGINAL_PATH_EXTENSION, originalPath,
                RouteCustomizerOpenApiCustomizer.MAPPING_INDEX_EXTENSION, producedByMappingIndex));
        return operation;
    }

    private RouteCustomizerProperties.RouteMapping exposingMapping() {
        RouteCustomizerProperties.RouteMapping mapping = mappingByRouteId("Demo-doThis");
        mapping.setExposeAs(List.of("/api/dothis"));
        return mapping;
    }

    @Test
    void filtersOnlyMappingMatchesOriginalOperationByRouteId() {
        assertTrue(RouteMappingMatcher.matches(
                mappingByRouteId("Demo-doThis"), 0, originalOperation(), "/v1/api/dothis", HttpMethod.GET));
    }

    @Test
    void filtersOnlyMappingMatchesEveryAliasOfItsRoute() {
        Operation clone = cloneOperation("Demo-doThis", "/v1/api/dothis", 1);
        assertTrue(RouteMappingMatcher.matches(
                mappingByRouteId("Demo-doThis"), 0, clone, "/api/dothis", HttpMethod.GET));
    }

    @Test
    void exposingMappingMatchesOnlyTheAliasesItProduced() {
        assertTrue(RouteMappingMatcher.matches(exposingMapping(), 1,
                cloneOperation("Demo-doThis", "/v1/api/dothis", 1), "/api/dothis", HttpMethod.GET));
        assertFalse(RouteMappingMatcher.matches(exposingMapping(), 1,
                cloneOperation("Demo-doThis", "/v1/api/dothis", 2), "/api/other", HttpMethod.GET));
        assertFalse(RouteMappingMatcher.matches(exposingMapping(), 1,
                originalOperation(), "/v1/api/dothis", HttpMethod.GET));
    }

    @Test
    void doesNotMatchUnrelatedRouteId() {
        assertFalse(RouteMappingMatcher.matches(
                mappingByRouteId("Other-op"), 0, originalOperation(), "/v1/api/dothis", HttpMethod.GET));
    }

    @Test
    void matchesOriginalOperationByPath() {
        assertTrue(RouteMappingMatcher.matches(
                mappingByPath("/v1/api/dothis", null), 0, originalOperation(), "/v1/api/dothis", HttpMethod.GET));
    }

    @Test
    void pathMappingMatchesAliasByOriginalPathExtension() {
        Operation clone = cloneOperation("Demo-doThis", "/v1/api/dothis", 1);
        assertTrue(RouteMappingMatcher.matches(
                mappingByPath("/v1/api/dothis", null), 0, clone, "/api/dothis", HttpMethod.GET));
    }

    @Test
    void pathMatchRespectsConfiguredMethod() {
        assertFalse(RouteMappingMatcher.matches(
                mappingByPath("/v1/api/dothis", "POST"), 0, originalOperation(), "/v1/api/dothis", HttpMethod.GET));
        assertTrue(RouteMappingMatcher.matches(
                mappingByPath("/v1/api/dothis", "GET"), 0, originalOperation(), "/v1/api/dothis", HttpMethod.GET));
    }

    @Test
    void unrelatedUntaggedOperationNeverThrows() {
        Operation untagged = new Operation();
        assertFalse(RouteMappingMatcher.matches(
                mappingByRouteId("Demo-doThis"), 0, untagged, "/v1/api/dothis", HttpMethod.GET));
    }
}
