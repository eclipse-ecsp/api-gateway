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

package org.eclipse.ecsp.gateway.customizers;

import org.eclipse.ecsp.gateway.config.ClientAccessControlProperties;
import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test purpose    - Verify ClientAccessControlCustomizer route customization logic.
 * Test data       - Various route definitions and skip path patterns.
 * Test expected   - Correct filter addition/skip behavior.
 * Test type       - Positive and Negative.
 */
@ExtendWith(MockitoExtension.class)
class ClientAccessControlCustomizerTest {

    @Mock(strictness = Mock.Strictness.LENIENT)
    private ClientAccessControlProperties properties;

    private ClientAccessControlCustomizer customizer;
    private RouteDefinition routeDefinition;
    private IgniteRouteDefinition igniteRouteDefinition;

    @BeforeEach
    void setUp() {
        customizer = new ClientAccessControlCustomizer(properties);
        routeDefinition = new RouteDefinition();
        routeDefinition.setFilters(new ArrayList<>());
        igniteRouteDefinition = new IgniteRouteDefinition();
        igniteRouteDefinition.setId("test-route");
        igniteRouteDefinition.setService("test-service");
        igniteRouteDefinition.setApiDocs(false);
        FilterDefinition filterDefinition = new FilterDefinition();
        filterDefinition.setName("JwtAuthValidator");
        Map<String, String> args = new HashMap<>();
        args.put("scopes", "SelfManage");
        filterDefinition.setArgs(args);
        igniteRouteDefinition.getFilters().add(filterDefinition);
        when(properties.getSkipPaths()).thenReturn(List.of());
    }

    /**
     * Test purpose          - Verify filter is added for standard route.
     * Test data             - Route with path that doesn't match skip patterns.
     * Test expected result  - Filter is added to route definition.
     * Test type             - Positive.
     */
    @Test
    void customizeStandardRouteAddsFilter() {
        // GIVEN: Route with path that should not be skipped
        setupRouteWithPath("/api/users");

        // WHEN: Route is customized
        RouteDefinition result = customizer.customize(routeDefinition, igniteRouteDefinition);

        // THEN: Filter should be added
        assertEquals(1, result.getFilters().size());
        FilterDefinition filter = result.getFilters().get(0);
        assertTrue(filter.getName().contains("ClientAccessControl"));
        assertEquals("test-service", filter.getArgs().get("serviceName"));
        assertEquals("test-route", filter.getArgs().get("routeId"));
    }

    /**
     * Test purpose          - Verify filter is not added when path matches skip pattern.
     * Test data             - Route path matching skip pattern.
     * Test expected result  - Filter is not added.
     * Test type             - Positive.
     */
    @Test
    void customizePathMatchesSkipPatternSkipsFilter() {
        // GIVEN: Route with path matching skip pattern
        setupRouteWithPath("/actuator/health");
        when(properties.getSkipPaths()).thenReturn(List.of("/actuator/**", "/health/**"));

        // WHEN: Route is customized
        RouteDefinition result = customizer.customize(routeDefinition, igniteRouteDefinition);

        // THEN: Filter should not be added
        assertTrue(result.getFilters().isEmpty());
    }

    /**
     * Test purpose          - Verify filter is not added for API docs routes.
     * Test data             - Route with apiDocs flag set to true.
     * Test expected result  - Filter is not added.
     * Test type             - Positive.
     */
    @Test
    void customizeApiDocsRouteSkipsFilter() {
        // GIVEN: Route marked as API docs
        IgniteRouteDefinition apiDocsDefinition = new IgniteRouteDefinition();
        apiDocsDefinition.setId("api-docs-route");
        apiDocsDefinition.setService("api-docs-service");
        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        Map<String, String> args = new HashMap<>();
        args.put("arg_0", "/v3/api-docs/**");
        pathPredicate.setArgs(args);
        apiDocsDefinition.setPredicates(List.of(pathPredicate));
        apiDocsDefinition.setApiDocs(true);

        // WHEN: Route is customized
        RouteDefinition result = customizer.customize(routeDefinition, apiDocsDefinition);

        // THEN: Filter should not be added
        assertTrue(result.getFilters().isEmpty());
    }

    /**
     * Test purpose          - Verify filter handles wildcard skip patterns.
     * Test data             - Route path with wildcard pattern in skip paths.
     * Test expected result  - Filter is not added for matching wildcards.
     * Test type             - Positive.
     */
    @Test
    void customizeWildcardSkipPatternSkipsMatchingPaths() {
        // GIVEN: Route with path matching wildcard pattern
        setupRouteWithPath("/public/images/logo.png");
        when(properties.getSkipPaths()).thenReturn(List.of("/public/**"));

        // WHEN: Route is customized
        RouteDefinition result = customizer.customize(routeDefinition, igniteRouteDefinition);

        // THEN: Filter should not be added
        assertTrue(result.getFilters().isEmpty());
    }

    /**
     * Test purpose          - Verify filter is added when path doesn't match any skip patterns.
     * Test data             - Multiple skip patterns with non-matching route path.
     * Test expected result  - Filter is added.
     * Test type             - Negative.
     */
    @Test
    void customizePathDoesNotMatchSkipPatternsAddsFilter() {
        // GIVEN: Route with path not matching any skip patterns
        setupRouteWithPath("/api/private/users");
        when(properties.getSkipPaths()).thenReturn(List.of("/actuator/**", "/health", "/public/**"));

        // WHEN: Route is customized
        RouteDefinition result = customizer.customize(routeDefinition, igniteRouteDefinition);

        // THEN: Filter should be added
        assertFalse(result.getFilters().isEmpty());
        assertEquals(1, result.getFilters().size());
    }

    /**
     * Test purpose          - Verify filter handles empty route path.
     * Test data             - Route with no path predicate.
     * Test expected result  - Filter is added (empty path doesn't match skip patterns).
     * Test type             - Negative.
     */
    @Test
    void customizeEmptyRoutePathAddsFilter() {
        // GIVEN: Route with no path predicates
        igniteRouteDefinition.setPredicates(new ArrayList<>());
        when(properties.getSkipPaths()).thenReturn(List.of("/actuator/**"));

        // WHEN: Route is customized
        RouteDefinition result = customizer.customize(routeDefinition, igniteRouteDefinition);

        // THEN: Filter should be added (empty path doesn't match skip patterns)
        assertEquals(1, result.getFilters().size());
    }

    /**
     * Test purpose          - Verify filter handles exact path matching.
     * Test data             - Exact path in skip patterns.
     * Test expected result  - Filter is not added for exact match.
     * Test type             - Positive.
     */
    @Test
    void customizeExactPathMatchSkipsFilter() {
        // GIVEN: Route with exact path in skip list
        setupRouteWithPath("/health");
        when(properties.getSkipPaths()).thenReturn(List.of("/health", "/metrics"));

        // WHEN: Route is customized
        RouteDefinition result = customizer.customize(routeDefinition, igniteRouteDefinition);

        // THEN: Filter should not be added
        assertTrue(result.getFilters().isEmpty());
    }

    /**
     * Test purpose          - Verify filter preserves existing filters.
     * Test data             - Route definition with existing filters.
     * Test expected result  - New filter is added to existing list.
     * Test type             - Positive.
     */
    @Test
    void customizeRouteWithExistingFiltersPreservesAndAddsFilter() {
        // GIVEN: Route with existing filters
        setupRouteWithPath("/api/users");
        FilterDefinition existingFilter = new FilterDefinition();
        existingFilter.setName("ExistingFilter");
        routeDefinition.getFilters().add(existingFilter);

        // WHEN: Route is customized
        RouteDefinition result = customizer.customize(routeDefinition, igniteRouteDefinition);

        // THEN: Both filters should be present
        assertEquals(2, result.getFilters().size());
        assertEquals("ExistingFilter", result.getFilters().get(0).getName());
        assertTrue(result.getFilters().get(1).getName().contains("ClientAccessControl"));
    }

    private void setupRouteWithPath(String path) {
        final List<PredicateDefinition> predicates = new ArrayList<>();
        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        Map<String, String> args = new HashMap<>();
        args.put("pattern", path);
        pathPredicate.setArgs(args);
        predicates.add(pathPredicate);
        igniteRouteDefinition.setPredicates(predicates);
    }
}
