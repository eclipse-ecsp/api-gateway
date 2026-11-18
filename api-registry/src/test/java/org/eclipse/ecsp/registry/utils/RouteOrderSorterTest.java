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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.eclipse.ecsp.register.model.PredicateDefinition;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Test class for RouteOrderSorter.
 */
class RouteOrderSorterTest {

    private static final int SHUFFLES = 20;

    private Logger sorterLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        sorterLogger = (Logger) LoggerFactory.getLogger(RouteOrderSorter.class);
        appender = new ListAppender<>();
        appender.start();
        sorterLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        sorterLogger.detachAppender(appender);
    }

    /**
     * Test purpose          - Verify a specific path is placed before a broader path that also matches it.
     * Test data             - Catch-all, parameterized and exact paths whose route ids sort the other way round.
     * Test expected result  - Exact path first, then parameterized, then catch-all.
     * Test type             - Positive.
     */
    @Test
    void testSortPlacesSpecificPathsBeforeBroaderOnes() {
        List<RouteDefinition> routes = List.of(
                route("a-catch-all", "/v1/entitlements/**"),
                route("b-parameterized", "/v1/entitlements/{productId}"),
                route("c-exact", "/v1/entitlements/search"));

        Assertions.assertEquals(List.of("c-exact", "b-parameterized", "a-catch-all"),
                ids(RouteOrderSorter.sort(routes)));
    }

    /**
     * Test purpose          - Verify routes of equal specificity are ordered by route id.
     * Test data             - Three routes with the same path shape, supplied out of id order.
     * Test expected result  - Routes in ascending route id order.
     * Test type             - Positive.
     */
    @Test
    void testSortBreaksTiesByRouteId() {
        List<RouteDefinition> routes = List.of(
                route("zeta", "/v1/b/{id}"),
                route("alpha", "/v1/a/{id}"),
                route("mid", "/v1/c/{id}"));

        Assertions.assertEquals(List.of("alpha", "mid", "zeta"), ids(RouteOrderSorter.sort(routes)));
    }

    /**
     * Test purpose          - Verify the order does not depend on the order in which routes are supplied.
     * Test data             - Routes with and without ties, shuffled repeatedly.
     * Test expected result  - Every shuffle yields the same order.
     * Test type             - Positive.
     */
    @Test
    void testSortIsIndependentOfInputOrder() {
        List<RouteDefinition> routes = new ArrayList<>(List.of(
                route("r1", "/v1/**"),
                route("r2", "/v1/vehicles/{vin}"),
                route("r3", "/v1/vehicles/{vin}/status"),
                route("r4", "/v1/users/{id}"),
                route("r5", "/v1/vehicles/count"),
                route("r6", "/v1/vehicles/**")));
        List<String> expected = ids(RouteOrderSorter.sort(routes));

        for (int seed = 0; seed < SHUFFLES; seed++) {
            Collections.shuffle(routes, new Random(seed));
            Assertions.assertEquals(expected, ids(RouteOrderSorter.sort(routes)));
        }
    }

    /**
     * Test purpose          - Verify the given list is left untouched.
     * Test data             - Routes supplied in an order that differs from route order.
     * Test expected result  - The given list keeps its order; the result is a separate list.
     * Test type             - Positive.
     */
    @Test
    void testSortDoesNotModifyTheGivenList() {
        List<RouteDefinition> routes = new ArrayList<>(List.of(route("a", "/v1/**"), route("b", "/v1/exact")));

        List<RouteDefinition> sorted = RouteOrderSorter.sort(routes);

        Assertions.assertEquals(List.of("a", "b"), ids(routes));
        Assertions.assertEquals(List.of("b", "a"), ids(sorted));
    }

    /**
     * Test purpose          - Verify the path is read the way api-gateway reads it.
     * Test data             - A predicate named in lower case whose last argument is a catch-all.
     * Test expected result  - The last argument decides the position of the route.
     * Test type             - Positive.
     */
    @Test
    void testSortReadsTheLastArgumentOfThePathPredicate() {
        RouteDefinition catchAllLast = route("a-multi", "/v1/items/search", "/v1/items/**");
        catchAllLast.getPredicates().get(0).setName("path");
        RouteDefinition parameterized = route("b-parameterized", "/v1/items/{id}");

        Assertions.assertEquals(List.of("b-parameterized", "a-multi"),
                ids(RouteOrderSorter.sort(List.of(catchAllLast, parameterized))));
    }

    /**
     * Test purpose          - Verify a route without a usable path never fails the ordering.
     * Test data             - Routes with no path argument, no Path predicate and an invalid path pattern.
     * Test expected result  - Those routes come last in route id order, each with a warning; no exception.
     * Test type             - Negative.
     */
    @Test
    void testSortRanksRoutesWithoutUsablePathLastAndWarns() {
        RouteDefinition noPathArgument = route("a-no-argument");
        RouteDefinition noPathPredicate = new RouteDefinition();
        noPathPredicate.setId("b-no-predicate");
        RouteDefinition invalidPattern = route("c-invalid", "/v1/{unclosed");
        RouteDefinition catchAll = route("d-catch-all", "/v1/**");
        RouteDefinition exact = route("e-exact", "/v1/exact");

        List<RouteDefinition> sorted = RouteOrderSorter.sort(
                List.of(noPathArgument, noPathPredicate, invalidPattern, catchAll, exact));

        Assertions.assertEquals(
                List.of("e-exact", "d-catch-all", "a-no-argument", "b-no-predicate", "c-invalid"), ids(sorted));
        Assertions.assertEquals(3, warnings().size());
        assertWarned("a-no-argument", "missing_path");
        assertWarned("b-no-predicate", "missing_path");
        assertWarned("c-invalid", "invalid_pattern");
    }

    private void assertWarned(String routeId, String reason) {
        Assertions.assertTrue(warnings().stream().anyMatch(message ->
                message.contains(RegistryConstants.LOG_EVENT_ROUTE_ORDER_PATH_UNPARSEABLE)
                        && message.contains("routeId=" + routeId + " ")
                        && message.contains("reason=" + reason)), "no warning for route " + routeId);
    }

    private List<String> warnings() {
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private static List<String> ids(List<RouteDefinition> routes) {
        return routes.stream().map(RouteDefinition::getId).toList();
    }

    private static RouteDefinition route(String id, String... pathArguments) {
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");
        for (int i = 0; i < pathArguments.length; i++) {
            predicate.addArg("_genkey_" + i, pathArguments[i]);
        }
        RouteDefinition route = new RouteDefinition();
        route.setId(id);
        route.setPredicates(new ArrayList<>(List.of(predicate)));
        return route;
    }

    private static RouteDefinition routeForMethod(String id, String method, String path) {
        RouteDefinition route = route(id, path);
        PredicateDefinition methodPredicate = new PredicateDefinition();
        methodPredicate.setName("Method");
        methodPredicate.addArg("_genkey_0", method);
        route.getPredicates().add(0, methodPredicate);
        return route;
    }

    /**
     * Test purpose          - Verify a route for one method comes before a route for every method on the same path.
     * Test data             - A route for every method and a GET route on one path, with ids that sort the other
     *                         way round, in both input orders.
     * Test expected result  - The GET route first, so the other route does not serve GET requests.
     * Test type             - Positive.
     */
    @Test
    void testSortPlacesRouteForOneMethodBeforeRouteForEveryMethod() {
        RouteDefinition everyMethod = route("a-every-method", "/v1/items");
        RouteDefinition get = routeForMethod("b-get", "GET", "/v1/items");
        List<String> expected = List.of("b-get", "a-every-method");

        Assertions.assertEquals(expected, ids(RouteOrderSorter.sort(List.of(everyMethod, get))));
        Assertions.assertEquals(expected, ids(RouteOrderSorter.sort(List.of(get, everyMethod))));
    }

    /**
     * Test purpose          - Verify the path specificity still decides before the method does.
     * Test data             - A route for every method on a static path and a GET route on a parameterized path.
     * Test expected result  - The static path first, although the other route serves one method.
     * Test type             - Positive.
     */
    @Test
    void testSortRanksPathSpecificityBeforeMethod() {
        RouteDefinition staticForEveryMethod = route("a-static", "/v1/items/search");
        RouteDefinition parameterizedForGet = routeForMethod("b-parameterized", "GET", "/v1/items/{id}");

        Assertions.assertEquals(List.of("a-static", "b-parameterized"),
                ids(RouteOrderSorter.sort(List.of(parameterizedForGet, staticForEveryMethod))));
    }

    /**
     * Test purpose          - Verify a static path is ordered before a parameterized path at the same position.
     * Test data             - GET /v1/vehicle/status and GET /v1/vehicle/{vehicleId} in both input orders, with ids
     *                         that sort the other way round.
     * Test expected result  - The static path first, so the route that the gateway evaluates first is the static one.
     * Test type             - Positive.
     */
    @Test
    void testSortPlacesStaticPathBeforeParameterizedPath() {
        RouteDefinition parameterized = routeForMethod("a-by-id", "GET", "/v1/vehicle/{vehicleId}");
        RouteDefinition fixed = routeForMethod("b-status", "GET", "/v1/vehicle/status");
        List<String> expected = List.of("b-status", "a-by-id");

        Assertions.assertEquals(expected, ids(RouteOrderSorter.sort(List.of(parameterized, fixed))));
        Assertions.assertEquals(expected, ids(RouteOrderSorter.sort(List.of(fixed, parameterized))));
    }
}
