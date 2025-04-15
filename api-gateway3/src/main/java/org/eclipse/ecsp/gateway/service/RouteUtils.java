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

import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map.Entry;

/**
 * Utility class for route related operations.
 */
@Component
public class RouteUtils {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RouteUtils.class);
    @Value("${api-gateway.uri}")
    private String apiGatewayUri;

    /**
     * getRoutePath returns .
     *
     * @param predicates route predicates
     * @return returns the route path
     */
    public static String getRoutePath(List<PredicateDefinition> predicates) {
        String routePath = null;
        for (PredicateDefinition predicate : predicates) {
            LOGGER.info("predicate name {}", predicate.getName());
            if (GatewayConstants.PATH.equalsIgnoreCase(predicate.getName())) {
                LOGGER.info("predicates info {}", predicate.getArgs().toString());
                for (Entry<String, String> entry : predicate.getArgs().entrySet()) {
                    LOGGER.info("Route: " + entry.getValue());
                    routePath =  entry.getValue();
                }
            }
        }
        return routePath;
    }

    /**
     * getRouteMethod returns route method.
     *
     * @param predicates route predicate
     * @return route method
     */
    public static String getRouteMethod(List<PredicateDefinition> predicates) {
        String routeMethod = null;
        for (PredicateDefinition predicate : predicates) {
            if (GatewayConstants.METHOD.equalsIgnoreCase(predicate.getName())) {
                for (Entry<String, String> entry : predicate.getArgs().entrySet()) {
                    LOGGER.info("Route: " + entry.getValue());
                    routeMethod =  entry.getValue();
                }
            }
        }
        return routeMethod;
    }

    /**
     * get dummy route definition.
     *
     * @return dummy route definition
     */
    public IgniteRouteDefinition getDummyRoute() {
        IgniteRouteDefinition dummy = new IgniteRouteDefinition();
        LOGGER.info("Loading Dummy Route");
        try {
            // Return this dummy in case of error from api-registry service.
            dummy.setId("DUMMY");
            // Set Route Predicates
            PredicateDefinition pathPred = new PredicateDefinition();
            pathPred.setName(GatewayConstants.PATH);
            pathPred.getArgs().put(GatewayConstants.KEY_0, "/**");
            dummy.getPredicates().add(pathPred);
            dummy.setUri(new URI(apiGatewayUri));
        } catch (URISyntaxException e) {
            LOGGER.error("Exception occurred {}", e);
        }
        return dummy;
    }

}
