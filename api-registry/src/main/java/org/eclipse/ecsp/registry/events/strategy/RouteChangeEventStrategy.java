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

package org.eclipse.ecsp.registry.events.strategy;

import org.eclipse.ecsp.registry.events.RouteEventThrottler;
import org.eclipse.ecsp.registry.events.RouteEventType;
import org.eclipse.ecsp.registry.events.data.RouteChangeEventData;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Strategy for route change events with throttling.
 */
@Component
public class RouteChangeEventStrategy implements EventPublishingStrategy<RouteChangeEventData> {
    
    private static final IgniteLogger LOGGER = 
        IgniteLoggerFactory.getLogger(RouteChangeEventStrategy.class);
    
    private final RouteEventThrottler throttler;
    
    /**
     * Constructor for RouteChangeEventStrategy.
     *
     * @param throttler event throttler
     */
    public RouteChangeEventStrategy(RouteEventThrottler throttler) {
        this.throttler = throttler;
    }
    
    @Override
    public boolean publish(RouteChangeEventData eventData) {
        // Apply throttling for route changes - metrics tracked automatically by throttler
        eventData.getServices().forEach(throttler::scheduleEvent);
        
        LOGGER.debug("Scheduled route change events for {} services", 
            eventData.getServices().size());
        return true;
    }
    
    @Override
    public RouteEventType getEventType() {
        return RouteEventType.ROUTE_CHANGE;
    }
}
