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
import org.eclipse.ecsp.registry.events.data.ServiceHealthEventData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ServiceHealthEventStrategy.
 */
@ExtendWith(MockitoExtension.class)
class ServiceHealthEventStrategyTest {

    @Mock
    private RouteEventThrottler throttler;

    private ServiceHealthEventStrategy strategy;

    @BeforeEach
    void setUp() {
        // GIVEN: Strategy with mocked throttler
        strategy = new ServiceHealthEventStrategy(throttler);
    }

    /**
     * Test purpose          - Verify publish sends event via throttler and returns true.
     * Test data             - ServiceHealthEventData with services.
     * Test expected result  - Throttler sendEvent called, returns true.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishValidEventDataSendViaThrottlerAndReturnTrue() {
        // GIVEN: Valid event data
        ServiceHealthEventData eventData = new ServiceHealthEventData(
                List.of("service-1"),
                Map.of("service-1", "UP"));

        when(throttler.sendEvent(eventData)).thenReturn(true);

        // WHEN: Publish event
        boolean result = strategy.publish(eventData);

        // THEN: Should delegate to throttler and return true
        assertTrue(result);
        verify(throttler, times(1)).sendEvent(eventData);
    }

    /**
     * Test purpose          - Verify publish returns false when throttler fails.
     * Test data             - ServiceHealthEventData, throttler returns false.
     * Test expected result  - Returns false.
     * Test type             - Negative.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishThrottlerFailsReturnFalse() {
        // GIVEN: Event data and failing throttler
        ServiceHealthEventData eventData = new ServiceHealthEventData(
                List.of("service-1"),
                Map.of("service-1", "DOWN"));

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
     * Test expected result  - Returns SERVICE_HEALTH_CHANGE.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void getEventTypeCalledReturnServiceHealthChange() {
        // GIVEN: Strategy instance

        // WHEN: Get event type
        RouteEventType eventType = strategy.getEventType();

        // THEN: Should return SERVICE_HEALTH_CHANGE
        assertEquals(RouteEventType.SERVICE_HEALTH_CHANGE, eventType);
    }

    /**
     * Test purpose          - Verify publish with multiple services.
     * Test data             - ServiceHealthEventData with 3 services.
     * Test expected result  - Event sent with all services.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishMultipleServicesSendEventWithAllServices() {
        // GIVEN: Event data with multiple services
        List<String> services = List.of("service-1", "service-2", "service-3");
        Map<String, String> healthStatuses = Map.of(
                "service-1", "UP",
                "service-2", "UP",
                "service-3", "DEGRADED");
        ServiceHealthEventData eventData = new ServiceHealthEventData(services, healthStatuses);

        when(throttler.sendEvent(eventData)).thenReturn(true);

        // WHEN: Publish event
        boolean result = strategy.publish(eventData);

        // THEN: Should send event and return true
        assertTrue(result);
        verify(throttler, times(1)).sendEvent(eventData);
    }

    /**
     * Test purpose          - Verify publish with empty service list.
     * Test data             - ServiceHealthEventData with empty list.
     * Test expected result  - Event sent successfully.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishEmptyServiceListSendEventSuccessfully() {
        // GIVEN: Event data with empty service list
        ServiceHealthEventData eventData = new ServiceHealthEventData(
                List.of(),
                Map.of());

        when(throttler.sendEvent(eventData)).thenReturn(true);

        // WHEN: Publish event
        boolean result = strategy.publish(eventData);

        // THEN: Should send event and return true
        assertTrue(result);
        verify(throttler, times(1)).sendEvent(eventData);
    }
}
