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

package org.eclipse.ecsp.gateway.events;

import lombok.Getter;
import java.util.List;

/**
 * Reports the routes api-gateway excluded from its routing table during one route refresh.
 *
 * <p>The event is published once per refresh, also when no route was rejected, so that a listener always knows
 * the outcome of the latest refresh and can clear a previously reported problem.
 */
@Getter
public class RouteRejectionsEvent {

    /**
     * Reason of a route that configures a filter no installed gateway filter factory provides.
     */
    public static final String REASON_UNKNOWN_FILTER = "unknown_filter";

    private final List<RejectedRoute> rejectedRoutes;

    /**
     * Creates the event for one route refresh.
     *
     * @param rejectedRoutes the routes rejected by the refresh; empty when every route was accepted
     */
    public RouteRejectionsEvent(List<RejectedRoute> rejectedRoutes) {
        this.rejectedRoutes = List.copyOf(rejectedRoutes);
    }

    /**
     * A route that was excluded from the routing table.
     *
     * @param routeId id of the excluded route
     * @param reason  why the route was excluded, one of the {@code REASON_*} constants
     * @param detail  what caused the exclusion, such as the name of the unknown filter
     */
    public record RejectedRoute(String routeId, String reason, String detail) {
    }
}
