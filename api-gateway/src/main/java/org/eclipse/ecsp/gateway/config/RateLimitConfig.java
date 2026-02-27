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

import org.eclipse.ecsp.gateway.clients.ApiRegistryClient;
import org.eclipse.ecsp.gateway.conditions.RateLimitEnabledCondition;
import org.eclipse.ecsp.gateway.customizers.RateLimitRouteCustomizer;
import org.eclipse.ecsp.gateway.plugins.PluginLoader;
import org.eclipse.ecsp.gateway.ratelimit.GatewayRateLimiter;
import org.eclipse.ecsp.gateway.ratelimit.configresolvers.DefaultRateLimitConfigResolver;
import org.eclipse.ecsp.gateway.ratelimit.configresolvers.RateLimitConfigResolver;
import org.eclipse.ecsp.gateway.ratelimit.keyresolvers.ClientIpKeyResolver;
import org.eclipse.ecsp.gateway.ratelimit.keyresolvers.RequestHeaderKeyResolver;
import org.eclipse.ecsp.gateway.ratelimit.keyresolvers.RouteNameKeyResolver;
import org.eclipse.ecsp.gateway.ratelimit.keyresolvers.RoutePathKeyResolver;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import java.util.List;

/**
 * configuration class for Rate Limiting.
 *
 * @author Abhishek Kumar
 */
@Configuration
@AutoConfigureAfter(PluginLoader.class)
@Conditional(RateLimitEnabledCondition.class)
public class RateLimitConfig {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RateLimitConfig.class);
    private final RateLimitProperties properties;

    /**
     * Constructor for RateLimitConfig.
     *
     * @param rateLimitProperties rate limit properties
     */
    public RateLimitConfig(RateLimitProperties rateLimitProperties) {
        this.properties = rateLimitProperties;
        LOGGER.info("RateLimitConfig loaded, rate limiting is enabled: {}", properties.isEnabled());
    }

    /**
     * Creates a logger instance for this configuration.
     */
    private static final IgniteLogger LOG = IgniteLoggerFactory.getLogger(RateLimitConfig.class);

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "api.gateway.rate-limit.resolver", havingValue = "default", matchIfMissing = true)
    public RateLimitConfigResolver rateLimitConfigResolver(
            ApiRegistryClient apiRegistryClient,
            RateLimitProperties rateLimitProperties) {
        return new DefaultRateLimitConfigResolver(apiRegistryClient, rateLimitProperties);
    }

    /**
     * Bean definition for ClientIpKeyResolver.
     *
     * @return the ClientIpKeyResolver bean
     */
    @Bean(GatewayConstants.CLIENT_IP_KEY_RESOLVER)
    @Primary
    public KeyResolver clientIpKeyResolver() {
        LOG.debug("Creating ClientIpKeyResolver bean");
        return new ClientIpKeyResolver();
    }

    /**
     * Bean definition for RequestHeaderKeyResolver.
     *
     * @return the RequestHeaderKeyResolver bean
     */
    @Bean(GatewayConstants.HEADER_KEY_RESOLVER)
    public KeyResolver requestHeaderKeyResolver() {
        LOG.debug("Creating RequestHeaderKeyResolver bean");
        return new RequestHeaderKeyResolver();
    }

    /**
     * Bean definition for RoutePathKeyResolver.
     *
     * @return the RoutePathKeyResolver bean
     */
    @Bean(GatewayConstants.ROUTE_PATH_KEY_RESOLVER)
    public KeyResolver routePathKeyResolver() {
        LOG.debug("Creating RoutePathKeyResolver bean");
        return new RoutePathKeyResolver();
    }

    /**
     * Bean definition for RouteNameKeyResolver.
     *
     * @return the RouteNameKeyResolver bean
     */
    @Bean(GatewayConstants.ROUTE_NAME_KEY_RESOLVER)
    public KeyResolver routeNameKeyResolver() {
        LOG.debug("Creating RouteNameKeyResolver bean");
        return new RouteNameKeyResolver();
    }

    /**
     * Creates a GatewayRateLimiter that uses a configurable namespace instead of routeId.
     * This enables all routes to share the same rate limit bucket based on the namespace,
     * while still respecting per-route rate limit configurations.
     *
     * @param redisTemplate the Redis template for executing commands
     * @param script the Lua script for rate limiting
     * @param configurationService the configuration service for per-route config resolution
     * @param rateLimitProperties the rate limit properties containing namespace configuration
     * @return the gateway rate limiter
     */
    @Bean
    @Primary
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public RateLimiter<RedisRateLimiter.Config> gatewayRateLimiter(
            ReactiveStringRedisTemplate redisTemplate,
            RedisScript<List<Long>> script,
            ConfigurationService configurationService,
            RateLimitProperties rateLimitProperties) {
        LOG.info("Creating GatewayRateLimiter with namespace: {}", rateLimitProperties.getNamespace());
        return new GatewayRateLimiter(redisTemplate, script, configurationService, 
                rateLimitProperties.getNamespace());
    }

    /**
     * Bean definition for RateLimitRouteCustomizer.
     *
     * @param rateLimitConfigResolver the rate limit config resolver
     * @param applicationContext the application context for dynamic key resolver lookup
     * @return the RateLimitRouteCustomizer bean
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimitRouteCustomizer rateLimitRouteCustomizer(RateLimitConfigResolver rateLimitConfigResolver,
        ApplicationContext applicationContext) {
        LOG.debug("Creating RateLimitRouteCustomizer bean with ApplicationContext for dynamic KeyResolver lookup");
        return new RateLimitRouteCustomizer(rateLimitConfigResolver, applicationContext);
    }
}
