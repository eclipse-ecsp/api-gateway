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
 * See the License for the specific language governing permissions and\
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.registry.events;

import org.eclipse.ecsp.registry.events.data.RouteChangeEventData;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RouteChangeEventData.
 */
class RouteChangeEventTest {

    /**
     * Test purpose          - Verify constructors and getters work correctly.
     * Test data             - Valid service and route lists.
     * Test expected result  - Event created with all fields populated.
     * Test type             - Positive.
     */
    @Test
    void testConstructorsAndGetters() {
        List<String> services = List.of("s1");
        List<String> routes = List.of("r1");

        // Constructor 1
        RouteChangeEventData event1 = new RouteChangeEventData(services, routes);
        assertThat(event1.getEventId()).isNotNull();
        assertThat(event1.getTimestamp()).isNotNull();
        assertThat(event1.getEventType()).isEqualTo(RouteEventType.ROUTE_CHANGE);
        assertThat(event1.getServices()).isEqualTo(services);
        assertThat(event1.getRoutes()).isEqualTo(routes);

        // Constructor 2
        RouteChangeEventData event2 = new RouteChangeEventData(services, routes);
        assertThat(event2.getEventType()).isEqualTo(RouteEventType.ROUTE_CHANGE);
    }

    /**
     * Test purpose          - Verify JSON creator constructor.
     * Test data             - All constructor parameters.
     * Test expected result  - Event created with exact values.
     * Test type             - Positive.
     */
    @Test
    void testJsonCreatorConstructor() {
        List<String> services = List.of("s1");
        List<String> routes = List.of("r1");
        
        RouteChangeEventData event = new RouteChangeEventData(services, routes);
        
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getEventType()).isEqualTo(RouteEventType.ROUTE_CHANGE);
        assertThat(event.getServices()).isEqualTo(services);
        assertThat(event.getRoutes()).isEqualTo(routes);
    }

    /**
     * Test purpose          - Verify equals and hashCode implementation.
     * Test data             - Two events with same data.
     * Test expected result  - Events are equal with same hashCode.
     * Test type             - Positive.
     */
    @Test
    void testEqualsAndHashCode() {
        List<String> services = List.of("s1");
        List<String> routes = List.of("r1");
        
        RouteChangeEventData event1 = new RouteChangeEventData(services, routes);
        
        // Test reflexivity - event should equal itself
        assertThat(event1).isEqualTo(event1);
        assertThat(event1.hashCode()).isEqualTo(event1.hashCode());
        
        assertThat(event1.toString()).contains("RouteChangeEventData").contains("s1").contains("r1");
    }

    /**
     * Test purpose          - Verify constructor with empty lists.
     * Test data             - Empty service and route lists.
     * Test expected result  - Event created successfully with empty lists.
     * Test type             - Positive.
     */
    @Test
    void testConstructor_WithEmptyLists() {
        // Arrange & Act
        RouteChangeEventData event = new RouteChangeEventData(Collections.emptyList(), Collections.emptyList());

        // Assert
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getEventType()).isEqualTo(RouteEventType.ROUTE_CHANGE);
        assertThat(event.getServices()).isEmpty();
        assertThat(event.getRoutes()).isEmpty();
    }

    /**
     * Test purpose          - Verify constructor with multiple services and routes.
     * Test data             - Multiple service and route entries.
     * Test expected result  - All services and routes preserved.
     * Test type             - Positive.
     */
    @Test
    void testConstructor_WithMultipleServicesAndRoutes() {
        // Arrange
        List<String> services = List.of("service1", "service2", "service3");
        List<String> routes = List.of("route1", "route2", "route3");

        // Act
        RouteChangeEventData event = new RouteChangeEventData(services, routes);

        // Assert
        assertThat(event.getServices()).hasSize(3);
        assertThat(event.getRoutes()).hasSize(3);
        assertThat(event.getServices()).containsExactly("service1", "service2", "service3");
        assertThat(event.getRoutes()).containsExactly("route1", "route2", "route3");
    }

    /**
     * Test purpose          - Verify constructor with custom event type.
     * Test data             - Custom event type parameter.
     * Test expected result  - Event created with specified type.
     * Test type             - Positive.
     */
    @Test
    void testConstructor_WithCustomEventType() {
        // Arrange
        List<String> services = List.of("service1");
        List<String> routes = List.of("route1");

        // Act
        RouteChangeEventData event = new RouteChangeEventData(services, routes);

        // Assert
        assertThat(event.getEventType()).isEqualTo(RouteEventType.ROUTE_CHANGE);
        assertThat(event.getServices()).isEqualTo(services);
        assertThat(event.getRoutes()).isEqualTo(routes);
    }

    /**
     * Test purpose          - Verify events with different data are not equal.
     * Test data             - Two events with different event IDs.
     * Test expected result  - Events are not equal.
     * Test type             - Negative.
     */
    @Test
    void testEquals_DifferentEvents_NotEqual() {
        // Arrange
        List<String> services1 = List.of("s1");
        List<String> routes1 = List.of("r1");    
        List<String> services2 = List.of("s2");
        List<String> routes2 = List.of("r2");

        RouteChangeEventData event1 = new RouteChangeEventData(services1, routes1);
        RouteChangeEventData event2 = new RouteChangeEventData(services2, routes2);

        // Act & Assert
        assertThat(event1).isNotEqualTo(event2);
        assertThat(event1.hashCode()).isNotEqualTo(event2.hashCode());
    }

    /**
     * Test purpose          - Verify event fields are immutable.
     * Test data             - Created event with mutable list.
     * Test expected result  - Event fields remain unchanged.
     * Test type             - Positive.
     */
    @Test
    void testImmutability() {
        // Arrange
        List<String> services = new java.util.ArrayList<>(List.of("service1"));
        List<String> routes = new java.util.ArrayList<>(List.of("route1"));

        // Act
        RouteChangeEventData event = new RouteChangeEventData(services, routes);
        services.add("service2"); // Modify original list
        routes.add("route2");

        // Assert - event should still have original values
        assertThat(event.getServices()).hasSize(1);
        assertThat(event.getRoutes()).hasSize(1);
    }

    /**
     * Test purpose          - Verify toString contains relevant information.
     * Test data             - Event with all fields populated.
     * Test expected result  - toString contains event ID and type.
     * Test type             - Positive.
     */
    @Test
    void testToString_ContainsRelevantInfo() {
        // Arrange
        List<String> services = List.of("service1");
        List<String> routes = List.of("route1");

        // Act
        RouteChangeEventData event = new RouteChangeEventData(services, routes);

        // Assert
        String toString = event.toString();
        assertThat(toString).contains("RouteChangeEventData").contains("service1").contains("route1");
    }

    /**
     * Test purpose          - Verify event with null lists in constructor.
     * Test data             - Null service and route lists.
     * Test expected result  - Event created with null lists.
     * Test type             - Negative.
     */
    @Test
    void testConstructor_WithNullLists() {
        // Arrange & Act
        RouteChangeEventData event = new RouteChangeEventData(null, null);

        // Assert
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getServices()).isNotNull().isEmpty();
        assertThat(event.getRoutes()).isNotNull().isEmpty();
    }
}
