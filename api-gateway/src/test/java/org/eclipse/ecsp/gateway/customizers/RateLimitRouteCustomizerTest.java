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

import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.eclipse.ecsp.gateway.model.RateLimit;
import org.eclipse.ecsp.gateway.ratelimit.configresolvers.RateLimitConfigResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RateLimitRouteCustomizer.
 */
@ExtendWith(SpringExtension.class)
class RateLimitRouteCustomizerTest {

    private static final int REPLENISH_RATE_50 = 50;
    private static final int REPLENISH_RATE_100 = 100;
    private static final int BURST_CAPACITY_100 = 100;
    private static final int BURST_CAPACITY_200 = 200;
    private static final int REQUESTED_TOKENS_2 = 2;
    private static final int MIN_FILTER_COUNT_WITH_REMOVE_HEADERS = 5;

    @Mock
    private RateLimitConfigResolver rateLimitConfigResolver;

    @Mock
    private KeyResolver clientIpKeyResolver;

    @Mock
    private KeyResolver userIdKeyResolver;

    private Map<String, KeyResolver> keyResolvers;
    private RateLimitRouteCustomizer customizer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        setupKeyResolvers();
    }

    private void setupKeyResolvers() {
        keyResolvers = new HashMap<>();
        // toCamelCase("clientIp") -> "clientip", then adds "KeyResolver" suffix
        keyResolvers.put("clientipKeyResolver", clientIpKeyResolver);
        keyResolvers.put("useridKeyResolver", userIdKeyResolver);
        keyResolvers.put("clientIpKeyResolver", clientIpKeyResolver);
        keyResolvers.put("userIdKeyResolver", userIdKeyResolver);
        customizer = new RateLimitRouteCustomizer(rateLimitConfigResolver, keyResolvers);
    }

    @Test
    void constructor_WithValidDependencies_InitializesSuccessfully() {
        RateLimitRouteCustomizer testCustomizer =
            new RateLimitRouteCustomizer(rateLimitConfigResolver, keyResolvers);
        assertNotNull(testCustomizer, "Customizer should be initialized");
    }

    @Test
    void customize_WithNullRateLimit_ReturnsRouteUnchanged() throws URISyntaxException {
        IgniteRouteDefinition igniteRoute = createIgniteRoute();
        RouteDefinition routeDef = createRouteDefinition();
        int originalFilterCount = routeDef.getFilters().size();

        when(rateLimitConfigResolver.resolveRateLimit(igniteRoute)).thenReturn(null);

        RouteDefinition result = customizer.customize(routeDef, igniteRoute);

        assertNotNull(result, "Result should not be null");
        assertEquals(originalFilterCount, result.getFilters().size(),
            "Filter count should not change when rate limit is null");
    }

    @Test
    void customize_WithValidRateLimit_AddsRateLimitFilter() throws URISyntaxException {
        IgniteRouteDefinition igniteRoute = createIgniteRoute();
        RouteDefinition routeDef = createRouteDefinition();
        RateLimit rateLimit = createRateLimit("clientIp", REPLENISH_RATE_100, BURST_CAPACITY_200);

        when(rateLimitConfigResolver.resolveRateLimit(igniteRoute)).thenReturn(rateLimit);

        RouteDefinition result = customizer.customize(routeDef, igniteRoute);

        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.getFilters().size(), "Should have one filter");
        FilterDefinition filter = result.getFilters().get(0);
        assertEquals("RequestRateLimiter", filter.getName(), "Filter name should be RequestRateLimiter");
    }

    @Test
    void customize_WithRateLimit_SetsCorrectConfiguration() throws URISyntaxException {
        final IgniteRouteDefinition igniteRoute = createIgniteRoute();
        final RouteDefinition routeDef = createRouteDefinition();
        RateLimit rateLimit = createRateLimit("client-ip", REPLENISH_RATE_50, BURST_CAPACITY_100);
        rateLimit.setRequestedTokens(REQUESTED_TOKENS_2);
        rateLimit.setIncludeHeaders(false);

        when(rateLimitConfigResolver.resolveRateLimit(igniteRoute)).thenReturn(rateLimit);

        RouteDefinition result = customizer.customize(routeDef, igniteRoute);

        FilterDefinition filter = result.getFilters().get(0);
        Map<String, String> args = filter.getArgs();

        assertEquals("50", args.get("redis-rate-limiter.replenishRate"),
            "Replenish rate should match");
        assertEquals("100", args.get("redis-rate-limiter.burstCapacity"),
            "Burst capacity should match");
        assertEquals("2", args.get("redis-rate-limiter.requestedTokens"),
            "Requested tokens should match");
        assertTrue(args.get("key-resolver").contains("clientIpKeyResolver"),
            "Key resolver should reference clientIpKeyResolver");
    }

    @Test
    void customize_WithCamelCaseKeyResolver_ResolvesCorrectly() throws URISyntaxException {
        assertTrue(testKeyResolverResolves("client-ip"));
        assertTrue(testKeyResolverResolves("client_ip"));
        assertTrue(testKeyResolverResolves("CLIENT-IP"));
        assertTrue(testKeyResolverResolves("CLIENT_IP"));
    }

    private boolean testKeyResolverResolves(String keyResolverName) throws URISyntaxException {
        IgniteRouteDefinition igniteRoute = createIgniteRoute();
        RouteDefinition routeDef = createRouteDefinition();
        RateLimit rateLimit = createRateLimit(keyResolverName, REPLENISH_RATE_100, BURST_CAPACITY_200);

        when(rateLimitConfigResolver.resolveRateLimit(igniteRoute)).thenReturn(rateLimit);

        RouteDefinition result = customizer.customize(routeDef, igniteRoute);

        FilterDefinition filter = result.getFilters().get(0);
        String keyResolver = filter.getArgs().get("key-resolver");
        assertNotNull(keyResolver, "Key resolver should not be null");
        assertTrue(keyResolver.contains("clientIpKeyResolver"), "Should resolve to clientipKeyResolver");
        return true;
    }

    @Test
    void customize_WithNonExistentKeyResolver_ThrowsIllegalStateException() throws URISyntaxException {
        IgniteRouteDefinition igniteRoute = createIgniteRoute();
        RouteDefinition routeDef = createRouteDefinition();
        RateLimit rateLimit = createRateLimit("nonExistentResolver", REPLENISH_RATE_100, BURST_CAPACITY_200);

        when(rateLimitConfigResolver.resolveRateLimit(igniteRoute)).thenReturn(rateLimit);

        assertThrows(IllegalStateException.class,
            () -> customizer.customize(routeDef, igniteRoute),
            "Should throw IllegalStateException for non-existent resolver");
    }

    @Test
    void customize_WithCustomArgs_AddsArgsToMetadata() throws URISyntaxException {
        final IgniteRouteDefinition igniteRoute = createIgniteRoute();
        final RouteDefinition routeDef = createRouteDefinition();
        RateLimit rateLimit = createRateLimit("clientIp", REPLENISH_RATE_100, BURST_CAPACITY_200);
        Map<String, String> customArgs = new HashMap<>();
        customArgs.put("customArg1", "value1");
        customArgs.put("customArg2", "value2");
        rateLimit.setArgs(customArgs);

        when(rateLimitConfigResolver.resolveRateLimit(igniteRoute)).thenReturn(rateLimit);

        RouteDefinition result = customizer.customize(routeDef, igniteRoute);

        assertEquals("value1", result.getMetadata().get("X-Rate-Limit-Args-customArg1"),
            "Custom arg 1 should be in metadata");
        assertEquals("value2", result.getMetadata().get("X-Rate-Limit-Args-customArg2"),
            "Custom arg 2 should be in metadata");
    }

    @Test
    void customize_WithCustomArgs_AddsArgsToFilterConfig() throws URISyntaxException {
        final IgniteRouteDefinition igniteRoute = createIgniteRoute();
        final RouteDefinition routeDef = createRouteDefinition();
        RateLimit rateLimit = createRateLimit("clientIp", REPLENISH_RATE_100, BURST_CAPACITY_200);
        Map<String, String> customArgs = new HashMap<>();
        customArgs.put("headerName", "X-Custom-Header");
        rateLimit.setArgs(customArgs);

        when(rateLimitConfigResolver.resolveRateLimit(igniteRoute)).thenReturn(rateLimit);

        RouteDefinition result = customizer.customize(routeDef, igniteRoute);

        FilterDefinition filter = result.getFilters().get(0);
        assertEquals("X-Custom-Header", filter.getArgs().get("headerName"),
            "Custom arg should be in filter config");
    }

    @Test
    void customize_WithEmptyCustomArgs_DoesNotAddMetadata() throws URISyntaxException {
        IgniteRouteDefinition igniteRoute = createIgniteRoute();
        RouteDefinition routeDef = createRouteDefinition();
        RateLimit rateLimit = createRateLimit("clientIp", REPLENISH_RATE_100, BURST_CAPACITY_200);
        rateLimit.setArgs(new HashMap<>());
        int originalMetadataSize = routeDef.getMetadata().size();

        when(rateLimitConfigResolver.resolveRateLimit(igniteRoute)).thenReturn(rateLimit);

        RouteDefinition result = customizer.customize(routeDef, igniteRoute);

        assertEquals(originalMetadataSize, result.getMetadata().size(),
            "Metadata size should not change with empty custom args");
    }

    @Test
    void customize_WithResolverNameNeedingSuffix_AppendsKeyResolverSuffix() throws URISyntaxException {
        // Create a fresh customizer with only useridKeyResolver (needs suffix)
        Map<String, KeyResolver> specificResolvers = new HashMap<>();
        specificResolvers.put("useridKeyResolver", userIdKeyResolver);
        RateLimitRouteCustomizer specificCustomizer =
            new RateLimitRouteCustomizer(rateLimitConfigResolver, specificResolvers);

        IgniteRouteDefinition igniteRoute = createIgniteRoute();
        RouteDefinition routeDef = createRouteDefinition();
        RateLimit rateLimit = createRateLimit("userId", REPLENISH_RATE_100, BURST_CAPACITY_200);

        when(rateLimitConfigResolver.resolveRateLimit(igniteRoute)).thenReturn(rateLimit);

        RouteDefinition result = specificCustomizer.customize(routeDef, igniteRoute);

        FilterDefinition filter = result.getFilters().get(0);
        String keyResolver = filter.getArgs().get("key-resolver");
        assertNotNull(keyResolver, "Key resolver should not be null");
        assertTrue(keyResolver.contains("useridKeyResolver"),
            "Should append KeyResolver suffix");
    }

    private IgniteRouteDefinition createIgniteRoute() {
        IgniteRouteDefinition route = new IgniteRouteDefinition();
        route.setId("test-route");
        route.setService("test-service");
        return route;
    }

    private RouteDefinition createRouteDefinition() throws URISyntaxException {
        RouteDefinition routeDef = new RouteDefinition();
        routeDef.setId("test-route");
        routeDef.setUri(new URI("http://localhost:8080"));
        routeDef.setMetadata(new HashMap<>());
        return routeDef;
    }

    private RateLimit createRateLimit(String keyResolver, long replenishRate, long burstCapacity) {
        RateLimit rateLimit = new RateLimit();
        rateLimit.setKeyResolver(keyResolver);
        rateLimit.setReplenishRate(replenishRate);
        rateLimit.setBurstCapacity(burstCapacity);
        rateLimit.setRequestedTokens(1);
        rateLimit.setIncludeHeaders(true);
        return rateLimit;
    }

    @Test
    void customize_WithIncludeHeadersFalse_AddsRemoveHeaderFilters() throws URISyntaxException {
        IgniteRouteDefinition igniteRoute = createIgniteRoute();
        RouteDefinition routeDef = createRouteDefinition();
        RateLimit rateLimit = createRateLimit("clientIp", REPLENISH_RATE_100, BURST_CAPACITY_200);
        rateLimit.setIncludeHeaders(false);

        when(rateLimitConfigResolver.resolveRateLimit(igniteRoute)).thenReturn(rateLimit);

        RouteDefinition result = customizer.customize(routeDef, igniteRoute);

        // Should have RequestRateLimiter + 4 RemoveResponseHeader filters
        assertTrue(result.getFilters().size() >= MIN_FILTER_COUNT_WITH_REMOVE_HEADERS,
            "Should have multiple filters");
        assertEquals("false", result.getFilters().get(0).getArgs().get("include-headers"),
            "include-headers should be false");
    }

    @Test
    void customize_WithHeaderKeyResolver_ResolvesProperly() throws URISyntaxException {
        Map<String, KeyResolver> specificResolvers = new HashMap<>();
        KeyResolver headerResolver = org.mockito.Mockito.mock(KeyResolver.class);
        specificResolvers.put("headerKeyResolver", headerResolver);
        RateLimitRouteCustomizer specificCustomizer =
            new RateLimitRouteCustomizer(rateLimitConfigResolver, specificResolvers);

        IgniteRouteDefinition igniteRoute = createIgniteRoute();
        RouteDefinition routeDef = createRouteDefinition();
        RateLimit rateLimit = createRateLimit("header", REPLENISH_RATE_100, BURST_CAPACITY_200);

        when(rateLimitConfigResolver.resolveRateLimit(igniteRoute)).thenReturn(rateLimit);

        RouteDefinition result = specificCustomizer.customize(routeDef, igniteRoute);

        FilterDefinition filter = result.getFilters().get(0);
        String keyResolver = filter.getArgs().get("key-resolver");
        assertNotNull(keyResolver);
        assertTrue(keyResolver.contains("headerKeyResolver"));
    }

    @Test
    void customize_WithRoutePathKeyResolver_ResolvesProperly() throws URISyntaxException {
        Map<String, KeyResolver> specificResolvers = new HashMap<>();
        specificResolvers.put("userIdKeyResolver", userIdKeyResolver);
        RateLimitRouteCustomizer specificCustomizer =
            new RateLimitRouteCustomizer(rateLimitConfigResolver, specificResolvers);

        IgniteRouteDefinition igniteRoute = createIgniteRoute();
        RouteDefinition routeDef = createRouteDefinition();
        RateLimit rateLimit = createRateLimit("route_path", REPLENISH_RATE_100, BURST_CAPACITY_200);

        when(rateLimitConfigResolver.resolveRateLimit(igniteRoute)).thenReturn(rateLimit);

        RouteDefinition result = specificCustomizer.customize(routeDef, igniteRoute);

        FilterDefinition filter = result.getFilters().get(0);
        String keyResolver = filter.getArgs().get("key-resolver");
        assertNotNull(keyResolver);
        assertTrue(keyResolver.contains("userIdKeyResolver"));
    }

    @Test
    void customize_WithRouteNameKeyResolver_ResolvesProperly() throws URISyntaxException {
        Map<String, KeyResolver> specificResolvers = new HashMap<>();
        KeyResolver apiKeyResolver = org.mockito.Mockito.mock(KeyResolver.class);
        specificResolvers.put("apiKeyKeyResolver", apiKeyResolver);
        RateLimitRouteCustomizer specificCustomizer =
            new RateLimitRouteCustomizer(rateLimitConfigResolver, specificResolvers);

        IgniteRouteDefinition igniteRoute = createIgniteRoute();
        RouteDefinition routeDef = createRouteDefinition();
        RateLimit rateLimit = createRateLimit("route_name", REPLENISH_RATE_100, BURST_CAPACITY_200);

        when(rateLimitConfigResolver.resolveRateLimit(igniteRoute)).thenReturn(rateLimit);

        RouteDefinition result = specificCustomizer.customize(routeDef, igniteRoute);

        FilterDefinition filter = result.getFilters().get(0);
        String keyResolver = filter.getArgs().get("key-resolver");
        assertNotNull(keyResolver);
        assertTrue(keyResolver.contains("apiKeyKeyResolver"));
    }

    @Test
    void customize_WithEmptyKeyResolver_ThrowsIllegalStateException() throws URISyntaxException {
        IgniteRouteDefinition igniteRoute = createIgniteRoute();
        RouteDefinition routeDef = createRouteDefinition();
        RateLimit rateLimit = createRateLimit("", REPLENISH_RATE_100, BURST_CAPACITY_200);

        when(rateLimitConfigResolver.resolveRateLimit(igniteRoute)).thenReturn(rateLimit);

        try {
            customizer.customize(routeDef, igniteRoute);
            org.junit.jupiter.api.Assertions.fail("Expected IllegalStateException was not thrown");
        } catch (IllegalStateException exception) {
            assertTrue(exception.getMessage().contains("No valid KeyResolver found"));
        }
    }

    @Test
    void customize_WithCustomResolverName_ResolvesCorrectly() throws URISyntaxException {
        KeyResolver customResolver = org.mockito.Mockito.mock(KeyResolver.class);
        Map<String, KeyResolver> specificResolvers = new HashMap<>();
        specificResolvers.put("myCustomResolver", customResolver);
        RateLimitRouteCustomizer specificCustomizer =
            new RateLimitRouteCustomizer(rateLimitConfigResolver, specificResolvers);

        IgniteRouteDefinition igniteRoute = createIgniteRoute();
        RouteDefinition routeDef = createRouteDefinition();
        RateLimit rateLimit = createRateLimit("myCustomResolver", REPLENISH_RATE_100, BURST_CAPACITY_200);

        when(rateLimitConfigResolver.resolveRateLimit(igniteRoute)).thenReturn(rateLimit);

        RouteDefinition result = specificCustomizer.customize(routeDef, igniteRoute);

        FilterDefinition filter = result.getFilters().get(0);
        String keyResolver = filter.getArgs().get("key-resolver");
        assertNotNull(keyResolver);
        assertTrue(keyResolver.contains("myCustomResolver"));
    }

    @Test
    void customize_WithDenyEmptyKeyAndEmptyKeyStatus_AddsToFilterConfig() throws URISyntaxException {
        IgniteRouteDefinition igniteRoute = createIgniteRoute();
        final RouteDefinition routeDef = createRouteDefinition();
        RateLimit rateLimit = createRateLimit("clientIp", REPLENISH_RATE_100, BURST_CAPACITY_200);
        rateLimit.setDenyEmptyKey(true);
        rateLimit.setEmptyKeyStatus("429");

        when(rateLimitConfigResolver.resolveRateLimit(igniteRoute)).thenReturn(rateLimit);

        RouteDefinition result = customizer.customize(routeDef, igniteRoute);

        FilterDefinition filter = result.getFilters().get(0);
        Map<String, String> args = filter.getArgs();

        assertEquals("true", args.get("denyEmptyKey"));
        assertEquals("429", args.get("emptyKeyStatus"));
    }
}
