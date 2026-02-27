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

import org.eclipse.ecsp.registry.events.RouteEventType;
import org.eclipse.ecsp.registry.events.data.AbstractEventData;

/**
 * Strategy interface for publishing events.
 *
 * @param <T> The specific AbstractEventData type this strategy handles
 */
public interface EventPublishingStrategy<T extends AbstractEventData> {
    /**
     * Publish event to Redis.
     *
     * @param eventData Data specific to the event type
     * @return true if published successfully
     */
    boolean publish(T eventData);
    
    /**
     * Get the event type this strategy handles.
     *
     * @return event type
     */
    RouteEventType getEventType();
}
