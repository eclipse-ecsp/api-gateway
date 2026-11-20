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

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.eclipse.ecsp.gateway.events.RouteRejectionsEvent;
import org.eclipse.ecsp.gateway.events.RouteRejectionsEvent.RejectedRoute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RouteRejectionMetrics}.
 */
class RouteRejectionMetricsTest {

    private static final String REASON = RouteRejectionsEvent.REASON_UNKNOWN_FILTER;

    private PrometheusMeterRegistry meterRegistry;
    private RouteRejectionMetrics metrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        metrics = new RouteRejectionMetrics(meterRegistry);
    }

    @Test
    void testCounterExistsAtZeroBeforeAnyRejection() {
        assertThat(count()).isZero();
    }

    @Test
    void testCounterIsExportedAsGatewayRouteRejectedTotal() {
        metrics.countRejectedRoutes(event(rejected("route-1")));

        assertThat(meterRegistry.scrape()).contains("gateway_route_rejected_total", "reason=\"unknown_filter\"");
    }

    @Test
    void testEveryRejectedRouteIsCountedOncePerRefresh() {
        metrics.countRejectedRoutes(event(rejected("route-1"), rejected("route-2")));
        metrics.countRejectedRoutes(event(rejected("route-1")));

        assertThat(count()).isEqualTo(3.0);
    }

    @Test
    void testRefreshWithoutRejectionsDoesNotCount() {
        metrics.countRejectedRoutes(event());

        assertThat(count()).isZero();
    }

    private double count() {
        return meterRegistry.get(RouteRejectionMetrics.METRIC_NAME).tag("reason", REASON).counter().count();
    }

    private static RejectedRoute rejected(String routeId) {
        return new RejectedRoute(routeId, REASON, "NoSuchFilter");
    }

    private static RouteRejectionsEvent event(RejectedRoute... rejectedRoutes) {
        return new RouteRejectionsEvent(List.of(rejectedRoutes));
    }
}
