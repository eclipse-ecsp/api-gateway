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

package org.eclipse.ecsp.interceptors;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches the result of inspecting whether a controller method is annotated with
 * {@link SecurityRequirement}, eliminating repeated reflection calls on hot paths.
 *
 * <p>Routes are fixed at startup, so the cache never needs to be evicted.
 * {@link ConcurrentHashMap#computeIfAbsent} ensures the reflection call is performed
 * at most once per {@link HandlerMethod}, even under concurrent first-requests.
 *
 * <p>This class is not annotated with {@code @Component} — it is registered exclusively
 * by {@link org.eclipse.ecsp.config.TokenValidationConfiguration} to prevent
 * duplicate-bean conflicts if a consuming application scans the library packages.
 */
public class SecurityRequirementCache {

    private final ConcurrentHashMap<HandlerMethod, Boolean> cache = new ConcurrentHashMap<>();

    /**
     * Default constructor.
     */
    public SecurityRequirementCache() {
        // Default constructor
    }

    /**
     * Returns {@code true} if the handler method is annotated with {@link SecurityRequirement}.
     *
     * <p>The result is cached after the first call for each method, making subsequent
     * calls an O(1) map lookup.
     *
     * @param handlerMethod the resolved handler method
     * @return {@code true} if the endpoint requires JWT authentication; {@code false} if public
     */
    public boolean isSecured(HandlerMethod handlerMethod) {
        return cache.computeIfAbsent(handlerMethod,
            m -> m.getMethodAnnotation(SecurityRequirement.class) != null);
    }

    /**
     * Returns the scopes required by the handler method's {@link SecurityRequirement} annotation.
     *
     * @param handlerMethod the resolved handler method
     * @return a list of required scopes, or an empty list if none are specified
     */
    public List<String> getScopes(HandlerMethod handlerMethod) {
        SecurityRequirement securityRequirement = handlerMethod.getMethodAnnotation(SecurityRequirement.class);
        if (securityRequirement != null) {
            // Assuming only one security requirement is used, get the first one
            // split scopes by comma and trim whitespace, then filter out empty scopes
            return Arrays.stream(securityRequirement.scopes())
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        }
        return List.of();
    }
}
