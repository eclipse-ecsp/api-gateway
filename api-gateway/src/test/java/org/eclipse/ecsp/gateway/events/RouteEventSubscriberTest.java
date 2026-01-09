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

package org.eclipse.ecsp.gateway.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.ecsp.gateway.model.RouteChangeEvent;
import org.eclipse.ecsp.gateway.model.RouteEventType;
import org.eclipse.ecsp.gateway.service.RouteRefreshService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RouteEventSubscriber.
 */
@ExtendWith(MockitoExtension.class)
class RouteEventSubscriberTest {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int TWO = 2;
    private static final int FOUR = 4;

    @Mock
    private RouteRefreshService routeRefreshService;

    private RetryTemplate retryTemplate;

    @Mock
    private Message message;

    private ObjectMapper objectMapper;
    private RouteEventSubscriber subscriber;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        
        // Create a real RetryTemplate with immediate retry (max 3 attempts)
        retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(MAX_RETRY_ATTEMPTS);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        meterRegistry = new SimpleMeterRegistry();
        subscriber = new RouteEventSubscriber(routeRefreshService, retryTemplate, objectMapper, meterRegistry);
        ReflectionTestUtils.setField(subscriber, "totalEventsReceivedMetricName", "route.events.received.total");
        ReflectionTestUtils.setField(subscriber, "refreshSuccessMetricName", "route.refresh.success.total");
        ReflectionTestUtils.setField(subscriber, "refreshFailureMetricName", "route.refresh.failure.total");
        subscriber.initializeMetrics();
    }

    @Test
    void testOnMessage_ValidEvent_TriggersRefresh() throws Exception {
        // Arrange
        RouteChangeEvent event = new RouteChangeEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                RouteEventType.ROUTE_CHANGE,
                List.of("service-1", "service-2")
        );
        String eventJson = objectMapper.writeValueAsString(event);
        when(message.getBody()).thenReturn(eventJson.getBytes());

        // Act
        subscriber.onMessage(message, null);

        // Assert
        verify(routeRefreshService, times(1)).refreshRoutes();
    }

    @Test
    void testOnMessage_InvalidJson_HandlesGracefully() {
        // Arrange
        when(message.getBody()).thenReturn("invalid-json".getBytes());

        // Act & Assert - should not throw exception
        subscriber.onMessage(message, null);
        
        // Verify refresh was not called
        verifyNoInteractions(routeRefreshService);
    }

    @Test
    void testOnMessage_NullMessage_HandlesGracefully() {
        // Act & Assert - should not throw exception
        subscriber.onMessage(null, null);
        
        // Verify refresh was not called
        verify(routeRefreshService, times(0)).refreshRoutes();
    }

    @Test
    void testOnMessage_EmptyServicesList_TriggersRefresh() throws Exception {
        // Arrange
        RouteChangeEvent event = new RouteChangeEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                RouteEventType.ROUTE_CHANGE,
                List.of()
        );
        String eventJson = objectMapper.writeValueAsString(event);
        when(message.getBody()).thenReturn(eventJson.getBytes());

        // Act
        subscriber.onMessage(message, null);

        // Assert - should still trigger refresh
        verify(routeRefreshService, atLeastOnce()).refreshRoutes();
    }

    @Test
    void testOnMessage_WithRetry_RetriesOnFailure() throws Exception {
        // Arrange
        RouteChangeEvent event = new RouteChangeEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                RouteEventType.ROUTE_CHANGE,
                List.of("service-1")
        );
        String eventJson = objectMapper.writeValueAsString(event);
        when(message.getBody()).thenReturn(eventJson.getBytes());
        
        // Mock refresh service to always throw exception
        doThrow(new RestClientException("API Registry unreachable"))
            .when(routeRefreshService).refreshRoutes();

        // Act - should handle exception after retries
        subscriber.onMessage(message, null);
        
        // Verify retry was attempted 3 times (max attempts)
        verify(routeRefreshService, times(MAX_RETRY_ATTEMPTS)).refreshRoutes();
    }

    @Test
    void testOnMessage_MultipleServices_TriggersRefresh() throws Exception {
        // Arrange
        RouteChangeEvent event = new RouteChangeEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                RouteEventType.ROUTE_CHANGE,
                List.of("service-1", "service-2", "service-3", "service-4")
        );
        String eventJson = objectMapper.writeValueAsString(event);
        when(message.getBody()).thenReturn(eventJson.getBytes());

        // Act
        subscriber.onMessage(message, null);

        // Assert
        verify(routeRefreshService, times(1)).refreshRoutes();
    }

    @Test
    void testOnMessage_RouteChangeEvent_IncrementsReceivedCounter() throws Exception {
        // Arrange
        RouteChangeEvent event = new RouteChangeEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                RouteEventType.ROUTE_CHANGE,
                List.of("service-1")
        );
        String eventJson = objectMapper.writeValueAsString(event);
        when(message.getBody()).thenReturn(eventJson.getBytes());
        
        Counter counter = meterRegistry.counter("route.events.received.total", 
                "event_type", RouteEventType.ROUTE_CHANGE.name());
        double initialCount = counter.count();

        // Act
        subscriber.onMessage(message, null);

        // Assert
        assertThat(counter.count()).isEqualTo(initialCount + 1);
    }

    @Test
    void testOnMessage_RateLimitConfigChangeEvent_IncrementsReceivedCounter() throws Exception {
        // Arrange
        RouteChangeEvent event = new RouteChangeEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                RouteEventType.RATE_LIMIT_CONFIG_CHANGE,
                List.of("service-1")
        );
        String eventJson = objectMapper.writeValueAsString(event);
        when(message.getBody()).thenReturn(eventJson.getBytes());
        
        Counter counter = meterRegistry.counter("route.events.received.total", 
                "event_type", RouteEventType.RATE_LIMIT_CONFIG_CHANGE.name());
        double initialCount = counter.count();

        // Act
        subscriber.onMessage(message, null);

        // Assert
        assertThat(counter.count()).isEqualTo(initialCount + 1);
    }

    @Test
    void testOnMessage_ServiceHealthChangeEvent_IncrementsReceivedCounter() throws Exception {
        // Arrange
        RouteChangeEvent event = new RouteChangeEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                RouteEventType.SERVICE_HEALTH_CHANGE,
                List.of("service-1", "service-2")
        );
        String eventJson = objectMapper.writeValueAsString(event);
        when(message.getBody()).thenReturn(eventJson.getBytes());
        
        Counter counter = meterRegistry.counter("route.events.received.total", 
                "event_type", RouteEventType.SERVICE_HEALTH_CHANGE.name());
        double initialCount = counter.count();

        // Act
        subscriber.onMessage(message, null);

        // Assert
        assertThat(counter.count()).isEqualTo(initialCount + 1);
    }

    @Test
    void testOnMessage_SuccessfulRefresh_IncrementsSuccessCounter() throws Exception {
        // Arrange
        RouteChangeEvent event = new RouteChangeEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                RouteEventType.ROUTE_CHANGE,
                List.of("service-1")
        );
        String eventJson = objectMapper.writeValueAsString(event);
        when(message.getBody()).thenReturn(eventJson.getBytes());
        
        Counter successCounter = meterRegistry.counter("route.refresh.success.total");
        double initialSuccessCount = successCounter.count();

        // Act
        subscriber.onMessage(message, null);

        // Assert
        assertThat(successCounter.count()).isEqualTo(initialSuccessCount + 1);
    }

    @Test
    void testOnMessage_FailedRefresh_IncrementsFailureCounter() throws Exception {
        // Arrange
        RouteChangeEvent event = new RouteChangeEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                RouteEventType.ROUTE_CHANGE,
                List.of("service-1")
        );
        String eventJson = objectMapper.writeValueAsString(event);
        when(message.getBody()).thenReturn(eventJson.getBytes());
        
        doThrow(new RestClientException("API Registry unreachable"))
            .when(routeRefreshService).refreshRoutes();
        
        Counter failureCounter = meterRegistry.counter("route.refresh.failure.total");
        double initialFailureCount = failureCounter.count();

        // Act
        subscriber.onMessage(message, null);

        // Assert - failure counter should be incremented once after all retries exhausted
        assertThat(failureCounter.count()).isEqualTo(initialFailureCount + 1);
    }

    @Test
    void testOnMessage_InvalidJson_DoesNotIncrementAnyCounter() {
        // Arrange
        when(message.getBody()).thenReturn("invalid-json".getBytes());
        
        Counter routeChangeCounter = meterRegistry.counter("route.events.received.total", 
                "event_type", RouteEventType.ROUTE_CHANGE.name());
        Counter successCounter = meterRegistry.counter("route.refresh.success.total");
        Counter failureCounter = meterRegistry.counter("route.refresh.failure.total");
        
        final double routeChangeInitial = routeChangeCounter.count();
        final double successInitial = successCounter.count();
        final double failureInitial = failureCounter.count();

        // Act
        subscriber.onMessage(message, null);

        // Assert - no counters should be incremented
        assertThat(routeChangeCounter.count()).isEqualTo(routeChangeInitial);
        assertThat(successCounter.count()).isEqualTo(successInitial);
        assertThat(failureCounter.count()).isEqualTo(failureInitial);
    }

    @Test
    void testOnMessage_MultipleEventTypes_TracksSeparateReceivedCounters() throws Exception {
        // Arrange
        Counter routeChangeCounter = meterRegistry.counter("route.events.received.total", 
                "event_type", RouteEventType.ROUTE_CHANGE.name());
        Counter rateLimitCounter = meterRegistry.counter("route.events.received.total", 
                "event_type", RouteEventType.RATE_LIMIT_CONFIG_CHANGE.name());
        Counter healthCounter = meterRegistry.counter("route.events.received.total", 
                "event_type", RouteEventType.SERVICE_HEALTH_CHANGE.name());

        final double routeChangeInitial = routeChangeCounter.count();
        final double rateLimitInitial = rateLimitCounter.count();
        final double healthInitial = healthCounter.count();

        // Act - send different event types
        RouteChangeEvent routeEvent = new RouteChangeEvent(
                UUID.randomUUID().toString(), Instant.now(),
                RouteEventType.ROUTE_CHANGE, List.of("service-1"));
        when(message.getBody()).thenReturn(objectMapper.writeValueAsString(routeEvent).getBytes());
        subscriber.onMessage(message, null);

        RouteChangeEvent rateLimitEvent = new RouteChangeEvent(
                UUID.randomUUID().toString(), Instant.now(),
                RouteEventType.RATE_LIMIT_CONFIG_CHANGE, List.of("service-2"));
        when(message.getBody()).thenReturn(objectMapper.writeValueAsString(rateLimitEvent).getBytes());
        subscriber.onMessage(message, null);

        RouteChangeEvent healthEvent = new RouteChangeEvent(
                UUID.randomUUID().toString(), Instant.now(),
                RouteEventType.SERVICE_HEALTH_CHANGE, List.of("service-3"));
        when(message.getBody()).thenReturn(objectMapper.writeValueAsString(healthEvent).getBytes());
        subscriber.onMessage(message, null);

        // Assert - each counter tracks its own events
        assertThat(routeChangeCounter.count()).isEqualTo(routeChangeInitial + 1);
        assertThat(rateLimitCounter.count()).isEqualTo(rateLimitInitial + 1);
        assertThat(healthCounter.count()).isEqualTo(healthInitial + 1);
    }

    @Test
    void testOnMessage_MultipleSuccessfulRefreshes_IncrementsSuccessCounterCorrectly() throws Exception {
        // Arrange
        Counter successCounter = meterRegistry.counter("route.refresh.success.total");
        double initialCount = successCounter.count();

        // Act - process multiple events
        for (int i = 0; i < FOUR; i++) {
            RouteChangeEvent event = new RouteChangeEvent(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    RouteEventType.ROUTE_CHANGE,
                    List.of("service-" + i)
            );
            when(message.getBody()).thenReturn(objectMapper.writeValueAsString(event).getBytes());
            subscriber.onMessage(message, null);
        }

        // Assert
        assertThat(successCounter.count()).isEqualTo(initialCount + FOUR);
    }

    @Test
    void testOnMessage_PartialSuccess_TracksCorrectly() throws Exception {
        // Arrange
        Counter successCounter = meterRegistry.counter("route.refresh.success.total");
        Counter failureCounter = meterRegistry.counter("route.refresh.failure.total");
        final double successInitial = successCounter.count();
        final double failureInitial = failureCounter.count();

        // Act - first event succeeds
        RouteChangeEvent event1 = new RouteChangeEvent(
                UUID.randomUUID().toString(), Instant.now(),
                RouteEventType.ROUTE_CHANGE, List.of("service-1"));
        when(message.getBody()).thenReturn(objectMapper.writeValueAsString(event1).getBytes());
        subscriber.onMessage(message, null);

        // Second event fails
        doThrow(new RestClientException("API Registry unreachable"))
            .when(routeRefreshService).refreshRoutes();
        RouteChangeEvent event2 = new RouteChangeEvent(
                UUID.randomUUID().toString(), Instant.now(),
                RouteEventType.ROUTE_CHANGE, List.of("service-2"));
        when(message.getBody()).thenReturn(objectMapper.writeValueAsString(event2).getBytes());
        subscriber.onMessage(message, null);

        // Assert
        assertThat(successCounter.count()).isEqualTo(successInitial + 1);
        assertThat(failureCounter.count()).isEqualTo(failureInitial + 1);
    }

    @Test
    void testMetricsCounterNames_AreCorrect() throws Exception {
        // Act - trigger events
        RouteChangeEvent event = new RouteChangeEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                RouteEventType.ROUTE_CHANGE,
                List.of("service-1")
        );
        when(message.getBody()).thenReturn(objectMapper.writeValueAsString(event).getBytes());
        subscriber.onMessage(message, null);

        // Assert - verify all counters exist with correct names
        assertThat(meterRegistry.find("route.events.received.total")
                .tag("event_type", RouteEventType.ROUTE_CHANGE.name())
                .counter()).isNotNull();
        assertThat(meterRegistry.find("route.refresh.success.total")
                .counter()).isNotNull();
    }

    @Test
    void testOnMessage_RefreshSuccessAfterRetry_IncrementsSuccessCounter() throws Exception {
        // Arrange
        RouteChangeEvent event = new RouteChangeEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                RouteEventType.ROUTE_CHANGE,
                List.of("service-1")
        );
        String eventJson = objectMapper.writeValueAsString(event);
        when(message.getBody()).thenReturn(eventJson.getBytes());
        
        // Mock refresh service to fail first, then succeed
        doThrow(new RestClientException("Temporary failure"))
            .doNothing()
            .when(routeRefreshService).refreshRoutes();
        
        Counter successCounter = meterRegistry.counter("route.refresh.success.total");
        double initialSuccessCount = successCounter.count();

        // Act
        subscriber.onMessage(message, null);

        // Assert - success counter should be incremented
        assertThat(successCounter.count()).isEqualTo(initialSuccessCount + 1);
        verify(routeRefreshService, times(TWO)).refreshRoutes();
    }
}
