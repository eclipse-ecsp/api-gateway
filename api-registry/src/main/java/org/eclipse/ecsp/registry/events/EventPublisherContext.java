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

import org.eclipse.ecsp.registry.events.data.AbstractEventData;
import org.eclipse.ecsp.registry.events.strategy.EventPublishingStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Context class that uses strategies for publishing events.
 */
@Component
public class EventPublisherContext {
    
    private final Map<RouteEventType, EventPublishingStrategy<? extends AbstractEventData>> strategies;

    /**
     * Constructor for EventPublisherContext.
     *
     * @param strategyList list of all available event publishing strategies
     */
    public EventPublisherContext(List<EventPublishingStrategy<? extends AbstractEventData>> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                EventPublishingStrategy::getEventType,
                Function.identity()
            ));
    }
    
    /**
     * Publish an event using the appropriate strategy.
     *
     * @param eventData event data to publish
     * @param <T> type of event data
     * @return true if published successfully, false otherwise
     */
    @SuppressWarnings("unchecked")
    public <T extends AbstractEventData> boolean publishEvent(T eventData) {
        EventPublishingStrategy<T> strategy = 
            (EventPublishingStrategy<T>) strategies.get(eventData.getEventType());
        
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy for event type: " + eventData.getEventType());
        }
        
        return strategy.publish(eventData);
    }
}
