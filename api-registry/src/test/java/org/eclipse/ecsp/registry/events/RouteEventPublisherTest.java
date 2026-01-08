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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for RouteEventPublisher.
 */
@ExtendWith(MockitoExtension.class)
class RouteEventPublisherTest {

    private static final int TWO = 2;
    private static final int THREE = 3;
    private static final int FOUR = 4;

    @Mock
    private RouteEventThrottler throttler;

    private RouteEventPublisher publisher;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        publisher = new RouteEventPublisher(throttler, meterRegistry);
    }

    @Test
    void testPublishRouteChangeEvent_DelegatesToThrottler() {
        // Arrange
        String serviceName = "test-service";

        // Act
        publisher.publishRouteChangeEvent(serviceName);

        // Assert
        verify(throttler, times(1)).scheduleEvent(serviceName);
    }

    @Test
    void testPublishRouteChangeEvent_MultipleServices() {
        // Arrange
        String service1 = "service-1";
        String service2 = "service-2";
        String service3 = "service-3";

        // Act
        publisher.publishRouteChangeEvent(service1);
        publisher.publishRouteChangeEvent(service2);
        publisher.publishRouteChangeEvent(service3);

        // Assert
        verify(throttler, times(1)).scheduleEvent(service1);
        verify(throttler, times(1)).scheduleEvent(service2);
        verify(throttler, times(1)).scheduleEvent(service3);
    }

    @Test
    void testPublishRouteChangeEvent_SameServiceMultipleTimes() {
        // Arrange
        String serviceName = "test-service";

        // Act
        publisher.publishRouteChangeEvent(serviceName);
        publisher.publishRouteChangeEvent(serviceName);
        publisher.publishRouteChangeEvent(serviceName);

        // Assert - each call should be delegated
        verify(throttler, times(THREE)).scheduleEvent(serviceName);
    }

    @Test
    void testPublishRouteChangeEvent_IncrementsMetric() {
        // Arrange
        String serviceName = "test-service";
        Counter counter = meterRegistry.counter("route.events.published.total", 
                "event_type", RouteEventType.ROUTE_CHANGE.name());
        double initialCount = counter.count();

        // Act
        publisher.publishRouteChangeEvent(serviceName);

        // Assert
        assertThat(counter.count()).isEqualTo(initialCount + 1);
    }

    @Test
    void testPublishRouteChangeEvent_NullServiceName_DoesNotIncrementMetric() {
        // Arrange
        Counter counter = meterRegistry.counter("route.events.published.total", 
                "event_type", RouteEventType.ROUTE_CHANGE.name());
        double initialCount = counter.count();

        // Act
        publisher.publishRouteChangeEvent(null);

        // Assert - metric should not be incremented
        assertThat(counter.count()).isEqualTo(initialCount);
    }

    @Test
    void testPublishRouteChangeEvent_EmptyServiceName_DoesNotIncrementMetric() {
        // Arrange
        Counter counter = meterRegistry.counter("route.events.published.total", 
                "event_type", RouteEventType.ROUTE_CHANGE.name());
        double initialCount = counter.count();

        // Act
        publisher.publishRouteChangeEvent("");

        // Assert - metric should not be incremented
        assertThat(counter.count()).isEqualTo(initialCount);
    }

    @Test
    void testPublishRateLimitConfigChangeEvent_IncrementsMetric() {
        // Arrange
        List<String> serviceNames = List.of("service-1");
        List<String> routeIds = List.of("route-1");
        Counter counter = meterRegistry.counter("route.events.published.total", 
                "event_type", RouteEventType.RATE_LIMIT_CONFIG_CHANGE.name());
        double initialCount = counter.count();

        // Act
        publisher.publishRateLimitConfigChangeEvent(serviceNames, routeIds);

        // Assert
        assertThat(counter.count()).isEqualTo(initialCount + 1);
        verify(throttler, times(1)).sendEvent(RouteEventType.RATE_LIMIT_CONFIG_CHANGE, serviceNames, routeIds);
    }

    @Test
    void testPublishRateLimitConfigChangeEvent_NullServiceNames_DoesNotIncrementMetric() {
        // Arrange
        List<String> routeIds = List.of("route-1");
        Counter counter = meterRegistry.counter("route.events.published.total", 
                "event_type", RouteEventType.RATE_LIMIT_CONFIG_CHANGE.name());
        double initialCount = counter.count();

        // Act
        publisher.publishRateLimitConfigChangeEvent(null, routeIds);

        // Assert - metric should not be incremented
        assertThat(counter.count()).isEqualTo(initialCount);
    }

    @Test
    void testPublishRateLimitConfigChangeEvent_EmptyRouteIds_DoesNotIncrementMetric() {
        // Arrange
        List<String> serviceNames = List.of("service-1");
        Counter counter = meterRegistry.counter("route.events.published.total", 
                "event_type", RouteEventType.RATE_LIMIT_CONFIG_CHANGE.name());
        double initialCount = counter.count();

        // Act
        publisher.publishRateLimitConfigChangeEvent(serviceNames, Collections.emptyList());

        // Assert - metric should not be incremented
        assertThat(counter.count()).isEqualTo(initialCount);
    }

    @Test
    void testPublishServiceHealthChangeEvent_IncrementsMetric() {
        // Arrange
        List<String> serviceNames = List.of("service-1", "service-2");
        Counter counter = meterRegistry.counter("route.events.published.total", 
                "event_type", RouteEventType.SERVICE_HEALTH_CHANGE.name());
        double initialCount = counter.count();

        // Act
        publisher.publishServiceHealthChangeEvent(serviceNames);

        // Assert
        assertThat(counter.count()).isEqualTo(initialCount + 1);
        verify(throttler, times(1)).sendEvent(RouteEventType.SERVICE_HEALTH_CHANGE, 
                serviceNames, Collections.emptyList());
    }

    @Test
    void testPublishServiceHealthChangeEvent_NullServiceNames_DoesNotIncrementMetric() {
        // Arrange
        Counter counter = meterRegistry.counter("route.events.published.total", 
                "event_type", RouteEventType.SERVICE_HEALTH_CHANGE.name());
        double initialCount = counter.count();

        // Act
        publisher.publishServiceHealthChangeEvent(null);

        // Assert - metric should not be incremented
        assertThat(counter.count()).isEqualTo(initialCount);
    }

    @Test
    void testPublishServiceHealthChangeEvent_EmptyServiceNames_DoesNotIncrementMetric() {
        // Arrange
        Counter counter = meterRegistry.counter("route.events.published.total", 
                "event_type", RouteEventType.SERVICE_HEALTH_CHANGE.name());
        double initialCount = counter.count();

        // Act
        publisher.publishServiceHealthChangeEvent(Collections.emptyList());

        // Assert - metric should not be incremented
        assertThat(counter.count()).isEqualTo(initialCount);
    }

    @Test
    void testMultipleEventTypes_TrackSeparateMetrics() {
        // Arrange
        Counter routeChangeCounter = meterRegistry.counter("route.events.published.total", 
                "event_type", RouteEventType.ROUTE_CHANGE.name());
        Counter rateLimitCounter = meterRegistry.counter("route.events.published.total", 
                "event_type", RouteEventType.RATE_LIMIT_CONFIG_CHANGE.name());
        Counter healthCounter = meterRegistry.counter("route.events.published.total", 
                "event_type", RouteEventType.SERVICE_HEALTH_CHANGE.name());

        final double routeChangeInitial = routeChangeCounter.count();
        final double rateLimitInitial = rateLimitCounter.count();
        final double healthInitial = healthCounter.count();

        // Act
        publisher.publishRouteChangeEvent("service-1");
        publisher.publishRouteChangeEvent("service-2");
        publisher.publishRateLimitConfigChangeEvent(List.of("service-1"), List.of("route-1"));
        publisher.publishServiceHealthChangeEvent(List.of("service-1", "service-2"));
        publisher.publishServiceHealthChangeEvent(List.of("service-3"));

        // Assert - each counter should track its own events
        assertThat(routeChangeCounter.count()).isEqualTo(routeChangeInitial + TWO);
        assertThat(rateLimitCounter.count()).isEqualTo(rateLimitInitial + 1);
        assertThat(healthCounter.count()).isEqualTo(healthInitial + TWO);
    }

    @Test
    void testMetricsCounterNames_AreCorrect() {
        // Act
        publisher.publishRouteChangeEvent("service-1");
        publisher.publishRateLimitConfigChangeEvent(List.of("service-1"), List.of("route-1"));
        publisher.publishServiceHealthChangeEvent(List.of("service-1"));

        // Assert - verify all counters exist with correct names
        assertThat(meterRegistry.find("route.events.published.total")
                .tag("event_type", RouteEventType.ROUTE_CHANGE.name())
                .counter()).isNotNull();
        assertThat(meterRegistry.find("route.events.published.total")
                .tag("event_type", RouteEventType.RATE_LIMIT_CONFIG_CHANGE.name())
                .counter()).isNotNull();
        assertThat(meterRegistry.find("route.events.published.total")
                .tag("event_type", RouteEventType.SERVICE_HEALTH_CHANGE.name())
                .counter()).isNotNull();
    }

    @Test
    void testPublishMultipleRouteChangeEvents_IncrementsCounterCorrectly() {
        // Arrange
        Counter counter = meterRegistry.counter("route.events.published.total", 
                "event_type", RouteEventType.ROUTE_CHANGE.name());
        final double initialCount = counter.count();
        final int expectedIncrement = FOUR;

        // Act
        publisher.publishRouteChangeEvent("service-1");
        publisher.publishRouteChangeEvent("service-2");
        publisher.publishRouteChangeEvent("service-3");
        publisher.publishRouteChangeEvent("service-4");

        // Assert
        assertThat(counter.count()).isEqualTo(initialCount + expectedIncrement);
    }
}

