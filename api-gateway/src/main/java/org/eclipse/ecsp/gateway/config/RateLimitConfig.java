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
import org.eclipse.ecsp.gateway.ratelimit.configresolvers.DefaultRateLimitConfigResolver;
import org.eclipse.ecsp.gateway.ratelimit.configresolvers.RateLimitConfigResolver;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * configuration class for Rate Limiting.
 *
 * @author Abhishek Kumar
 */
@Configuration
@Conditional(RateLimitEnabledCondition.class)
@ImportAutoConfiguration({RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class})
public class RateLimitConfig {

    /**
     * Creates a logger instance for this configuration.
     */
    private static final IgniteLogger LOG = IgniteLoggerFactory.getLogger(RateLimitConfig.class);

    /**
     * Default constructor that logs the activation of rate limiting configuration.
     */
    public RateLimitConfig() {
        LOG.debug("RateLimitConfig is enabled..");
    }

    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "api.gateway.rate-limit.resolver", havingValue = "default", matchIfMissing = true)
    public RateLimitConfigResolver rateLimitConfigResolver(
            ApiRegistryClient apiRegistryClient,
            RateLimitProperties rateLimitProperties) {
        return new DefaultRateLimitConfigResolver(apiRegistryClient, rateLimitProperties);
    }

    @ConditionalOnMissingBean
    public RateLimitRouteCustomizer rateLimitRouteCustomizer(RateLimitConfigResolver rateLimitConfigResolver) {
        return new RateLimitRouteCustomizer(rateLimitConfigResolver);
    }

}