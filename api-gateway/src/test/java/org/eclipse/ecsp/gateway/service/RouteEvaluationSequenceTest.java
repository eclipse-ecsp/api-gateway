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

package org.eclipse.ecsp.gateway.service;

import org.eclipse.ecsp.gateway.clients.ApiRegistryClient;
import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webflux.autoconfigure.WebFluxProperties;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.handler.predicate.MethodRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.CachingRouteLocator;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Proves that api-gateway evaluates routes in the sequence api-registry returns them.
 *
 * <p>The routes go through a real {@link IgniteRouteLocator}, a real {@link RouteLocatorBuilder} and the
 * {@link CachingRouteLocator} api-gateway serves its routing table from. api-gateway ranks nothing itself, so the
 * sequence api-registry puts the routes in decides which of several overlapping routes serves a request.
 */
class RouteEvaluationSequenceTest {

    private static final String SEARCH_PATH = "/v1/entitlements/search";
    private static final String BY_ID_PATH = "/v1/entitlements/42";
    private static final String HISTORY_PATH = "/v1/entitlements/42/history";

    private RouteLocatorBuilder routeLocatorBuilder;

    @BeforeEach
    void setUp() {
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("pathRoutePredicateFactory",
                new PathRoutePredicateFactory(new WebFluxProperties()));
        context.getBeanFactory().registerSingleton("methodRoutePredicateFactory", new MethodRoutePredicateFactory());
        context.refresh();
        routeLocatorBuilder = new RouteLocatorBuilder(context);
    }

    /**
     * Test purpose          - Verify the first route that matches is the one api-registry ranked first.
     * Test data             - Exact, parameterized and catch-all routes received in route order.
     * Test expected result  - Each request is served by the most specific route that matches it.
     * Test type             - Positive.
     */
    @Test
    void testRoutesAreEvaluatedInTheSequenceReceivedFromTheRegistry() {
        List<Route> routes = routingTable(
                route("search", "/v1/entitlements/search"),
                route("by-id", "/v1/entitlements/{productId}"),
                route("catch-all", "/v1/entitlements/**"));

        assertEquals("search", firstMatch(routes, SEARCH_PATH));
        assertEquals("by-id", firstMatch(routes, BY_ID_PATH));
        assertEquals("catch-all", firstMatch(routes, HISTORY_PATH));
    }

    /**
     * Test purpose          - Verify api-gateway adds no ranking of its own.
     * Test data             - The same routes received with the catch-all first.
     * Test expected result  - The catch-all serves every request, which is why api-registry must rank the routes.
     * Test type             - Negative.
     */
    @Test
    void testGatewayDoesNotRankRoutesItselfSoTheReceivedSequenceDecides() {
        List<Route> routes = routingTable(
                route("catch-all", "/v1/entitlements/**"),
                route("by-id", "/v1/entitlements/{productId}"),
                route("search", "/v1/entitlements/search"));

        assertEquals("catch-all", firstMatch(routes, SEARCH_PATH));
        assertEquals("catch-all", firstMatch(routes, BY_ID_PATH));
        assertEquals("catch-all", firstMatch(routes, HISTORY_PATH));
    }

    private List<Route> routingTable(IgniteRouteDefinition... receivedFromRegistry) {
        ApiRegistryClient apiRegistryClient = mock(ApiRegistryClient.class);
        when(apiRegistryClient.getRoutes()).thenReturn(Flux.just(receivedFromRegistry));
        IgniteRouteLocator igniteRouteLocator = new IgniteRouteLocator(mock(ConfigurationService.class),
                new ArrayList<>(), new GatewayProperties(), false, null, apiRegistryClient, routeLocatorBuilder,
                mock(ApplicationEventPublisher.class), null, new ArrayList<>());
        return new CachingRouteLocator(igniteRouteLocator).getRoutes().collectList().block();
    }

    private static String firstMatch(List<Route> routes, String path) {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path));
        return routes.stream()
                .filter(route -> Boolean.TRUE.equals(Mono.from(route.getPredicate().apply(exchange)).block()))
                .map(Route::getId)
                .findFirst()
                .orElse(null);
    }

    private static IgniteRouteDefinition route(String id, String path) {
        IgniteRouteDefinition route = new IgniteRouteDefinition();
        route.setId(id);
        route.setUri(URI.create("http://" + id + ":8080"));
        route.setService("entitlement-service");
        route.setPredicates(List.of(predicate("Path", path), predicate("Method", "GET")));
        return route;
    }

    private static PredicateDefinition predicate(String name, String value) {
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName(name);
        predicate.addArg("_genkey_0", value);
        return predicate;
    }
}
