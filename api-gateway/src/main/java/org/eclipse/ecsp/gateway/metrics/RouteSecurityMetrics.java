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

package org.eclipse.ecsp.gateway.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.event.RefreshRoutesResultEvent;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Component that collects and logs metrics for secured and unsecured routes in the API Gateway.
 * This class listens for route refresh events and processes the routes to determine their security status.
 * Metrics are recorded using Micrometer.
 *
 * @author Abhishek Kumar
 */
@Component
@ConditionalOnProperty(name = "api.gateway.metrics.enabled", havingValue = "true", matchIfMissing = true)
public class RouteSecurityMetrics {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RouteSecurityMetrics.class);

    private final RouteLocator routeLocator;
    private final AtomicInteger totalRoutesCount = new AtomicInteger(0);
    private final AtomicInteger secureRoutesCount = new AtomicInteger(0);
    private final AtomicInteger unsecuredRoutesCount = new AtomicInteger(0);
    private boolean isEnabled = true;
    /**
     * The name of the security filter used to determine if a route is secured.
     * This value is configurable via application properties.
     */
    private String securityFilterName = "JwtAuthFilter";

    /**
     * Constructor for RouteSecurityMetrics.
     *
     * @param routeLocator             the RouteLocator used to get the routes
     * @param meterRegistry            the MeterRegistry used to record metrics
     * @param gatewayMetricsProperties gateway metrics properties
     */
    public RouteSecurityMetrics(RouteLocator routeLocator,
                                MeterRegistry meterRegistry,
                                GatewayMetricsProperties gatewayMetricsProperties) {
        this.routeLocator = routeLocator;
        if (gatewayMetricsProperties != null
                && gatewayMetricsProperties.getSecurityMetrics() != null) {
            String metricsPrefix = "api.gateway";
            if (Boolean.FALSE.equals(gatewayMetricsProperties.getSecurityMetrics().getEnabled())) {
                LOGGER.warn("Route Security metrics is disabled");
                this.isEnabled = false;
            } else {
                if (StringUtils.isNotEmpty(gatewayMetricsProperties.getSecurityMetrics().getPrefix())) {
                    metricsPrefix = gatewayMetricsProperties.getSecurityMetrics().getPrefix();
                    LOGGER.info("RouteSecurityMetrics metrics prefix is {}", metricsPrefix);
                }
                if (StringUtils.isNotEmpty(gatewayMetricsProperties.getSecurityMetrics().getSecurityFilterName())) {
                    this.securityFilterName = gatewayMetricsProperties.getSecurityMetrics().getSecurityFilterName();
                    LOGGER.info("RouteSecurityMetrics securityFilterName is {}", this.securityFilterName);
                }
            }
            Gauge.builder(metricsPrefix + ".routes.secure.count", secureRoutesCount::get).register(meterRegistry);
            Gauge.builder(metricsPrefix + ".routes.unsecure.count", unsecuredRoutesCount::get).register(meterRegistry);
            Gauge.builder(metricsPrefix + ".total.routes.count", totalRoutesCount::get).register(meterRegistry);
        }

    }

    /**
     * Event listener that processes route security metrics when routes are refreshed.
     * It counts the number of secured and unsecured routes and logs the results.
     * Metrics are recorded using the MeterRegistry.
     */
    @EventListener(RefreshRoutesResultEvent.class)
    public void processRouteSecurityMetrics() {
        if (!isEnabled) {
            LOGGER.debug("RouteSecurityMetrics not enabled, skip processing..");
            return;
        }
        // Initialize counters for secure and total routes, and a list to store unsecured route IDs
        AtomicInteger secureRoutes = new AtomicInteger(0);
        AtomicInteger totalRoutes = new AtomicInteger(0);
        Set<String> unsecuredRoutes = new HashSet<>();

        // Subscribe to the route locator to process each route
        routeLocator.getRoutes().subscribe(route -> {
            // Determine if the route is secured by checking its filters
            boolean isSecured = route.getFilters().stream()
                    .map(filter ->
                            filter instanceof AbstractGatewayFilterFactory<?> factory
                                    ? factory.name() : filter.getClass().getSimpleName())
                    .anyMatch(filterName -> filterName.equalsIgnoreCase(this.securityFilterName));
            if (isSecured) {
                secureRoutes.incrementAndGet();
            } else {
                // Log unsecured routes and add them to the list
                LOGGER.debug("Route: {} is not secured", route.getId());
                unsecuredRoutes.add(route.getId());
            }
            // Log detailed information about each route
            totalRoutes.incrementAndGet();
            LOGGER.debug("Route: {}, Secured: {}", route.getId(), isSecured);
        });


        // Log the summary of secure and unsecured routes
        LOGGER.info("Route SecurityMetrics Total Routes: {}"
                        + ", Secure Routes: {}"
                        + ", Unsecure Routes: {}, "
                        + "unsecured route ids: {}",
                totalRoutes.get(),
                secureRoutes.get(),
                unsecuredRoutes.size(),
                unsecuredRoutes.toArray());

        this.secureRoutesCount.set(secureRoutes.get());
        this.unsecuredRoutesCount.set(unsecuredRoutes.size());
        this.totalRoutesCount.set(totalRoutes.get());
    }
}
