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


import jakarta.annotation.PostConstruct;
import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * RegistryRouteLoader.
 */
@Service
public class RegistryRouteLoader {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RegistryRouteLoader.class);

    @Value("${api.registry.uri}")
    private String apiRegistryUri;

    @Value("${api.registry.route-path:/api/v1/routes}")
    private String apiRegistryRoutePath;

    private final RouteUtils routeUtils;

    private final WebClient.Builder builder;

    private WebClient client;

    /**
     * Constructor to initialize the RegistryRouteLoader.
     *
     * @param routeUtils the RouteUtils
     * @param builder    the WebClient.Builder
     */
    public RegistryRouteLoader(RouteUtils routeUtils,
                               WebClient.Builder builder) {
        this.routeUtils = routeUtils;
        this.builder = builder;
    }

    /**
     * Prepare WebClient.
     */
    @PostConstruct
    public void prepareWebClient() {
        this.client = builder.baseUrl(apiRegistryUri).build();
    }

    /**
     * Get the Routes.
     *
     * @return IgniteRouteDefinition
     */
    public Flux<IgniteRouteDefinition> getRoutes() {
        LOGGER.debug("Loading API Routes");

        // @formatter:off
        return this.client.get().uri(apiRegistryRoutePath)
            .accept(MediaType.APPLICATION_JSON)
            .header(GatewayConstants.USER_ID, "1")
            .header(GatewayConstants.SCOPE, "SYSTEM_READ")
            .retrieve().bodyToFlux(IgniteRouteDefinition.class)
            .onErrorReturn(routeUtils.getDummyRoute());
        // @formatter:on
    }

}