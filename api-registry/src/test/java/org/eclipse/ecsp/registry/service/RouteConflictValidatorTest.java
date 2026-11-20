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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.ecsp.register.model.FilterDefinition;
import org.eclipse.ecsp.register.model.PredicateDefinition;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.register.model.RouteKey;
import org.eclipse.ecsp.registry.config.ChecksumProperties;
import org.eclipse.ecsp.registry.config.RouteConflictProperties;
import org.eclipse.ecsp.registry.entity.ApiRouteEntity;
import org.eclipse.ecsp.registry.events.EventPublisherContext;
import org.eclipse.ecsp.registry.exception.RouteConflictException;
import org.eclipse.ecsp.registry.repo.ApiRouteRepo;
import org.eclipse.ecsp.registry.repo.InMemoryApiRouteRepo;
import org.eclipse.ecsp.registry.utils.RegistryTestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for RouteConflictValidator: what a rejected registration leaves behind, the three modes, and the
 * registrations that are checked and the ones that are not.
 */
class RouteConflictValidatorTest {

    private static final String SERVICE_A = "service-a";
    private static final String SERVICE_B = "service-b";
    private static final String SERVICE_C = "service-c";
    private static final String GET = "GET";
    private static final String REJECTED = "rejected";
    private static final String ACCEPTED = "accepted";

    private InMemoryApiRouteRepo repo;
    private RouteConflictProperties properties;
    private SimpleMeterRegistry meterRegistry;
    private EventPublisherContext eventPublisher;
    private ApiRouteService service;
    private Logger validatorLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        repo = new InMemoryApiRouteRepo();
        properties = new RouteConflictProperties();
        meterRegistry = new SimpleMeterRegistry();
        eventPublisher = mock(EventPublisherContext.class);
        service = newService(true);
        validatorLogger = (Logger) LoggerFactory.getLogger(RouteConflictValidator.class);
        appender = new ListAppender<>();
        appender.start();
        validatorLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        validatorLogger.detachAppender(appender);
    }

    /**
     * Test purpose          - Verify a rejected registration leaves no trace in the registry.
     * Test data             - A conflicting registration after an accepted one.
     * Test expected result  - Nothing is saved or published, only the conflict counter and the log event show it.
     * Test type             - Negative.
     */
    @Test
    void testRejectedRegistrationChangesNothing() {
        register(SERVICE_A, "route-a", GET, "/v1/items/{id}");

        assertThrows(RouteConflictException.class, () -> register(SERVICE_B, "route-b", GET, "/v1/items/{itemId}"));

        assertEquals(1, repo.saveCalls());
        assertEquals(1, repo.size());
        verify(eventPublisher, times(1)).publishEvent(any());
        assertEquals(1.0, conflicts(REJECTED));
        assertEquals(0.0, conflicts(ACCEPTED));
        String logged = warnings().get(0);
        assertTrue(logged.startsWith("ROUTE_CONFLICT_REJECTED"), logged);
        assertTrue(logged.contains("routeId=route-b"), logged);
        assertTrue(logged.contains("api=GET /v1/items/{itemId}"), logged);
        assertTrue(logged.contains("conflictingRouteId=route-a"), logged);
        assertTrue(logged.contains("conflictingService=service-a"), logged);
        assertTrue(logged.contains("conflictingApi=GET /v1/items/{id}"), logged);
        assertTrue(logged.contains("conflictingActive=true"), logged);
    }

    /**
     * Test purpose          - Verify a route id that another service registered does not hide a conflict.
     * Test data             - Service B registers the route id and the method and path of a route of service A.
     * Test expected result  - The registration is rejected and the route of service A stays as it was.
     * Test type             - Negative.
     */
    @Test
    void testSameRouteIdOfAnotherServiceWithTheSameMethodAndPathConflicts() {
        register(SERVICE_A, "route-x", GET, "/v1/items");

        RouteConflictException conflict =
                assertThrows(RouteConflictException.class, () -> register(SERVICE_B, "route-x", GET, "/v1/items"));

        assertEquals(List.of("route-x"), conflict.getConflictingRouteIds());
        assertEquals(SERVICE_A, repo.findById("route-x").orElseThrow().getService());
    }

    /**
     * Test purpose          - Verify a route id of another service with another path is no path conflict.
     * Test data             - Service B registers the route id of service A for another path.
     * Test expected result  - The registration is accepted and takes the route over, and the log says so.
     * Test type             - Positive.
     */
    @Test
    void testSameRouteIdOfAnotherServiceWithAnotherPathIsAcceptedWithWarning() {
        register(SERVICE_A, "route-x", GET, "/v1/a");

        register(SERVICE_B, "route-x", GET, "/v1/b");

        assertEquals(SERVICE_B, repo.findById("route-x").orElseThrow().getService());
        assertEquals(0.0, conflicts(REJECTED));
        assertEquals(1, warnings().size());
        assertTrue(warnings().get(0).startsWith("ROUTE_OWNER_CHANGED"), warnings().get(0));
        assertTrue(warnings().get(0).contains("previousService=service-a"), warnings().get(0));
    }

    /**
     * Test purpose          - Verify an API that moves is checked against the method and path it moves to.
     * Test data             - An API that moves to the path of another route, then to a free path.
     * Test expected result  - The first move is rejected, the second is accepted and frees the old path.
     * Test type             - Positive and negative.
     */
    @Test
    void testApiThatMovesIsCheckedAgainstTheNewMethodAndPath() {
        register(SERVICE_A, "route-x", GET, "/v1/a");
        register(SERVICE_B, "route-y", GET, "/v1/b");

        RouteConflictException conflict =
                assertThrows(RouteConflictException.class, () -> register(SERVICE_A, "route-x", GET, "/v1/b"));
        assertEquals(List.of("route-y"), conflict.getConflictingRouteIds());
        assertEquals("/v1/a", RouteKey.pathOf(repo.findById("route-x").orElseThrow().getRoute()));

        register(SERVICE_A, "route-x", GET, "/v1/c");
        register(SERVICE_C, "route-z", GET, "/v1/a");

        assertEquals(3, repo.size());
    }

    /**
     * Test purpose          - Verify a route that is not active still owns its method and path.
     * Test data             - A route that the health monitor deactivated and a new registration for its method and
     *                         path.
     * Test expected result  - The registration is rejected, and the message says the owner is inactive.
     * Test type             - Negative.
     */
    @Test
    void testInactiveRouteStillOwnsItsMethodAndPath() {
        register(SERVICE_A, "route-a", GET, "/v1/items");
        repo.deactivate("route-a");

        RouteConflictException conflict =
                assertThrows(RouteConflictException.class, () -> register(SERVICE_B, "route-b", GET, "/v1/items"));

        assertTrue(conflict.getMessage().contains("inactive"), conflict.getMessage());
    }

    /**
     * Test purpose          - Verify a route for every method is not the same as a route for one method.
     * Test data             - A route without a Method predicate and routes with a method, on one path.
     * Test expected result  - They do not conflict, but two routes for every method do.
     * Test type             - Positive and negative.
     */
    @Test
    void testRouteForEveryMethodIsNotTheSameAsRouteForOneMethod() {
        register(SERVICE_A, "any-items", null, "/v1/items");

        register(SERVICE_B, "get-items", GET, "/v1/items");
        register(SERVICE_B, "post-items", "POST", "/v1/items");

        assertThrows(RouteConflictException.class, () -> register(SERVICE_C, "other-any-items", null, "/v1/items"));
        assertEquals(3, repo.size());
    }

    /**
     * Test purpose          - Verify a route without a usable path is never checked.
     * Test data             - Two routes with only a Method predicate, from different services.
     * Test expected result  - Both are accepted and no route is read for them.
     * Test type             - Positive.
     */
    @Test
    void testRouteWithoutPathNeverConflicts() {
        assertDoesNotThrow(() -> {
            registerWithoutPath(SERVICE_A, "route-a");
            registerWithoutPath(SERVICE_B, "route-b");
        });

        assertEquals(2, repo.size());
        assertEquals(0, repo.findAllCalls());
    }

    /**
     * Test purpose          - Verify conflicts are rejected when change detection is switched off as well.
     * Test data             - A service without change detection: a conflict, and the same API again.
     * Test expected result  - The conflict is rejected, the same API is accepted.
     * Test type             - Positive and negative.
     */
    @Test
    void testConflictsAreRejectedWithoutChangeDetectionToo() {
        ApiRouteService withoutChangeDetection = newService(false);
        withoutChangeDetection.createOrUpdate(RegistryTestUtil.getRoute(SERVICE_A, "route-a", GET, "/v1/items/{id}"));

        RouteDefinition route1 = RegistryTestUtil.getRoute(SERVICE_B, "route-b", GET, "/v1/items/{itemId}");
        assertThrows(RouteConflictException.class, () -> withoutChangeDetection.createOrUpdate(route1));
        RouteDefinition route2 = RegistryTestUtil.getRoute(SERVICE_A, "route-a", GET, "/v1/items/{itemId}");
        assertDoesNotThrow(() -> withoutChangeDetection.createOrUpdate(route2));

        assertEquals(1, repo.size());
    }

    /**
     * Test purpose          - Verify duplicates registered before conflict detection existed keep working.
     * Test data             - Two routes of different services with the same method and path that are stored, a
     *                         stored route without a definition, and a new route for the same method and path.
     * Test expected result  - The stored routes register again unchanged and with a new filter; the new route is
     *                         rejected and both stored routes are named.
     * Test type             - Positive and negative.
     */
    @Test
    void testDuplicatesRegisteredBeforeConflictDetectionKeepWorking() {
        repo.seed(entity(RegistryTestUtil.getRoute(SERVICE_A, "route-a", GET, "/v1/dup")));
        repo.seed(entity(RegistryTestUtil.getRoute(SERVICE_B, "route-b", GET, "/v1/dup")));
        ApiRouteEntity withoutDefinition = new ApiRouteEntity();
        withoutDefinition.setId("route-legacy");
        withoutDefinition.setActive(Boolean.TRUE);
        repo.seed(withoutDefinition);

        RouteDefinition changed = RegistryTestUtil.getRoute(SERVICE_B, "route-b", GET, "/v1/dup");
        FilterDefinition filter = new FilterDefinition();
        filter.setName("JwtAuthValidator");
        changed.getFilters().add(filter);

        assertDoesNotThrow(() -> service.createOrUpdate(
                RegistryTestUtil.getRoute(SERVICE_A, "route-a", GET, "/v1/dup")));
        assertDoesNotThrow(() -> service.createOrUpdate(changed));
        RouteConflictException conflict = assertThrows(RouteConflictException.class,
                () -> register(SERVICE_C, "route-c", GET, "/v1/dup"));

        assertEquals(List.of("route-a", "route-b"), conflict.getConflictingRouteIds());
    }

    /**
     * Test purpose          - Verify only a registration that claims a method and path reads the registered routes.
     * Test data             - A new route, the same API again, and the same API with a new filter.
     * Test expected result  - Only the new route reads the registered routes.
     * Test type             - Positive.
     */
    @Test
    void testOnlyRegistrationsThatClaimMethodAndPathReadTheRegisteredRoutes() {
        register(SERVICE_A, "route-a", GET, "/v1/items/{id}");
        assertEquals(1, repo.findAllCalls());

        register(SERVICE_A, "route-a", GET, "/v1/items/{id}");
        assertEquals(1, repo.findAllCalls());
        assertEquals(1, repo.saveCalls());

        RouteDefinition withFilter = RegistryTestUtil.getRoute(SERVICE_A, "route-a", GET, "/v1/items/{id}");
        FilterDefinition filter = new FilterDefinition();
        filter.setName("JwtAuthValidator");
        withFilter.getFilters().add(filter);
        service.createOrUpdate(withFilter);

        assertEquals(1, repo.findAllCalls());
        assertEquals(2, repo.saveCalls());
    }

    /**
     * Test purpose          - Verify the warn mode accepts a conflict and reports it.
     * Test data             - A conflicting registration in WARN mode.
     * Test expected result  - Both routes are stored, the conflict is counted as accepted and logged.
     * Test type             - Positive.
     */
    @Test
    void testWarnModeAcceptsAndReportsConflicts() {
        properties.setMode(RouteConflictProperties.Mode.WARN);
        register(SERVICE_A, "route-a", GET, "/v1/items/{id}");

        register(SERVICE_B, "route-b", GET, "/v1/items/{itemId}");

        assertEquals(2, repo.size());
        assertEquals(1.0, conflicts(ACCEPTED));
        assertEquals(0.0, conflicts(REJECTED));
        assertTrue(warnings().get(0).startsWith("ROUTE_CONFLICT_ACCEPTED"), warnings().get(0));
    }

    /**
     * Test purpose          - Verify the off mode does not look for conflicts.
     * Test data             - A conflicting registration in OFF mode.
     * Test expected result  - Both routes are stored, no route is read and nothing is reported.
     * Test type             - Positive.
     */
    @Test
    void testOffModeDoesNotLookForConflicts() {
        properties.setMode(RouteConflictProperties.Mode.OFF);
        register(SERVICE_A, "route-a", GET, "/v1/items/{id}");

        register(SERVICE_B, "route-b", GET, "/v1/items/{itemId}");

        assertEquals(2, repo.size());
        assertEquals(0, repo.findAllCalls());
        assertEquals(0.0, conflicts(ACCEPTED));
        assertTrue(warnings().isEmpty());
    }

    /**
     * Test purpose          - Verify a repository that returns no routes at all is no problem.
     * Test data             - A repository whose findAll returns null.
     * Test expected result  - The registration is accepted.
     * Test type             - Negative.
     */
    @Test
    void testMissingRegisteredRoutesAreNoConflict() {
        ApiRouteRepo emptyRepo = mock(ApiRouteRepo.class);
        when(emptyRepo.findAll()).thenReturn(null);
        RouteConflictValidator validator = new RouteConflictValidator(emptyRepo, properties, meterRegistry);

        assertDoesNotThrow(() -> validator.validate(
                RegistryTestUtil.getRoute(SERVICE_A, "route-a", GET, "/v1/items"), Optional.empty()));
    }

    private void register(String serviceName, String routeId, String method, String path) {
        service.createOrUpdate(RegistryTestUtil.getRoute(serviceName, routeId, method, path));
    }

    private void registerWithoutPath(String serviceName, String routeId) {
        RouteDefinition route = new RouteDefinition();
        route.setId(routeId);
        route.setService(serviceName);
        PredicateDefinition method = new PredicateDefinition();
        method.setName("Method");
        method.addArg("_genkey_0", GET);
        route.getPredicates().add(method);
        service.createOrUpdate(route);
    }

    private ApiRouteService newService(boolean changeDetection) {
        ChecksumProperties checksumProperties = new ChecksumProperties();
        checksumProperties.setEnabled(changeDetection);
        return new ApiRouteService(repo, Optional.of(eventPublisher), checksumProperties,
                new RouteChecksumService(checksumProperties, new ObjectMapper()), meterRegistry,
                new RouteConflictValidator(repo, properties, meterRegistry));
    }

    private static ApiRouteEntity entity(RouteDefinition route) {
        ApiRouteEntity entity = new ApiRouteEntity();
        entity.setId(route.getId());
        entity.setService(route.getService());
        entity.setRoute(route);
        entity.setActive(Boolean.TRUE);
        return entity;
    }

    private double conflicts(String outcome) {
        Counter counter = meterRegistry.find("api.route.conflict.total").tag("outcome", outcome).counter();
        return counter == null ? 0.0 : counter.count();
    }

    private List<String> warnings() {
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }
}
