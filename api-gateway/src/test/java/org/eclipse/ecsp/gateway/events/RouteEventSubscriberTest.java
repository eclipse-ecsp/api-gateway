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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteEventSubscriberTest {

    private static final int MAX_RETRY_ATTEMPTS = 3;

    @Mock
    private RouteRefreshService routeRefreshService;

    @Mock
    private Message message;

    private ObjectMapper objectMapper;
    private RouteEventSubscriber subscriber;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        RetryTemplate retryTemplate = new RetryTemplate();
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
        RouteChangeEvent event = newEvent(RouteEventType.ROUTE_CHANGE);
        when(message.getBody()).thenReturn(objectMapper.writeValueAsBytes(event));

        subscriber.onMessage(message, null);

        verify(routeRefreshService, times(1)).refreshRoutes();
    }

    @Test
    void testOnMessage_InvalidJson_HandlesGracefully() {
        when(message.getBody()).thenReturn("invalid-json".getBytes());

        subscriber.onMessage(message, null);

        verifyNoInteractions(routeRefreshService);
    }

    @Test
    void testOnMessage_NullMessage_HandlesGracefully() {
        subscriber.onMessage(null, null);

        verifyNoInteractions(routeRefreshService);
    }

    @Test
    void testOnMessage_RetryOnFailure() throws Exception {
        RouteChangeEvent event = newEvent(RouteEventType.ROUTE_CHANGE);
        when(message.getBody()).thenReturn(objectMapper.writeValueAsBytes(event));
        doThrow(new RestClientException("API Registry unreachable")).when(routeRefreshService).refreshRoutes();

        subscriber.onMessage(message, null);

        verify(routeRefreshService, times(MAX_RETRY_ATTEMPTS)).refreshRoutes();
    }

    @Test
    void testOnMessage_UpdatesMetrics() throws Exception {
        RouteChangeEvent event = newEvent(RouteEventType.ROUTE_CHANGE);
        when(message.getBody()).thenReturn(objectMapper.writeValueAsBytes(event));

        Counter receivedCounter = meterRegistry.counter(
                "route.events.received.total",
                "event_type",
                RouteEventType.ROUTE_CHANGE.name());
        Counter successCounter = meterRegistry.counter("route.refresh.success.total");
        double initialReceived = receivedCounter.count();
        double initialSuccess = successCounter.count();

        subscriber.onMessage(message, null);

        assertThat(receivedCounter.count()).isEqualTo(initialReceived + 1);
        assertThat(successCounter.count()).isEqualTo(initialSuccess + 1);
    }

    private RouteChangeEvent newEvent(RouteEventType type) {
        return new RouteChangeEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                type,
                List.of("service-1"),
                List.of(),
                List.of(),
                "UPDATE");
    }
}
