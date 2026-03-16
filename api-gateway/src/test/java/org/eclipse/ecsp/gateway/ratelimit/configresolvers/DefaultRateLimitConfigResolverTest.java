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

package org.eclipse.ecsp.gateway.ratelimit.configresolvers;

import org.eclipse.ecsp.gateway.clients.ApiRegistryClient;
import org.eclipse.ecsp.gateway.config.RateLimitProperties;
import org.eclipse.ecsp.gateway.model.IgniteRouteDefinition;
import org.eclipse.ecsp.gateway.model.RateLimit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test class for DefaultRateLimitConfigResolver.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:VariableDeclarationUsageDistance"})
class DefaultRateLimitConfigResolverTest {

    @Mock
    private ApiRegistryClient apiRegistryClient;

    @Mock
    private RateLimitProperties rateLimitProperties;

    private DefaultRateLimitConfigResolver resolver;

    private RateLimit defaultRateLimit;

    @BeforeEach
    void setUp() {
        defaultRateLimit = new RateLimit();
        defaultRateLimit.setReplenishRate(100);
        defaultRateLimit.setBurstCapacity(200);
        defaultRateLimit.setRequestedTokens(1);

        when(rateLimitProperties.getDefaults()).thenReturn(defaultRateLimit);
        when(rateLimitProperties.getOverrides()).thenReturn(Collections.emptyList());
    }

    @Test
    void initializeWithNoRateLimitsFromRegistryUsesDefaultOnly() {
        // Arrange
        when(apiRegistryClient.getRateLimits()).thenReturn(Collections.emptyList());
        resolver = new DefaultRateLimitConfigResolver(apiRegistryClient, rateLimitProperties);

        // Act
        resolver.initialize();

        // Assert
        IgniteRouteDefinition route = new IgniteRouteDefinition();
        route.setId("test-route");
        route.setService("test-service");

        RateLimit result = resolver.resolveRateLimit(route);
        assertThat(result).isEqualTo(defaultRateLimit);
    }

    @Test
    void initializeWithNullRateLimitsFromRegistryHandlesGracefully() {
        // Arrange
        when(apiRegistryClient.getRateLimits()).thenReturn(null);
        resolver = new DefaultRateLimitConfigResolver(apiRegistryClient, rateLimitProperties);

        // Act
        resolver.initialize();

        // Assert
        IgniteRouteDefinition route = new IgniteRouteDefinition();
        route.setId("any-route");
        route.setService("any-service");

        RateLimit result = resolver.resolveRateLimit(route);
        assertThat(result).isEqualTo(defaultRateLimit);
    }

    @Test
    void resolveRateLimitWithRouteSpecificConfigReturnsRouteConfig() {
        // Arrange
        RateLimit routeRateLimit = new RateLimit();
        routeRateLimit.setRouteId("user-route-123");
        routeRateLimit.setReplenishRate(50);
        routeRateLimit.setBurstCapacity(100);

        when(apiRegistryClient.getRateLimits()).thenReturn(Collections.singletonList(routeRateLimit));
        resolver = new DefaultRateLimitConfigResolver(apiRegistryClient, rateLimitProperties);
        resolver.initialize();

        // Act
        IgniteRouteDefinition route = new IgniteRouteDefinition();
        route.setId("user-route-123");
        route.setService("user-service");

        RateLimit result = resolver.resolveRateLimit(route);

        // Assert
        assertThat(result).isEqualTo(routeRateLimit);
        assertThat(result.getReplenishRate()).isEqualTo(50);
    }

    @Test
    void resolveRateLimitWithServiceLevelConfigReturnsServiceConfig() {
        // Arrange
        RateLimit serviceRateLimit = new RateLimit();
        serviceRateLimit.setService("payment-service");
        serviceRateLimit.setReplenishRate(75);
        serviceRateLimit.setBurstCapacity(150);

        when(apiRegistryClient.getRateLimits()).thenReturn(Collections.singletonList(serviceRateLimit));
        resolver = new DefaultRateLimitConfigResolver(apiRegistryClient, rateLimitProperties);
        resolver.initialize();

        // Act
        IgniteRouteDefinition route = new IgniteRouteDefinition();
        route.setId("payment-route-456");
        route.setService("payment-service");

        RateLimit result = resolver.resolveRateLimit(route);

        // Assert
        assertThat(result).isEqualTo(serviceRateLimit);
        assertThat(result.getReplenishRate()).isEqualTo(75);
    }

    @Test
    void resolveRateLimitWithBothRouteAndServiceConfigPrefersRouteConfig() {
        // Arrange
        RateLimit routeRateLimit = new RateLimit();
        routeRateLimit.setRouteId("specific-route");
        routeRateLimit.setReplenishRate(25);

        RateLimit serviceRateLimit = new RateLimit();
        serviceRateLimit.setService("generic-service");
        serviceRateLimit.setReplenishRate(50);

        when(apiRegistryClient.getRateLimits()).thenReturn(Arrays.asList(routeRateLimit, serviceRateLimit));
        resolver = new DefaultRateLimitConfigResolver(apiRegistryClient, rateLimitProperties);
        resolver.initialize();

        // Act
        IgniteRouteDefinition route = new IgniteRouteDefinition();
        route.setId("specific-route");
        route.setService("generic-service");

        RateLimit result = resolver.resolveRateLimit(route);

        // Assert - Route-specific should win
        assertThat(result).isEqualTo(routeRateLimit);
        assertThat(result.getReplenishRate()).isEqualTo(25);
    }

    @Test
    void resolveRateLimitWithNoMatchingConfigReturnsDefault() {
        // Arrange
        RateLimit otherRouteLimit = new RateLimit();
        otherRouteLimit.setRouteId("other-route");
        otherRouteLimit.setReplenishRate(999);

        when(apiRegistryClient.getRateLimits()).thenReturn(Collections.singletonList(otherRouteLimit));
        resolver = new DefaultRateLimitConfigResolver(apiRegistryClient, rateLimitProperties);
        resolver.initialize();

        // Act
        IgniteRouteDefinition route = new IgniteRouteDefinition();
        route.setId("unmatched-route");
        route.setService("unmatched-service");

        RateLimit result = resolver.resolveRateLimit(route);

        // Assert
        assertThat(result).isEqualTo(defaultRateLimit);
        assertThat(result.getReplenishRate()).isEqualTo(100);
    }

    @Test
    void initializeWithPropertyOverridesAppliesOverrides() {
        // Arrange
        RateLimit registryLimit = new RateLimit();
        registryLimit.setRouteId("override-route");
        registryLimit.setReplenishRate(10);

        RateLimit overrideLimit = new RateLimit();
        overrideLimit.setRouteId("override-route");
        overrideLimit.setReplenishRate(500);

        when(apiRegistryClient.getRateLimits()).thenReturn(Collections.singletonList(registryLimit));
        when(rateLimitProperties.getOverrides()).thenReturn(Collections.singletonList(overrideLimit));

        resolver = new DefaultRateLimitConfigResolver(apiRegistryClient, rateLimitProperties);

        // Act
        resolver.initialize();

        // Assert
        IgniteRouteDefinition route = new IgniteRouteDefinition();
        route.setId("override-route");

        RateLimit result = resolver.resolveRateLimit(route);
        assertThat(result.getReplenishRate()).isEqualTo(500); // Override wins
    }

    @Test
    void initializeWithServiceOverrideOverridesRegistryServiceConfig() {
        // Arrange
        RateLimit registryServiceLimit = new RateLimit();
        registryServiceLimit.setService("auth-service");
        registryServiceLimit.setReplenishRate(20);

        RateLimit overrideServiceLimit = new RateLimit();
        overrideServiceLimit.setService("auth-service");
        overrideServiceLimit.setReplenishRate(300);

        when(apiRegistryClient.getRateLimits()).thenReturn(Collections.singletonList(registryServiceLimit));
        when(rateLimitProperties.getOverrides()).thenReturn(Collections.singletonList(overrideServiceLimit));

        resolver = new DefaultRateLimitConfigResolver(apiRegistryClient, rateLimitProperties);

        // Act
        resolver.initialize();

        // Assert
        IgniteRouteDefinition route = new IgniteRouteDefinition();
        route.setId("any-auth-route");
        route.setService("auth-service");

        RateLimit result = resolver.resolveRateLimit(route);
        assertThat(result.getReplenishRate()).isEqualTo(300);
    }

    @Test
    void initializeWithMixedRoutesAndServicesLoadsAllCorrectly() {
        // Arrange
        RateLimit route1 = new RateLimit();
        route1.setRouteId("route-a");
        route1.setReplenishRate(10);

        RateLimit route2 = new RateLimit();
        route2.setRouteId("route-b");
        route2.setReplenishRate(20);

        RateLimit service1 = new RateLimit();
        service1.setService("service-x");
        service1.setReplenishRate(30);

        RateLimit service2 = new RateLimit();
        service2.setService("service-y");
        service2.setReplenishRate(40);

        List<RateLimit> allLimits = Arrays.asList(route1, route2, service1, service2);

        when(apiRegistryClient.getRateLimits()).thenReturn(allLimits);
        resolver = new DefaultRateLimitConfigResolver(apiRegistryClient, rateLimitProperties);

        // Act
        resolver.initialize();

        // Assert
        IgniteRouteDefinition routeA = new IgniteRouteDefinition();
        routeA.setId("route-a");
        assertThat(resolver.resolveRateLimit(routeA).getReplenishRate()).isEqualTo(10);

        IgniteRouteDefinition routeB = new IgniteRouteDefinition();
        routeB.setId("route-b");
        assertThat(resolver.resolveRateLimit(routeB).getReplenishRate()).isEqualTo(20);

        IgniteRouteDefinition serviceX = new IgniteRouteDefinition();
        serviceX.setId("some-route");
        serviceX.setService("service-x");
        assertThat(resolver.resolveRateLimit(serviceX).getReplenishRate()).isEqualTo(30);

        IgniteRouteDefinition serviceY = new IgniteRouteDefinition();
        serviceY.setId("another-route");
        serviceY.setService("service-y");
        assertThat(resolver.resolveRateLimit(serviceY).getReplenishRate()).isEqualTo(40);
    }

    @Test
    void resolveRateLimitWithEmptyRouteIdAndServiceReturnsDefault() {
        // Arrange
        when(apiRegistryClient.getRateLimits()).thenReturn(Collections.emptyList());
        resolver = new DefaultRateLimitConfigResolver(apiRegistryClient, rateLimitProperties);
        resolver.initialize();

        // Act
        IgniteRouteDefinition route = new IgniteRouteDefinition();
        route.setId("");
        route.setService("");

        RateLimit result = resolver.resolveRateLimit(route);

        // Assert
        assertThat(result).isEqualTo(defaultRateLimit);
    }

    @Test
    void initializeWithNullOverridesHandlesGracefully() {
        // Arrange
        when(apiRegistryClient.getRateLimits()).thenReturn(Collections.emptyList());
        when(rateLimitProperties.getOverrides()).thenReturn(null);

        // Act
        resolver = new DefaultRateLimitConfigResolver(apiRegistryClient, rateLimitProperties);
        resolver.initialize();

        // Assert - should not throw exception
        IgniteRouteDefinition route = new IgniteRouteDefinition();
        route.setId("test");
        RateLimit result = resolver.resolveRateLimit(route);
        assertThat(result).isEqualTo(defaultRateLimit);
    }

    @Test
    void initializeWithEmptyStringsInRateLimitIgnoresInvalidEntries() {
        // Arrange
        RateLimit invalidLimit = new RateLimit();
        invalidLimit.setRouteId("");  // Empty route ID
        invalidLimit.setService("");   // Empty service
        invalidLimit.setReplenishRate(999);

        when(apiRegistryClient.getRateLimits()).thenReturn(Collections.singletonList(invalidLimit));
        resolver = new DefaultRateLimitConfigResolver(apiRegistryClient, rateLimitProperties);

        // Act
        resolver.initialize();

        // Assert - should fall back to default since empty strings are not stored
        IgniteRouteDefinition route = new IgniteRouteDefinition();
        route.setId("any-route");
        route.setService("any-service");

        RateLimit result = resolver.resolveRateLimit(route);
        assertThat(result).isEqualTo(defaultRateLimit);
    }
}
