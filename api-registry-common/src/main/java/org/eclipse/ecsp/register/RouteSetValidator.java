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

package org.eclipse.ecsp.register;

import org.eclipse.ecsp.register.model.PredicateDefinition;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.utils.RegistryCommonConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates a fully-assembled route set (auto-derived operations plus fully-static
 * {@code ApiRoutesConfig} routes, plus API-doc routes) for identifier and predicate collisions
 * before it is sent to api-registry.
 *
 * <p>{@code RouteCustomizerOpenApiCustomizer} can only see, and de-duplicate against, the
 * OpenAPI-derived routes it mutates &mdash; it has no visibility into fully-static routes. This
 * validator is the single point that sees the complete, final set.
 */
public final class RouteSetValidator {

    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{[^/}]+}");

    private RouteSetValidator() {
    }

    /**
     * Validates the given routes for duplicate route ids and duplicate
     * {@code (method, path)} pairs.
     *
     * @param routes the complete, final list of routes about to be registered
     * @throws IllegalStateException listing every conflict found, if any
     */
    public static void validate(List<RouteDefinition> routes) {
        List<String> conflicts = findConflicts(routes);
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Route set validation failed: " + conflicts);
        }
    }

    /**
     * Lists duplicate route ids and duplicate {@code (method, path shape)} pairs. The shape
     * ignores path-variable names: {@code /x/{id}} and {@code /x/{name}} overlap at the gateway.
     *
     * @param routes the complete, final list of routes about to be registered
     * @return one human-readable entry per conflict; empty if none
     */
    public static List<String> findConflicts(List<RouteDefinition> routes) {
        List<String> conflicts = new ArrayList<>(findDuplicateIds(routes));
        conflicts.addAll(findDuplicateMethodPaths(routes));
        return conflicts;
    }

    private static List<String> findDuplicateIds(List<RouteDefinition> routes) {
        Set<String> seen = new HashSet<>();
        List<String> conflicts = new ArrayList<>();
        for (RouteDefinition route : routes) {
            if (route.getId() != null && !seen.add(route.getId())) {
                conflicts.add("duplicate route id: " + route.getId());
            }
        }
        return conflicts;
    }

    private static List<String> findDuplicateMethodPaths(List<RouteDefinition> routes) {
        Map<String, String> seenKeyToId = new HashMap<>();
        List<String> conflicts = new ArrayList<>();
        for (RouteDefinition route : routes) {
            String key = methodPathKey(route);
            if (key == null) {
                continue;
            }
            String previousId = seenKeyToId.putIfAbsent(key, route.getId());
            if (previousId != null && !previousId.equals(route.getId())) {
                conflicts.add("duplicate method/path " + key + " between routes '"
                        + previousId + "' and '" + route.getId() + "'");
            }
        }
        return conflicts;
    }

    private static String methodPathKey(RouteDefinition route) {
        String method = null;
        String path = null;
        for (PredicateDefinition predicate : route.getPredicates()) {
            if (RegistryCommonConstants.METHOD.equals(predicate.getName())) {
                method = predicate.getArgs().get(RegistryCommonConstants.KEY_0);
            } else if (RegistryCommonConstants.PATH.equals(predicate.getName())) {
                path = predicate.getArgs().get(RegistryCommonConstants.KEY_0);
            }
        }
        return (method == null || path == null) ? null
                : method.toUpperCase(Locale.ROOT) + " " + PATH_VARIABLE.matcher(path).replaceAll("{}");
    }
}
