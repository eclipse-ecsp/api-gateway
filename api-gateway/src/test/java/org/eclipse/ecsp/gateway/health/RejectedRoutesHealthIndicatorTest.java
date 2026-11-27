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
import org.eclipse.ecsp.gateway.events.RouteRejectionsEvent.RejectedRoute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RejectedRoutesHealthIndicator}.
 */
class RejectedRoutesHealthIndicatorTest {

    private static final Status DEGRADED = new Status("DEGRADED");

    private RejectedRoutesHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new RejectedRoutesHealthIndicator();
    }

    @Test
    void testUpBeforeTheFirstRefresh() {
        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void testDegradedListsEveryRejectedRouteWithItsReason() {
        indicator.onRouteRejections(new RouteRejectionsEvent(List.of(
                new RejectedRoute("route-b", RouteRejectionsEvent.REASON_UNKNOWN_FILTER, "FilterB"),
                new RejectedRoute("route-a", RouteRejectionsEvent.REASON_UNKNOWN_FILTER, "FilterA"))));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(DEGRADED);
        assertThat(health.getDetails()).containsEntry("rejectedRouteCount", 2);
        assertThat(health.getDetails()).containsEntry("rejectedRoutes", Map.of(
                "route-a", "unknown_filter: FilterA",
                "route-b", "unknown_filter: FilterB"));
    }

    @Test
    void testRecoversWhenLaterRefreshRejectsNothing() {
        indicator.onRouteRejections(new RouteRejectionsEvent(List.of(
                new RejectedRoute("route-a", RouteRejectionsEvent.REASON_UNKNOWN_FILTER, "FilterA"))));

        indicator.onRouteRejections(new RouteRejectionsEvent(List.of()));

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }
}
