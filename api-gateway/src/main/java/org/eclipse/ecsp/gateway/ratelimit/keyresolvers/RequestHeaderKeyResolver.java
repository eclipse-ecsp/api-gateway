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

import org.eclipse.ecsp.gateway.service.RouteUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono; 
import java.util.Map;

/**
 * Class to get the header value from server web exchange object.
 *
 * @author Abhishek Kumar
 */
public class RequestHeaderKeyResolver implements KeyResolver {
    
    /**
     * Creates a logger instance.
     */
    private static final IgniteLogger LOGGER
            = IgniteLoggerFactory.getLogger(RequestHeaderKeyResolver.class);

    /**
     * method to return the header value.
     *
     * @param exchange ServerWebExchange object
     * @return header value from ServerWebExchange object
     */
    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        Map<String, Object> rateLimitConfig = RouteUtils.getRateLimitArgs(route);

        String headerName = (String) rateLimitConfig.get("headerName");
        if (headerName == null || headerName.isEmpty()) {
            LOGGER.error("Header name is not configured for RequestHeaderKeyResolver");
            return Mono.empty();
        }

        String headerValue = exchange.getRequest().getHeaders().entrySet()
            .stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(headerName))
            .map(entry -> entry.getValue().get(0))
            .findFirst()
            .orElse(null);

        if (headerValue == null || headerValue.isEmpty()) {
            LOGGER.error("Header value is empty for header name: {}, route: {}", headerName, route.getId());
            return Mono.empty();
        }

        LOGGER.debug("Request Header Key Resolver - Header Name: {}, Key Value: {}", headerName, headerValue);
        return Mono.just(headerValue);
    }
}