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

package org.eclipse.ecsp.registry.events;

import org.eclipse.ecsp.registry.events.data.AbstractEventData;
import org.eclipse.ecsp.registry.events.data.ClientAccessControlEventData;
import org.eclipse.ecsp.registry.events.data.RateLimitConfigEventData;
import org.eclipse.ecsp.registry.events.data.RouteChangeEventData;
import org.eclipse.ecsp.registry.events.strategy.EventPublishingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EventPublisherContext.
 *
 * <p>Tests strategy pattern implementation for event publishing.
 */
@ExtendWith(MockitoExtension.class)
class EventPublisherContextTest {

    private static final String CLIENT_1 = "client-1";

    @Mock
    private EventPublishingStrategy<ClientAccessControlEventData> clientAccessControlStrategy;

    @Mock
    private EventPublishingStrategy<RateLimitConfigEventData> rateLimitConfigStrategy;

    @Mock
    private EventPublishingStrategy<RouteChangeEventData> routeChangeStrategy;

    private EventPublisherContext context;

    @BeforeEach
    void setUp() {
        // GIVEN: Mock strategies with different event types
        when(clientAccessControlStrategy.getEventType()).thenReturn(RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED);
        when(rateLimitConfigStrategy.getEventType()).thenReturn(RouteEventType.RATE_LIMIT_CONFIG_CHANGE);
        when(routeChangeStrategy.getEventType()).thenReturn(RouteEventType.ROUTE_CHANGE);

        List<EventPublishingStrategy<? extends AbstractEventData>> strategies = List.of(
                clientAccessControlStrategy,
                rateLimitConfigStrategy,
                routeChangeStrategy
        );

        context = new EventPublisherContext(strategies);
    }

    /**
     * Test purpose          - Verify publishEvent routes to correct strategy and returns success.
     * Test data             - ClientAccessControlEventData with client IDs.
     * Test expected result  - Correct strategy called, returns true.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishEventClientAccessControlEventRouteToCorrectStrategyAndReturnTrue() {
        // GIVEN: Client access control event data
        ClientAccessControlEventData eventData = new ClientAccessControlEventData(List.of(CLIENT_1));

        when(clientAccessControlStrategy.publish(eventData)).thenReturn(true);

        // WHEN: Publish event
        boolean result = context.publishEvent(eventData);

        // THEN: Should route to client access control strategy and return true
        assertTrue(result);
        verify(clientAccessControlStrategy, times(1)).publish(eventData);
    }

    /**
     * Test purpose          - Verify publishEvent routes to correct strategy for rate limit events.
     * Test data             - RateLimitConfigEventData with services and routes.
     * Test expected result  - Correct strategy called, returns true.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishEventRateLimitConfigEventRouteToCorrectStrategyAndReturnTrue() {
        // GIVEN: Rate limit config event data
        RateLimitConfigEventData eventData = new RateLimitConfigEventData(
                List.of("service-1"), List.of("route-1"));

        when(rateLimitConfigStrategy.publish(eventData)).thenReturn(true);

        // WHEN: Publish event
        boolean result = context.publishEvent(eventData);

        // THEN: Should route to rate limit config strategy and return true
        assertTrue(result);
        verify(rateLimitConfigStrategy, times(1)).publish(eventData);
    }

    /**
     * Test purpose          - Verify publishEvent routes to correct strategy for route change events.
     * Test data             - RouteChangeEventData with services.
     * Test expected result  - Correct strategy called, returns true.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishEventRouteChangeEventRouteToCorrectStrategyAndReturnTrue() {
        // GIVEN: Route change event data
        RouteChangeEventData eventData = new RouteChangeEventData(
                List.of("service-1"), List.of());

        when(routeChangeStrategy.publish(eventData)).thenReturn(true);

        // WHEN: Publish event
        boolean result = context.publishEvent(eventData);

        // THEN: Should route to route change strategy and return true
        assertTrue(result);
        verify(routeChangeStrategy, times(1)).publish(eventData);
    }

    /**
     * Test purpose          - Verify publishEvent returns false when strategy fails.
     * Test data             - ClientAccessControlEventData.
     * Test expected result  - Returns false when strategy publish fails.
     * Test type             - Negative.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishEventStrategyPublishFailsReturnFalse() {
        // GIVEN: Event data and strategy that fails
        ClientAccessControlEventData eventData = new ClientAccessControlEventData(List.of(CLIENT_1));

        when(clientAccessControlStrategy.publish(eventData)).thenReturn(false);

        // WHEN: Publish event
        boolean result = context.publishEvent(eventData);

        // THEN: Should return false
        assertFalse(result);
        verify(clientAccessControlStrategy, times(1)).publish(eventData);
    }

    /**
     * Test purpose          - Verify publishEvent throws exception for unsupported event type.
     * Test data             - Event data with unknown event type.
     * Test expected result  - IllegalArgumentException thrown.
     * Test type             - Negative.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishEventUnsupportedEventTypeThrowIllegalArgumentException() {
        // GIVEN: Event data with unsupported event type
        AbstractEventData eventData = new AbstractEventData() {
            @Override
            public RouteEventType getEventType() {
                return RouteEventType.SERVICE_HEALTH_CHANGE; // Not registered
            }
        };

        // WHEN/THEN: Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> context.publishEvent(eventData)
        );

        assertTrue(exception.getMessage().contains("No strategy for event type"));
        assertTrue(exception.getMessage().contains("SERVICE_HEALTH_CHANGE"));
    }

    /**
     * Test purpose          - Verify constructor initializes strategies correctly.
     * Test data             - Empty strategy list.
     * Test expected result  - Context created, publishEvent throws for any event.
     * Test type             - Negative.
     *
     * @throws Exception if test fails
     **/
    @Test
    void constructorEmptyStrategyListThrowsExceptionOnPublish() {
        // GIVEN: Empty strategy list
        EventPublisherContext emptyContext = new EventPublisherContext(List.of());

        ClientAccessControlEventData eventData = new ClientAccessControlEventData(List.of(CLIENT_1));

        // WHEN/THEN: Should throw IllegalArgumentException when publishing
        assertThrows(IllegalArgumentException.class, () -> emptyContext.publishEvent(eventData));
    }

    /**
     * Test purpose          - Verify publishEvent works with multiple client IDs.
     * Test data             - ClientAccessControlEventData with 3 client IDs.
     * Test expected result  - Strategy called with correct data, returns true.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void publishEventMultipleClientIdsStrategyCalledWithCorrectData() {
        // GIVEN: Event data with multiple client IDs
        List<String> clientIds = List.of(CLIENT_1, "client-2", "client-3");
        ClientAccessControlEventData eventData = new ClientAccessControlEventData(clientIds);

        when(clientAccessControlStrategy.publish(eventData)).thenReturn(true);

        // WHEN: Publish event
        boolean result = context.publishEvent(eventData);

        // THEN: Should succeed and strategy called once
        assertTrue(result);
        verify(clientAccessControlStrategy, times(1)).publish(eventData);
    }
}
