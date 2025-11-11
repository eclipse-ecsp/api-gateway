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

package org.eclipse.ecsp.gateway.clients;

import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.eclipse.ecsp.gateway.model.RateLimit;
import org.eclipse.ecsp.gateway.service.RouteUtils;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client for interacting with the API Registry to routes.
 * This client fetches route definitions from the API registry service.
 * It uses WebClient to make HTTP requests and retrieve route definitions.
 * The base URL for the API registry is configurable via application properties.
 *
 * @author Abhishek Kumar
 */
@Component
@ConditionalOnBooleanProperty(name = "api.registry.enabled")
public class ApiRegistryClient {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ApiRegistryClient.class);
    @Value("${api.registry.routes-endpoint:/api/v1/routes}")
    private String routesEndpoint;
    @Value("${api.registry.route-scopes:SYSTEM_READ}")
    private String routeScopes;
    @Value("${api.registry.route.user-id:1}")
    private String routeUserId;
    @Value("${api.registry.rate-limits-endpoint:/v1/config/rate-limits}")
    private String rateLimitsEndpoint;
    private final WebClient webClient;
    private final RouteUtils routeUtils;
    
    // Thread-safe cache to store last successfully fetched routes
    private final List<IgniteRouteDefinition> cachedRoutes = new CopyOnWriteArrayList<>();

    /**
     * Constructor for ApiRegistryClient.
     *
     * @param baseUrl the base URL of the API registry
     * @param webClientBuilder the WebClient builder to create the WebClient instance
     * @param routeUtils routeUtils for handling routes
     */
    public ApiRegistryClient(@Value("${api.registry.base-url:http://localhost:7000}") String baseUrl,
                             WebClient.Builder webClientBuilder,
                             RouteUtils routeUtils) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.routeUtils = routeUtils;
        LOGGER.info("ApiRegistryClient initialized with base URL: {}", baseUrl);
    }

    /**
     * Fetches routes from the API registry.
     * If the registry is unavailable or returns empty routes, returns cached routes from the last successful fetch.
     * Only returns dummy route if no cached routes are available.
     *
     * @return list of routes from registry or cached routes
     */
    public Flux<IgniteRouteDefinition> getRoutes() {
        LOGGER.debug("Loading API Routes");
        // @formatter:off
        return this.webClient.get().uri(routesEndpoint)
                .accept(MediaType.APPLICATION_JSON)
                .header(GatewayConstants.USER_ID, routeUserId)
                .header(GatewayConstants.SCOPE, routeScopes)
                .retrieve()
                .bodyToFlux(IgniteRouteDefinition.class)
                .collectList()
                .flatMapMany(routes -> {
                    if (routes != null && !routes.isEmpty()) {
                        LOGGER.info("Successfully fetched {} routes from api-registry", routes.size());
                        // Update cache with successfully fetched routes
                        clearCache();
                        cachedRoutes.addAll(routes);
                        return Flux.fromIterable(routes);
                    } else {
                        LOGGER.warn("API registry returned empty routes list");
                        return handleEmptyOrErrorResponse();
                    }
                })
                .doOnError(throwable -> LOGGER.error("Error while fetching routes from api-registry: {}",
                        throwable.getMessage()))
                .onErrorResume(e -> handleEmptyOrErrorResponse());
        // @formatter:on
    }

    /**
     * Handles the case when API registry is unavailable or returns empty routes.
     * Returns cached routes if available, otherwise returns dummy route.
     *
     * @return cached routes or dummy route
     */
    private Flux<IgniteRouteDefinition> handleEmptyOrErrorResponse() {
        if (hasCachedRoutes()) {
            LOGGER.warn("API registry unavailable or returned empty routes. "
                    + "Using {} cached routes from last successful fetch", 
                    getCachedRoutesCount());
            return Flux.fromIterable(new ArrayList<>(cachedRoutes));
        } else {
            LOGGER.error("No cached routes available. Returning dummy route");
            return Flux.just(routeUtils.getDummyRoute());
        }
    }

    /**
     * Checks if cached routes are available.
     *
     * @return true if cached routes exist, false otherwise
     */
    public boolean hasCachedRoutes() {
        return !cachedRoutes.isEmpty();
    }

    /**
     * Gets the number of cached routes.
     *
     * @return number of cached routes
     */
    public int getCachedRoutesCount() {
        return cachedRoutes.size();
    }

    /**
     * Clears the cached routes.
     * This method is useful for testing or manual cache invalidation.
     */
    public void clearCache() {
        LOGGER.info("Clearing route cache");
        cachedRoutes.clear();
    }

    /**
     * Fetches rate limit configurations from the API registry service.
     *
     * @return list of rate limit definitions provided by the registry
     */
    public List<RateLimit> getRateLimits() {
        LOGGER.debug("Loading API Rate Limits");
        // @formatter:off
        return this.webClient.get().uri(rateLimitsEndpoint)
                .accept(MediaType.APPLICATION_JSON)
                .header(GatewayConstants.USER_ID, routeUserId)
                .header(GatewayConstants.SCOPE, routeScopes)
                .retrieve()
                .bodyToFlux(RateLimit.class)
                .collectList()
                .block();
        // @formatter:on
    }
}
