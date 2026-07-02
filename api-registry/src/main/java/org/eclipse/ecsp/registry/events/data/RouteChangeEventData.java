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
import org.eclipse.ecsp.registry.service.RouteChangeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Event data for route configuration changes.
 *
 * <p>Contains service names and route IDs that were modified, and — when change-detection
 * is enabled — the resolved {@link RouteChangeType} and checksum values for auditing.
 * The new fields are additive; existing consumers that ignore unknown fields are unaffected.
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RouteChangeEventData extends AbstractEventData {
    private static final long serialVersionUID = 1L;

    /**
     * List of service names that changed.
     */
    private final List<String> services;

    /**
     * List of route IDs that changed.
     */
    private final List<String> routes;

    /**
     * Type of change detected for the route; {@code null} when change-detection is disabled.
     */
    private final RouteChangeType changeType;

    /**
     * Checksum of the route after the change; {@code null} for {@link RouteChangeType#DELETED}
     * or when change-detection is disabled.
     */
    private final String currentChecksum;

    /**
     * Checksum of the route before the change; {@code null} for {@link RouteChangeType#NEW}
     * or when change-detection is disabled.
     */
    private final String previousChecksum;

    /**
     * UUID correlating this event to its structured log entry; {@code null} when
     * change-detection is disabled.
     */
    private final String correlationId;

    /**
     * Backward-compatible constructor used when change-detection is disabled.
     *
     * @param services list of service names that changed
     * @param routes   list of route IDs that changed
     */
    public RouteChangeEventData(List<String> services, List<String> routes) {
        this(services, routes, null, null, null, null);
    }

    /**
     * Full constructor used when change-detection is enabled.
     *
     * @param services         list of service names that changed
     * @param routes           list of route IDs that changed
     * @param changeType       resolved change type
     * @param currentChecksum  checksum after the change; {@code null} for DELETED
     * @param previousChecksum checksum before the change; {@code null} for NEW
     * @param correlationId    UUID linking this event to its log entry
     */
    public RouteChangeEventData(List<String> services,
                                List<String> routes,
                                RouteChangeType changeType,
                                String currentChecksum,
                                String previousChecksum,
                                String correlationId) {
        super();
        this.services = services == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(services));
        this.routes = routes == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(routes));
        this.changeType = changeType;
        this.currentChecksum = currentChecksum;
        this.previousChecksum = previousChecksum;
        this.correlationId = correlationId;
    }

    @Override
    public RouteEventType getEventType() {
        return RouteEventType.ROUTE_CHANGE;
    }
}
