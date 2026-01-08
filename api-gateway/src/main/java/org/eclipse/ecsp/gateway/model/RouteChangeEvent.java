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

package org.eclipse.ecsp.gateway.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Event published when routes are created, updated, or deleted.
 *
 * <p>Contains minimal information (service names) to trigger route refresh on API Gateway instances.
 */
public class RouteChangeEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String eventId;
    private final Instant timestamp;
    private final RouteEventType eventType;
    private final List<String> services;

    /**
     * Constructor for RouteChangeEvent.
     *
     * @param services list of service names that changed
     */
    public RouteChangeEvent(List<String> services) {
        this(UUID.randomUUID().toString(), Instant.now(), RouteEventType.ROUTE_CHANGE, services);
    }

    /**
     * Full constructor for RouteChangeEvent (for deserialization).
     *
     * @param eventId   unique event identifier
     * @param timestamp event generation time
     * @param eventType type of route event
     * @param services  list of service names that changed
     */
    @JsonCreator
    public RouteChangeEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("eventType") RouteEventType eventType,
            @JsonProperty("services") List<String> services) {
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.services = services;
    }

    /**
     * Get event ID.
     *
     * @return event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Get event timestamp.
     *
     * @return timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Get event type.
     *
     * @return event type
     */
    public RouteEventType getEventType() {
        return eventType;
    }

    /**
     * Get list of services that changed.
     *
     * @return list of service names
     */
    public List<String> getServices() {
        return services;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RouteChangeEvent that = (RouteChangeEvent) o;
        return Objects.equals(eventId, that.eventId)
                && Objects.equals(timestamp, that.timestamp)
                && eventType == that.eventType
                && Objects.equals(services, that.services);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, timestamp, eventType, services);
    }

    @Override
    public String toString() {
        return "RouteChangeEvent{"
                + "eventId='" + eventId + '\''
                + ", timestamp=" + timestamp
                + ", eventType=" + eventType
                + ", services=" + services
                + '}';
    }
}
