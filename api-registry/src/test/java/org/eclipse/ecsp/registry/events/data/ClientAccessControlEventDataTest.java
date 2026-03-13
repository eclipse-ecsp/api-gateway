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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ClientAccessControlEventData.
 *
 * <p>Tests event data construction and properties.
 */
class ClientAccessControlEventDataTest {

    /**
     * Test purpose          - Verify constructor with client IDs creates instance correctly.
     * Test data             - List of 2 client IDs.
     * Test expected result  - Instance created with client IDs, null operation.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void constructorWithClientIdsCreateInstanceCorrectly() {
        // GIVEN: List of client IDs
        List<String> clientIds = List.of("client-1", "client-2");

        // WHEN: Create event data
        ClientAccessControlEventData eventData = new ClientAccessControlEventData(clientIds);

        // THEN: Should be created correctly
        assertNotNull(eventData);
        assertNotNull(eventData.getClientIds());
        assertEquals(2, eventData.getClientIds().size());
        assertEquals("client-1", eventData.getClientIds().get(0));
        assertEquals("client-2", eventData.getClientIds().get(1));
        assertNull(eventData.getOperation());
        assertNotNull(eventData.getEventId());
        assertNotNull(eventData.getTimestamp());
    }

    /**
     * Test purpose          - Verify constructor with client IDs and operation.
     * Test data             - List of client IDs and CREATE operation.
     * Test expected result  - Instance created with client IDs and operation.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void constructorWithClientIdsAndOperationCreateInstanceCorrectly() {
        // GIVEN: List of client IDs and operation
        List<String> clientIds = List.of("client-1");
        String operation = "CREATE";

        // WHEN: Create event data
        ClientAccessControlEventData eventData = new ClientAccessControlEventData(clientIds, operation);

        // THEN: Should be created correctly
        assertNotNull(eventData);
        assertEquals(1, eventData.getClientIds().size());
        assertEquals("client-1", eventData.getClientIds().get(0));
        assertEquals("CREATE", eventData.getOperation());
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
        String eventId = "event-123";
        Instant timestamp = Instant.now();
        List<String> clientIds = List.of("client-1", "client-2", "client-3");
        String operation = "UPDATE";

        // WHEN: Create event data using full constructor
        ClientAccessControlEventData eventData = new ClientAccessControlEventData(
                eventId, timestamp, clientIds, operation);

        // THEN: Should be created with all fields
        assertNotNull(eventData);
        assertEquals(3, eventData.getClientIds().size());
        assertEquals("UPDATE", eventData.getOperation());
    }

    /**
     * Test purpose          - Verify getEventType returns correct type.
     * Test data             - ClientAccessControlEventData instance.
     * Test expected result  - Returns CLIENT_ACCESS_CONTROL_UPDATED.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void getEventTypeCalledReturnClientAccessControlUpdated() {
        // GIVEN: Event data
        ClientAccessControlEventData eventData = new ClientAccessControlEventData(List.of("client-1"));

        // WHEN: Get event type
        RouteEventType eventType = eventData.getEventType();

        // THEN: Should return CLIENT_ACCESS_CONTROL_UPDATED
        assertEquals(RouteEventType.CLIENT_ACCESS_CONTROL_UPDATED, eventType);
    }

    /**
     * Test purpose          - Verify client IDs list is immutable.
     * Test data             - ClientAccessControlEventData with client IDs.
     * Test expected result  - Attempting to modify list throws UnsupportedOperationException.
     * Test type             - Negative.
     *
     * @throws Exception if test fails
     **/
    @Test
    void getClientIdsAttemptToModifyThrowUnsupportedOperationException() {
        // GIVEN: Event data
        ClientAccessControlEventData eventData = new ClientAccessControlEventData(List.of("client-1"));

        // WHEN/THEN: Attempt to modify list should throw exception
        try {
            eventData.getClientIds().add("client-2");
            assertTrue(false, "Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected exception
            assertNotNull(e);
        }
    }

    /**
     * Test purpose          - Verify constructor with empty client ID list.
     * Test data             - Empty list of client IDs.
     * Test expected result  - Instance created with empty list.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void constructorEmptyClientIdListCreateInstanceCorrectly() {
        // GIVEN: Empty client ID list
        List<String> clientIds = List.of();

        // WHEN: Create event data
        ClientAccessControlEventData eventData = new ClientAccessControlEventData(clientIds);

        // THEN: Should be created with empty list
        assertNotNull(eventData);
        assertNotNull(eventData.getClientIds());
        assertTrue(eventData.getClientIds().isEmpty());
    }

    /**
     * Test purpose          - Verify constructor with null operation.
     * Test data             - Client IDs and null operation.
     * Test expected result  - Instance created with null operation.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void constructorNullOperationCreateInstanceCorrectly() {
        // GIVEN: Client IDs and null operation
        List<String> clientIds = List.of("client-1");

        // WHEN: Create event data with null operation
        ClientAccessControlEventData eventData = new ClientAccessControlEventData(clientIds, null);

        // THEN: Should have null operation
        assertNotNull(eventData);
        assertNull(eventData.getOperation());
    }

    /**
     * Test purpose          - Verify toString and equals methods work.
     * Test data             - Two event data instances with same client IDs.
     * Test expected result  - toString returns non-null, equals works.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void toStringAndEqualsTwoInstancesWorkCorrectly() {
        // GIVEN: Two event data instances
        List<String> clientIds = List.of("client-1");
        ClientAccessControlEventData eventData1 = new ClientAccessControlEventData(clientIds, "CREATE");
        ClientAccessControlEventData eventData2 = new ClientAccessControlEventData(clientIds, "CREATE");

        // WHEN: Call toString
        String toString = eventData1.toString();

        // THEN: toString should not be null
        assertNotNull(toString);
        assertTrue(toString.contains("client-1"));
        
        // AND: Two instances with same values should be equal
        assertEquals(eventData1, eventData2);
    }
}
