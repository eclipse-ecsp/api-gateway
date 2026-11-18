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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.ArrayList;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for RouteKey: which method and path spellings are the same route and which are not.
 */
class RouteKeyTest {

    /**
     * Test purpose          - Verify the name of a path variable never makes a route unique.
     * Test data             - Paths that differ only in the names of their variables.
     * Test expected result  - Both paths normalize to the same key.
     * Test type             - Positive.
     */
    @ParameterizedTest(name = "{0} is {1}")
    @CsvSource(delimiter = '|', value = {
        "/v1/vehicle/{vehicleId}                         | /v1/vehicle/{vin}",
        "/v1/vehicle/{id}                                | /v1/vehicle/{vehicleId}",
        "/v1/vehicle/{vehicleId}/owner/{ownerId}         | /v1/vehicle/{vin}/owner/{userId}",
        "/v1/vehicle/{vehicleId}/owner                    | /v1/vehicle/{vin}/owner",
        "/v1/{tenant}/vehicle/{vehicleId}                | /v1/{org}/vehicle/{id}",
        "/files/{name}.{ext}                             | /files/{file}.{extension}",
        "/v1/{a}-{b}                                     | /v1/{x}-{y}",
        "/v1/vehicle/{vehicleId}                         | /v1/vehicle/{vehicleId}",
        "/v1/vehicle/{id:[0-9]+}                         | /v1/vehicle/{vin:[0-9]+}",
        "/v1/vehicle/{id:[0-9]{3}}                       | /v1/vehicle/{vin:[0-9]{3}}",
        "/v1/vehicle/{*rest}                             | /v1/vehicle/{*path}",
        "/v1/vehicle/{*rest}                             | /v1/vehicle/**"
    })
    void testParameterNamesNeverMakePathsDifferent(String first, String second) {
        assertEquals(key("POST", first.trim()), key("POST", second.trim()));
    }

    /**
     * Test purpose          - Verify parameter names in any naming convention normalize to the same key.
     * Test data             - camelCase, PascalCase, snake_case, kebab-case, UPPER, dotted and numbered names.
     * Test expected result  - Every spelling normalizes to the {param} placeholder.
     * Test type             - Positive.
     */
    @ParameterizedTest(name = "parameter name {0}")
    @CsvSource({"vehicleId", "VehicleId", "vehicle_id", "vehicle-id", "VEHICLE_ID", "VIN", "vehicle.id", "id1",
        "v", "_id", "$id"})
    void testParameterNamingConventionsAreEquivalent(String name) {
        assertEquals("/v1/vehicle/{param}", key("GET", "/v1/vehicle/{" + name + "}").path());
    }

    /**
     * Test purpose          - Verify every parameterized segment is normalized on its own.
     * Test data             - A path with two parameters and a path with a parameter in the middle.
     * Test expected result  - Each variable becomes {param}; the static segments are untouched.
     * Test type             - Positive.
     */
    @Test
    void testEveryParameterizedSegmentIsNormalized() {
        assertEquals("/v1/vehicle/{param}/owner/{param}",
                key("POST", "/v1/vehicle/{vehicleId}/owner/{ownerId}").path());
        assertEquals("/v1/vehicle/{param}/owner", key("POST", "/v1/vehicle/{vin}/owner").path());
    }

    /**
     * Test purpose          - Verify a static segment is not the same as a parameterized one.
     * Test data             - A static and a parameterized path at the same position.
     * Test expected result  - The keys differ, because api-gateway tells the two apart by route precedence.
     * Test type             - Negative.
     */
    @Test
    void testStaticAndParameterizedSegmentsStayDifferent() {
        assertNotEquals(key("GET", "/v1/vehicle/status"), key("GET", "/v1/vehicle/{vehicleId}"));
        assertNotEquals(key("GET", "/v1/vehicle/{vehicleId}/owner"), key("GET", "/v1/vehicle/123/owner"));
    }

    /**
     * Test purpose          - Verify only the structure of the path matters, not just the number of parameters.
     * Test data             - Paths that differ in the position of the parameter or in a static segment.
     * Test expected result  - The keys differ.
     * Test type             - Negative.
     */
    @ParameterizedTest(name = "{0} is not {1}")
    @CsvSource(delimiter = '|', value = {
        "/v1/{a}/b                       | /v1/b/{a}",
        "/v1/vehicle/{id}/owner          | /v1/vehicle/{id}/status",
        "/v1/vehicle/{id}                | /v1/vehicle/{id}/owner",
        "/v1/vehicle/{id}                | /v2/vehicle/{id}",
        "/v1/vehicle/{id}/owner/{owner}  | /v1/vehicle/{id}/owner",
        "/v1/vehicle/{id}                | /v1/vehicle/*",
        "/v1/vehicle/{id:[0-9]+}         | /v1/vehicle/{id}",
        "/v1/vehicle/{id:[0-9]+}         | /v1/vehicle/{id:[a-z]+}",
        "/v1/vehicle/{id}                | /v1/vehicle/{*rest}",
        "/v1/Vehicle/{id}                | /v1/vehicle/{id}",
        "/v1/vehicle/{id}                | /v1/vehicle/{id}/"
    })
    void testDifferentStructuresStayDifferent(String first, String second) {
        assertNotEquals(key("GET", first.trim()), key("GET", second.trim()));
    }

    /**
     * Test purpose          - Verify the HTTP method is part of the key and compares without regard to case.
     * Test data             - The same path with different methods and with a lower-case method.
     * Test expected result  - Different methods give different keys; the case of the method does not matter.
     * Test type             - Positive and negative.
     */
    @Test
    void testMethodIsPartOfTheKey() {
        assertNotEquals(key("POST", "/v1/vehicle/{vehicleId}"), key("GET", "/v1/vehicle/{vin}"));
        assertEquals(key("POST", "/v1/vehicle/{vehicleId}"), key("post", "/v1/vehicle/{vin}"));
        assertEquals("POST", key(" post ", "/v1/vehicle").method());
    }

    /**
     * Test purpose          - Verify a route without a Method predicate serves every method.
     * Test data             - Routes without a method, with a blank method and with a method.
     * Test expected result  - No method in the key, and the key differs from the one of a method-specific route.
     * Test type             - Positive.
     */
    @Test
    void testRouteWithoutMethodServesEveryMethod() {
        assertNull(key(null, "/v1/vehicle").method());
        assertNull(key("  ", "/v1/vehicle").method());
        assertEquals(key(null, "/v1/vehicle/{id}"), key(null, "/v1/vehicle/{vin}"));
        assertNotEquals(key(null, "/v1/vehicle"), key("GET", "/v1/vehicle"));
    }

    /**
     * Test purpose          - Verify routes with no usable path have no key.
     * Test data             - No route, no predicates, no Path predicate, no path argument and a blank path.
     * Test expected result  - Empty key every time.
     * Test type             - Negative.
     */
    @Test
    void testRouteWithoutUsablePathHasNoKey() {
        assertTrue(RouteKey.of(null).isEmpty());
        assertTrue(RouteKey.of(new RouteDefinition()).isEmpty());
        RouteDefinition methodOnly = new RouteDefinition();
        methodOnly.getPredicates().add(predicate("Method", "GET"));
        assertTrue(RouteKey.of(methodOnly).isEmpty());
        RouteDefinition noArgument = new RouteDefinition();
        noArgument.getPredicates().add(predicate("Path"));
        assertTrue(RouteKey.of(noArgument).isEmpty());
        assertTrue(RouteKey.of(route("GET", " ")).isEmpty());
        RouteDefinition nullPredicates = new RouteDefinition();
        nullPredicates.setPredicates(null);
        assertTrue(RouteKey.of(nullPredicates).isEmpty());
    }

    /**
     * Test purpose          - Verify the path and the method are read the way api-gateway reads them.
     * Test data             - Predicates in lower case with several arguments, and several Path predicates.
     * Test expected result  - The last argument of the last predicate of that name is used.
     * Test type             - Positive.
     */
    @Test
    void testLastArgumentWinsLikeInTheGateway() {
        RouteDefinition route = new RouteDefinition();
        route.getPredicates().add(predicate("path", "/v1/first", "/v1/second/{id}"));
        route.getPredicates().add(predicate("method", "GET", "POST"));
        route.getPredicates().add(predicate("Path", "/v1/last/{id}"));

        RouteKey key = RouteKey.of(route).orElseThrow();

        assertEquals("/v1/last/{param}", key.path());
        assertEquals("POST", key.method());
        assertEquals("/v1/last/{id}", RouteKey.pathOf(route));
        assertEquals("POST", RouteKey.methodOf(route));
    }

    /**
     * Test purpose          - Verify a malformed path never fails the key computation.
     * Test data             - Paths with an unclosed brace.
     * Test expected result  - The unclosed brace is kept as it is; complete variables after it are normalized.
     * Test type             - Negative.
     */
    @Test
    void testUnbalancedBraceIsKeptAsIs() {
        assertEquals("/v1/{unclosed", key("GET", "/v1/{unclosed").path());
        assertEquals("/v1/{param}/{unclosed/{param}", key("GET", "/v1/{a}/{unclosed/{x}").path());
    }

    private static RouteKey key(String method, String path) {
        return RouteKey.of(route(method, path)).orElseThrow();
    }

    private static RouteDefinition route(String method, String path) {
        RouteDefinition route = new RouteDefinition();
        route.setPredicates(new ArrayList<>());
        if (method != null) {
            route.getPredicates().add(predicate("Method", method));
        }
        route.getPredicates().add(predicate("Path", path));
        return route;
    }

    private static PredicateDefinition predicate(String name, String... arguments) {
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName(name);
        for (int index = 0; index < arguments.length; index++) {
            predicate.addArg("_genkey_" + index, arguments[index]);
        }
        return predicate;
    }

    /**
     * Test purpose          - Verify the optional wrapper agrees with the key of a plain route.
     * Test data             - One route.
     * Test expected result  - A present key equal to the one built directly.
     * Test type             - Positive.
     */
    @Test
    void testOfReturnsTheKey() {
        Optional<RouteKey> key = RouteKey.of(route("DELETE", "/v1/vehicle/{id}"));

        assertEquals(Optional.of(new RouteKey("DELETE", "/v1/vehicle/{param}")), key);
    }
}
