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

package org.eclipse.ecsp.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.eclipse.ecsp.gateway.events.RouteRejectionsEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Counts the routes api-gateway excluded from its routing table.
 *
 * <p>Registers the counter {@code gateway.route.rejected}, tagged with the rejection {@code reason}, which Prometheus
 * exposes as {@code gateway_route_rejected_total}. The counter is incremented once per rejected route and per route
 * refresh, so a route that stays rejected keeps counting for as long as the refresh runs.
 */
@Component
@ConditionalOnProperty(name = "api.gateway.metrics.enabled", havingValue = "true", matchIfMissing = true)
public class RouteRejectionMetrics {

    static final String METRIC_NAME = "gateway.route.rejected";
    private static final String TAG_REASON = "reason";

    private final MeterRegistry meterRegistry;

    /**
     * Constructor for RouteRejectionMetrics.
     *
     * @param meterRegistry the MeterRegistry used to record the counter
     */
    public RouteRejectionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        // Registered up front so the series exists at zero before the first rejection, which keeps rate() usable.
        counter(RouteRejectionsEvent.REASON_UNKNOWN_FILTER);
    }

    /**
     * Counts every route rejected by a route refresh.
     *
     * @param event the routes rejected by the refresh
     */
    @EventListener(RouteRejectionsEvent.class)
    public void countRejectedRoutes(RouteRejectionsEvent event) {
        event.getRejectedRoutes().forEach(route -> counter(route.reason()).increment());
    }

    private Counter counter(String reason) {
        return Counter.builder(METRIC_NAME)
                .description("Routes excluded from the routing table, by reason")
                .tag(TAG_REASON, reason)
                .register(meterRegistry);
    }
}
