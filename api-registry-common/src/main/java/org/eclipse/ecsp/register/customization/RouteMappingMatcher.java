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
import org.springframework.util.StringUtils;
import java.util.Map;

/**
 * Shared matching rule between the original (never-cloned) operation and its fan-out clones.
 *
 * <p>A naive match on {@code routeId}/{@code path} alone only ever matches the original
 * operation, because a clone deliberately carries a different {@code operationId}/route id and
 * lives at a different path key. This matcher additionally consults the
 * {@code x-route-alias-of} / {@code x-route-original-path} traceability extensions so that a
 * mapping combining {@code exposeAs} and {@code filters} applies filters to every route the
 * mapping produced, not just the original.
 */
public final class RouteMappingMatcher {

    private RouteMappingMatcher() {
    }

    /**
     * Determines whether the given operation (original or fan-out clone) is targeted by the
     * given mapping.
     *
     * @param mapping   the configured mapping
     * @param operation the candidate operation
     * @param path      the OpenAPI path key the operation is currently registered at
     * @param method    the resolved HTTP method for the operation
     * @return {@code true} if the mapping targets this operation
     */
    public static boolean matches(RouteCustomizerProperties.RouteMapping mapping,
                                   Operation operation, String path, HttpMethod method) {
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

    private static Object extension(Operation operation, String key) {
        Map<String, Object> extensions = operation.getExtensions();
        return extensions == null ? null : extensions.get(key);
    }
}
