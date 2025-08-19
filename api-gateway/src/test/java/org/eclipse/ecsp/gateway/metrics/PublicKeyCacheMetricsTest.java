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

package org.eclipse.ecsp.gateway.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.ecsp.gateway.cache.PublicKeyCache;
import org.eclipse.ecsp.gateway.model.PublicKeySource;
import org.eclipse.ecsp.gateway.plugins.keysources.PublicKeySourceProvider;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PublicKeyMetrics}.
 * Verifies that public key cache metrics are properly exposed for monitoring.
 *
 * @author Abhishek Kumar
 */
@ExtendWith(MockitoExtension.class)
class PublicKeyCacheMetricsTest {

    @Mock
    private PublicKeyCache publicKeyCache;

    @Mock
    private PublicKeySourceProvider sourceProvider1;

    @Mock
    private PublicKeySourceProvider sourceProvider2;

    @Mock
    private PublicKeySource publicKeySource1;

    @Mock
    private PublicKeySource publicKeySource2;

    @Mock
    private PublicKeySource publicKeySource3;

    @Mock
    private PublicKey publicKey;

    private MeterRegistry meterRegistry;
    private GatewayMetricsProperties gatewayMetricsProperties;
    private PublicKeyMetrics publicKeyCacheMetrics;

    // Refactored components
    private PublicKeyCacheMetricsRegistrar cacheMetricsRegistrar;
    private PublicKeyRefreshMetricsRecorder refreshMetricsRecorder;

    // Test constants to avoid magic numbers
    private static final int CACHE_SIZE_1 = 1;
    private static final int CACHE_SIZE_2 = 2;
    private static final int CACHE_SIZE_3 = 3;
    private static final int CACHE_SIZE_5 = 5;
    private static final int CACHE_SIZE_7 = 7;
    private static final double CACHE_SIZE_1_DOUBLE = 1.0;
    private static final double CACHE_SIZE_2_DOUBLE = 2.0;
    private static final double CACHE_SIZE_3_DOUBLE = 3.0;
    private static final double CACHE_SIZE_5_DOUBLE = 5.0;
    private static final double CACHE_SIZE_7_DOUBLE = 7.0;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        gatewayMetricsProperties = new GatewayMetricsProperties();
        
        // Setup metric configuration
        GatewayMetricsProperties.PublicKeyCacheMetrics config = new GatewayMetricsProperties.PublicKeyCacheMetrics();
        GatewayMetricsProperties.PublicKeyCacheMetrics.CacheSize cacheSize = 
                new GatewayMetricsProperties.PublicKeyCacheMetrics.CacheSize();
        GatewayMetricsProperties.PublicKeyCacheMetrics.KeySources keySources = 
                new GatewayMetricsProperties.PublicKeyCacheMetrics.KeySources();
        
        cacheSize.setName("test_cache_size");
        keySources.setName("test_key_sources_count");
        config.setCacheSize(cacheSize);
        config.setKeySources(keySources);
        gatewayMetricsProperties.setPublicKeyCache(config);

        List<PublicKeySourceProvider> sourceProviders = Arrays.asList(sourceProvider1, sourceProvider2);

        // Create the refactored components
        cacheMetricsRegistrar = new PublicKeyCacheMetricsRegistrar(
                meterRegistry, publicKeyCache, sourceProviders, gatewayMetricsProperties);
        refreshMetricsRecorder = new PublicKeyRefreshMetricsRecorder(
                meterRegistry, gatewayMetricsProperties);

        // Create PublicKeyMetrics with new constructor
        publicKeyCacheMetrics = new PublicKeyMetrics(cacheMetricsRegistrar, refreshMetricsRecorder);
    }

    @Test
    @DisplayName("Should register cache size metric with correct value")
    void shouldRegisterCacheSizeMetricWithCorrectValue() {
        // Given
        when(publicKeyCache.size()).thenReturn(CACHE_SIZE_5);

        // When
        publicKeyCacheMetrics.initializeMetrics();

        // Then
        Gauge cacheGauge = meterRegistry.find("test_cache_size").gauge();
        assertNotNull(cacheGauge, "Cache size gauge should be registered");
        assertEquals(CACHE_SIZE_5_DOUBLE, cacheGauge.value(), "Cache size should match mocked value");
    }

    @Test
    @DisplayName("Should register key sources count metric with correct value")
    void shouldRegisterKeySourcesCountMetricWithCorrectValue() {
        // Given
        when(sourceProvider1.keySources()).thenReturn(Arrays.asList(publicKeySource1, publicKeySource2));
        when(sourceProvider2.keySources()).thenReturn(Arrays.asList(publicKeySource3));

        // When
        publicKeyCacheMetrics.initializeMetrics();

        // Then
        Gauge keySourcesGauge = meterRegistry.find("test_key_sources_count").gauge();
        assertNotNull(keySourcesGauge, "Key sources gauge should be registered");
        assertEquals(CACHE_SIZE_3_DOUBLE, keySourcesGauge.value(), "Key sources count should be 3");
    }

    @Test
    @DisplayName("Should handle empty key sources gracefully")
    void shouldHandleEmptyKeySourcesGracefully() {
        // Given
        when(sourceProvider1.keySources()).thenReturn(Collections.emptyList());
        when(sourceProvider2.keySources()).thenReturn(null);

        // When
        publicKeyCacheMetrics.initializeMetrics();

        // Then
        Gauge keySourcesGauge = meterRegistry.find("test_key_sources_count").gauge();
        assertNotNull(keySourcesGauge, "Key sources gauge should be registered");
        assertEquals(0.0, keySourcesGauge.value(), "Key sources count should be 0 for empty/null sources");
    }

    @Test
    @DisplayName("Should handle cache size changes dynamically")
    void shouldHandleCacheSizeChangesDynamically() {
        // Given
        when(publicKeyCache.size()).thenReturn(CACHE_SIZE_3);
        publicKeyCacheMetrics.initializeMetrics();

        // When - cache size changes
        when(publicKeyCache.size()).thenReturn(CACHE_SIZE_7);

        // Then
        Gauge cacheGauge = meterRegistry.find("test_cache_size").gauge();
        assertEquals(CACHE_SIZE_7_DOUBLE, cacheGauge.value(), "Cache size should reflect the new value");
    }

    @Test
    @DisplayName("Should handle key sources count changes dynamically")
    void shouldHandleKeySourcesCountChangesDynamically() {
        // Given
        when(sourceProvider1.keySources()).thenReturn(Arrays.asList(publicKeySource1));
        when(sourceProvider2.keySources()).thenReturn(Collections.emptyList());
        publicKeyCacheMetrics.initializeMetrics();

        // When - key sources change
        when(sourceProvider1.keySources()).thenReturn(Arrays.asList(publicKeySource1, publicKeySource2));
        when(sourceProvider2.keySources()).thenReturn(Arrays.asList(publicKeySource3));

        // Then
        Gauge keySourcesGauge = meterRegistry.find("test_key_sources_count").gauge();
        assertEquals(CACHE_SIZE_3_DOUBLE, keySourcesGauge.value(), "Key sources count should reflect the new value");
    }

    @Test
    @DisplayName("Should use default metric names when configuration is missing")
    void shouldUseDefaultMetricNamesWhenConfigurationIsMissing() {
        // Given
        gatewayMetricsProperties.setPublicKeyCache(null);

        // Create new components with null configuration
        List<PublicKeySourceProvider> sourceProviders = Arrays.asList(sourceProvider1);
        PublicKeyCacheMetricsRegistrar newCacheRegistrar = new PublicKeyCacheMetricsRegistrar(
                meterRegistry, publicKeyCache, sourceProviders, gatewayMetricsProperties);
        PublicKeyRefreshMetricsRecorder newRefreshRecorder = new PublicKeyRefreshMetricsRecorder(
                meterRegistry, gatewayMetricsProperties);
        publicKeyCacheMetrics = new PublicKeyMetrics(newCacheRegistrar, newRefreshRecorder);

        when(publicKeyCache.size()).thenReturn(CACHE_SIZE_2);
        when(sourceProvider1.keySources()).thenReturn(Arrays.asList(publicKeySource1));

        // When
        publicKeyCacheMetrics.initializeMetrics();

        // Then
        Gauge cacheGauge = meterRegistry.find("public_key_cache_size").gauge();
        Gauge keySourcesGauge = meterRegistry.find("public_key_sources_count").gauge();
        
        assertNotNull(cacheGauge, "Default cache size gauge should be registered");
        assertNotNull(keySourcesGauge, "Default key sources gauge should be registered");
        assertEquals(CACHE_SIZE_2_DOUBLE, cacheGauge.value(), 
                "Cache size should be correct with default name");
        assertEquals(CACHE_SIZE_1_DOUBLE, keySourcesGauge.value(), 
                "Key sources count should be correct with default name");
    }

    @Test
    @DisplayName("Should include correct tags in metrics")
    void shouldIncludeCorrectTagsInMetrics() {
        // Given - simplified test focusing only on tag verification
        when(publicKeyCache.size()).thenReturn(CACHE_SIZE_1);
        when(sourceProvider1.keySources()).thenReturn(Arrays.asList(publicKeySource1));
        when(sourceProvider2.keySources()).thenReturn(Collections.emptyList());

        // When
        publicKeyCacheMetrics.initializeMetrics();

        // Then - verify tags are correct
        Gauge cacheGauge = meterRegistry.find("test_cache_size")
                .tag("component", "public-key-cache")
                .tag("type", "cache-size")
                .gauge();
        
        Gauge keySourcesGauge = meterRegistry.find("test_key_sources_count")
                .tag("component", "public-key-cache")
                .tag("type", "key-sources")
                .gauge();

        assertNotNull(cacheGauge, "Cache gauge with correct tags should be found");
        assertNotNull(keySourcesGauge, "Key sources gauge with correct tags should be found");
        
        // Verify the values are as expected (this ensures the mocks were actually used)
        assertEquals(CACHE_SIZE_1_DOUBLE, cacheGauge.value(), "Cache size should match mock");
        assertEquals(CACHE_SIZE_1_DOUBLE, keySourcesGauge.value(), "Key sources count should match mock");
    }

    @Test
    @DisplayName("Should handle exceptions in source providers gracefully")
    void shouldHandleExceptionsInSourceProvidersGracefully() {
        // Given
        when(sourceProvider1.keySources()).thenThrow(new RuntimeException("Test exception"));
        when(sourceProvider2.keySources()).thenReturn(Arrays.asList(publicKeySource1));

        // When
        publicKeyCacheMetrics.initializeMetrics();

        // Then
        Gauge keySourcesGauge = meterRegistry.find("test_key_sources_count").gauge();
        assertNotNull(keySourcesGauge, "Key sources gauge should be registered despite exception");
        assertEquals(CACHE_SIZE_1_DOUBLE, keySourcesGauge.value(), "Should count sources from working providers only");
    }

    @Test
    @DisplayName("Should record source refresh events correctly")
    void shouldRecordSourceRefreshEventsCorrectly() {
        // Given
        publicKeyCacheMetrics.initializeMetrics();

        // When
        publicKeyCacheMetrics.recordSourceRefresh("source1");
        publicKeyCacheMetrics.recordSourceRefresh("source2");
        publicKeyCacheMetrics.recordSourceRefresh("source1");

        // Then
        // Counter increments should be reflected in the registry
        assertNotNull(meterRegistry.find(GatewayConstants.DEFAULT_SOURCE_REFRESH_COUNT_METRIC).counter(),
                "Source refresh counter should be registered");
    }
}
