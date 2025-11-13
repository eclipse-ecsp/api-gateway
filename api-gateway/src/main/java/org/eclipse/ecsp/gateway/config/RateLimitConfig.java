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
import org.eclipse.ecsp.gateway.ratelimit.configresolvers.DefaultRateLimitConfigResolver;
import org.eclipse.ecsp.gateway.ratelimit.configresolvers.RateLimitConfigResolver;
import org.eclipse.ecsp.gateway.ratelimit.keyresolvers.ClientIpKeyResolver;
import org.eclipse.ecsp.gateway.ratelimit.keyresolvers.RequestHeaderKeyResolver;
import org.eclipse.ecsp.gateway.ratelimit.keyresolvers.RouteNameKeyResolver;
import org.eclipse.ecsp.gateway.ratelimit.keyresolvers.RoutePathKeyResolver;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.Map;

/**
 * configuration class for Rate Limiting.
 *
 * @author Abhishek Kumar
 */
@Configuration
@AutoConfigureAfter(PluginLoader.class)
@Conditional(RateLimitEnabledCondition.class)
@Import({RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class })
public class RateLimitConfig {

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

    @Bean
    @Primary
    public ClientIpKeyResolver clientIpKeyResolver() {
        LOG.debug("Creating ClientIpKeyResolver bean");
        return new ClientIpKeyResolver();
    }

    @Bean("headerKeyResolver")
    public RequestHeaderKeyResolver requestHeaderKeyResolver() {
        LOG.debug("Creating RequestHeaderKeyResolver bean");
        return new RequestHeaderKeyResolver();
    }

    @Bean("routePathKeyResolver")
    public RoutePathKeyResolver routePathKeyResolver() {
        LOG.debug("Creating RoutePathKeyResolver bean");
        return new RoutePathKeyResolver();
    }

    @Bean("routeNameKeyResolver")
    public RouteNameKeyResolver routeNameKeyResolver() {
        LOG.debug("Creating RouteNameKeyResolver bean");
        return new RouteNameKeyResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitRouteCustomizer rateLimitRouteCustomizer(RateLimitConfigResolver rateLimitConfigResolver,
        Map<String, KeyResolver> keyResolvers) {
        LOG.debug("Creating RateLimitRouteCustomizer bean with {} key resolvers", keyResolvers.size());
        return new RateLimitRouteCustomizer(rateLimitConfigResolver, keyResolvers);
    }

}