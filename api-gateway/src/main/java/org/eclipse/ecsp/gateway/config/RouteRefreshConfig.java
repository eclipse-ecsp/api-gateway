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

import org.eclipse.ecsp.gateway.conditions.RouteRefreshEventEnabledCondition;
import org.eclipse.ecsp.gateway.events.RouteEventSubscriber;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClientException;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis configuration for route event subscription in API Gateway.
 */
@Configuration
@Conditional(RouteRefreshEventEnabledCondition.class)
public class RouteRefreshConfig {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RouteRefreshConfig.class);

    private final RouteRefreshProperties properties;

    /**
     * Constructor for RouteRefreshRedisConfig.
     *
     * @param properties route refresh properties
     */
    public RouteRefreshConfig(RouteRefreshProperties properties) {
        this.properties = properties;
    }

    /**
     * Configure Redis message listener container for route events.
     *
     * @param connectionFactory Redis connection factory
     * @param subscriber        route event subscriber
     * @return configured RedisMessageListenerContainer
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RouteEventSubscriber subscriber) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new PatternTopic(properties.getRedis().getChannel()));

        // Add error handler for connection failures
        container.setErrorHandler(throwable ->
            LOGGER.error("Redis connection error in route refresh listener." 
                + "Fallback to polling may be needed.", throwable)
        );
        LOGGER.info("RedisMessageListenerContainer configured for channel: {}",
                properties.getRedis().getChannel());

        return container;
    }

    /**
     * Configure RetryTemplate with exponential backoff.
     *
     * @return configured RetryTemplate
     */
    @Bean("routesRefreshRetryTemplate")
    public RetryTemplate routesRefreshRetryTemplate() {
        // Configure exponential backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(properties.getRetry().getInitialIntervalMs());
        backOffPolicy.setMultiplier(properties.getRetry().getMultiplier());
        backOffPolicy.setMaxInterval(properties.getRetry().getMaxIntervalMs());
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Configure retry policy with max attempts
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(RestClientException.class, true);
        retryableExceptions.put(Exception.class, true);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                properties.getRetry().getMaxAttempts(),
                retryableExceptions
        );
        retryTemplate.setRetryPolicy(retryPolicy);

        LOGGER.info("RetryTemplate configured: maxAttempts={}, initialInterval={}ms, multiplier={}, maxInterval={}ms",
                properties.getRetry().getMaxAttempts(),
                properties.getRetry().getInitialIntervalMs(),
                properties.getRetry().getMultiplier(),
                properties.getRetry().getMaxIntervalMs());

        return retryTemplate;
    }
}
