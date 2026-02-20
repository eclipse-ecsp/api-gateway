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

package org.eclipse.ecsp.gateway.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for extracting service and route components from request paths.
 *
 * <p>Request paths follow the pattern: /service-name/route-path
 * - Service: First path segment after leading slash
 * - Route: Remainder of the path
 *
 * <p>Examples:
 * - /user-service/get-profile → service="user-service", route="get-profile"
 * - /vehicle-service/api/v1/vehicles → service="vehicle-service", route="api/v1/vehicles"
 * - /healthz → service="healthz", route="" (empty)
 */
@Service
@Slf4j
public class PathExtractor {
    /**
     * Default constructor.
     */
    public PathExtractor() {
        // Default constructor
    }

    private static final int NOT_FOUND = -1;

    /**
     * Extract service name from request path.
     *
     * @param path Request path (e.g., "/user-service/get-profile")
     * @return Service name (e.g., "user-service"), or path itself if no slash found
     */
    public String extractService(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }

        // Remove leading slash
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;

        // Find first slash to separate service from route
        int slashIndex = normalizedPath.indexOf('/');
        if (slashIndex == NOT_FOUND) {
            // No route part, entire path is service
            return normalizedPath;
        }

        return normalizedPath.substring(0, slashIndex);
    }

    /**
     * Extract route path from request path.
     *
     * @param path Request path (e.g., "/user-service/get-profile")
     * @return Route path (e.g., "get-profile"), or empty string if no route part
     */
    public String extractRoute(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }

        // Remove leading slash
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;

        // Find first slash to separate service from route
        int slashIndex = normalizedPath.indexOf('/');
        if (slashIndex == NOT_FOUND) {
            // No route part
            return "";
        }

        // Return everything after the first slash
        return normalizedPath.substring(slashIndex + 1);
    }
}
