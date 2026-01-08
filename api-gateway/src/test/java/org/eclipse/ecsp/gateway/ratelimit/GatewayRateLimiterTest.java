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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GatewayRateLimiter} covering namespace key generation,.
 * default filter fallbacks, and redis error handling behaviour.
 */
@ExtendWith(MockitoExtension.class)
class GatewayRateLimiterTest {

    private static final String ROUTE_ID = "test-route";
    private static final String UNKNOWN_ROUTE = "unknown-route";
    private static final String RESOLVED_KEY = "client-key";
    private static final String NAMESPACE = "shared-namespace";
    private static final long TOKENS_LEFT = 7L;
    private static final long DENIED_TOKENS_LEFT = 0L;
    private static final long ERROR_TOKENS_LEFT = -1L;
    private static final int REPLENISH_RATE = 10;
    private static final int BURST_CAPACITY = 20;
    private static final int REQUESTED_TOKENS = 2;
    private static final int DEFAULT_REPLENISH_RATE = 5;
    private static final int DEFAULT_BURST_CAPACITY = 15;
    private static final int DEFAULT_REQUESTED_TOKENS = 3;
    private static final int EXPECTED_REDIS_KEY_COUNT = 2;
    private static final int LUA_ARGUMENT_COUNT = 4;
    private static final int REQUESTED_TOKENS_ARG_INDEX = 3;

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private RedisScript<List<Long>> redisScript;

    private GatewayRateLimiter gatewayRateLimiter;

    @BeforeEach
    void setUp() {
        gatewayRateLimiter = new GatewayRateLimiter(redisTemplate, redisScript, null, NAMESPACE);
    }

    @Test
    void isAllowed_WithSharedNamespace_UsesNamespaceBackedKeys() {
        registerRouteConfig(ROUTE_ID, REPLENISH_RATE, BURST_CAPACITY, REQUESTED_TOKENS);
        when(redisTemplate.execute(eq(redisScript), anyList(), anyList()))
            .thenReturn(Flux.just(List.of(1L, TOKENS_LEFT)));

        StepVerifier.create(gatewayRateLimiter.isAllowed(ROUTE_ID, RESOLVED_KEY))
            .assertNext(response -> {
                assertTrue(response.isAllowed(), "Request should be allowed when redis reports success");
                assertEquals(String.valueOf(TOKENS_LEFT),
                    response.getHeaders().get(RedisRateLimiter.REMAINING_HEADER),
                    "Tokens left header should match redis result");
            })
            .verifyComplete();

        ArgumentCaptor<List<String>> keysCaptor = createListCaptor();
        verify(redisTemplate).execute(eq(redisScript), keysCaptor.capture(), anyList());

        List<String> keys = keysCaptor.getValue();
        assertEquals(EXPECTED_REDIS_KEY_COUNT, keys.size(),
            "Two redis keys (tokens + timestamp) should be generated");
        String expectedFragment = "{" + NAMESPACE + "." + RESOLVED_KEY + "}";
        assertTrue(keys.get(0).contains(expectedFragment + ".tokens"),
            "Token key should embed namespace based fragment");
        assertTrue(keys.get(1).contains(expectedFragment + ".timestamp"),
            "Timestamp key should embed namespace based fragment");
    }

    @Test
    void isAllowed_WhenRouteConfigMissing_UsesDefaultFiltersConfiguration() {
        registerRouteConfig(RouteDefinitionRouteLocator.DEFAULT_FILTERS, DEFAULT_REPLENISH_RATE,
            DEFAULT_BURST_CAPACITY, DEFAULT_REQUESTED_TOKENS);
        when(redisTemplate.execute(eq(redisScript), anyList(), anyList()))
            .thenReturn(Flux.just(List.of(1L, TOKENS_LEFT)));

        StepVerifier.create(gatewayRateLimiter.isAllowed(UNKNOWN_ROUTE, RESOLVED_KEY))
            .assertNext(response -> {
                assertEquals(String.valueOf(DEFAULT_REPLENISH_RATE),
                    response.getHeaders().get(RedisRateLimiter.REPLENISH_RATE_HEADER),
                    "Should expose default replenish rate in headers");
                assertEquals(String.valueOf(DEFAULT_BURST_CAPACITY),
                    response.getHeaders().get(RedisRateLimiter.BURST_CAPACITY_HEADER),
                    "Should expose default burst capacity in headers");
                assertEquals(String.valueOf(DEFAULT_REQUESTED_TOKENS),
                    response.getHeaders().get(RedisRateLimiter.REQUESTED_TOKENS_HEADER),
                    "Should expose default requested tokens in headers");
            })
            .verifyComplete();

        ArgumentCaptor<List<String>> argsCaptor = createListCaptor();
        verify(redisTemplate).execute(eq(redisScript), anyList(), argsCaptor.capture());

        List<String> scriptArgs = argsCaptor.getValue();
        assertEquals(LUA_ARGUMENT_COUNT, scriptArgs.size(), "Lua script should receive four arguments");
        assertEquals(String.valueOf(DEFAULT_REPLENISH_RATE), scriptArgs.get(0),
            "First argument should be replenish rate");
        assertEquals(String.valueOf(DEFAULT_BURST_CAPACITY), scriptArgs.get(1),
            "Second argument should be burst capacity");
        assertEquals(String.valueOf(DEFAULT_REQUESTED_TOKENS), scriptArgs.get(REQUESTED_TOKENS_ARG_INDEX),
            "Fourth argument should be requested tokens");
    }

    @Test
    void isAllowed_WhenRedisScriptFails_AllowsRequestWithDefaultTokens() {
        registerRouteConfig(ROUTE_ID, REPLENISH_RATE, BURST_CAPACITY, REQUESTED_TOKENS);
        when(redisTemplate.execute(eq(redisScript), anyList(), anyList()))
            .thenReturn(Flux.error(new IllegalStateException("boom")));

        StepVerifier.create(gatewayRateLimiter.isAllowed(ROUTE_ID, RESOLVED_KEY))
            .assertNext(response -> {
                assertTrue(response.isAllowed(), "Requests should be allowed when lua execution fails");
                assertEquals(String.valueOf(ERROR_TOKENS_LEFT),
                    response.getHeaders().get(RedisRateLimiter.REMAINING_HEADER),
                    "Fallback header should indicate default error tokens left");
            })
            .verifyComplete();
    }

    @Test
    void isAllowed_WhenRedisDeniesRequest_PropagatesDeniedResponseAndHeaders() {
        registerRouteConfig(ROUTE_ID, REPLENISH_RATE, BURST_CAPACITY, REQUESTED_TOKENS);
        when(redisTemplate.execute(eq(redisScript), anyList(), anyList()))
            .thenReturn(Flux.just(List.of(0L, DENIED_TOKENS_LEFT)));

        StepVerifier.create(gatewayRateLimiter.isAllowed(ROUTE_ID, RESOLVED_KEY))
            .assertNext(response -> {
                assertFalse(response.isAllowed(), "Response should reflect denied decision from redis script");
                assertEquals(String.valueOf(DENIED_TOKENS_LEFT),
                    response.getHeaders().get(RedisRateLimiter.REMAINING_HEADER),
                    "Header should expose remaining tokens reported by redis script");
            })
            .verifyComplete();
    }

    private void registerRouteConfig(String routeId, int replenishRate, int burstCapacity, int requestedTokens) {
        RedisRateLimiter.Config config = new RedisRateLimiter.Config()
            .setReplenishRate(replenishRate)
            .setBurstCapacity(burstCapacity)
            .setRequestedTokens(requestedTokens);
        gatewayRateLimiter.getConfig().put(routeId, config);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<List<String>> createListCaptor() {
        return ArgumentCaptor.forClass((Class) List.class);
    }
}
