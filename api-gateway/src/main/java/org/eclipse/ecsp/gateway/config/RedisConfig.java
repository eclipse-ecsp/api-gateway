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

import org.eclipse.ecsp.gateway.annotations.ConditionOnRedisEnabled;
import org.eclipse.ecsp.gateway.conditions.RedisCacheEnabledCondition;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for Redis caching.
 *
 * @author Abhishek Kumar
 */
@Configuration
@EnableCaching
@Conditional(RedisCacheEnabledCondition.class)
@ImportAutoConfiguration({RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class})
public class RedisConfig {

    /**
     * Create a Logger instance.
     */
    private static final IgniteLogger LOG = IgniteLoggerFactory.getLogger(RedisConfig.class);
    /**
     * Property holds cacheName.
     */
    @Value("${" + GatewayConstants.CACHING_PREFIX + ".cacheName}")
    private String cacheName;
    /**
     * property holds cache ttl.
     */
    @Value("${" + GatewayConstants.CACHING_PREFIX + ".ttl}")
    private long ttl;

    /**
     * Constructor to initialize RedisConfig.
     */
    public RedisConfig() {
        LOG.debug("RedisConfig is enabled..");
    }

    /**
     * Creates RedisSerializer bean.
     *
     * @return RedisSerializer
     */
    @Bean
    public RedisSerializer<Object> jsonRedisSerializer() {
        return new JacksonRedisSerializer<>();
    }

    /**
     * Creates RedisCacheManager bean.
     *
     * @param redisConnectionFactory RedisConnectionFactory
     * @param jsonRedisSerializer    RedisSerializer
     * @return RedisCacheManager
     */
    @Bean(value = "redisCacheManager")
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory,
                                          RedisSerializer<Object> jsonRedisSerializer) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(ttl))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(jsonRedisSerializer));

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfig)
                .withInitialCacheConfigurations(
                        getCacheConfigurations(jsonRedisSerializer))
                .build();

        LOG.debug("RedisCacheManager : {}", redisCacheManager);

        return redisCacheManager;

    }

    /**
     * Creates cache configurations.
     *
     * @return cache Configuration
     */
    private Map<String, RedisCacheConfiguration> getCacheConfigurations(
            RedisSerializer<Object> jacksonRedisSerializer) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put(cacheName,
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(ttl))
                        .disableCachingNullValues().serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(jacksonRedisSerializer)));
        LOG.debug("RedisCacheConfiguration : {}", cacheConfigurations.toString());

        return cacheConfigurations;
    }
}