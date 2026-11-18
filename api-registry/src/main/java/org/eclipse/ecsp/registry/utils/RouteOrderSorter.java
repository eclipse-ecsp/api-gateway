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

package org.eclipse.ecsp.registry.utils;

import org.eclipse.ecsp.register.conflict.RouteKey;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Puts routes in the order in which api-gateway must evaluate them.
 *
 * <p>Spring Cloud Gateway serves a request with the first route that matches. Of two routes that can match the
 * same request, the more specific one therefore has to come first:
 * <ul>
 *   <li>a more specific path before a broader one: {@code /v1/entitlements/search} before
 *       {@code /v1/entitlements/{productId}}, and a parameterized path before a catch-all;</li>
 *   <li>on the same path, a route for one HTTP method before a route for every method, so that the route for every
 *       method does not serve, and does not skip the filters of, a method that has a route of its own.</li>
 * </ul>
 *
 * <p>Routes are ranked by Spring's own path-pattern specificity, then by whether they serve one method, then by
 * route id. The route id makes the order total, so it is the same on every registry replica and does not depend on
 * how the routes were read. A route without a usable path is ranked after all others.
 */
public final class RouteOrderSorter {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RouteOrderSorter.class);
    private static final String REASON_MISSING_PATH = "missing_path";
    private static final String REASON_INVALID_PATTERN = "invalid_pattern";

    private RouteOrderSorter() {
    }

    /**
     * Returns the routes in route order: by path specificity, then routes for one method before routes for every
     * method, then by route id.
     *
     * <p>The given list is not modified. A route whose {@code Path} predicate is missing or is not a valid path
     * pattern is logged and ranked last, so one malformed route never prevents the others from being served.
     *
     * @param routes the routes to order; must not be {@code null}
     * @return a new list with the routes in route order
     */
    public static List<RouteDefinition> sort(List<RouteDefinition> routes) {
        Map<RouteDefinition, PathPattern> patterns = new IdentityHashMap<>();
        Set<RouteDefinition> everyMethod = Collections.newSetFromMap(new IdentityHashMap<>());
        for (RouteDefinition route : routes) {
            PathPattern pattern = parsePath(route);
            if (pattern != null) {
                patterns.put(route, pattern);
            }
            if (!StringUtils.hasText(RouteKey.methodOf(route))) {
                everyMethod.add(route);
            }
        }
        Comparator<RouteDefinition> bySpecificity = Comparator.comparing(patterns::get,
                Comparator.nullsLast(PathPattern.SPECIFICITY_COMPARATOR));
        Comparator<RouteDefinition> byMethod = Comparator.comparing(everyMethod::contains);
        Comparator<RouteDefinition> byId = Comparator.comparing(RouteDefinition::getId,
                Comparator.nullsLast(Comparator.naturalOrder()));
        List<RouteDefinition> sorted = new ArrayList<>(routes);
        sorted.sort(bySpecificity.thenComparing(byMethod).thenComparing(byId));
        return sorted;
    }

    private static PathPattern parsePath(RouteDefinition route) {
        String path = RouteKey.pathOf(route);
        if (!StringUtils.hasText(path)) {
            warnUnparseable(route, REASON_MISSING_PATH, path);
            return null;
        }
        try {
            return PathPatternParser.defaultInstance.parse(path);
        } catch (PatternParseException ex) {
            warnUnparseable(route, REASON_INVALID_PATTERN, path);
            return null;
        }
    }

    private static void warnUnparseable(RouteDefinition route, String reason, String path) {
        LOGGER.warn("{} | routeId={} | reason={} | path={}",
                RegistryConstants.LOG_EVENT_ROUTE_ORDER_PATH_UNPARSEABLE, route.getId(), reason, path);
    }
}
