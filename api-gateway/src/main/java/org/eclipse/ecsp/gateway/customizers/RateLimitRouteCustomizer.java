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
import org.springframework.cloud.gateway.filter.factory.RemoveResponseHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.support.NameUtils;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.ecsp.gateway.utils.GatewayConstants.RATE_LIMITING_METADATA_PREFIX;
import static org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter.CONFIGURATION_PROPERTY_NAME;

/**
 * Customizer to apply rate limiting to routes based on RateLimit configuration.
 *
 * @author Abhishek Kumar
 */
public class RateLimitRouteCustomizer implements RouteCustomizer {

    private static final String KEY_RESOLVER = "KeyResolver";

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RateLimitRouteCustomizer.class);

    private final RateLimitConfigResolver rateLimitConfigResolver;
    private final ApplicationContext applicationContext;

    /**
     * Constructor to initialize RateLimitRouteCustomizer.
     *
     * @param rateLimitConfigResolver the RateLimitConfigResolver
     * @param applicationContext the ApplicationContext for dynamic bean lookup
     */
    public RateLimitRouteCustomizer(RateLimitConfigResolver rateLimitConfigResolver, 
        ApplicationContext applicationContext) {
        this.rateLimitConfigResolver = rateLimitConfigResolver;
        this.applicationContext = applicationContext;
        LOGGER.info("RateLimitRouteCustomizer initialized with ApplicationContext for dynamic KeyResolver lookup");
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
            
            config.put("denyEmptyKey", String.valueOf(rateLimit.getDenyEmptyKey()));
            config.put("emptyKeyStatus", String.valueOf(rateLimit.getEmptyKeyStatus()));
            
            String resolverName = getKeyResolverBeanName(rateLimit);            

            config.put("key-resolver", "#{@" + resolverName + "}");

            // Spring Cloud Gateway uses 'include-headers' (with hyphen) not 'includeHeaders'
            config.put("include-headers", String.valueOf(rateLimit.isIncludeHeaders()));
            FilterDefinition rateLimitFilter = new FilterDefinition();
            rateLimitFilter.setName(NameUtils.normalizeFilterFactoryName(RequestRateLimiterGatewayFilterFactory.class));
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
            
            // Add a filter to remove rate limit headers if includeHeaders is false
            if (!rateLimit.isIncludeHeaders()) {
                List.of("X-RateLimit-Remaining", 
                    "X-RateLimit-Replenish-Rate", 
                    "X-RateLimit-Burst-Capacity", 
                    "X-RateLimit-Requested-Tokens")
                    .forEach(header -> addRemoveHeaderFilter(routeDefinition, header));
                
                LOGGER.debug("Added filters to remove rate limit headers for route {}", 
                    igniteRouteDefinition.getId());
            }
        }
        return routeDefinition;
    }

    /**
     * Get the KeyResolver bean name based on the RateLimit configuration.
     * Supports multiple formats:
     * - Standard names: CLIENT_IP, client_ip, client-ip, clientIpKeyResolver
     * - Custom resolvers: customKeyResolver, CustomKeyResolver, VehicleIdKeyResolver
     *
     * @param rateLimit the RateLimit configuration
     * @return the KeyResolver bean name
     */
    private String getKeyResolverBeanName(RateLimit rateLimit) {
        String originalResolverName = rateLimit.getKeyResolver();
        
        // First, try the original name as-is (handles cases like customKeyResolver)
        if (validateIfKeyResolverExists(originalResolverName)) {
            return originalResolverName;
        }
        
        // Try Spring bean naming convention (first letter lowercase, rest unchanged)
        // Example: CustomKeyResolver -> customKeyResolver
        String springBeanName = toSpringBeanName(originalResolverName);
        if (validateIfKeyResolverExists(springBeanName)) {
            return springBeanName;
        }
        
        // Normalize the input by removing "KeyResolver" suffix and converting to lowercase for standard resolvers
        String normalized = originalResolverName.toLowerCase()
                .replace("keyresolver", "")
                .replace("-", "_");
        
        // Map standard resolver names to their bean names
        String resolverName;
        switch (normalized) {
            case "client_ip", "clientip" -> resolverName = "clientIpKeyResolver";
            case "header" -> resolverName = "headerKeyResolver";
            case "route_path", "routepath" -> resolverName = "userIdKeyResolver";
            case "route_name", "routename" -> resolverName = "apiKeyKeyResolver";
            default -> {
                // For custom resolvers with underscores/hyphens, try camelCase conversion
                resolverName = toCamelCase(originalResolverName);
                if (!resolverName.endsWith(KEY_RESOLVER)) {
                    resolverName = resolverName + KEY_RESOLVER;
                }
            }
        }

        // Validate the resolved name
        if (validateIfKeyResolverExists(resolverName)) {
            return resolverName;
        }
        
        // If still not found, log error and throw exception
        LOGGER.error(
                "No KeyResolver bean found, attempted resolver names: [{}, {}]", 
                originalResolverName, springBeanName);
        throw new IllegalStateException(
                "No valid KeyResolver found for rate limiting: " + originalResolverName);
    }

    /**
     * Convert class name to Spring bean naming convention (first letter lowercase, rest unchanged).
     * Examples:
     * <ul>
     *   <li>VehicleIdKeyResolver -> vehicleIdKeyResolver</li>
     *   <li>CustomKeyResolver -> customKeyResolver</li>
     *   <li>ClassA -> classA</li>
     * </ul>
     *
     * @param className the class name or bean name
     * @return the Spring bean name (camelCase with first letter lowercase)
     */
    private String toSpringBeanName(String className) {
        if (StringUtils.isEmpty(className)) {
            return className;
        }
        // Convert only the first character to lowercase, preserve the rest
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    /**
     * Validate if a KeyResolver bean exists with the given name.
     * Dynamically looks up the bean from ApplicationContext to support late-registered plugins.
     *
     * @param resolverName the KeyResolver bean name
     * @return true if the KeyResolver bean exists, false otherwise
     */
    private boolean validateIfKeyResolverExists(String resolverName) {
        try {
            // Dynamic lookup from ApplicationContext to find beans registered after initialization
            Map<String, KeyResolver> allKeyResolvers = 
                applicationContext.getBeansOfType(KeyResolver.class);
            
            if (allKeyResolvers.containsKey(resolverName)) {
                LOGGER.debug("Using KeyResolver: {} for rate limiting", resolverName);
                return true;
            }
            LOGGER.debug("No KeyResolver bean found for name: {} (available: {})", 
                resolverName, allKeyResolvers.keySet());
            return false;
        } catch (Exception e) {
            LOGGER.debug("Error looking up KeyResolver bean: {}", resolverName, e);
            return false;
        }
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
        String[] parts = input.toLowerCase().split("[_-]");
        StringBuilder camelCase = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            camelCase.append(StringUtils.capitalize(parts[i]));
        }
        LOGGER.debug("Converted {} to camelCase: {}", input, camelCase.toString());
        return camelCase.toString();
    }

    private void addRemoveHeaderFilter(RouteDefinition routeDefinition, String headerName) {
        FilterDefinition filter = new FilterDefinition();
        filter.setName(NameUtils.normalizeFilterFactoryName(RemoveResponseHeaderGatewayFilterFactory.class));
        Map<String, String> args = new HashMap<>();
        args.put("name", headerName);
        filter.setArgs(args);
        routeDefinition.getFilters().add(filter);
    }
}
