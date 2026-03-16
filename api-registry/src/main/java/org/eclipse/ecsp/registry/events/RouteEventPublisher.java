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
import org.eclipse.ecsp.registry.utils.RegistryConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

/**
 * Publisher for route change events.
 * Coordinates with the throttler to implement debouncing.
 */
@Component
@ConditionalOnProperty(name = RegistryConstants.REGISTRY_EVENT_ENABLED, havingValue = "true")
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
     * Handle application ready event to perform any initialization if needed.
     *
     * @param event the application ready event
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        LOGGER.info("Application is ready. RouteEventPublisher is operational. started at {}", event.getTimestamp());
        RouteChangeEventData eventData = new RouteChangeEventData(List.of("all"), Collections.emptyList());
        throttler.sendEvent(eventData);
        LOGGER.info("Initial route change event published for all services");
    }
}
