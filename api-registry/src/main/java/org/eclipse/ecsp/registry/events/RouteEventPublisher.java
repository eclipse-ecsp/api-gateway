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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * Publisher for route change events.
 * Coordinates with the throttler to implement debouncing.
 */
@Component
@ConditionalOnProperty(name = "api-registry.events.enabled", havingValue = "true")
public class RouteEventPublisher {

    private static final String EVENT_TYPE = "event_type";

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RouteEventPublisher.class);

    private final RouteEventThrottler throttler;
    private final Counter routeChangeEventCounter;
    private final Counter rateLimitConfigChangeEventCounter;
    private final Counter serviceHealthChangeEventCounter;

    @Value("${api-registry.events.metrics.total.published.metrics-name:route.events.published.total}")
    private String totalPublishedMetricsName;

    /**
     * Constructor for RouteEventPublisher.
     *
     * @param throttler event throttler with debouncing
     * @param meterRegistry Micrometer meter registry for metrics
     */
    public RouteEventPublisher(RouteEventThrottler throttler, MeterRegistry meterRegistry) {
        this.throttler = throttler;
        
        // Initialize counters for each event type
        this.routeChangeEventCounter = Counter.builder(totalPublishedMetricsName)
                .tag(EVENT_TYPE, RouteEventType.ROUTE_CHANGE.name())
                .description("Total number of route change events published")
                .register(meterRegistry);
        
        this.rateLimitConfigChangeEventCounter = Counter.builder(totalPublishedMetricsName)
                .tag(EVENT_TYPE, RouteEventType.RATE_LIMIT_CONFIG_CHANGE.name())
                .description("Total number of rate limit config change events published")
                .register(meterRegistry);
        
        this.serviceHealthChangeEventCounter = Counter.builder(totalPublishedMetricsName)
                .tag(EVENT_TYPE, RouteEventType.SERVICE_HEALTH_CHANGE.name())
                .description("Total number of service health change events published")
                .register(meterRegistry);
        
        LOGGER.info("RouteEventPublisher initialized with metrics");
    }

    /**
     * Publish a route change event for a service.
     * Event is debounced and consolidated with other pending events.
     *
     * @param serviceName name of the service that changed
     */
    public void publishRouteChangeEvent(String serviceName) {
        if (serviceName == null || serviceName.isEmpty()) {
            LOGGER.warn("Cannot publish event with null or empty service name");
            return;
        }

        LOGGER.debug("Publishing route change event for service: {}", serviceName);
        throttler.scheduleEvent(serviceName);
        routeChangeEventCounter.increment();
    }

    /**
     * Publish a rate limit config change event for a service.
     *
     * @param serviceName list of service names that changed
     */
    public void publishRateLimitConfigChangeEvent(List<String> serviceName, List<String> routeIds) {
        if (CollectionUtils.isEmpty(serviceName) || CollectionUtils.isEmpty(routeIds)) {
            LOGGER.warn("Cannot publish event with null or empty service name or route IDs");
            return;
        }

        LOGGER.debug("Publishing rate limit config change event for service: {}, route IDs: {}", serviceName, routeIds);
        throttler.sendEvent(RouteEventType.RATE_LIMIT_CONFIG_CHANGE, serviceName, routeIds);
        rateLimitConfigChangeEventCounter.increment();
    }

    /**
     * Publish a service health change event for a list of services.
     *
     * @param serviceNames list of service names that changed
     */
    public void publishServiceHealthChangeEvent(List<String> serviceNames) {
        if (serviceNames == null || serviceNames.isEmpty()) {
            LOGGER.warn("Cannot publish event with null or empty service names");
            return;
        }

        LOGGER.debug("Publishing service health change event for services: {}", serviceNames);
        throttler.sendEvent(RouteEventType.SERVICE_HEALTH_CHANGE, serviceNames, Collections.emptyList());
        serviceHealthChangeEventCounter.increment();
    }
}
