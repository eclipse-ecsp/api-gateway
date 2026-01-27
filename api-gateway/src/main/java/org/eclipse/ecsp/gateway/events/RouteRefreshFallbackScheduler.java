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

package org.eclipse.ecsp.gateway.events;

import lombok.RequiredArgsConstructor;
import org.eclipse.ecsp.gateway.conditions.RouteRefreshEventEnabledCondition;
import org.eclipse.ecsp.gateway.service.RouteRefreshService;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler that periodically checks Redis connectivity and triggers.
 * route refresh polling if Redis is unavailable.
 */
@Component
@EnableAsync
@EnableScheduling
@RequiredArgsConstructor
@Conditional(RouteRefreshEventEnabledCondition.class)
public class RouteRefreshFallbackScheduler {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RouteRefreshFallbackScheduler.class);
    private final RouteRefreshService routeRefreshService;
    private final RedisConnectionFactory redisConnectionFactory;
    
    private final AtomicBoolean redisAvailable = new AtomicBoolean(true);
    private final AtomicBoolean fallbackActive = new AtomicBoolean(false);

    /**
     * Periodically checks Redis connectivity and triggers polling if unavailable.
     * Runs every 30 seconds (configurable via properties).
     */
    @Async
    @Scheduled(cron = "${" + GatewayConstants.ROUTE_REFRESH_POLLING_CRON_EXPR + ":0/30 * * * * *}")
    public void checkRedisAndRefreshRoutes() {
        boolean currentRedisStatus = checkRedisConnection();
        
        // Redis became unavailable
        if (!currentRedisStatus && redisAvailable.getAndSet(false)) {
            LOGGER.warn("Redis connection lost. Activating polling fallback for route refresh.");
            fallbackActive.set(true);
        }
        
        // Redis became available again
        if (currentRedisStatus && !redisAvailable.getAndSet(true)) {
            LOGGER.info("Redis connection restored. Event-driven route refresh is active."
                + " Deactivating polling fallback.");
            fallbackActive.set(false);
            return; // Don't poll when Redis is back
        }
        
        // Execute polling refresh if in fallback mode
        if (!currentRedisStatus && fallbackActive.get()) {
            LOGGER.debug("Executing fallback polling route refresh (Redis unavailable)");
            try {
                routeRefreshService.refreshRoutes();
                LOGGER.debug("Fallback polling route refresh completed successfully");
            } catch (Exception e) {
                LOGGER.error("Failed to refresh routes during fallback polling", e);
            }
        }
    }

    /**
     * Checks if Redis connection is available.
     *
     * @return true if Redis is reachable, false otherwise
     */
    private boolean checkRedisConnection() {
        try {
            // Try to get a connection and ping Redis
            redisConnectionFactory.getConnection().ping();
            return true;
        } catch (Exception e) {
            LOGGER.debug("Redis connection check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Manually trigger fallback mode (useful for testing or emergency scenarios).
     */
    public void activateFallback() {
        LOGGER.warn("Manually activating polling fallback mode");
        redisAvailable.set(false);
        fallbackActive.set(true);
    }

    /**
     * Check if fallback mode is currently active.
     *
     * @return true if using polling fallback, false if using event-driven
     */
    public boolean isFallbackActive() {
        return fallbackActive.get();
    }
}
