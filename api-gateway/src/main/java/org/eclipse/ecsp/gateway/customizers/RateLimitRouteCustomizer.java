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
import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.eclipse.ecsp.gateway.model.RateLimit;
import org.eclipse.ecsp.gateway.ratelimit.configresolvers.RateLimitConfigResolver;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.RouteDefinition;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.ecsp.gateway.utils.GatewayConstants.RATE_LIMITING_METADATA_PREFIX;
import static org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter.CONFIGURATION_PROPERTY_NAME;

/**
 * Customizer to apply rate limiting to routes based on RateLimit configuration.
 *
 * @author Abhishek Kumar
 */
public class RateLimitRouteCustomizer implements RouteCustomizer {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RateLimitRouteCustomizer.class);

    private final RateLimitConfigResolver rateLimitConfigResolver;
    private final Map<String, KeyResolver> keyResolvers;

    /**
     * Constructor to initialize RateLimitRouteCustomizer.
     *
     * @param rateLimitConfigResolver the RateLimitConfigResolver
     */
    public RateLimitRouteCustomizer(RateLimitConfigResolver rateLimitConfigResolver, 
        Map<String, KeyResolver> keyResolvers) {
        this.rateLimitConfigResolver = rateLimitConfigResolver;
        this.keyResolvers = keyResolvers;
        LOGGER.info("RateLimitRouteCustomizer initialized with {} key resolvers : {}", 
            keyResolvers.size(), keyResolvers.keySet());
    }

    /** {@inheritDoc} */
    @Override
    public RouteDefinition customize(RouteDefinition routeDefinition, IgniteRouteDefinition igniteRouteDefinition) {
        LOGGER.debug("Customizing route {} with rate limit", igniteRouteDefinition.getId());
        RateLimit rateLimit = rateLimitConfigResolver.resolveRateLimit(igniteRouteDefinition);
        if (rateLimit != null) {
            LOGGER.debug(
                    "Applying rate limit: replenishRate={}, burstCapacity={} to route {}",
                    rateLimit.getReplenishRate(),
                    rateLimit.getBurstCapacity(),
                    igniteRouteDefinition.getId());
            Map<String, String> config = new HashMap<>();
            config.put(
                    CONFIGURATION_PROPERTY_NAME + ".replenishRate",
                    String.valueOf(rateLimit.getReplenishRate()));
            config.put(
                    CONFIGURATION_PROPERTY_NAME + ".burstCapacity",
                    String.valueOf(rateLimit.getBurstCapacity()));
            config.put(
                    CONFIGURATION_PROPERTY_NAME + ".requestedTokens",
                    String.valueOf(rateLimit.getRequestedTokens()));
            
            String resolverName = getKeyResolverBeanName(rateLimit);            

            config.put("key-resolver", "#{@" + resolverName + "}");

            config.put("includeheaders", String.valueOf(rateLimit.isIncludeHeaders()));
            FilterDefinition rateLimitFilter = new FilterDefinition();
            rateLimitFilter.setName("RequestRateLimiter");
            rateLimitFilter.setArgs(config);

            
            // add custom arguments for KeyResolver if any
            if (rateLimit.getArgs() != null && !rateLimit.getArgs().isEmpty()) {
                for (Map.Entry<String, String> entry : rateLimit.getArgs().entrySet()) {
                    String argKey = entry.getKey();
                    String argValue = entry.getValue();
                    LOGGER.debug("Adding custom KeyResolver argument: {}={}", argKey, argValue);
                    // Mapping to Filter args, args will not be recognized if not part of its Filter arg config
                    config.put(argKey, argValue); 
                    // add to route metadata for KeyResolver to use
                    routeDefinition.getMetadata().put(RATE_LIMITING_METADATA_PREFIX + argKey, argValue);
                }
            }

            LOGGER.info(
                    "Adding rate limit filter to route {}: {}",
                    igniteRouteDefinition.getId(),
                    rateLimitFilter);
            routeDefinition.getFilters().add(rateLimitFilter);
        }
        return routeDefinition;
    }

    /**
     * Get the KeyResolver bean name based on the RateLimit configuration.
     *
     * @param rateLimit the RateLimit configuration
     * @return the KeyResolver bean name
     */
    private String getKeyResolverBeanName(RateLimit rateLimit) {
        String resolverName = toCamelCase(rateLimit.getKeyResolver());
        
        if (!validateIfKeyResolverExists(resolverName)) {
            resolverName = resolverName + "KeyResolver";
            LOGGER.debug("Attempting alternative KeyResolver name  {}", resolverName);
            if (!validateIfKeyResolverExists(resolverName)) {
                LOGGER.error(
                        "No KeyResolver bean found, attempted resolver names: [{},{}]", 
                        toCamelCase(rateLimit.getKeyResolver()), resolverName);
                throw new IllegalStateException(
                        "No valid KeyResolver found for rate limiting: " + rateLimit.getKeyResolver());
            }
        }
        return resolverName;
    }

    /**
     * Validate if a KeyResolver bean exists with the given name.
     *
     * @param resolverName the KeyResolver bean name
     * @return true if the KeyResolver bean exists, false otherwise
     */
    private boolean validateIfKeyResolverExists(String resolverName) {
        if (keyResolvers.containsKey(resolverName)) {
            LOGGER.debug("Using KeyResolver: {} for rate limiting", resolverName);
            return true;
        }
        LOGGER.debug("No KeyResolver bean found for name: {}", resolverName);
        return false;
    }

    /**
     * Convert a string to camelCase.
     * Examples:
     * <ol>>
     *   <li>client_ip -> clientIp</li>
     *   <li>client-ip -> clientIp</li>
     *   <li>HEADER -> header</li>
     *   <li>KEY_RESOLVER -> keyResolver</li>
     *   <li>KEY-RESOLVER -> keyResolver</li>
     * </ol>
     *
     * @param input the input string
     * @return the camelCase string
     */
    private String toCamelCase(String input) {
        if (StringUtils.isEmpty(input)) {
            return input;
        }
        String[] parts = input.toLowerCase().split("_|-");
        StringBuilder camelCase = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            camelCase.append(StringUtils.capitalize(parts[i]));
        }
        LOGGER.debug("Converted {} to camelCase: {}", input, camelCase.toString());
        return camelCase.toString();
    }
}
