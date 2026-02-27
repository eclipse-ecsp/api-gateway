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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test purpose    - Verify NoOpsEventPublisher behavior.
 * Test data       - Various event data.
 * Test expected   - Events are logged but not published.
 * Test type       - Positive.
 */
class NoOpsEventPublisherTest {

    private NoOpsEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new NoOpsEventPublisher();
    }

    /**
     * Test purpose          - Verify publishEvent returns true without publishing.
     * Test data             - Valid event data.
     * Test expected result  - Returns true.
     * Test type             - Positive.
     */
    @Test
    void publishEventValidEventDataReturnsTrue() {
        // GIVEN: Valid event data
        TestEventData eventData = new TestEventData("test-event-1", RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED);

        // WHEN: Event is published
        boolean result = publisher.publishEvent(eventData);

        // THEN: Should return true (no-op success)
        assertTrue(result);
    }

    /**
     * Test purpose          - Verify publishEvent handles null event ID.
     * Test data             - Event with null ID.
     * Test expected result  - Returns true.
     * Test type             - Negative.
     */
    @Test
    void publishEventNullEventIdReturnsTrue() {
        // GIVEN: Event with null ID
        TestEventData eventData = new TestEventData(null, RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED);

        // WHEN: Event is published
        boolean result = publisher.publishEvent(eventData);

        // THEN: Should return true (no-op)
        assertTrue(result);
    }

    /**
     * Test purpose          - Verify publishEvent handles multiple events.
     * Test data             - Multiple event data instances.
     * Test expected result  - All return true.
     * Test type             - Positive.
     */
    @Test
    void publishEventMultipleEventsAllReturnTrue() {
        // GIVEN: Multiple events
        TestEventData event1 = new TestEventData("event-1", RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED);
        TestEventData event2 = new TestEventData("event-2", RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED);

        // WHEN: Events are published
        boolean result1 = publisher.publishEvent(event1);
        boolean result2 = publisher.publishEvent(event2);

        // THEN: All should return true
        assertTrue(result1);
        assertTrue(result2);
    }

    /**
     * Test event data class for testing.
     */
    private static class TestEventData extends AbstractEventData {
        private final RouteEventType eventType;
        private final String eventId;
        
        public TestEventData(String eventId, RouteEventType eventType) {
            super();
            this.eventId = eventId;
            this.eventType = eventType;
        }

        @Override
        public RouteEventType getEventType() {
            return eventType;
        }

        @Override
        public String getEventId() {
            return eventId;
        }
    }
}
