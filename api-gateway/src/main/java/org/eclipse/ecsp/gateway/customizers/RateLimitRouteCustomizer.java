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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.eclipse.ecsp.gateway.model.RateLimit;
import org.eclipse.ecsp.gateway.ratelimit.configresolvers.RateLimitConfigResolver;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteDefinition;
import java.util.HashMap;
import java.util.Map;

/**
 * Customizer to apply rate limiting to routes based on RateLimit configuration.
 *
 * @author Abhishek Kumar
 */
public class RateLimitRouteCustomizer implements RouteCustomizer {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RateLimitRouteCustomizer.class);

    private final RateLimitConfigResolver rateLimitConfigResolver;

    /**
     * Constructor to initialize RateLimitRouteCustomizer.
     *
     * @param rateLimitConfigResolver the RateLimitConfigResolver
     */
    public RateLimitRouteCustomizer(RateLimitConfigResolver rateLimitConfigResolver) {
        this.rateLimitConfigResolver = rateLimitConfigResolver;
    }

    /** {@inheritDoc} */
    @Override
    public RouteDefinition customize(RouteDefinition routeDefinition, IgniteRouteDefinition igniteRouteDefinition) {
        LOGGER.debug("Customizing route {} with rate limit", igniteRouteDefinition.getId());
        RateLimit rateLimit = rateLimitConfigResolver.resolveRateLimit(igniteRouteDefinition);
        if (rateLimit != null) {
            LOGGER.debug(
                    "Applying rate limit: replenishRate={}, burstCapacity={}, rateLimitType={}",
                    rateLimit.getReplenishRate(),
                    rateLimit.getBurstCapacity(),
                    rateLimit.getRateLimitType());
            Map<String, String> config = new HashMap<>();
            config.put(
                    RedisRateLimiter.CONFIGURATION_PROPERTY_NAME + ".replenishRate",
                    String.valueOf(rateLimit.getReplenishRate()));
            config.put(
                    RedisRateLimiter.CONFIGURATION_PROPERTY_NAME + ".burstCapacity",
                    String.valueOf(rateLimit.getBurstCapacity()));
            config.put(
                    RedisRateLimiter.CONFIGURATION_PROPERTY_NAME + ".requestedTokens",
                    String.valueOf(1));
            String resolverName = toCamelCase(rateLimit.getRateLimitType().name()) + "KeyResolver";
            config.put("key-resolver", "#{@" + resolverName + "}");
            config.put("includeheaders", String.valueOf(rateLimit.isIncludeHeaders()));
            FilterDefinition rateLimitFilter = new FilterDefinition();
            rateLimitFilter.setName("RequestRateLimiter");
            rateLimitFilter.setArgs(config);

            // Add metadata for HEADER type rate limiting
            if (Strings.CI.equals(rateLimit.getRateLimitType().name(), "HEADER")) {
                LOGGER.debug(
                        "Adding metadata for HEADER type rate limiting: headerName={}",
                        rateLimit.getHeaderName());
                routeDefinition.getMetadata().put("x-rate-limit-header", rateLimit.getHeaderName());
            }
            LOGGER.info(
                    "Adding rate limit filter to route {}: {}",
                    igniteRouteDefinition.getId(),
                    rateLimitFilter);
            routeDefinition.getFilters().add(rateLimitFilter);
        }
        return routeDefinition;
    }

    private String toCamelCase(String input) {
        if (StringUtils.isEmpty(input)) {
            return input;
        }
        String[] parts = input.toLowerCase().split("_");
        StringBuilder camelCase = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            camelCase.append(StringUtils.capitalize(parts[i]));
        }
        LOGGER.debug("Converted {} to camelCase: {}", input, camelCase.toString());
        return camelCase.toString();
    }
}
