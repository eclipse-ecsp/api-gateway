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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.ecsp.registry.events.RouteEventType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Event data for rate limit configuration changes.
 * Contains service names and route IDs affected by rate limit changes.
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public class RateLimitConfigEventData extends AbstractEventData {
    private static final long serialVersionUID = 1L;
    
    private final List<String> services;
    private final List<String> routes;
    
    /**
     * Constructor for RateLimitConfigEventData.
     *
     * @param services list of service names affected
     * @param routes list of route IDs affected
     */
    public RateLimitConfigEventData(
            List<String> services,
            List<String> routes) {
        this.services = Collections.unmodifiableList(new ArrayList<>(services));
        this.routes = Collections.unmodifiableList(new ArrayList<>(routes));
    }
    
    /**
     * Full constructor for RateLimitConfigEventData (for deserialization).
     *
     * @param services list of service names affected
     * @param routes list of route IDs affected
     */
    @JsonCreator
    public RateLimitConfigEventData(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("services") List<String> services,
            @JsonProperty("routes") List<String> routes) {
        super();
        this.services = Collections.unmodifiableList(new ArrayList<>(services));
        this.routes = Collections.unmodifiableList(new ArrayList<>(routes));
    }
    
    @Override
    public RouteEventType getEventType() {
        return RouteEventType.RATE_LIMIT_CONFIG_CHANGE;
    }
}
