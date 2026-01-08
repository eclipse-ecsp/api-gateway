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
    private final Counter routeChangeEventReceivedCounter;
    private final Counter rateLimitConfigChangeEventReceivedCounter;
    private final Counter serviceHealthChangeEventReceivedCounter;
    private final Counter refreshSuccessCounter;
    private final Counter refreshFailureCounter;

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
        
        // Initialize counters for events received by type
        this.routeChangeEventReceivedCounter = Counter.builder(totalEventsReceivedMetricName)
                .tag(EVENT_TYPE, RouteEventType.ROUTE_CHANGE.name())
                .description("Total number of route change events received")
                .register(meterRegistry);
        
        this.rateLimitConfigChangeEventReceivedCounter = Counter.builder(totalEventsReceivedMetricName)
                .tag(EVENT_TYPE, RouteEventType.RATE_LIMIT_CONFIG_CHANGE.name())
                .description("Total number of rate limit config change events received")
                .register(meterRegistry);
        
        this.serviceHealthChangeEventReceivedCounter = Counter.builder(totalEventsReceivedMetricName)
                .tag(EVENT_TYPE, RouteEventType.SERVICE_HEALTH_CHANGE.name())
                .description("Total number of service health change events received")
                .register(meterRegistry);
        
        // Initialize counters for refresh operations
        this.refreshSuccessCounter = Counter.builder(refreshSuccessMetricName)
                .description("Total number of successful route refreshes")
                .register(meterRegistry);
        
        this.refreshFailureCounter = Counter.builder(refreshFailureMetricName)
                .description("Total number of failed route refreshes")
                .register(meterRegistry);
        
        LOGGER.info("RouteEventSubscriber initialized with metrics");
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (message == null) {
            LOGGER.warn("Received null message");
            return;
        }
        try {
            String messageBody = new String(message.getBody());
            LOGGER.debug("Received route change event: {}", messageBody);

            // Parse event
            RouteChangeEvent event = objectMapper.readValue(messageBody, RouteChangeEvent.class);
            
            if (event == null) {
                LOGGER.warn("Received null route change event");
                return;
            }
            
            // Increment counter based on event type
            incrementEventReceivedCounter(event.getEventType());

            LOGGER.info("Processing route change event: eventId={}, eventType={}, serviceCount={}, services={}",
                    event.getEventId(),
                    event.getEventType(),
                    event.getServices() != null ? event.getServices().size() : 0, 
                    event.getServices());

            // Refresh routes with retry
            retryTemplate.execute(context -> {
                int attemptCount = context.getRetryCount() + 1;
                try {
                    LOGGER.debug("Route refresh attempt {}", attemptCount);
                    routeRefreshService.refreshRoutes();
                    LOGGER.info("Successfully refreshed routes for {} services", 
                            event.getServices() != null ? event.getServices().size() : 0);
                    
                    // Increment success counter only on first successful attempt after retries
                    if (context.getRetryCount() == 0 || attemptCount > 1) {
                        refreshSuccessCounter.increment();
                    }
                    return null;
                } catch (Exception e) {
                    LOGGER.error("Failed to refresh routes (attempt {}): {}", attemptCount, e.getMessage());
                    // Don't increment failure counter here - only in the exhausted handler below
                    throw e;
                }
            }, context -> {
                // This recovery callback is invoked only when all retries are exhausted
                refreshFailureCounter.increment();
                LOGGER.error("All retry attempts exhausted for route refresh");
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
        
        switch (eventType) {
            case ROUTE_CHANGE:
                routeChangeEventReceivedCounter.increment();
                break;
            case RATE_LIMIT_CONFIG_CHANGE:
                rateLimitConfigChangeEventReceivedCounter.increment();
                break;
            case SERVICE_HEALTH_CHANGE:
                serviceHealthChangeEventReceivedCounter.increment();
                break;
            default:
                LOGGER.warn("Unknown event type: {}", eventType);
                break;
        }
    }
}
