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
import org.eclipse.ecsp.registry.events.data.RateLimitConfigEventData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RateLimitConfigEventStrategy.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitConfigEventStrategyTest {

    @Mock
    private RouteEventThrottler throttler;

    private RateLimitConfigEventStrategy strategy;

    @BeforeEach
    void setUp() {
        // GIVEN: Strategy with mocked throttler
        strategy = new RateLimitConfigEventStrategy(throttler);
    }

    /**
     * Test purpose          - Verify publish sends event via throttler and returns true.
     * Test data             - RateLimitConfigEventData with services and routes.
     * Test expected result  - Throttler sendEvent called, returns true.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishValidEventDataSendViaThrottlerAndReturnTrue() {
        // GIVEN: Valid event data
        RateLimitConfigEventData eventData = new RateLimitConfigEventData(
                List.of("service-1"), List.of("route-1"));

        when(throttler.sendEvent(eventData)).thenReturn(true);

        // WHEN: Publish event
        boolean result = strategy.publish(eventData);

        // THEN: Should delegate to throttler and return true
        assertTrue(result);
        verify(throttler, times(1)).sendEvent(eventData);
    }

    /**
     * Test purpose          - Verify publish returns false when throttler fails.
     * Test data             - RateLimitConfigEventData, throttler returns false.
     * Test expected result  - Returns false.
     * Test type             - Negative.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishThrottlerFailsReturnFalse() {
        // GIVEN: Event data and failing throttler
        RateLimitConfigEventData eventData = new RateLimitConfigEventData(
                List.of("service-1"), List.of("route-1"));

        when(throttler.sendEvent(eventData)).thenReturn(false);

        // WHEN: Publish event
        boolean result = strategy.publish(eventData);

        // THEN: Should return false
        assertFalse(result);
        verify(throttler, times(1)).sendEvent(eventData);
    }

    /**
     * Test purpose          - Verify getEventType returns correct event type.
     * Test data             - None.
     * Test expected result  - Returns RATE_LIMIT_CONFIG_CHANGE.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void getEventTypeCalledReturnRateLimitConfigChange() {
        // GIVEN: Strategy instance

        // WHEN: Get event type
        RouteEventType eventType = strategy.getEventType();

        // THEN: Should return RATE_LIMIT_CONFIG_CHANGE
        assertEquals(RouteEventType.RATE_LIMIT_CONFIG_CHANGE, eventType);
    }

    /**
     * Test purpose          - Verify publish with multiple services and routes.
     * Test data             - RateLimitConfigEventData with 2 services and 3 routes.
     * Test expected result  - Event sent successfully.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishMultipleServicesAndRoutesSendEventSuccessfully() {
        // GIVEN: Event data with multiple services and routes
        List<String> services = List.of("service-1", "service-2");
        List<String> routes = List.of("route-1", "route-2", "route-3");
        RateLimitConfigEventData eventData = new RateLimitConfigEventData(services, routes);

        when(throttler.sendEvent(eventData)).thenReturn(true);

        // WHEN: Publish event
        boolean result = strategy.publish(eventData);

        // THEN: Should send event and return true
        assertTrue(result);
        verify(throttler, times(1)).sendEvent(eventData);
    }

    /**
     * Test purpose          - Verify publish with empty lists.
     * Test data             - RateLimitConfigEventData with empty services and routes.
     * Test expected result  - Event sent successfully.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishEmptyListsSendEventSuccessfully() {
        // GIVEN: Event data with empty lists
        RateLimitConfigEventData eventData = new RateLimitConfigEventData(List.of(), List.of());

        when(throttler.sendEvent(eventData)).thenReturn(true);

        // WHEN: Publish event
        boolean result = strategy.publish(eventData);

        // THEN: Should send event and return true
        assertTrue(result);
        verify(throttler, times(1)).sendEvent(eventData);
    }
}
