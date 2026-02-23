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

package org.eclipse.ecsp.registry.events.strategy;

import org.eclipse.ecsp.registry.events.RouteEventThrottler;
import org.eclipse.ecsp.registry.events.RouteEventType;
import org.eclipse.ecsp.registry.events.data.RouteChangeEventData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for RouteChangeEventStrategy.
 */
@ExtendWith(MockitoExtension.class)
class RouteChangeEventStrategyTest {

    @Mock
    private RouteEventThrottler throttler;

    private RouteChangeEventStrategy strategy;

    @BeforeEach
    void setUp() {
        // GIVEN: Strategy with mocked throttler
        strategy = new RouteChangeEventStrategy(throttler);
    }

    /**
     * Test purpose          - Verify publish schedules events for all services.
     * Test data             - RouteChangeEventData with 2 services.
     * Test expected result  - Throttler scheduleEvent called for each service, returns true.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishTwoServicesScheduleEventForEachService() {
        // GIVEN: Event data with 2 services
        List<String> services = List.of("service-1", "service-2");
        RouteChangeEventData eventData = new RouteChangeEventData(services, List.of());

        // WHEN: Publish event
        boolean result = strategy.publish(eventData);

        // THEN: Should schedule events for both services and return true
        assertTrue(result);
        verify(throttler, times(1)).scheduleEvent("service-1");
        verify(throttler, times(1)).scheduleEvent("service-2");
    }

    /**
     * Test purpose          - Verify publish with single service schedules correctly.
     * Test data             - RouteChangeEventData with 1 service.
     * Test expected result  - Throttler scheduleEvent called once.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishSingleServiceScheduleEventOnce() {
        // GIVEN: Event data with single service
        RouteChangeEventData eventData = new RouteChangeEventData(List.of("service-1"), List.of());

        // WHEN: Publish event
        boolean result = strategy.publish(eventData);

        // THEN: Should schedule event once and return true
        assertTrue(result);
        verify(throttler, times(1)).scheduleEvent("service-1");
    }

    /**
     * Test purpose          - Verify publish with no services returns true without scheduling.
     * Test data             - RouteChangeEventData with empty services list.
     * Test expected result  - Returns true, no scheduleEvent calls.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishNoServicesReturnTrueWithoutScheduling() {
        // GIVEN: Event data with no services
        RouteChangeEventData eventData = new RouteChangeEventData(List.of(), List.of());

        // WHEN: Publish event
        boolean result = strategy.publish(eventData);

        // THEN: Should return true without calling scheduleEvent
        assertTrue(result);
        verify(throttler, times(0)).scheduleEvent(org.mockito.ArgumentMatchers.anyString());
    }

    /**
     * Test purpose          - Verify getEventType returns correct event type.
     * Test data             - None.
     * Test expected result  - Returns ROUTE_CHANGE.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void getEventTypeCalledReturnRouteChange() {
        // GIVEN: Strategy instance

        // WHEN: Get event type
        RouteEventType eventType = strategy.getEventType();

        // THEN: Should return ROUTE_CHANGE
        assertEquals(RouteEventType.ROUTE_CHANGE, eventType);
    }

    /**
     * Test purpose          - Verify publish with multiple services schedules all.
     * Test data             - RouteChangeEventData with 5 services.
     * Test expected result  - Throttler scheduleEvent called 5 times.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishMultipleServicesScheduleAllServices() {
        // GIVEN: Event data with 5 services
        List<String> services = List.of("service-1", "service-2", "service-3", "service-4", "service-5");
        RouteChangeEventData eventData = new RouteChangeEventData(services, List.of());

        // WHEN: Publish event
        boolean result = strategy.publish(eventData);

        // THEN: Should schedule event for all services
        assertTrue(result);
        verify(throttler, times(1)).scheduleEvent("service-1");
        verify(throttler, times(1)).scheduleEvent("service-2");
        verify(throttler, times(1)).scheduleEvent("service-3");
        verify(throttler, times(1)).scheduleEvent("service-4");
        verify(throttler, times(1)).scheduleEvent("service-5");
    }

    /**
     * Test purpose          - Verify publish always returns true (throttling strategy).
     * Test data             - RouteChangeEventData with services.
     * Test expected result  - Always returns true regardless of throttler behavior.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishValidEventDataAlwaysReturnTrue() {
        // GIVEN: Event data
        RouteChangeEventData eventData = new RouteChangeEventData(List.of("service-1"), List.of());

        // WHEN: Publish event
        boolean result = strategy.publish(eventData);

        // THEN: Should always return true (throttling strategy)
        assertTrue(result);
    }
}
