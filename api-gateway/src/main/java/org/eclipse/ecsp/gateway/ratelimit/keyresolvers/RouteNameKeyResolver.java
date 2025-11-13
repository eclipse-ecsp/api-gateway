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

package org.eclipse.ecsp.gateway.ratelimit.keyresolvers;

import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.eclipse.ecsp.gateway.utils.GatewayConstants.SERVICE_NAME;

/**
 * Key Resolver to get the route name from server web exchange object for rate limiting.
 *
 * @author Abhishek Kumar
 */
public class RouteNameKeyResolver implements KeyResolver {

    /**
     * Creates a logger instance.
     */
    private static final IgniteLogger LOGGER
            = IgniteLoggerFactory.getLogger(RouteNameKeyResolver.class);

    /**
     * Returns the route name for rate limiting.
     *
     * @param exchange ServerWebExchange object
     * @return route name from ServerWebExchange object
     */
    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        LOGGER.debug("Resolving Route Name for Rate Limiting");
        Route route = (Route) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        String serviceName = (String) route.getMetadata().get(SERVICE_NAME);

        LOGGER.debug("Route Name Key Resolver - Service Name: {}, Route ID: {}", serviceName, route.getId());
        return Mono.just(serviceName + ":" + route.getId());
        
    }
}