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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.ecsp.gateway.events.RouteRejectionsEvent.RejectedRoute;
import org.eclipse.ecsp.gateway.health.RejectedRoutesHealthIndicator;
import org.eclipse.ecsp.gateway.metrics.RouteRejectionMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves in a Spring context that the observers of {@link RouteRejectionsEvent} receive the event published through
 * the application context, as {@code IgniteRouteLocator} publishes it, and that their conditions apply.
 */
class RouteRejectionsEventWiringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
            .withUserConfiguration(RouteRejectionMetrics.class, RejectedRoutesHealthIndicator.class);

    @Test
    void testPublishedEventReachesTheCounterAndTheHealthIndicator() {
        contextRunner.withPropertyValues("api.dynamic.routes.enabled=true").run(context -> {
            context.publishEvent(new RouteRejectionsEvent(List.of(
                    new RejectedRoute("route-1", RouteRejectionsEvent.REASON_UNKNOWN_FILTER, "NoSuchFilter"))));

            double rejected = context.getBean(MeterRegistry.class).get("gateway.route.rejected")
                    .tag("reason", RouteRejectionsEvent.REASON_UNKNOWN_FILTER).counter().count();
            assertThat(rejected).isEqualTo(1.0);
            assertThat(context.getBean(RejectedRoutesHealthIndicator.class).health().getStatus())
                    .isEqualTo(new Status("DEGRADED"));

            context.publishEvent(new RouteRejectionsEvent(List.of()));

            assertThat(context.getBean(RejectedRoutesHealthIndicator.class).health().getStatus())
                    .isEqualTo(Status.UP);
        });
    }

    @Test
    void testHealthIndicatorExistsOnlyWhenDynamicRoutesAreEnabled() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(RejectedRoutesHealthIndicator.class));
        contextRunner.withPropertyValues("api.dynamic.routes.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(RejectedRoutesHealthIndicator.class));
    }

    @Test
    void testMetricsCanBeSwitchedOffWithTheGatewayMetricsFlag() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(RouteRejectionMetrics.class));
        contextRunner.withPropertyValues("api.gateway.metrics.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(RouteRejectionMetrics.class));
    }
}
