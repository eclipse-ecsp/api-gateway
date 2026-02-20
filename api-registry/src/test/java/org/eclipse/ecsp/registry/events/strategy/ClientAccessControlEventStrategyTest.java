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
import org.eclipse.ecsp.registry.events.data.ClientAccessControlEventData;
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
 * Unit tests for ClientAccessControlEventStrategy.
 */
@ExtendWith(MockitoExtension.class)
class ClientAccessControlEventStrategyTest {

    @Mock
    private RouteEventThrottler throttler;

    private ClientAccessControlEventStrategy strategy;

    @BeforeEach
    void setUp() {
        // GIVEN: Strategy with mocked throttler
        strategy = new ClientAccessControlEventStrategy(throttler);
    }

    /**
     * Test purpose          - Verify publish sends event via throttler and returns true.
     * Test data             - ClientAccessControlEventData with one client ID.
     * Test expected result  - Throttler sendEvent called, returns true.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publish_ValidEventData_SendViaThrottlerAndReturnTrue() {
        // GIVEN: Valid event data
        ClientAccessControlEventData eventData = new ClientAccessControlEventData(List.of("client-1"));

        when(throttler.sendEvent(eventData)).thenReturn(true);

        // WHEN: Publish event
        boolean result = strategy.publish(eventData);

        // THEN: Should delegate to throttler and return true
        assertTrue(result);
        verify(throttler, times(1)).sendEvent(eventData);
    }

    /**
     * Test purpose          - Verify publish returns false when throttler fails.
     * Test data             - ClientAccessControlEventData, throttler returns false.
     * Test expected result  - Returns false.
     * Test type             - Negative.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publish_ThrottlerFails_ReturnFalse() {
        // GIVEN: Event data and failing throttler
        ClientAccessControlEventData eventData = new ClientAccessControlEventData(List.of("client-1"));

        when(throttler.sendEvent(eventData)).thenReturn(false);

        // WHEN: Publish event
        boolean result = strategy.publish(eventData);

        // THEN: Should return false
        assertFalse(result);
        verify(throttler, times(1)).sendEvent(eventData);
    }

    /**
     * Test purpose          - Verify publish handles multiple client IDs.
     * Test data             - ClientAccessControlEventData with 3 client IDs.
     * Test expected result  - Event sent with all client IDs.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publish_MultipleClientIds_SendEventWithAllIds() {
        // GIVEN: Event data with multiple client IDs
        List<String> clientIds = List.of("client-1", "client-2", "client-3");
        ClientAccessControlEventData eventData = new ClientAccessControlEventData(clientIds);

        when(throttler.sendEvent(eventData)).thenReturn(true);

        // WHEN: Publish event
        boolean result = strategy.publish(eventData);

        // THEN: Should send event and return true
        assertTrue(result);
        verify(throttler, times(1)).sendEvent(eventData);
    }

    /**
     * Test purpose          - Verify getEventType returns correct event type.
     * Test data             - None.
     * Test expected result  - Returns CLIENT_ACCESS_CONTROL_UPDATED.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void getEventType_Called_ReturnClientAccessControlUpdated() {
        // GIVEN: Strategy instance

        // WHEN: Get event type
        RouteEventType eventType = strategy.getEventType();

        // THEN: Should return CLIENT_ACCESS_CONTROL_UPDATED
        assertEquals(RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED, eventType);
    }

    /**
     * Test purpose          - Verify publish with empty client ID list.
     * Test data             - ClientAccessControlEventData with empty list.
     * Test expected result  - Throttler called, returns true.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publish_EmptyClientIdList_SendEventSuccessfully() {
        // GIVEN: Event data with empty client ID list
        ClientAccessControlEventData eventData = new ClientAccessControlEventData(List.of());

        when(throttler.sendEvent(eventData)).thenReturn(true);

        // WHEN: Publish event
        boolean result = strategy.publish(eventData);

        // THEN: Should send event and return true
        assertTrue(result);
        verify(throttler, times(1)).sendEvent(eventData);
    }
}
