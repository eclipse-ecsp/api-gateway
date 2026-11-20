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

package org.eclipse.ecsp.registry.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.registry.config.ChecksumProperties;
import org.eclipse.ecsp.registry.config.GlobalExceptionHandler;
import org.eclipse.ecsp.registry.config.RouteConflictProperties;
import org.eclipse.ecsp.registry.repo.InMemoryApiRouteRepo;
import org.eclipse.ecsp.registry.service.ApiRouteService;
import org.eclipse.ecsp.registry.service.RouteChecksumService;
import org.eclipse.ecsp.registry.service.RouteConflictValidator;
import org.eclipse.ecsp.registry.utils.RegistryTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the HTTP answers of route registration when routes conflict: the real controller, the real
 * exception handler and the real service over an in-memory repository.
 */
class RouteRegistrationConflictHttpTest {

    private static final String ROUTES = "/api/v1/routes";
    private static final String VEHICLE_SERVICE = "vehicle-service";
    private static final String INVENTORY_SERVICE = "inventory-service";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InMemoryApiRouteRepo repo = new InMemoryApiRouteRepo();
        ChecksumProperties checksumProperties = new ChecksumProperties();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ApiRouteService service = new ApiRouteService(repo, Optional.empty(), checksumProperties,
                new RouteChecksumService(checksumProperties, objectMapper), meterRegistry,
                new RouteConflictValidator(repo, new RouteConflictProperties(), meterRegistry));
        mockMvc = MockMvcBuilders.standaloneSetup(new ApiRouteController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    /**
     * Test purpose          - Verify a conflicting registration is answered with 409 and names the owner.
     * Test data             - Two services that register POST /v1/vehicle/{vehicleId} and POST /v1/vehicle/{vin}.
     * Test expected result  - 201 for the first, 409 for the second with the conflicting route id and a message.
     * Test type             - Negative.
     */
    @Test
    void testConflictingRegistrationIsAnsweredWithConflict() throws Exception {
        register(VEHICLE_SERVICE, "update-vehicle", "POST", "/v1/vehicle/{vehicleId}").andExpect(status().isCreated());

        register(INVENTORY_SERVICE, "update-inventory-vehicle", "POST", "/v1/vehicle/{vin}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.duplicates[0]").value("update-vehicle"))
                .andExpect(jsonPath("$.message", containsString("update-inventory-vehicle")))
                .andExpect(jsonPath("$.message", containsString("POST /v1/vehicle/{vin}")));
    }

    /**
     * Test purpose          - Verify the same API registered again by a replica is no conflict.
     * Test data             - The same service, route id, method and path registered twice.
     * Test expected result  - 201 both times.
     * Test type             - Positive.
     */
    @Test
    void testReplicaOfTheSameServiceIsNoConflict() throws Exception {
        register(VEHICLE_SERVICE, "update-vehicle", "POST", "/v1/vehicle/{vehicleId}").andExpect(status().isCreated());

        register(VEHICLE_SERVICE, "update-vehicle", "POST", "/v1/vehicle/{vehicleId}").andExpect(status().isCreated());
    }

    /**
     * Test purpose          - Verify routes that differ in method or in a static segment are accepted and listed in
     *                         route order.
     * Test data             - POST and GET for one parameterized path, and a static path next to it.
     * Test expected result  - 201 for all, and the static path is listed before the parameterized path.
     * Test type             - Positive.
     */
    @Test
    void testRoutesThatDoNotConflictAreAcceptedAndListedInRouteOrder() throws Exception {
        register(VEHICLE_SERVICE, "get-vehicle", "GET", "/v1/vehicle/{vehicleId}").andExpect(status().isCreated());
        register(INVENTORY_SERVICE, "create-vehicle", "POST", "/v1/vehicle/{vin}").andExpect(status().isCreated());
        register(INVENTORY_SERVICE, "get-vehicle-status", "GET", "/v1/vehicle/status")
                .andExpect(status().isCreated());

        mockMvc.perform(get(ROUTES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("get-vehicle-status"))
                .andExpect(jsonPath("$.length()").value(3));
    }

    private ResultActions register(String serviceName, String routeId, String method, String path)
            throws Exception {
        RouteDefinition route = RegistryTestUtil.getRoute(serviceName, routeId, method, path);
        return mockMvc.perform(post(ROUTES).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(route)));
    }
}
