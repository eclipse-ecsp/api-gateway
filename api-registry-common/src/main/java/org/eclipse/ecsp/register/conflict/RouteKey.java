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

package org.eclipse.ecsp.register.conflict;

import org.eclipse.ecsp.register.model.PredicateDefinition;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.utils.RegistryCommonConstants;
import org.springframework.util.StringUtils;
import java.util.Locale;
import java.util.Optional;

/**
 * The part of a route that decides which requests it serves: its HTTP method and its normalized path pattern.
 *
 * <p>Two routes with the same key serve exactly the same requests, so api-gateway can only ever use one of them.
 * Registering a second route under an existing key is a conflict, unless it is the same API registered again by
 * another replica of the service that owns it.
 *
 * <p>The path and the method are read the way api-gateway reads them: the value of the last argument of the
 * predicate named {@code Path} or {@code Method}. A route without a {@code Method} predicate serves every method
 * and has no method in its key.
 *
 * <p>The path is normalized so that spellings that match the same requests compare equal:
 * <ul>
 *   <li>the name of a path variable never matters: {@code /vehicle/{vehicleId}}, {@code /vehicle/{vin}} and
 *       {@code /vehicle/{id}} all become {@code /vehicle/{param}}. A variable with a constraining regular
 *       expression keeps the expression: {@code {id:[0-9]+}} becomes {@code {param:[0-9]+}};</li>
 *   <li>{@code {*rest}} captures the rest of the path like {@code **} does, so both become {@code **}.</li>
 * </ul>
 * Nothing else is normalized, because nothing else makes two patterns match the same requests. A literal segment
 * and a variable at the same position stay different ({@code /vehicle/status} is not {@code /vehicle/{param}}):
 * api-gateway tells them apart by route precedence. The case of the path matters, and so does a trailing slash,
 * because Spring Framework 7 does not match {@code /vehicle} against {@code /vehicle/}.
 *
 * @param method the upper-case HTTP method, or {@code null} when the route serves every method
 * @param path   the normalized path pattern
 */
public record RouteKey(String method, String path) {

    private static final String CATCH_ALL = "**";
    private static final String PARAMETER = "param";
    private static final int NOT_FOUND = -1;

    /**
     * Returns the key of a route.
     *
     * @param route the route; may be {@code null}
     * @return the key, or empty when the route has no usable {@code Path} predicate
     */
    public static Optional<RouteKey> of(RouteDefinition route) {
        String path = pathOf(route);
        if (!StringUtils.hasText(path)) {
            return Optional.empty();
        }
        String method = methodOf(route);
        return Optional.of(new RouteKey(
                StringUtils.hasText(method) ? method.trim().toUpperCase(Locale.ROOT) : null, normalize(path)));
    }

    /**
     * Returns the path pattern of a route as api-gateway reads it.
     *
     * @param route the route; may be {@code null}
     * @return the last argument of the last {@code Path} predicate, or {@code null} when there is none
     */
    public static String pathOf(RouteDefinition route) {
        return lastArgument(route, RegistryCommonConstants.PATH);
    }

    /**
     * Returns the HTTP method of a route as api-gateway reads it.
     *
     * @param route the route; may be {@code null}
     * @return the last argument of the last {@code Method} predicate, or {@code null} when there is none
     */
    public static String methodOf(RouteDefinition route) {
        return lastArgument(route, RegistryCommonConstants.METHOD);
    }

    private static String lastArgument(RouteDefinition route, String predicateName) {
        String value = null;
        if (route != null && route.getPredicates() != null) {
            for (PredicateDefinition predicate : route.getPredicates()) {
                if (predicate != null && predicateName.equalsIgnoreCase(predicate.getName())
                        && predicate.getArgs() != null) {
                    for (String argument : predicate.getArgs().values()) {
                        value = argument;
                    }
                }
            }
        }
        return value;
    }

    private static String normalize(String path) {
        StringBuilder normalized = new StringBuilder(path.length());
        int index = 0;
        while (index < path.length()) {
            char current = path.charAt(index);
            int close = current == '{' ? closingBrace(path, index) : NOT_FOUND;
            if (close == NOT_FOUND) {
                normalized.append(current);
                index++;
            } else {
                normalized.append(variable(path.substring(index + 1, close)));
                index = close + 1;
            }
        }
        return normalized.toString();
    }

    /**
     * Finds the brace that closes the one at {@code open}; braces nested in a regular expression are skipped.
     */
    private static int closingBrace(String path, int open) {
        int depth = 0;
        for (int index = open; index < path.length(); index++) {
            char current = path.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return index;
            }
        }
        return NOT_FOUND;
    }

    private static String variable(String declaration) {
        if (declaration.startsWith("*")) {
            return CATCH_ALL;
        }
        int regexStart = declaration.indexOf(':');
        return "{" + PARAMETER + (regexStart < 0 ? "" : declaration.substring(regexStart)) + "}";
    }
}
