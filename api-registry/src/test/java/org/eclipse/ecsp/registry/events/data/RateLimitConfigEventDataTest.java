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

package org.eclipse.ecsp.registry.events.data;

import org.eclipse.ecsp.registry.events.RouteEventType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for RateLimitConfigEventData.
 *
 * <p>Tests event data construction and properties.
 */
class RateLimitConfigEventDataTest {

    /**
     * Test purpose          - Verify constructor with services and routes creates instance correctly.
     * Test data             - Lists of services and routes.
     * Test expected result  - Instance created with services and routes.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void constructorWithServicesAndRoutesCreateInstanceCorrectly() {
        // GIVEN: Lists of services and routes
        List<String> services = List.of("service-1", "service-2");
        List<String> routes = List.of("route-1", "route-2", "route-3");

        // WHEN: Create event data
        RateLimitConfigEventData eventData = new RateLimitConfigEventData(services, routes);

        // THEN: Should be created correctly
        assertNotNull(eventData);
        assertNotNull(eventData.getServices());
        assertNotNull(eventData.getRoutes());
        assertEquals(2, eventData.getServices().size());
        assertEquals(3, eventData.getRoutes().size());
        assertEquals("service-1", eventData.getServices().get(0));
        assertEquals("service-2", eventData.getServices().get(1));
        assertEquals("route-1", eventData.getRoutes().get(0));
        assertNotNull(eventData.getEventId());
        assertNotNull(eventData.getTimestamp());
    }

    /**
     * Test purpose          - Verify full constructor for deserialization.
     * Test data             - All fields including eventId and timestamp.
     * Test expected result  - Instance created with all fields.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void constructorFullConstructorCreateInstanceWithAllFields() {
        // GIVEN: All fields
        String eventId = "event-456";
        Instant timestamp = Instant.now();
        List<String> services = List.of("service-1");
        List<String> routes = List.of("route-1", "route-2");

        // WHEN: Create event data using full constructor
        RateLimitConfigEventData eventData = new RateLimitConfigEventData(
                eventId, timestamp, services, routes);

        // THEN: Should be created with all fields
        assertNotNull(eventData);
        assertEquals(1, eventData.getServices().size());
        assertEquals(2, eventData.getRoutes().size());
    }

    /**
     * Test purpose          - Verify getEventType returns correct type.
     * Test data             - RateLimitConfigEventData instance.
     * Test expected result  - Returns RATE_LIMIT_CONFIG_CHANGE.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void getEventTypeCalledReturnRateLimitConfigChange() {
        // GIVEN: Event data
        RateLimitConfigEventData eventData = new RateLimitConfigEventData(
                List.of("service-1"), List.of("route-1"));

        // WHEN: Get event type
        RouteEventType eventType = eventData.getEventType();

        // THEN: Should return RATE_LIMIT_CONFIG_CHANGE
        assertEquals(RouteEventType.RATE_LIMIT_CONFIG_CHANGE, eventType);
    }

    /**
     * Test purpose          - Verify services list is immutable.
     * Test data             - RateLimitConfigEventData with services.
     * Test expected result  - Attempting to modify list throws UnsupportedOperationException.
     * Test type             - Negative.
     *
     * @throws Exception if test fails
     **/
    @Test
    void getServicesAttemptToModifyThrowUnsupportedOperationException() {
        // GIVEN: Event data
        RateLimitConfigEventData eventData = new RateLimitConfigEventData(
                List.of("service-1"), List.of("route-1"));

        // WHEN/THEN: Attempt to modify list should throw exception
        try {
            eventData.getServices().add("service-2");
            assertTrue(false, "Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected exception
            assertNotNull(e);
        }
    }

    /**
     * Test purpose          - Verify routes list is immutable.
     * Test data             - RateLimitConfigEventData with routes.
     * Test expected result  - Attempting to modify list throws UnsupportedOperationException.
     * Test type             - Negative.
     *
     * @throws Exception if test fails
     **/
    @Test
    void getRoutesAttemptToModifyThrowUnsupportedOperationException() {
        // GIVEN: Event data
        RateLimitConfigEventData eventData = new RateLimitConfigEventData(
                List.of("service-1"), List.of("route-1"));

        // WHEN/THEN: Attempt to modify list should throw exception
        try {
            eventData.getRoutes().add("route-2");
            assertTrue(false, "Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected exception
            assertNotNull(e);
        }
    }

    /**
     * Test purpose          - Verify constructor with empty lists.
     * Test data             - Empty lists for services and routes.
     * Test expected result  - Instance created with empty lists.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void constructorEmptyListsCreateInstanceCorrectly() {
        // GIVEN: Empty lists
        List<String> services = List.of();
        List<String> routes = List.of();

        // WHEN: Create event data
        RateLimitConfigEventData eventData = new RateLimitConfigEventData(services, routes);

        // THEN: Should be created with empty lists
        assertNotNull(eventData);
        assertNotNull(eventData.getServices());
        assertNotNull(eventData.getRoutes());
        assertTrue(eventData.getServices().isEmpty());
        assertTrue(eventData.getRoutes().isEmpty());
    }

    /**
     * Test purpose          - Verify constructor with multiple services and routes.
     * Test data             - 5 services and 10 routes.
     * Test expected result  - All services and routes stored correctly.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void constructorMultipleServicesAndRoutesStoreAllCorrectly() {
        // GIVEN: Multiple services and routes
        List<String> services = List.of("service-1", "service-2", "service-3", "service-4", "service-5");
        List<String> routes = List.of("route-1", "route-2", "route-3", "route-4", "route-5",
                "route-6", "route-7", "route-8", "route-9", "route-10");

        // WHEN: Create event data
        RateLimitConfigEventData eventData = new RateLimitConfigEventData(services, routes);

        // THEN: All should be stored
        assertEquals(5, eventData.getServices().size());
        assertEquals(10, eventData.getRoutes().size());
    }

    /**
     * Test purpose          - Verify toString method works correctly.
     * Test data             - RateLimitConfigEventData instance.
     * Test expected result  - toString returns non-null string with data.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void toStringCalledReturnNonNullStringWithData() {
        // GIVEN: Event data
        RateLimitConfigEventData eventData = new RateLimitConfigEventData(
                List.of("service-1"), List.of("route-1"));

        // WHEN: Call toString
        String toString = eventData.toString();

        // THEN: Should return non-null string containing data
        assertNotNull(toString);
        assertTrue(toString.contains("service-1") || toString.contains("route-1") || toString.contains("RateLimitConfig"));
    }

    /**
     * Test purpose          - Verify constructor with only services, no routes.
     * Test data             - Services list and empty routes list.
     * Test expected result  - Instance created correctly.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void constructorOnlyServicesNoRoutesCreateInstanceCorrectly() {
        // GIVEN: Services but no routes
        List<String> services = List.of("service-1", "service-2");
        List<String> routes = List.of();

        // WHEN: Create event data
        RateLimitConfigEventData eventData = new RateLimitConfigEventData(services, routes);

        // THEN: Should be created correctly
        assertEquals(2, eventData.getServices().size());
        assertTrue(eventData.getRoutes().isEmpty());
    }

    /**
     * Test purpose          - Verify constructor with only routes, no services.
     * Test data             - Empty services list and routes list.
     * Test expected result  - Instance created correctly.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void constructorOnlyRoutesNoServicesCreateInstanceCorrectly() {
        // GIVEN: Routes but no services
        List<String> services = List.of();
        List<String> routes = List.of("route-1", "route-2");

        // WHEN: Create event data
        RateLimitConfigEventData eventData = new RateLimitConfigEventData(services, routes);

        // THEN: Should be created correctly
        assertTrue(eventData.getServices().isEmpty());
        assertEquals(2, eventData.getRoutes().size());
    }
}
