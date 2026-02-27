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

package org.eclipse.ecsp.registry.events.data;

import org.eclipse.ecsp.registry.events.RouteEventType;

import java.io.Serializable;
import java.time.Instant;

/**
 * Base interface for all event data.
 * Provides common fields required by all events.
 */
public interface EventData extends Serializable {
    /**
     * Unique event identifier for deduplication.
     *
     * @return event ID
     */
    String getEventId();
    
    /**
     * Timestamp when the event was generated.
     *
     * @return event timestamp
     */
    Instant getTimestamp();
    
    /**
     * Type of event.
     *
     * @return event type
     */
    RouteEventType getEventType();
}
