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

import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Service to handle route refresh operations in API Gateway.
 */
@Service
public class RouteRefreshService {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RouteRefreshService.class);

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructor for RouteRefreshService.
     *
     * @param eventPublisher Spring application event publisher
     */
    public RouteRefreshService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Refresh routes by publishing RefreshRoutesEvent.
     * This triggers the route definition locator to reload routes.
     */
    public void refreshRoutes() {
        LOGGER.info("Refreshing routes");
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
        LOGGER.debug("RefreshRoutesEvent published");
    }
}
