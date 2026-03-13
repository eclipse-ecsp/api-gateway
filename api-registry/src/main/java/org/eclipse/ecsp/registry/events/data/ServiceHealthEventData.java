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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.ecsp.registry.events.RouteEventType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Event data for service health status changes.
 * Contains service names and their health status.
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceHealthEventData extends AbstractEventData {
    private static final long serialVersionUID = 1L;
    
    /**
     * List of service names with health status changes.
     */
    private final List<String> services;
    
    /**
     * Health status for each service.
     * Map of serviceName -&gt; healthStatus (UP, DOWN, DEGRADED)
     */
    private final Map<String, String> healthStatuses;
    
    /**
     * Constructor for ServiceHealthEventData.
     *
     * @param services list of service names
     * @param healthStatuses map of service health statuses
     */
    public ServiceHealthEventData(
            List<String> services,
            Map<String, String> healthStatuses) {
        super();
        this.services = Collections.unmodifiableList(new ArrayList<>(services));
        this.healthStatuses = healthStatuses != null
            ? Collections.unmodifiableMap(new HashMap<>(healthStatuses))
            : Collections.emptyMap();
    }
    
    @Override
    public RouteEventType getEventType() {
        return RouteEventType.SERVICE_HEALTH_CHANGE;
    }
}
