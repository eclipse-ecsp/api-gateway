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

package org.eclipse.ecsp.gateway.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.eclipse.ecsp.gateway.conditions.RouteRefreshEventEnabledCondition;
import org.eclipse.ecsp.gateway.model.RouteChangeEvent;
import org.eclipse.ecsp.gateway.model.RouteEventType;
import org.eclipse.ecsp.gateway.service.RouteRefreshService;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Redis message listener for route change events.
 * Subscribes to Redis channel and triggers route refresh on API Gateway.
 */
@Component
@Conditional(RouteRefreshEventEnabledCondition.class)
public class RouteEventSubscriber implements MessageListener {

    private static final String EVENT_TYPE = "event_type";

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RouteEventSubscriber.class);

    private final RouteRefreshService routeRefreshService;
    private final RetryTemplate retryTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private Map<RouteEventType, Counter> eventReceivedCounters;
    private Counter refreshSuccessCounter;
    private Counter refreshFailureCounter;
    

    @Value("${route.events.received.total.metric.name:route.events.received.total}")
    private String totalEventsReceivedMetricName;

    @Value("${route.refresh.success.total.metric.name:route.refresh.success.total}")
    private String refreshSuccessMetricName;

    @Value("${route.refresh.failure.total.metric.name:route.refresh.failure.total}")
    private String refreshFailureMetricName;

    /**
     * Constructor for RouteEventSubscriber.
     *
     * @param routeRefreshService service to handle route refresh
     * @param retryTemplate       retry template with exponential backoff
     * @param objectMapper        JSON object mapper
     * @param meterRegistry       Micrometer meter registry for metrics
     */
    public RouteEventSubscriber(RouteRefreshService routeRefreshService,
                                @Qualifier("routesRefreshRetryTemplate") RetryTemplate retryTemplate,
                                ObjectMapper objectMapper,
                                MeterRegistry meterRegistry) {
        this.routeRefreshService = routeRefreshService;
        this.retryTemplate = retryTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        LOGGER.info("RouteEventSubscriber initialized");
    }

    /**
     * Initialize metrics.
     */
    @PostConstruct
    public void initializeMetrics() {
        // Initialize counters for events received by type

        // create individual counters for each event type with appropriate tags
        eventReceivedCounters = new EnumMap<>(RouteEventType.class);
        for (RouteEventType eventType : RouteEventType.values()) {
            Counter counter = Counter.builder(totalEventsReceivedMetricName)
                    .tag(EVENT_TYPE, eventType.name())
                    .description("Total number of " + eventType.name() + " events received")
                    .register(meterRegistry);
            eventReceivedCounters.put(eventType, counter);
            
        }
        // Initialize counters for refresh operations
        this.refreshSuccessCounter = Counter.builder(refreshSuccessMetricName)
                .description("Total number of successful route refreshes")
                .register(meterRegistry);
        
        this.refreshFailureCounter = Counter.builder(refreshFailureMetricName)
                .description("Total number of failed route refreshes")
                .register(meterRegistry);
        LOGGER.info("RouteEventSubscriber metrics initialized");
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (message == null) {
            LOGGER.warn("Received null message");
            return;
        }
        try {
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            LOGGER.debug("Received route change event: {}", messageBody);

            // Parse event
            RouteChangeEvent event = objectMapper.readValue(messageBody, RouteChangeEvent.class);
            
            if (event == null) {
                LOGGER.warn("Received null route change event");
                return;
            }
            
            // Increment counter based on event type
            incrementEventReceivedCounter(event.getEventType());

            LOGGER.info("Processing route change event: eventId={}, eventData={}", event.getEventId(), messageBody);
            // Refresh routes with retry
            retryTemplate.execute(context -> {
                int attemptCount = context.getRetryCount() + 1;
                try {
                    LOGGER.debug("Route refresh attempt {}", attemptCount);
                    routeRefreshService.refreshRoutes();
                    LOGGER.info("Successfully refreshed routes");
                    
                    // Increment success counter only on first successful attempt after retries
                    if (context.getRetryCount() == 0 || attemptCount > 1) {
                        refreshSuccessCounter.increment();
                    }
                    return null;
                } catch (Exception e) {
                    LOGGER.error("Failed to refresh routes (attempt {}): {}", attemptCount, e.getMessage(), e);
                    // Don't increment failure counter here - only in the exhausted handler below
                    throw e;
                }
            }, context -> {
                // This recovery callback is invoked only when all retries are exhausted
                refreshFailureCounter.increment();
                LOGGER.error("All retry attempts exhausted for route refresh", context.getLastThrowable());
                return null;
            });

        } catch (Exception e) {
            LOGGER.error("Error processing route change event", e);
        }
    }
    
    /**
     * Increment the appropriate event received counter based on event type.
     *
     * @param eventType the type of event received
     */
    private void incrementEventReceivedCounter(RouteEventType eventType) {
        if (eventType == null) {
            return;
        }
        
        Counter counter = eventReceivedCounters.get(eventType);
        if (counter != null) {
            counter.increment();
        } else {
            LOGGER.warn("No counter found for event type: {}", eventType);
        }
    }
}
