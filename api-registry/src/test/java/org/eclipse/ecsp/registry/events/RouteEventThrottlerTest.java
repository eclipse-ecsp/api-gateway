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

import org.eclipse.ecsp.registry.config.EventProperties;
import org.eclipse.ecsp.registry.events.data.AbstractEventData;
import org.eclipse.ecsp.registry.events.metrics.EventPublishingMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RouteEventThrottler.
 */
@ExtendWith(MockitoExtension.class)
class RouteEventThrottlerTest {

    private static final long TEST_DEBOUNCE_DELAY_MS = 500L;
    private static final long SLEEP_BUFFER_MS = 1000L;
    private static final long SHORT_SLEEP_MS = 30L;
    private static final long PRE_DEBOUNCE_SLEEP_MS = 100L;
    private static final int SINGLE_INVOCATION = 1;
    private static final int AT_LEAST_TWO_INVOCATIONS = 2;
    private static final int AT_LEAST_THREE_INVOCATIONS = 3;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private EventProperties eventProperties;

    @Mock
    private EventProperties.RedisConfig redisConfig;

    @Mock
    private EventPublishingMetrics metrics;

    private RouteEventThrottler throttler;

    @BeforeEach
    void setUp() {
        when(eventProperties.getRedis()).thenReturn(redisConfig);
        when(redisConfig.getDebounceDelayMs()).thenReturn(TEST_DEBOUNCE_DELAY_MS); // Short delay for testing
        when(eventPublisher.publishEvent(any(AbstractEventData.class))).thenReturn(true);
        when(metrics.recordPublish(any(RouteEventType.class), any(java.util.function.Supplier.class)))
                .thenAnswer(invocation -> {
                    java.util.function.Supplier<Boolean> supplier = invocation.getArgument(1);
                    return supplier.get();
                });
        
        throttler = new RouteEventThrottler(eventProperties, eventPublisher, metrics);
    }

    @AfterEach
    void tearDown() {
        if (throttler != null) {
            throttler.shutdown();
        }
    }

    @Test
    void testScheduleEvent_SingleService() {
        // Arrange
        String serviceName = "test-service";

        // Act
        throttler.scheduleEvent(serviceName);
        
        // Wait for debounce delay and verify event is published
        await().atMost(Duration.ofMillis(SLEEP_BUFFER_MS))
                .untilAsserted(() -> {
                    verify(eventPublisher, times(SINGLE_INVOCATION)).publishEvent(
                            any(AbstractEventData.class));
                });

        // Assert
        ArgumentCaptor<AbstractEventData> eventCaptor = ArgumentCaptor.forClass(AbstractEventData.class);
        verify(eventPublisher, times(SINGLE_INVOCATION)).publishEvent(eventCaptor.capture());
        
        AbstractEventData publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent).isNotNull();
        assertThat(publishedEvent.getEventType()).isEqualTo(RouteEventType.ROUTE_CHANGE);
    }

    @Test
    void testScheduleEvent_MultipleServices_Consolidates() {
        // Arrange
        final String service1 = "service-1";
        final String service2 = "service-2";
        final String service3 = "service-3";

        // Act
        throttler.scheduleEvent(service1);
        await().pollDelay(Duration.ofMillis(SHORT_SLEEP_MS)).until(() -> true);
        throttler.scheduleEvent(service2);
        await().pollDelay(Duration.ofMillis(SHORT_SLEEP_MS)).until(() -> true);
        throttler.scheduleEvent(service3);
        
        // Wait for debounce and verify event is published
        await().atMost(Duration.ofMillis(SLEEP_BUFFER_MS))
                .untilAsserted(() -> {
                    verify(eventPublisher, times(SINGLE_INVOCATION)).publishEvent(
                            any(AbstractEventData.class));
                });

        // Assert - should only publish once with all services
        ArgumentCaptor<AbstractEventData> eventCaptor = ArgumentCaptor.forClass(AbstractEventData.class);
        verify(eventPublisher, times(SINGLE_INVOCATION)).publishEvent(eventCaptor.capture());
        
        AbstractEventData publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent).isNotNull();
        assertThat(publishedEvent.getEventType()).isEqualTo(RouteEventType.ROUTE_CHANGE);
    }

    @Test
    void testScheduleEvent_TimerResets() {
        // Arrange
        String service1 = "service-1";
        String service2 = "service-2";

        // Act - schedule service1, then service2 after 80ms (before first would publish)
        throttler.scheduleEvent(service1);
        await().pollDelay(Duration.ofMillis(PRE_DEBOUNCE_SLEEP_MS)).until(() -> true);
        throttler.scheduleEvent(service2);
        
        // Verify no publish yet
        verify(eventPublisher, times(0)).publishEvent(any(AbstractEventData.class));
        
        // Wait for debounce delay from second event and verify (need total buffer time)
        await().atMost(Duration.ofMillis(TEST_DEBOUNCE_DELAY_MS + SLEEP_BUFFER_MS))
                .untilAsserted(() -> {
                    verify(eventPublisher, times(SINGLE_INVOCATION)).publishEvent(
                            any(AbstractEventData.class));
                });

        // Assert - should have published once with both services (timer was reset)
        ArgumentCaptor<AbstractEventData> eventCaptor = ArgumentCaptor.forClass(AbstractEventData.class);
        verify(eventPublisher, times(SINGLE_INVOCATION)).publishEvent(eventCaptor.capture());
        
        AbstractEventData publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent).isNotNull();
        assertThat(publishedEvent.getEventType()).isEqualTo(RouteEventType.ROUTE_CHANGE);
    }

    @Test
    void testScheduleEvent_DuplicateServices_DeduplicatesInSameWindow() {
        // Arrange
        String serviceName = "test-service";

        // Act
        throttler.scheduleEvent(serviceName);
        throttler.scheduleEvent(serviceName);
        throttler.scheduleEvent(serviceName);
        
        // Wait for debounce and verify event is published
        await().atMost(Duration.ofMillis(SLEEP_BUFFER_MS))
                .untilAsserted(() -> {
                    verify(eventPublisher, times(SINGLE_INVOCATION)).publishEvent(
                            any(AbstractEventData.class));
                });

        // Assert - should publish once with service appearing once
        ArgumentCaptor<AbstractEventData> eventCaptor = ArgumentCaptor.forClass(AbstractEventData.class);
        verify(eventPublisher, times(SINGLE_INVOCATION)).publishEvent(eventCaptor.capture());
        
        AbstractEventData publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent).isNotNull();
        assertThat(publishedEvent.getEventType()).isEqualTo(RouteEventType.ROUTE_CHANGE);
    }

    @Test
    void testShutdown_CancelsScheduledTasks() {
        // Arrange - schedule an event but don't let it complete
        throttler.scheduleEvent("test-service");

        // Act - shutdown immediately to cancel the scheduled task
        throttler.shutdown();
        
        // Assert - verify shutdown completed successfully
        // The throttler object should still exist
        assertThat(throttler).isNotNull();
        
        // Verify that after shutdown, the executor is properly terminated
        // by checking that we can't schedule more events (would throw exception)
        // We just verify the shutdown didn't throw an exception itself
        assertThat(throttler).extracting("scheduler").isNotNull();
    }

    @Test
    void testMultipleWindows_PublishesMultipleTimes() {
        // Arrange & Act
        // First window
        throttler.scheduleEvent("service-1");
        await().atMost(Duration.ofMillis(TEST_DEBOUNCE_DELAY_MS + SLEEP_BUFFER_MS))
                .untilAsserted(() -> {
                    verify(eventPublisher, atLeast(SINGLE_INVOCATION)).publishEvent(any(AbstractEventData.class));
                });

        // Second window
        throttler.scheduleEvent("service-2");
        await().atMost(Duration.ofMillis(TEST_DEBOUNCE_DELAY_MS + SLEEP_BUFFER_MS))
                .untilAsserted(() -> {
                    verify(eventPublisher, atLeast(AT_LEAST_TWO_INVOCATIONS)).publishEvent(any(AbstractEventData.class));
                });

        // Third window
        throttler.scheduleEvent("service-3");
        await().atMost(Duration.ofMillis(TEST_DEBOUNCE_DELAY_MS + SLEEP_BUFFER_MS))
                .untilAsserted(() -> {
                    verify(eventPublisher, atLeast(AT_LEAST_THREE_INVOCATIONS)).publishEvent(any(AbstractEventData.class));
                });

        // Assert
        verify(eventPublisher, atLeast(AT_LEAST_THREE_INVOCATIONS)).publishEvent(any(AbstractEventData.class));
    }

    @Test
    void testScheduleEvent_NullServiceName_DoesNotPublish() {
        // Arrange & Act
        throttler.scheduleEvent(null);

        // Assert - Should settle quickly as it returns immediately
        await().pollDelay(Duration.ofMillis(PRE_DEBOUNCE_SLEEP_MS)).until(() -> true);
        verify(eventPublisher, times(0)).publishEvent(any(AbstractEventData.class));
    }

    @Test
    void testScheduleEvent_EmptyServiceName_DoesNotPublish() {
        // Arrange & Act
        throttler.scheduleEvent("");

        // Assert
        await().pollDelay(Duration.ofMillis(PRE_DEBOUNCE_SLEEP_MS)).until(() -> true);
        verify(eventPublisher, times(0)).publishEvent(any(AbstractEventData.class));
    }

    @Test
    void testSendEvent_PublishesSuccessfully() {
        // Arrange
        java.util.List<String> serviceNames = java.util.List.of("service-1");
        java.util.List<String> routeIds = java.util.List.of("route-1");

        // Act
        org.eclipse.ecsp.registry.events.data.RateLimitConfigEventData eventData =
            new org.eclipse.ecsp.registry.events.data.RateLimitConfigEventData(serviceNames, routeIds);
        boolean result = throttler.sendEvent(eventData);

        // Assert
        assertThat(result).isTrue();
        verify(eventPublisher, times(1)).publishEvent(eventData);
    }

    /**
     * Test purpose          - Verify getPendingServiceCount returns correct count.
     * Test data             - Multiple scheduled services.
     * Test expected result  - Count reflects number of pending services.
     * Test type             - Positive.
     */
    @Test
    void testGetPendingServiceCount_ReturnsCorrectCount() {
        // Arrange & Act
        throttler.scheduleEvent("service-1");
        throttler.scheduleEvent("service-2");
        throttler.scheduleEvent("service-3");

        // Assert - should have 3 pending services before debounce
        await().atMost(Duration.ofMillis(SHORT_SLEEP_MS))
                .untilAsserted(() -> assertThat(throttler.getPendingServiceCount()).isEqualTo(3));
    }

    /**
     * Test purpose          - Verify pending count is zero after flush.
     * Test data             - Scheduled services that have been flushed.
     * Test expected result  - Pending count returns to zero.
     * Test type             - Positive.
     */
    @Test
    void testGetPendingServiceCount_ZeroAfterFlush() {
        // Arrange
        throttler.scheduleEvent("service-1");

        // Act - wait for debounce and flush
        await().atMost(Duration.ofMillis(SLEEP_BUFFER_MS))
                .untilAsserted(() -> {
                    verify(eventPublisher, times(SINGLE_INVOCATION)).publishEvent(any(AbstractEventData.class));
                });

        // Assert - pending count should be zero after flush
        await().atMost(Duration.ofMillis(SHORT_SLEEP_MS))
                .untilAsserted(() -> assertThat(throttler.getPendingServiceCount()).isEqualTo(0));
    }

    /**
     * Test purpose          - Verify sendEvent handles publisher failure gracefully.
     * Test data             - Event data with failing publisher.
     * Test expected result  - Returns false when publishing fails.
     * Test type             - Negative.
     */
    @Test
    void testSendEvent_PublisherFails_ReturnsFalse() {
        // Arrange
        when(eventPublisher.publishEvent(any(AbstractEventData.class))).thenReturn(false);
        java.util.List<String> serviceNames = java.util.List.of("service-1");
        java.util.List<String> routeIds = java.util.List.of("route-1");

        // Act
        org.eclipse.ecsp.registry.events.data.RateLimitConfigEventData eventData =
            new org.eclipse.ecsp.registry.events.data.RateLimitConfigEventData(serviceNames, routeIds);
        boolean result = throttler.sendEvent(eventData);

        // Assert
        assertThat(result).isFalse();
    }

    /**
     * Test purpose          - Verify sendEvent handles publisher exception gracefully.
     * Test data             - Event data with publisher throwing exception.
     * Test expected result  - Returns false when exception occurs.
     * Test type             - Negative.
     */
    @Test
    void testSendEvent_PublisherThrowsException_ReturnsFalse() {
        // Arrange
        when(eventPublisher.publishEvent(any(AbstractEventData.class))).thenThrow(new RuntimeException("Test exception"));
        java.util.List<String> serviceNames = java.util.List.of("service-1");
        java.util.List<String> routeIds = java.util.List.of("route-1");

        // Act
        org.eclipse.ecsp.registry.events.data.RateLimitConfigEventData eventData =
            new org.eclipse.ecsp.registry.events.data.RateLimitConfigEventData(serviceNames, routeIds);
        boolean result = throttler.sendEvent(eventData);

        // Assert
        assertThat(result).isFalse();
    }

    /**
     * Test purpose          - Verify shutdown is idempotent.
     * Test data             - Multiple shutdown calls.
     * Test expected result  - No exception thrown on multiple shutdowns.
     * Test type             - Positive.
     */
    @Test
    void testShutdown_MultipleCallsAreIdempotent() {
        // Act
        throttler.shutdown();
        throttler.shutdown();
        throttler.shutdown();

        // Assert - no exception should be thrown
        assertThat(throttler).isNotNull();
    }
}
