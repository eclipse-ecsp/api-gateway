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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event published when routes are created, updated, or deleted.
 *
 * <p>Contains minimal information (service names) to trigger route refresh on API Gateway instances.
 */
@Getter
@EqualsAndHashCode
public class RouteChangeEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for the event.
     */
    private final String eventId;
    /**
     * Timestamp when the event was generated.
     */
    private final Instant timestamp;
    /**
     * Type of route event.
     */
    private final RouteEventType eventType;
    /**
     * List of service names that changed.
     */
    private final List<String> services;
    /**
     * List of route IDs that changed.
     */
    private final List<String> routes;

    /**
     * List of client IDs affected by the change (for client access control updates).
     */
    private final List<String> clientIds;

    /**
     * Type of operation performed (CREATE, UPDATE, DELETE).
     */
    private final String operation;

    /**
     * Constructor for RouteChangeEvent.
     *
     * @param services list of service names that changed
     */
    public RouteChangeEvent(List<String> services, List<String> routes, List<String> clientIds, String operation) {
        this(UUID.randomUUID().toString(), 
            Instant.now(), 
            RouteEventType.ROUTE_CHANGE, 
            services, 
            routes, 
            clientIds, 
            operation);
    }

    /**
     * Full constructor for RouteChangeEvent (for deserialization).
     *
     * @param eventId   unique event identifier
     * @param timestamp event generation time
     * @param eventType type of route event
     * @param services  list of service names that changed
     * @param routes    list of route IDs that changed
     * @param clientIds list of client IDs affected by the change
     * @param operation type of operation performed (CREATE, UPDATE, DELETE)
     */
    @JsonCreator
    public RouteChangeEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("eventType") RouteEventType eventType,
            @JsonProperty("services") List<String> services,
            @JsonProperty("routes") List<String> routes,
            @JsonProperty("clientIds") List<String> clientIds,
            @JsonProperty("operation") String operation) {
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.services = services;
        this.routes = routes;
        this.clientIds = clientIds;
        this.operation = operation;
    }
}
