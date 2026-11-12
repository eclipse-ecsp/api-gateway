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

package org.eclipse.ecsp.gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration properties for the route order override feature.
 *
 * <p>Allows the order of a route to be set in the gateway for services that are not yet deployed with an
 * api-registry-common version that can supply the route order. A matching mapping always replaces the order
 * received from the api-registry.
 *
 * <p>Loaded from application.yml under 'api.gateway.routes.order-override.*'
 *
 * <p>Example configuration:
 * <pre>
 * api:
 *   gateway:
 *     routes:
 *       order-override:
 *         enabled: true
 *         mappings:
 *           - route-id: Users-getUser
 *             order: -10
 *           - route-id: user-controller-listUsers
 *             order: 100
 * </pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = GatewayConstants.ROUTE_ORDER_OVERRIDE_PREFIX)
public class RouteOrderOverrideProperties {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RouteOrderOverrideProperties.class);

    /**
     * Enable or disable the route order override globally.
     * Default: false (disabled). When disabled, the mappings are ignored.
     */
    private boolean enabled = false;

    /**
     * Route order overrides, used only when {@link #enabled} is true.
     */
    private List<Mapping> mappings = new ArrayList<>();

    /**
     * Validates the configured mappings when the feature is enabled.
     *
     * @throws IllegalArgumentException if a mapping has a blank route id, a missing order or a duplicate route id
     */
    @PostConstruct
    public void validate() {
        if (!enabled) {
            LOGGER.info("Route order override is disabled");
            return;
        }

        // if mapping is empty - log and return
        if (CollectionUtils.isEmpty(mappings)) {
            LOGGER.debug("Route order override is empty, skipping route order processing");
            return;
        }

        Set<String> routeIds = new HashSet<>();
        for (int index = 0; index < mappings.size(); index++) {
            Mapping mapping = mappings.get(index);
            if (mapping == null || !StringUtils.hasText(mapping.getRouteId())) {
                throw new IllegalArgumentException(
                        "Route order override mapping at index " + index + " must define a non-blank route-id");
            }
            if (mapping.getOrder() == null) {
                throw new IllegalArgumentException(
                        "Route order override mapping for route-id '" + mapping.getRouteId()
                                + "' must define an order");
            }
            if (!routeIds.add(mapping.getRouteId())) {
                throw new IllegalArgumentException(
                        "Duplicate route order override mapping for route-id '" + mapping.getRouteId() + "'");
            }
        }
        LOGGER.info("Route order override is enabled with {} mapping(s)", mappings.size());
    }

    /**
     * A single route order override.
     */
    @Getter
    @Setter
    public static class Mapping {

        /**
         * Route id as stored in the api-registry: first tag and operationId joined by '-'.
         */
        private String routeId;

        /**
         * Spring Cloud Gateway order. Routes with a lower value are evaluated first.
         */
        private Integer order;
    }
}
