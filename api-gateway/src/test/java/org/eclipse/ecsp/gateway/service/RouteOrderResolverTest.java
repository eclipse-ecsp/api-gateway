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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test class for {@link RouteOrderResolver}.
 */
class RouteOrderResolverTest {

    private static final String USERS_GET_USER = "Users-getUser";
    private static final String USERS_LIST_USERS = "Users-listUsers";

    @Test
    void applyOverridesSetsOrderOfMatchingRoutesOnly() {
        RouteOrderResolver resolver = resolver(mapping(USERS_GET_USER, -10), mapping(USERS_LIST_USERS, 100));
        IgniteRouteDefinition getUser = route(USERS_GET_USER, 0);
        IgniteRouteDefinition listUsers = route(USERS_LIST_USERS, 0);
        IgniteRouteDefinition other = route("Other-op", 7);

        resolver.applyOverrides(List.of(getUser, listUsers, other));

        assertEquals(-10, getUser.getOrder());
        assertEquals(100, listUsers.getOrder());
        assertEquals(7, other.getOrder());
    }

    @Test
    void applyOverridesReturnsTheSameList() {
        RouteOrderResolver resolver = resolver(mapping(USERS_GET_USER, 1));
        List<IgniteRouteDefinition> routes = new ArrayList<>(List.of(route(USERS_GET_USER, 0)));

        assertSame(routes, resolver.applyOverrides(routes));
    }

    @Test
    void applyOverridesReplacesNonDefaultRegistryOrder() {
        RouteOrderResolver resolver = resolver(mapping(USERS_GET_USER, 5));
        IgniteRouteDefinition route = route(USERS_GET_USER, 3);

        resolver.applyOverrides(List.of(route));

        assertEquals(5, route.getOrder());
    }

    @Test
    void applyOverridesIsIdempotentWhenOrderAlreadyOverridden() {
        RouteOrderResolver resolver = resolver(mapping(USERS_GET_USER, 5));
        IgniteRouteDefinition route = route(USERS_GET_USER, 0);

        resolver.applyOverrides(List.of(route));
        resolver.applyOverrides(List.of(route));

        assertEquals(5, route.getOrder());
    }

    @Test
    void applyOverridesToleratesMappingsWithoutMatchingRoute() {
        RouteOrderResolver resolver = resolver(mapping("Unknown-op", 5));
        IgniteRouteDefinition route = route(USERS_GET_USER, 2);

        resolver.applyOverrides(List.of(route));

        assertEquals(2, route.getOrder());
    }

    @Test
    void applyOverridesWithNoMappingsLeavesRoutesUnchanged() {
        RouteOrderResolver resolver = resolver();
        IgniteRouteDefinition route = route(USERS_GET_USER, 2);

        resolver.applyOverrides(List.of(route));

        assertEquals(2, route.getOrder());
    }

    @Test
    void applyOverridesIgnoresRoutesWithoutId() {
        RouteOrderResolver resolver = resolver(mapping(USERS_GET_USER, 5));
        IgniteRouteDefinition route = route(null, 2);

        resolver.applyOverrides(List.of(route));

        assertEquals(2, route.getOrder());
    }

    private static RouteOrderResolver resolver(RouteOrderOverrideProperties.Mapping... mappings) {
        RouteOrderOverrideProperties properties = new RouteOrderOverrideProperties();
        properties.setEnabled(true);
        properties.setMappings(List.of(mappings));
        return new RouteOrderResolver(properties);
    }

    private static RouteOrderOverrideProperties.Mapping mapping(String routeId, int order) {
        RouteOrderOverrideProperties.Mapping mapping = new RouteOrderOverrideProperties.Mapping();
        mapping.setRouteId(routeId);
        mapping.setOrder(order);
        return mapping;
    }

    private static IgniteRouteDefinition route(String routeId, int order) {
        IgniteRouteDefinition route = new IgniteRouteDefinition();
        route.setId(routeId);
        route.setOrder(order);
        return route;
    }
}
