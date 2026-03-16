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

package org.eclipse.ecsp.gateway.customizers;

import org.eclipse.ecsp.gateway.config.ClientAccessControlProperties;
import org.eclipse.ecsp.gateway.filter.ClientAccessControlGatewayFilterFactory;
import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.support.NameUtils;
import org.springframework.util.AntPathMatcher;

import java.util.HashMap;
import java.util.Map;

/**
 * ClientAccessControlCustomizer is a RouteCustomizer that adds a ClientAccessControl filter to routes
 * based on certain conditions. It checks if the route should be skipped based on API docs,
 * skip paths, or if it has no filters defined. 
 * If not skipped, it adds the ClientAccessControl filter with the service name and route ID as arguments.
 */
public class ClientAccessControlCustomizer implements RouteCustomizer {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ClientAccessControlCustomizer.class);
    private final ClientAccessControlProperties clientAccessControlProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Constructor for ClientAccessControlCustomizer.
     *
     * @param clientAccessControlProperties properties for client access control configuration
     */
    public ClientAccessControlCustomizer(ClientAccessControlProperties clientAccessControlProperties) {
        this.clientAccessControlProperties = clientAccessControlProperties;
    }

    @Override
    public RouteDefinition customize(RouteDefinition routeDefinition, IgniteRouteDefinition igniteRouteDefinition) {
        // match if the route's predicates match any of the skipPaths, if so, skip
        // adding the filter
        String routePath = igniteRouteDefinition.getPredicates().stream()
                .filter(predicate -> "Path".equals(predicate.getName()))
                .flatMap(predicate -> predicate.getArgs().values().stream())
                .findFirst()
                .orElse("");

        // Check if the route is api docs
        // check if the route path matches any of the skip paths defined in properties
        // also check if the route has no filters defined (probably a public api without any auth)
        boolean shouldSkip = (igniteRouteDefinition.getApiDocs() != null && igniteRouteDefinition.getApiDocs())
                || (clientAccessControlProperties.getSkipPaths().stream() 
                    .anyMatch(skipPath -> pathMatcher.match(skipPath, routePath)))
                || (igniteRouteDefinition.getFilters().isEmpty()); 

        if (shouldSkip || Boolean.TRUE.equals(igniteRouteDefinition.getApiDocs())) {
            LOGGER.debug("Skipping ClientAccessControl filter for route {} with path {}",
                    igniteRouteDefinition.getId(), routePath);
            return routeDefinition;
        }

        FilterDefinition filter = new FilterDefinition();
        filter.setName(NameUtils.normalizeFilterFactoryName(ClientAccessControlGatewayFilterFactory.class));
        Map<String, String> args = new HashMap<>();
        args.put("serviceName", igniteRouteDefinition.getService());
        args.put("routeId", igniteRouteDefinition.getId());
        filter.setArgs(args);
        routeDefinition.getFilters().add(filter);
        LOGGER.debug("Added ClientAccessControl filter for route {} with path {}",
                igniteRouteDefinition.getId(), routePath);
        return routeDefinition;
    }

}
