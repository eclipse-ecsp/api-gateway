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
import org.eclipse.ecsp.registry.events.data.ServiceHealthEventData;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Strategy for service health events (immediate publish).
 */
@Component
public class ServiceHealthEventStrategy 
        implements EventPublishingStrategy<ServiceHealthEventData> {
    
    private static final IgniteLogger LOGGER = 
        IgniteLoggerFactory.getLogger(ServiceHealthEventStrategy.class);
    
    private final RouteEventThrottler throttler;
    
    /**
     * Constructor for ServiceHealthEventStrategy.
     *
     * @param throttler event throttler
     */
    public ServiceHealthEventStrategy(RouteEventThrottler throttler) {
        this.throttler = throttler;
    }
    
    @Override
    public boolean publish(ServiceHealthEventData eventData) {
        // Direct publish - metrics tracked automatically by throttler
        boolean sent = throttler.sendEvent(eventData);
        
        if (sent) {
            LOGGER.info("Published SERVICE_HEALTH_CHANGE event: eventId={}, services={}",
                eventData.getEventId(), eventData.getServices().size());
        }
        
        return sent;
    }
    
    @Override
    public RouteEventType getEventType() {
        return RouteEventType.SERVICE_HEALTH_CHANGE;
    }
}
