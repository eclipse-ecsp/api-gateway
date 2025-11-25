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

package org.eclipse.ecsp.gateway.ratelimit.configresolvers;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.clients.ApiRegistryClient;
import org.eclipse.ecsp.gateway.config.RateLimitProperties;
import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.eclipse.ecsp.gateway.model.RateLimit;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.event.EventListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rate limit resolver that loads configurations from API Registry
 * and supports hierarchical resolution (route → service → default).
 *
 * <p>Rate limits are loaded from the API Registry at startup and can be
 * overridden via application properties. Resolution follows a hierarchy:
 * <ol>
 *   <li>Route-specific rate limit</li>
 *   <li>Service-level rate limit</li>
 *   <li>Default rate limit</li>
 * </ol>
 *
 * @author Abhishek Kumar
 */
public class DefaultRateLimitConfigResolver implements RateLimitConfigResolver {
    private static final IgniteLogger LOGGER =
            IgniteLoggerFactory.getLogger(DefaultRateLimitConfigResolver.class);
    private final Map<String, RateLimit> routeRateLimitMap = new HashMap<>();
    private final Map<String, RateLimit> serviceRateLimitMap = new HashMap<>();
    private final ApiRegistryClient apiRegistryClient;
    private final List<RateLimit> overrides;
    private RateLimit defaultRateLimit;

    /**
     * Creates a resolver that loads rate limits from the API registry and property overrides.
     *
     * @param apiRegistryClient client used to retrieve registry-backed rate limits
     * @param rateLimitProperties application properties defining defaults and overrides
     */
    public DefaultRateLimitConfigResolver(
        ApiRegistryClient apiRegistryClient,
        RateLimitProperties rateLimitProperties) {
        this.apiRegistryClient = apiRegistryClient;
        this.defaultRateLimit = rateLimitProperties.getDefaults();
        List<RateLimit> configuredOverrides = rateLimitProperties.getOverrides();
        this.overrides = configuredOverrides == null ? Collections.emptyList() : configuredOverrides;
    }

    /**
     * Loads rate limit data from the API registry and applies configured overrides.
     */
    @PostConstruct
    @EventListener(RefreshRoutesEvent.class)
    public void initialize() {
        LOGGER.info("Initializing GatewayRateLimiter and loading rate limits from API Registry");
        List<RateLimit> registryRateLimits = apiRegistryClient.getRateLimits();
        routeRateLimitMap.clear();
        serviceRateLimitMap.clear();
        if (registryRateLimits == null) {
            LOGGER.warn("No rate limits found in API Registry");
        } else {
            LOGGER.info("Loaded {} rate limits from API Registry", registryRateLimits.size());
            registryRateLimits.forEach(rateLimit -> {
                if (StringUtils.isNotBlank(rateLimit.getRouteId())) {
                    LOGGER.info(
                            "Loaded rate limit for routeId: {}, config: {}",
                            rateLimit.getRouteId(),
                            rateLimit);
                    routeRateLimitMap.put(rateLimit.getRouteId(), rateLimit);
                } else if (StringUtils.isNotBlank(rateLimit.getService())) {
                    LOGGER.info(
                            "Loaded rate limit for service: {}, config: {}",
                            rateLimit.getService(),
                            rateLimit);
                    serviceRateLimitMap.put(rateLimit.getService(), rateLimit);
                }
            });
        }
        LOGGER.info("Finished loading rate limits from API Registry");
        for (RateLimit override : overrides) {
            if (StringUtils.isNotBlank(override.getRouteId())) {
                LOGGER.info(
                        "Applying override rate limit for routeId: {}, config: {}",
                        override.getRouteId(),
                        override);
                routeRateLimitMap.put(override.getRouteId(), override);
            } else if (StringUtils.isNotBlank(override.getService())) {
                LOGGER.info(
                        "Applying override rate limit for service: {}, config: {}",
                        override.getService(),
                        override);
                serviceRateLimitMap.put(override.getService(), override);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public RateLimit resolveRateLimit(IgniteRouteDefinition route) {
        LOGGER.debug("Resolving rate limit for routeId: {}", route.getId());
        
        RateLimit rateLimit = routeRateLimitMap.get(route.getId());
        if (rateLimit != null) {
            LOGGER.debug(
                    "Found rate limit for routeId: {}, config: {}",
                    route.getId(),
                    rateLimit);
            return rateLimit;
        }
        rateLimit = serviceRateLimitMap.get(route.getService());
        if (rateLimit != null) {
            LOGGER.debug(
                    "Found rate limit for service: {}, config: {}",
                    route.getService(),
                    rateLimit);
            return rateLimit;
        }
        LOGGER.debug("Using default rate limit for routeId: {}", route.getId());
        return defaultRateLimit;
    }

}
