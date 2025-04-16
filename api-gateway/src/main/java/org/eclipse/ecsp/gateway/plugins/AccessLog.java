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

package org.eclipse.ecsp.gateway.plugins;

import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * AccessLog is a Global Filter.
 */
@Component
public class AccessLog implements GlobalFilter, Ordered {
    /**
     * created a logger instance.
     */
    private static final IgniteLogger LOGGER =
            IgniteLoggerFactory.getLogger(AccessLog.class);

    /**
     * Method intercept the request and add the logger.
     *
     * @param exchange serverwebexchange object
     * @param chain    filter chain object
     * @return returns FilterChain obejct
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            if (LOGGER.isInfoEnabled()) {
                Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
                LOGGER.info("Url: {} {}, RouteId: {}, Status:{}, Time: {} ms",
                        exchange.getRequest().getMethod().name(),
                        exchange.getRequest().getURI(),
                        route != null ? route.getId() : "UNKNOWN",
                        exchange.getResponse().getStatusCode(),
                        (System.currentTimeMillis() - startTime));
            }
        }));
    }

    /**
     * setting execution order of the filter.
     *
     * @return returns order of the filter
     */
    @Override
    public int getOrder() {
        return GatewayConstants.ACCESS_LOG_FILTER_ORDER;
    }
}
