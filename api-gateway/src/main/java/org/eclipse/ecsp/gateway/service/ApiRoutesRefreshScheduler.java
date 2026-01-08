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

package org.eclipse.ecsp.gateway.service;

import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * ApiRoutesRefreshScheduler to load the routes dynamically.
 */
@Service
@EnableAsync
@EnableScheduling
@ConditionalOnProperty(name = GatewayConstants.ROUTE_REFRESH_EVENT_STRATEGY, havingValue = GatewayConstants.POLLING)
public class ApiRoutesRefreshScheduler {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ApiRoutesRefreshScheduler.class);

    private final RouteRefreshService routeRefreshService;

    /**
     * Constructor to initialize the ApiRoutesRefreshScheduler with RouteRefreshService.
     *
     * @param routeRefreshService the RouteRefreshService instance
     */
    public ApiRoutesRefreshScheduler(RouteRefreshService routeRefreshService) {
        this.routeRefreshService = routeRefreshService;
    }

    /**
     * Constructor to initialize the ApiRoutesRefreshScheduler with RouteRefreshService.
     */
    @Async()
    @Scheduled(
        cron = "${" + GatewayConstants.ROUTE_REFRESH_POLLING_CRON_EXPR + ":0/30 * * * * *}"
    )
    public void reload() {
        LOGGER.debug("Reload ApiRoutes Event");
        routeRefreshService.refreshRoutes();
    }

}