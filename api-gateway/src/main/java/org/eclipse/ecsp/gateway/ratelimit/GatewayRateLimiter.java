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

package org.eclipse.ecsp.gateway.ratelimit;

import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Custom Gateway Rate Limiter that supports shared buckets across routes using a namespace.
 *
 * <p>This rate limiter loads per-route configuration (replenishRate, burstCapacity, requestedTokens)
 * from the route definition, but uses a configurable namespace instead of routeId when generating
 * Redis keys. This allows multiple routes to share the same rate limit bucket.
 *
 * <p>Redis key format: {@code request_rate_limiter.{namespace.resolvedKey}.tokens}
 *
 * @author Abhishek Kumar
 */
public class GatewayRateLimiter extends RedisRateLimiter {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(GatewayRateLimiter.class);

    /** Default tokens left value when error occurs. */
    private static final long DEFAULT_TOKENS_ON_ERROR = -1L;

    /** Value indicating request is allowed. */
    private static final long ALLOWED_VALUE = 1L;

    private final String namespace;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<List<Long>> script;

    /**
     * Constructor to initialize GatewayRateLimiter with all required dependencies.
     *
     * @param redisTemplate the Redis template for executing commands
     * @param script the Lua script for rate limiting
     * @param configurationService the configuration service for per-route config resolution
     * @param namespace the namespace to use for shared bucket keys
     */
    public GatewayRateLimiter(ReactiveStringRedisTemplate redisTemplate,
                              RedisScript<List<Long>> script,
                              ConfigurationService configurationService,
                              String namespace) {
        super(redisTemplate, script, configurationService);
        this.redisTemplate = redisTemplate;
        this.script = script;
        this.namespace = namespace;
        LOGGER.info("GatewayRateLimiter initialized with namespace: {}", namespace);
    }

    /**
     * Check if the request is allowed based on rate limiting.
     *
     * <p>This method loads the per-route configuration using the routeId, but generates
     * Redis keys using the namespace. This enables:
     * <ul>
     *   <li>Per-route rate limit configuration (replenishRate, burstCapacity, requestedTokens)</li>
     *   <li>Shared bucket across routes with the same namespace</li>
     * </ul>
     *
     * @param routeId the route identifier (used to load per-route config)
     * @param id the resolved key from KeyResolver (e.g., client IP)
     * @return Mono emitting the rate limit response
     */
    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        // Load per-route configuration using the actual routeId
        // getConfig() returns the configuration map from parent class
        Config routeConfig = getConfig().getOrDefault(routeId, getConfig().get("defaultFilters"));
        
        if (routeConfig == null) {
            LOGGER.warn("No rate limit config available for route {}, allowing request", routeId);
            return Mono.just(new Response(true, getHeaders(routeConfig, DEFAULT_TOKENS_ON_ERROR)));
        }

        int replenishRate = routeConfig.getReplenishRate();
        int burstCapacity = routeConfig.getBurstCapacity();
        int requestedTokens = routeConfig.getRequestedTokens();

        LOGGER.debug("Rate limit for route {}: replenishRate={}, burstCapacity={}, requestedTokens={}, namespace={}",
                routeId, replenishRate, burstCapacity, requestedTokens, namespace);

        //Generate keys using namespace instead of routeId for shared bucket
        List<String> keys = getSharedBucketKeys(id, namespace);

        List<String> scriptArgs = Arrays.asList(
                String.valueOf(replenishRate),
                String.valueOf(burstCapacity),
                "",  // time is handled by Redis
                String.valueOf(requestedTokens)
        );

        Flux<List<Long>> flux = this.redisTemplate.execute(this.script, keys, scriptArgs);

        final Config finalRouteConfig = routeConfig;
        return flux.onErrorResume(throwable -> {
            LOGGER.error("Error calling rate limiter lua script", throwable);
            return Flux.just(Arrays.asList(ALLOWED_VALUE, DEFAULT_TOKENS_ON_ERROR));
        }).reduce(new ArrayList<Long>(), (longs, l) -> {
            longs.addAll(l);
            return longs;
        }).map(results -> {
            boolean allowed = results.get(0) == ALLOWED_VALUE;
            Long tokensLeft = results.get(1);

            Response response = new Response(allowed, getHeaders(finalRouteConfig, tokensLeft));

            LOGGER.debug("Rate limit response for route {} (namespace: {}): allowed={}, tokensLeft={}",
                    routeId, namespace, allowed, tokensLeft);

            return response;
        });
    }

    /**
     * Generate Redis keys for the shared bucket using namespace instead of routeId.
     *
     * @param id the resolved key from KeyResolver
     * @param namespaceKey the namespace for bucket sharing
     * @return list of Redis keys (tokens and timestamp)
     */
    private static List<String> getSharedBucketKeys(String id, String namespaceKey) {
        String prefix = "request_rate_limiter.{" + namespaceKey + "." + id + "}.";
        String tokenKey = prefix + "tokens";
        String timestampKey = prefix + "timestamp";
        return Arrays.asList(tokenKey, timestampKey);
    }
}
