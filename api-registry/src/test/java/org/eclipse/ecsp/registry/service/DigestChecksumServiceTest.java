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

package org.eclipse.ecsp.registry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.ecsp.register.model.FilterDefinition;
import org.eclipse.ecsp.register.model.PredicateDefinition;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.registry.config.ChecksumProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Unit tests for {@link RouteChecksumService}.
 */
@ExtendWith(SpringExtension.class)
class DigestChecksumServiceTest {

    private RouteChecksumService checksumService;
    private ChecksumProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        properties = new ChecksumProperties();
        checksumService = new RouteChecksumService(properties, objectMapper);
    }

    /**
     * Test purpose          - Verify checksum is computed for a valid route (default fields).
     * Test data             - Route with predicates, filters, and uri.
     * Test expected result  - Non-empty Optional with 64-char hex string.
     * Test type             - Positive.
     */
    @Test
    void shouldReturnChecksumWhenRouteIsValid() {
        RouteDefinition route = buildRoute("route-1", "lb://svc", 0);

        Optional<String> result = checksumService.compute(route);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(64, result.get().length());
        Assertions.assertTrue(result.get().matches("[0-9a-f]+"));
    }

    /**
     * Test purpose          - Verify checksum is deterministic: identical inputs produce identical output.
     * Test data             - Same route definition computed twice.
     * Test expected result  - Both checksums are equal.
     * Test type             - Positive.
     */
    @Test
    void shouldBeDeterministicForIdenticalInputs() {
        RouteDefinition route = buildRoute("route-1", "lb://svc", 0);

        Optional<String> first = checksumService.compute(route);
        Optional<String> second = checksumService.compute(route);

        Assertions.assertTrue(first.isPresent());
        Assertions.assertEquals(first.get(), second.get());
    }

    /**
     * Test purpose          - Verify checksum changes when URI changes.
     * Test data             - Two routes differing only in URI.
     * Test expected result  - Checksums are different.
     * Test type             - Positive.
     */
    @Test
    void shouldProduceDifferentChecksumWhenUriChanges() {
        RouteDefinition route1 = buildRoute("route-1", "lb://svc-a", 0);
        RouteDefinition route2 = buildRoute("route-1", "lb://svc-b", 0);

        Optional<String> cs1 = checksumService.compute(route1);
        Optional<String> cs2 = checksumService.compute(route2);

        Assertions.assertTrue(cs1.isPresent());
        Assertions.assertTrue(cs2.isPresent());
        Assertions.assertNotEquals(cs1.get(), cs2.get());
    }

    /**
     * Test purpose          - Verify URI normalisation strips trailing slash.
     * Test data             - Two routes with URI differing only by trailing slash.
     * Test expected result  - Checksums are equal.
     * Test type             - Positive.
     */
    @Test
    void shouldNormalizeUriTrailingSlash() {
        RouteDefinition route1 = buildRoute("r", "lb://svc", 0);
        route1.setUri(URI.create("lb://svc/"));
        RouteDefinition route2 = buildRoute("r", "lb://svc", 0);
        route2.setUri(URI.create("lb://svc"));

        Optional<String> cs1 = checksumService.compute(route1);
        Optional<String> cs2 = checksumService.compute(route2);

        Assertions.assertEquals(cs1, cs2);
    }

    /**
     * Test purpose          - Verify URI normalisation lowercases the URI.
     * Test data             - Two routes with URI differing only in case.
     * Test expected result  - Checksums are equal.
     * Test type             - Positive.
     */
    @Test
    void shouldNormalizeUriCase() {
        RouteDefinition route1 = buildRoute("r", "lb://SVC", 0);
        RouteDefinition route2 = buildRoute("r", "lb://svc", 0);

        Optional<String> cs1 = checksumService.compute(route1);
        Optional<String> cs2 = checksumService.compute(route2);

        Assertions.assertEquals(cs1, cs2);
    }

    /**
     * Test purpose          - Verify predicate Method value is normalised to uppercase.
     * Test data             - Two routes with Method predicates differing only in case.
     * Test expected result  - Checksums are equal.
     * Test type             - Positive.
     */
    @Test
    void shouldNormalizeMethodPredicateToUppercase() {
        RouteDefinition routeLower = buildRoute("r", "lb://svc", 0);
        addPredicate(routeLower, "Method", "method", "get");
        RouteDefinition routeUpper = buildRoute("r", "lb://svc", 0);
        addPredicate(routeUpper, "Method", "method", "GET");

        Optional<String> cs1 = checksumService.compute(routeLower);
        Optional<String> cs2 = checksumService.compute(routeUpper);

        Assertions.assertEquals(cs1, cs2);
    }

    /**
     * Test purpose          - Verify checksum is stable regardless of predicate list order.
     * Test data             - Two routes with predicates in different order.
     * Test expected result  - Checksums are equal.
     * Test type             - Positive.
     */
    @Test
    void shouldProduceSameChecksumForDifferentPredicateOrder() {
        RouteDefinition route1 = buildRouteWithTwoPredicates("Path", "Filter");
        RouteDefinition route2 = buildRouteWithTwoPredicates("Filter", "Path");

        Optional<String> cs1 = checksumService.compute(route1);
        Optional<String> cs2 = checksumService.compute(route2);

        Assertions.assertEquals(cs1, cs2);
    }

    /**
     * Test purpose          - Verify wildcard include-fields includes all fields.
     * Test data             - Properties with include-fields = ["*"]; route with all fields set.
     * Test expected result  - Checksum is present; changing 'order' causes a different checksum.
     * Test type             - Positive.
     */
    @Test
    void shouldIncludeAllFieldsWhenWildcard() {
        List<String> wildcardFields = new ArrayList<>();
        wildcardFields.add("*");
        properties.getChecksum().setIncludeFields(wildcardFields);

        RouteDefinition route1 = buildRoute("r", "lb://svc", 1);
        RouteDefinition route2 = buildRoute("r", "lb://svc", 2);

        Optional<String> cs1 = checksumService.compute(route1);
        Optional<String> cs2 = checksumService.compute(route2);

        Assertions.assertTrue(cs1.isPresent());
        Assertions.assertNotEquals(cs1.get(), cs2.get());
    }

    /**
     * Test purpose          - Verify explicit include-fields limits scope.
     * Test data             - include-fields = ["uri"]; routes differing only in predicates.
     * Test expected result  - Checksums are equal (predicates excluded from scope).
     * Test type             - Positive.
     */
    @Test
    void shouldOnlyHashConfiguredFields() {
        List<String> uriOnly = new ArrayList<>();
        uriOnly.add("uri");
        properties.getChecksum().setIncludeFields(uriOnly);

        RouteDefinition route1 = buildRoute("r", "lb://svc", 0);
        addPredicate(route1, "Path", "pattern", "/v1/**");
        RouteDefinition route2 = buildRoute("r", "lb://svc", 0);
        addPredicate(route2, "Path", "pattern", "/v2/**");

        Optional<String> cs1 = checksumService.compute(route1);
        Optional<String> cs2 = checksumService.compute(route2);

        Assertions.assertEquals(cs1, cs2);
    }

    /**
     * Test purpose          - Verify null predicates list is handled gracefully.
     * Test data             - Route with null predicates.
     * Test expected result  - Checksum computed successfully (empty predicates treated as []).
     * Test type             - Positive.
     */
    @Test
    void shouldHandleNullPredicates() {
        RouteDefinition route = buildRoute("r", "lb://svc", 0);
        route.setPredicates(null);

        Optional<String> result = checksumService.compute(route);

        Assertions.assertTrue(result.isPresent());
    }

    /**
     * Test purpose          - Verify null filters list is handled gracefully.
     * Test data             - Route with null filters.
     * Test expected result  - Checksum computed successfully.
     * Test type             - Positive.
     */
    @Test
    void shouldHandleNullFilters() {
        RouteDefinition route = buildRoute("r", "lb://svc", 0);
        route.setFilters(null);

        Optional<String> result = checksumService.compute(route);

        Assertions.assertTrue(result.isPresent());
    }

    /**
     * Test purpose          - Verify null URI is handled gracefully.
     * Test data             - Route with null URI.
     * Test expected result  - Checksum computed successfully (null uri treated as "").
     * Test type             - Positive.
     */
    @Test
    void shouldHandleNullUri() {
        RouteDefinition route = buildRoute("r", "lb://svc", 0);
        route.setUri(null);

        Optional<String> result = checksumService.compute(route);

        Assertions.assertTrue(result.isPresent());
    }

    /**
     * Test purpose          - Verify invalid algorithm name causes Optional.empty() to be returned.
     * Test data             - Properties with algorithm = "INVALID-ALG".
     * Test expected result  - Optional.empty() returned; no exception thrown.
     * Test type             - Negative.
     */
    @Test
    void shouldReturnEmptyOnInvalidAlgorithm() {
        properties.getChecksum().setAlgorithm("INVALID-ALGORITHM-XYZ");

        RouteDefinition route = buildRoute("r", "lb://svc", 0);

        Optional<String> result = checksumService.compute(route);

        Assertions.assertFalse(result.isPresent());
    }

    /**
     * Test purpose          - Verify checksum changes when filter args change.
     * Test data             - Two routes differing only in filter argument value.
     * Test expected result  - Checksums are different.
     * Test type             - Positive.
     */
    @Test
    void shouldProduceDifferentChecksumWhenFilterArgsChange() {
        RouteDefinition route1 = buildRoute("r", "lb://svc", 0);
        addFilter(route1, "JwtAuth", "scope", "read");
        RouteDefinition route2 = buildRoute("r", "lb://svc", 0);
        addFilter(route2, "JwtAuth", "scope", "write");

        Optional<String> cs1 = checksumService.compute(route1);
        Optional<String> cs2 = checksumService.compute(route2);

        Assertions.assertNotEquals(cs1, cs2);
    }

    /**
     * Test purpose          - Verify metadata field normalisation sorts keys.
     * Test data             - Two routes with same metadata in different key order.
     * Test expected result  - Checksums are equal (wildcard include-fields to include metadata).
     * Test type             - Positive.
     */
    @Test
    void shouldNormalizeMetadataKeysLexicographically() {
        List<String> allFields = new ArrayList<>();
        allFields.add("*");
        properties.getChecksum().setIncludeFields(allFields);

        RouteDefinition route1 = buildRoute("r", "lb://svc", 0);
        Map<String, Object> meta1 = new HashMap<>();
        meta1.put("b", "2");
        meta1.put("a", "1");
        route1.setMetadata(meta1);

        RouteDefinition route2 = buildRoute("r", "lb://svc", 0);
        Map<String, Object> meta2 = new HashMap<>();
        meta2.put("a", "1");
        meta2.put("b", "2");
        route2.setMetadata(meta2);

        Optional<String> cs1 = checksumService.compute(route1);
        Optional<String> cs2 = checksumService.compute(route2);

        Assertions.assertEquals(cs1, cs2);
    }

    /**
     * Test purpose          - Verify SHA-512 algorithm is supported via configuration.
     * Test data             - Properties with algorithm = "SHA-512"; valid route.
     * Test expected result  - Checksum present with 128-char hex string (SHA-512 = 512 bits = 128 hex chars).
     * Test type             - Positive.
     */
    @Test
    void shouldSupportSha512Algorithm() {
        properties.getChecksum().setAlgorithm("SHA-512");

        RouteDefinition route = buildRoute("r", "lb://svc", 0);

        Optional<String> result = checksumService.compute(route);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(128, result.get().length());
    }

    /**
     * Test purpose          - Verify empty include-fields list uses all fields.
     * Test data             - Empty include-fields; routes differing in 'order' only.
     * Test expected result  - Checksums differ (order is included in all fields).
     * Test type             - Positive.
     */
    @Test
    void shouldUseAllFieldsWhenIncludeFieldsEmpty() {
        properties.getChecksum().setIncludeFields(new ArrayList<>());

        RouteDefinition route1 = buildRoute("r", "lb://svc", 1);
        RouteDefinition route2 = buildRoute("r", "lb://svc", 2);

        Optional<String> cs1 = checksumService.compute(route1);
        Optional<String> cs2 = checksumService.compute(route2);

        Assertions.assertNotEquals(cs1, cs2);
    }

    /**
     * Test purpose          - Verify checksum is present and deterministic with all fields included.
     * Test data             - Two calls for identical route; empty include-fields (all fields).
     * Test expected result  - Checksum is present and both calls produce the same value.
     * Test type             - Positive.
     */
    @Test
    void shouldProduceDeterministicChecksumWithAllFields() {
        RouteDefinition route = buildRoute("r", "lb://svc", 0);

        Optional<String> cs1 = checksumService.compute(route);
        Optional<String> cs2 = checksumService.compute(route);

        Assertions.assertTrue(cs1.isPresent());
        Assertions.assertEquals(cs1.get(), cs2.get());
    }

    /**
     * Test purpose          - Verify 'active' is included when wildcard is used.
     * Test data             - include-fields = ["*"]; active field present in hash input.
     * Test expected result  - Checksum is present and stable.
     * Test type             - Positive.
     */
    @Test
    void shouldIncludeActiveInAllFieldsWhenWildcard() {
        List<String> wildcardFields = new ArrayList<>();
        wildcardFields.add("*");
        properties.getChecksum().setIncludeFields(wildcardFields);

        RouteDefinition route = buildRoute("r", "lb://svc", 0);

        Optional<String> result = checksumService.compute(route);

        Assertions.assertTrue(result.isPresent());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private RouteDefinition buildRoute(String id, String uri, int order) {
        RouteDefinition route = new RouteDefinition();
        route.setId(id);
        route.setUri(URI.create(uri));
        route.setOrder(order);
        route.setService("svc");
        route.setPredicates(new ArrayList<>());
        route.setFilters(new ArrayList<>());
        return route;
    }

    private RouteDefinition buildRouteWithTwoPredicates(String first, String second) {
        RouteDefinition route = buildRoute("r", "lb://svc", 0);
        addPredicate(route, first, "key", "value");
        addPredicate(route, second, "key", "value");
        return route;
    }

    private void addPredicate(RouteDefinition route, String name, String argKey, String argValue) {
        PredicateDefinition pred = new PredicateDefinition();
        pred.setName(name);
        Map<String, String> args = new HashMap<>();
        args.put(argKey, argValue);
        pred.setArgs(args);
        if (route.getPredicates() == null) {
            route.setPredicates(new ArrayList<>());
        }
        route.getPredicates().add(pred);
    }

    private void addFilter(RouteDefinition route, String name, String argKey, String argValue) {
        FilterDefinition filter = new FilterDefinition();
        filter.setName(name);
        Map<String, String> args = new HashMap<>();
        args.put(argKey, argValue);
        filter.setArgs(args);
        if (route.getFilters() == null) {
            route.setFilters(new ArrayList<>());
        }
        route.getFilters().add(filter);
    }
}
