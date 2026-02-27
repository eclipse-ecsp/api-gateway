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
 * See the License for the specific language governing permissions and\
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.gateway.health;

import lombok.RequiredArgsConstructor;
import org.eclipse.ecsp.gateway.conditions.RouteRefreshEventEnabledCondition;
import org.eclipse.ecsp.gateway.config.RouteRefreshProperties;
import org.eclipse.ecsp.gateway.events.RouteRefreshFallbackScheduler;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Health indicator for route refresh mechanism.
 * Reports the current mode (event-driven or polling fallback) and Redis status.
 */
@Component
@RequiredArgsConstructor
@Conditional(RouteRefreshEventEnabledCondition.class)
public class RouteRefreshHealthIndicator implements HealthIndicator {

    private final RouteRefreshFallbackScheduler fallbackScheduler;
    private final RouteRefreshProperties properties;

    @Override
    public Health health() {
        try {
            boolean redisConnected = fallbackScheduler.checkRedisConnection();
            boolean fallbackActive = fallbackScheduler.isFallbackActive();
            
            Health.Builder builder = redisConnected && !fallbackActive 
                ? Health.up() 
                : Health.status("DEGRADED");
            
            return builder
                .withDetail("strategy", properties.getStrategy())
                .withDetail("mode", fallbackActive ? "POLLING_FALLBACK" : "EVENT_DRIVEN")
                .withDetail("redisConnected", redisConnected)
                .withDetail("channel", properties.getRedis().getChannel())
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("strategy", properties.getStrategy())
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
