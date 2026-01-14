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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for RouteEventPublisher.
 */
@ExtendWith(MockitoExtension.class)
class RouteEventPublisherTest {
    
    private static final int THREE = 3;

    @Mock
    private RouteEventThrottler throttler;

    private RouteEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RouteEventPublisher(throttler);
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
    void testPublishRateLimitConfigChangeEvent_DelegatesToThrottler() {
        // Arrange
        List<String> serviceNames = List.of("service-1");
        List<String> routeIds = List.of("route-1");

        // Act
        publisher.publishRateLimitConfigChangeEvent(serviceNames, routeIds);

        // Assert
        verify(throttler, times(1)).sendEvent(RouteEventType.RATE_LIMIT_CONFIG_CHANGE, serviceNames, routeIds);
    }

    @Test
    void testPublishServiceHealthChangeEvent_DelegatesToThrottler() {
        // Arrange
        List<String> serviceNames = List.of("service-1", "service-2");

        // Act
        publisher.publishServiceHealthChangeEvent(serviceNames);

        // Assert
        verify(throttler, times(1)).sendEvent(RouteEventType.SERVICE_HEALTH_CHANGE, 
                serviceNames, Collections.emptyList());
    }

    @Test
    void testPublishRouteChangeEvent_NullOrEmptyServiceName_DoesNothing() {
        // Act
        publisher.publishRouteChangeEvent(null);
        publisher.publishRouteChangeEvent("");

        // Assert
        verify(throttler, times(0)).scheduleEvent(any());
    }

    @Test
    void testPublishRateLimitConfigChangeEvent_NullOrEmpty_DoesNothing() {
        // Act
        publisher.publishRateLimitConfigChangeEvent(null, null);
        publisher.publishRateLimitConfigChangeEvent(Collections.emptyList(), Collections.emptyList());

        // Assert
        verify(throttler, times(0)).sendEvent(any(), any(), any());
    }

    @Test
    void testPublishServiceHealthChangeEvent_NullOrEmpty_DoesNothing() {
        // Act
        publisher.publishServiceHealthChangeEvent(null);
        publisher.publishServiceHealthChangeEvent(Collections.emptyList());

        // Assert
        verify(throttler, times(0)).sendEvent(any(), any(), any());
    }
}


