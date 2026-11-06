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

package org.eclipse.ecsp.gateway.service;

import org.eclipse.ecsp.gateway.config.RouteOrderOverrideProperties;
import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies the configured route order overrides to the routes received from the api-registry.
 *
 * <p>The bean is available only when {@code api.gateway.routes.order-override.enabled} is true.
 */
@Component
@ConditionalOnProperty(prefix = GatewayConstants.ROUTE_ORDER_OVERRIDE_PREFIX, name = "enabled",
        havingValue = "true")
public class RouteOrderResolver {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RouteOrderResolver.class);

    private final Map<String, Integer> orderByRouteId = new LinkedHashMap<>();

    /**
     * Creates the resolver from the configured mappings.
     *
     * @param properties the route order override properties
     */
    public RouteOrderResolver(RouteOrderOverrideProperties properties) {
        properties.getMappings().forEach(mapping -> orderByRouteId.put(mapping.getRouteId(), mapping.getOrder()));
        LOGGER.info("order override mapping: {}", orderByRouteId);
    }

    /**
     * Replaces the order of every route that has a configured mapping.
     *
     * @param routes the routes to update
     * @return the same list of routes
     */
    public List<IgniteRouteDefinition> applyOverrides(List<IgniteRouteDefinition> routes) {
        Set<String> unmatchedRouteIds = new HashSet<>(orderByRouteId.keySet());
        for (IgniteRouteDefinition route : routes) {
            Integer overrideOrder = orderByRouteId.get(route.getId());
            if (overrideOrder == null) {
                continue;
            }
            unmatchedRouteIds.remove(route.getId());
            applyOverride(route, overrideOrder);
        }
        unmatchedRouteIds.forEach(routeId ->
                LOGGER.warn("Route order override for route-id: {} does not match any route", routeId));
        return routes;
    }

    private void applyOverride(IgniteRouteDefinition route, int overrideOrder) {
        int registryOrder = route.getOrder();
        if (registryOrder != 0 && registryOrder != overrideOrder) {
            LOGGER.warn("Route order override replaces the order {} received from api-registry with {}"
                    + " for route-id: {}", registryOrder, overrideOrder, route.getId());
        }
        LOGGER.debug("Route order for route-id: {} set to {}", route.getId(), overrideOrder);
        route.setOrder(overrideOrder);
    }
}
