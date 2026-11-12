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
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.util.Map;

/**
 * Decides which routes a mapping's configured {@code filters} apply to.
 *
 * <ul>
 *   <li>A mapping with {@code exposeAs} applies its filters only to the aliases it produced
 *       (identified by the {@code x-route-mapping-index} extension), so a filter can target one
 *       alias without touching the original or aliases produced by other mappings.</li>
 *   <li>A filters-only mapping applies to the matched operation and to every alias of it (via
 *       {@code x-route-alias-of} / {@code x-route-original-path}), so a route-wide filter can never
 *       be bypassed through an alias.</li>
 * </ul>
 */
public final class RouteMappingMatcher {

    private static final int NOT_AN_ALIAS = -1;

    private RouteMappingMatcher() {
    }

    /**
     * Determines whether the given mapping's filters apply to the given operation (original or
     * fan-out alias).
     *
     * @param mapping      the configured mapping
     * @param mappingIndex the mapping's index in {@code mappings}
     * @param operation    the candidate operation
     * @param path         the OpenAPI path key the operation is currently registered at
     * @param method       the resolved HTTP method for the operation
     * @return {@code true} if the mapping's filters apply to this operation
     */
    public static boolean matches(RouteCustomizerProperties.RouteMapping mapping, int mappingIndex,
                                  Operation operation, String path, HttpMethod method) {
        if (!CollectionUtils.isEmpty(mapping.getExposeAs())) {
            return producingMappingIndex(operation) == mappingIndex;
        }
        if (StringUtils.hasText(mapping.getRouteId())) {
            return matchesByRouteId(mapping.getRouteId(), operation);
        }
        if (!StringUtils.hasText(mapping.getPath())) {
            return false;
        }
        String effectivePath = originalPathOf(operation, path);
        if (!mapping.getPath().equals(effectivePath)) {
            return false;
        }
        return !StringUtils.hasText(mapping.getMethod()) || mapping.getMethod().equalsIgnoreCase(method.name());
    }

    private static boolean matchesByRouteId(String routeId, Operation operation) {
        if (routeId.equals(RouteIdUtils.computeRouteId(operation))) {
            return true;
        }
        Object aliasOf = extension(operation, RouteCustomizerOpenApiCustomizer.ALIAS_OF_EXTENSION);
        return routeId.equals(aliasOf);
    }

    private static String originalPathOf(Operation operation, String path) {
        Object originalPath = extension(operation, RouteCustomizerOpenApiCustomizer.ORIGINAL_PATH_EXTENSION);
        return originalPath instanceof String original ? original : path;
    }

    private static int producingMappingIndex(Operation operation) {
        Object index = extension(operation, RouteCustomizerOpenApiCustomizer.MAPPING_INDEX_EXTENSION);
        return index instanceof Number number ? number.intValue() : NOT_AN_ALIAS;
    }

    private static Object extension(Operation operation, String key) {
        Map<String, Object> extensions = operation.getExtensions();
        return extensions == null ? null : extensions.get(key);
    }
}
