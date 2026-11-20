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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.registry.config.ChecksumProperties;
import org.eclipse.ecsp.registry.config.RouteConflictProperties;
import org.eclipse.ecsp.registry.events.EventPublisherContext;
import org.eclipse.ecsp.registry.exception.RouteConflictException;
import org.eclipse.ecsp.registry.repo.InMemoryApiRouteRepo;
import org.eclipse.ecsp.registry.utils.RegistryTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntConsumer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Scenarios of conflicting route registrations: a route registered under the HTTP method and the path of a
 * registered route, whatever the names of its path variables are, is rejected unless it is the same API registered
 * again by a replica of its own service.
 *
 * <p>The scenarios run through {@link ApiRouteService#createOrUpdate(RouteDefinition)} against an in-memory
 * repository, with the real change detection and the real conflict validation.
 */
class RouteRegistrationConflictTest {

    private static final String VEHICLE_SERVICE = "vehicle-service";
    private static final String INVENTORY_SERVICE = "inventory-service";
    private static final String BILLING_SERVICE = "billing-service";
    private static final String POST = "POST";
    private static final String GET = "GET";
    private static final String VEHICLE_BY_ID = "/v1/vehicle/{vehicleId}";
    private static final String VEHICLE_BY_VIN = "/v1/vehicle/{vin}";
    private static final int REPLICAS = 8;
    private static final long SLOW_SAVE_MILLIS = 30;
    private static final long TIMEOUT_SECONDS = 30;

    private InMemoryApiRouteRepo repo;
    private ApiRouteService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryApiRouteRepo();
        service = newService(repo);
    }

    /**
     * Test purpose          - Verify the same method and the same static path conflict.
     * Test data             - Two services that register GET /v1/vehicle/status under different route ids.
     * Test expected result  - The second registration is rejected and names the route that owns the path.
     * Test type             - Negative.
     */
    @Test
    void testSameMethodAndSameStaticPathConflict() {
        register(VEHICLE_SERVICE, "get-vehicle-status", GET, "/v1/vehicle/status");

        RouteConflictException conflict =
                rejected(INVENTORY_SERVICE, "get-inventory-status", GET, "/v1/vehicle/status");

        assertEquals(List.of("get-vehicle-status"), conflict.getConflictingRouteIds());
        assertEquals(1, repo.size());
    }

    /**
     * Test purpose          - Verify the names of path variables never make a route unique.
     * Test data             - POST /v1/vehicle/{vehicleId} registered first, then the same path with {vin} and {id}.
     * Test expected result  - Both later registrations are rejected as conflicts with the first route.
     * Test type             - Negative.
     */
    @Test
    void testParameterizedPathsWithDifferentParameterNamesConflict() {
        register(VEHICLE_SERVICE, "update-vehicle", POST, VEHICLE_BY_ID);

        assertEquals(List.of("update-vehicle"),
                rejected(INVENTORY_SERVICE, "update-inventory-vehicle", POST, VEHICLE_BY_VIN)
                        .getConflictingRouteIds());
        assertEquals(List.of("update-vehicle"),
                rejected(BILLING_SERVICE, "bill-vehicle", POST, "/v1/vehicle/{id}").getConflictingRouteIds());
        assertEquals(1, repo.size());
    }

    /**
     * Test purpose          - Verify the conflict does not depend on which spelling was registered first.
     * Test data             - POST /v1/vehicle/{id} registered first, then POST /v1/vehicle/{vehicleId}.
     * Test expected result  - The second registration is rejected.
     * Test type             - Negative.
     */
    @Test
    void testConflictDoesNotDependOnTheOrderOfRegistration() {
        register(VEHICLE_SERVICE, "update-vehicle", POST, "/v1/vehicle/{id}");

        assertEquals(List.of("update-vehicle"),
                rejected(INVENTORY_SERVICE, "update-inventory-vehicle", POST, VEHICLE_BY_ID)
                        .getConflictingRouteIds());
    }

    /**
     * Test purpose          - Verify the same normalized path with another HTTP method is no conflict.
     * Test data             - POST /v1/vehicle/{vehicleId}, then GET, PUT and DELETE /v1/vehicle/{vin}.
     * Test expected result  - Every registration is accepted.
     * Test type             - Positive.
     */
    @Test
    void testSameNormalizedPathWithDifferentMethodsDoesNotConflict() {
        register(VEHICLE_SERVICE, "create-vehicle", POST, VEHICLE_BY_ID);

        register(INVENTORY_SERVICE, "get-inventory-vehicle", GET, VEHICLE_BY_VIN);
        register(INVENTORY_SERVICE, "put-inventory-vehicle", "PUT", VEHICLE_BY_VIN);
        register(BILLING_SERVICE, "delete-billing-vehicle", "DELETE", "/v1/vehicle/{id}");

        assertEquals(4, repo.size());
    }

    /**
     * Test purpose          - Verify every path variable of a path is normalized.
     * Test data             - Two registrations with two variables each, named differently.
     * Test expected result  - The second registration is rejected.
     * Test type             - Negative.
     */
    @Test
    void testMultipleParameterizedSegmentsConflict() {
        register(VEHICLE_SERVICE, "get-owner", POST, "/v1/vehicle/{vehicleId}/owner/{ownerId}");

        RouteConflictException conflict =
                rejected(INVENTORY_SERVICE, "get-vehicle-owner", POST, "/v1/vehicle/{vin}/owner/{userId}");

        assertEquals(List.of("get-owner"), conflict.getConflictingRouteIds());
    }

    /**
     * Test purpose          - Verify the same API registered by several replicas of its service is no conflict.
     * Test data             - The same service name, route id, method and path registered several times, once with
     *                         a renamed path variable.
     * Test expected result  - Every registration succeeds and one route is stored.
     * Test type             - Positive.
     */
    @Test
    void testSameApiRegisteredByReplicasOfTheSameServiceDoesNotConflict() {
        register(VEHICLE_SERVICE, "create-vehicle", POST, VEHICLE_BY_ID);

        RouteDefinition second = register(VEHICLE_SERVICE, "create-vehicle", POST, VEHICLE_BY_ID);
        RouteDefinition renamedParameter = register(VEHICLE_SERVICE, "create-vehicle", POST, VEHICLE_BY_VIN);

        assertEquals("create-vehicle", second.getId());
        assertEquals("create-vehicle", renamedParameter.getId());
        assertEquals(1, repo.size());
    }

    /**
     * Test purpose          - Verify replicas that register the same new API at the same moment all succeed.
     * Test data             - Several replicas of one service register one route together; saving is slow, so the
     *                         registrations overlap.
     * Test expected result  - No registration is rejected and one route is stored.
     * Test type             - Positive.
     */
    @Test
    void testReplicasRegisteringTheSameNewApiTogetherDoNotConflict() throws Exception {
        repo.beforeSave(RouteRegistrationConflictTest::pause);

        List<Throwable> failures = registerTogether(REPLICAS,
                replica -> register(VEHICLE_SERVICE, "create-vehicle", POST, VEHICLE_BY_ID));

        assertTrue(failures.isEmpty(), failures.toString());
        assertEquals(1, repo.size());
    }

    /**
     * Test purpose          - Verify the same normalized route registered by different services conflicts.
     * Test data             - vehicle-service/update-vehicle and inventory-service/update-inventory-vehicle, both
     *                         resolving to POST /v1/vehicle/{param}.
     * Test expected result  - The new registration is rejected, nothing of it is stored and the message names both.
     * Test type             - Negative.
     */
    @Test
    void testSameNormalizedRouteRegisteredByDifferentServicesConflicts() {
        register(VEHICLE_SERVICE, "update-vehicle", POST, VEHICLE_BY_ID);

        RouteConflictException conflict =
                rejected(INVENTORY_SERVICE, "update-inventory-vehicle", POST, VEHICLE_BY_VIN);

        assertEquals(List.of("update-vehicle"), conflict.getConflictingRouteIds());
        assertTrue(conflict.getMessage().contains("'update-inventory-vehicle' of service 'inventory-service'"),
                conflict.getMessage());
        assertTrue(conflict.getMessage().contains("POST /v1/vehicle/{vin}"), conflict.getMessage());
        assertTrue(conflict.getMessage().contains("route 'update-vehicle' of service 'vehicle-service'"),
                conflict.getMessage());
        assertTrue(conflict.getMessage().contains("POST /v1/vehicle/{vehicleId}"), conflict.getMessage());
        assertTrue(repo.findById("update-inventory-vehicle").isEmpty());
        assertEquals(1, repo.size());
    }

    /**
     * Test purpose          - Verify registrations of different services that arrive together cannot both succeed.
     * Test data             - Several services register POST /v1/vehicle/{param} at the same moment; saving is slow,
     *                         so every registration checks before any of them has saved.
     * Test expected result  - Exactly one registration succeeds, all others are rejected, one route is stored.
     * Test type             - Negative.
     */
    @Test
    void testRegistrationsOfDifferentServicesTogetherLetOnlyOneSucceed() throws Exception {
        repo.beforeSave(RouteRegistrationConflictTest::pause);

        List<Throwable> failures = registerTogether(REPLICAS,
                replica -> register("service-" + replica, "update-vehicle-" + replica, POST, VEHICLE_BY_ID));

        assertEquals(REPLICAS - 1, failures.size(), failures.toString());
        failures.forEach(failure -> assertInstanceOf(RouteConflictException.class, failure));
        assertEquals(1, repo.size());
    }

    /**
     * Test purpose          - Verify different route ids of one service with the same normalized path conflict.
     * Test data             - The same service registers POST /v1/vehicle/{vehicleId} and POST /v1/vehicle/{vin}
     *                         under different route ids.
     * Test expected result  - The second registration is rejected as a conflicting API definition.
     * Test type             - Negative.
     */
    @Test
    void testDifferentRouteIdsOfTheSameServiceWithTheSameNormalizedPathConflict() {
        register(VEHICLE_SERVICE, "update-vehicle", POST, VEHICLE_BY_ID);

        RouteConflictException conflict = rejected(VEHICLE_SERVICE, "update-vehicle-v2", POST, VEHICLE_BY_VIN);

        assertEquals(List.of("update-vehicle"), conflict.getConflictingRouteIds());
        assertEquals(1, repo.size());
    }

    /**
     * Test purpose          - Verify a static path and a parameterized path are no conflict and are ordered.
     * Test data             - GET /v1/vehicle/status and GET /v1/vehicle/{vehicleId}, registered in both orders and
     *                         by different services.
     * Test expected result  - Both are accepted, and the static path is always listed first, so api-gateway
     *                         evaluates it first.
     * Test type             - Positive.
     */
    @Test
    void testStaticPathAndParameterizedPathDoNotConflictAndAreOrdered() {
        register(VEHICLE_SERVICE, "get-vehicle", GET, VEHICLE_BY_ID);
        register(INVENTORY_SERVICE, "get-vehicle-status", GET, "/v1/vehicle/status");

        assertEquals(List.of("get-vehicle-status", "get-vehicle"), listedIds());

        ApiRouteService otherOrder = newService(new InMemoryApiRouteRepo());
        otherOrder.createOrUpdate(RegistryTestUtil.getRoute(INVENTORY_SERVICE, "get-vehicle-status", GET,
                "/v1/vehicle/status"));
        otherOrder.createOrUpdate(RegistryTestUtil.getRoute(VEHICLE_SERVICE, "get-vehicle", GET, VEHICLE_BY_ID));

        assertEquals(List.of("get-vehicle-status", "get-vehicle"),
                otherOrder.list().stream().map(RouteDefinition::getId).toList());
    }

    /**
     * Test purpose          - Verify only the routes with the same structure conflict when one segment is a variable.
     * Test data             - Routes that differ from /v1/vehicle/{id}/owner in one segment, and one that only
     *                         renames the variable.
     * Test expected result  - Only the one that renames the variable is rejected.
     * Test type             - Positive and negative.
     */
    @Test
    void testOnlyOneParameterizedSegmentDoesNotMakeDifferentRoutesConflict() {
        register(VEHICLE_SERVICE, "get-owner", GET, "/v1/vehicle/{id}/owner");

        register(VEHICLE_SERVICE, "get-status", GET, "/v1/vehicle/{id}/status");
        register(VEHICLE_SERVICE, "get-owner-of-123", GET, "/v1/vehicle/123/owner");
        register(VEHICLE_SERVICE, "get-vehicle", GET, "/v1/vehicle/{id}");
        register(VEHICLE_SERVICE, "get-owner-owner", GET, "/v1/vehicle/{id}/owner/{ownerId}");
        RouteConflictException conflict = rejected(INVENTORY_SERVICE, "get-vehicle-owner", GET,
                "/v1/vehicle/{vin}/owner");

        assertEquals(List.of("get-owner"), conflict.getConflictingRouteIds());
        assertEquals(5, repo.size());
    }

    /**
     * Test purpose          - Verify parameter names in any naming convention conflict with the same structure.
     * Test data             - POST /v1/vehicle/{vehicleId} registered first, then the same path with a parameter
     *                         named in another convention.
     * Test expected result  - Every later registration is rejected.
     * Test type             - Negative.
     */
    @ParameterizedTest(name = "parameter {0}")
    @ValueSource(strings = {"vehicle_id", "vehicle-id", "VehicleId", "VEHICLE_ID", "VIN", "vehicle.id", "id1", "v"})
    void testParameterNamesInAnyNamingConventionConflict(String parameterName) {
        register(VEHICLE_SERVICE, "update-vehicle", POST, VEHICLE_BY_ID);

        RouteConflictException conflict = rejected(INVENTORY_SERVICE, "update-other-vehicle", POST,
                "/v1/vehicle/{" + parameterName + "}");

        assertEquals(List.of("update-vehicle"), conflict.getConflictingRouteIds());
    }

    private RouteDefinition register(String serviceName, String routeId, String method, String path) {
        return service.createOrUpdate(RegistryTestUtil.getRoute(serviceName, routeId, method, path));
    }

    private RouteConflictException rejected(String serviceName, String routeId, String method, String path) {
        return assertThrows(RouteConflictException.class, () -> register(serviceName, routeId, method, path));
    }

    private List<String> listedIds() {
        return service.list().stream().map(RouteDefinition::getId).toList();
    }

    private static ApiRouteService newService(InMemoryApiRouteRepo repository) {
        ChecksumProperties checksumProperties = new ChecksumProperties();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        return new ApiRouteService(repository, Optional.of(mock(EventPublisherContext.class)), checksumProperties,
                new RouteChecksumService(checksumProperties, new ObjectMapper()), meterRegistry,
                new RouteConflictValidator(repository, new RouteConflictProperties(), meterRegistry));
    }

    /**
     * Runs the task on several threads that start together, and returns what the tasks threw.
     */
    private static List<Throwable> registerTogether(int threads, IntConsumer task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int replica = 0; replica < threads; replica++) {
                int number = replica;
                futures.add(executor.submit(() -> {
                    start.await();
                    task.accept(number);
                    return null;
                }));
            }
            start.countDown();
            List<Throwable> failures = new ArrayList<>();
            for (Future<?> future : futures) {
                try {
                    future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (ExecutionException ex) {
                    failures.add(ex.getCause());
                } catch (TimeoutException ex) {
                    failures.add(ex);
                }
            }
            return failures;
        } finally {
            executor.shutdownNow();
        }
    }

    @SuppressWarnings("java:S2925")
    private static void pause() {
        try {
            Thread.sleep(SLOW_SAVE_MILLIS);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
