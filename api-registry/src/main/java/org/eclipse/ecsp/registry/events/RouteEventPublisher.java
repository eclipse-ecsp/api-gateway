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

import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RouteEventPublisher.class);

    private final RouteEventThrottler throttler;
    
    /**
     * Constructor for RouteEventPublisher.
     *
     * @param throttler event throttler with debouncing
     */
    public RouteEventPublisher(RouteEventThrottler throttler) {
        this.throttler = throttler;
        LOGGER.info("RouteEventPublisher initialized");
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
    }

    /**
     * Publish a rate limit config change event for a service.
     *
     * @param serviceName list of service names that changed
     * @param routeIds    list of route IDs that changed
     */
    public void publishRateLimitConfigChangeEvent(List<String> serviceName, List<String> routeIds) {
        if (CollectionUtils.isEmpty(serviceName) && CollectionUtils.isEmpty(routeIds)) {
            LOGGER.warn("Cannot publish event with null or empty service name and route IDs");
            return;
        }

        LOGGER.debug("Publishing rate limit config change event for service: {}, route IDs: {}", serviceName, routeIds);
        throttler.sendEvent(RouteEventType.RATE_LIMIT_CONFIG_CHANGE, serviceName, routeIds);
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
    }

    /**
     * Handle application ready event to perform any initialization if needed.
     *
     * @param event the application ready event
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        LOGGER.info("Application is ready. RouteEventPublisher is operational. started at {}", event.getTimestamp());
        throttler.sendEvent(RouteEventType.ROUTE_CHANGE, List.of("all"), Collections.emptyList());
        LOGGER.info("Initial route change event published for all services");
    }
}
