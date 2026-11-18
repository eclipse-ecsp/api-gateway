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

package org.eclipse.ecsp.gateway.health;

import org.eclipse.ecsp.gateway.events.RouteRejectionsEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Health indicator for the routes api-gateway excluded from its routing table.
 *
 * <p>Reports {@code DEGRADED} while the latest route refresh excluded at least one route, with the id and the reason
 * of every excluded route, and {@code UP} otherwise. Spring Boot's default status aggregator does not rank a custom
 * status, so this indicator never changes the overall health of the gateway; a rejected route does not make the
 * gateway unready.
 */
@Component
@ConditionalOnProperty(value = "api.dynamic.routes.enabled", havingValue = "true")
public class RejectedRoutesHealthIndicator implements HealthIndicator {

    private volatile Map<String, String> rejectedRoutes = Map.of();

    /**
     * Remembers the outcome of the latest route refresh, replacing the one of the previous refresh.
     *
     * @param event the routes rejected by the refresh
     */
    @EventListener(RouteRejectionsEvent.class)
    public void onRouteRejections(RouteRejectionsEvent event) {
        Map<String, String> latest = new TreeMap<>();
        event.getRejectedRoutes().forEach(route ->
                latest.putIfAbsent(route.routeId(), route.reason() + ": " + route.detail()));
        rejectedRoutes = Collections.unmodifiableMap(latest);
    }

    @Override
    public Health health() {
        Map<String, String> current = rejectedRoutes;
        if (current.isEmpty()) {
            return Health.up().build();
        }
        return Health.status("DEGRADED")
                .withDetail("rejectedRouteCount", current.size())
                .withDetail("rejectedRoutes", current)
                .build();
    }
}
