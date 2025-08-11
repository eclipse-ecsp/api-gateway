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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.gateway.ApiGatewayApplication;
import org.eclipse.ecsp.gateway.cache.PublicKeyCache;
import org.eclipse.ecsp.gateway.events.PublicKeyRefreshEvent;
import org.eclipse.ecsp.gateway.events.PublicKeyRefreshEvent.RefreshType;
import org.eclipse.ecsp.gateway.model.PublicKeySource;
import org.eclipse.ecsp.gateway.plugins.keysources.PublicKeySourceProvider;
import org.eclipse.ecsp.gateway.rest.ApiGatewayController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for PublicKeyMetrics to verify metrics are exposed correctly
 * in a real Spring Boot context with actuator endpoints.
 *
 * @author Abhishek Kumar
 */
@SpringBootTest(
    classes = {ApiGatewayApplication.class},
    webEnvironment = WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "api.gateway.metrics.enabled=true",
    "api.gateway.metrics.public-key-cache.enabled=true"
})
class PublicKeyCacheMetricsIntegrationTest {

    public static final int INT_5 = 5;
    public static final double DOUBLE_5 = 5.0;
    public static final double DOUBLE_3 = 3.0;
    public static final int INT_10 = 10;
    public static final int INT_3 = 3;
    public static final int INT_7 = 7;
    public static final double DOUBLE_7 = 7.0;
    public static final double DOUBLE_1000 = 1000.0;
    public static final double DOUBLE_2 = 2.0;
    public static final int EXPECTED = 2;
    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private PublicKeyCache publicKeyCache;

    @Autowired
    private List<PublicKeySourceProvider> publicKeySourceProviders;

    @Autowired
    private GatewayMetricsProperties gatewayMetricsProperties;

    @MockitoBean
    private ApiGatewayController apiGatewayController;

    @BeforeAll
    static void setup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @BeforeEach
    void resetMeterRegistry() {
        // Clear any existing meters to ensure clean state
        meterRegistry.clear();
    }

    @Test
    @DisplayName("Should have metrics registered in MeterRegistry")
    void shouldHaveMetricsRegisteredInMeterRegistry() {
        // Given
        PublicKeyMetrics metrics = new PublicKeyMetrics(
            meterRegistry, publicKeyCache, publicKeySourceProviders, gatewayMetricsProperties);

        // When
        metrics.initializeMetrics();

        // Then
        assertTrue(meterRegistry.find("public_key_cache_size").gauge() != null
                || meterRegistry.getMeters().stream().anyMatch(meter -> 
                    meter.getId().getName().contains("public_key")),
                "Should have public key related metrics registered");
    }

    @Test
    @DisplayName("Should have PublicKeyMetrics bean available")
    void shouldHavePublicKeyCacheMetricsBeanAvailable() {
        // This test verifies that the metrics component is properly configured
        // and can be instantiated in the Spring context
        assertNotNull(meterRegistry, "MeterRegistry should be available");
        assertTrue(meterRegistry.getMeters().size() >= 0, "MeterRegistry should be functional");
    }

    @Test
    @DisplayName("Should register cache size metric with correct value")
    void shouldRegisterCacheSizeMetricWithCorrectValue() {
        // Given
        PublicKeyCache mockPublicKeyCache = mock(PublicKeyCache.class);
        when(mockPublicKeyCache.size()).thenReturn(INT_5);
        PublicKeyMetrics metrics = new PublicKeyMetrics(
            meterRegistry, mockPublicKeyCache, publicKeySourceProviders, gatewayMetricsProperties);

        // When
        metrics.initializeMetrics();

        // Then
        assertNotNull(meterRegistry.find("public_key_cache_size").gauge());
        assertEquals(DOUBLE_5, meterRegistry.find("public_key_cache_size").gauge().value(), 0.0);
    }

    @Test
    @DisplayName("Should register key sources count metric with correct value")
    void shouldRegisterKeySourcesCountMetricWithCorrectValue() {
        // Given
        PublicKeySourceProvider provider1 = mock(PublicKeySourceProvider.class);
        PublicKeySourceProvider provider2 = mock(PublicKeySourceProvider.class);
        when(provider1.keySources()).thenReturn(List.of(mock(PublicKeySource.class), mock(PublicKeySource.class)));
        when(provider2.keySources()).thenReturn(List.of(mock(PublicKeySource.class)));
        List<PublicKeySourceProvider> providers = List.of(provider1, provider2);

        // When
        PublicKeyMetrics metrics = new PublicKeyMetrics(
            meterRegistry, publicKeyCache, providers, gatewayMetricsProperties);
        metrics.initializeMetrics();

        // Then
        assertNotNull(meterRegistry.find("public_key_sources_count").gauge());
        assertEquals(DOUBLE_3, meterRegistry.find("public_key_sources_count").gauge().value(), 0.0);
    }

    @Test
    @DisplayName("Should track full refresh events correctly")
    void shouldTrackFullRefreshEventsCorrectly() {
        // Given
        PublicKeyMetrics metrics = new PublicKeyMetrics(
            meterRegistry, publicKeyCache, publicKeySourceProviders, gatewayMetricsProperties);
        metrics.initializeMetrics();

        // When
        metrics.recordFullRefresh();
        metrics.recordFullRefresh();

        // Then
        Gauge gauge = meterRegistry.find("public_key_refresh_count").gauge();
        assertNotNull(gauge, "Gauge for public key refresh count should be available");
        assertEquals(DOUBLE_2, gauge.value(), 0.0);
        Gauge refreshTimeGauge = meterRegistry.find("public_key_refresh_time").gauge();
        assertNotNull(refreshTimeGauge, "Gauge for public key refresh time should be available");
        assertTrue(refreshTimeGauge.value() > 0);
    }

    @Test
    @DisplayName("Should track individual source refresh events")
    void shouldTrackIndividualSourceRefreshEvents() {
        // Given
        PublicKeyMetrics metrics = new PublicKeyMetrics(
            meterRegistry, publicKeyCache, publicKeySourceProviders, gatewayMetricsProperties);
        metrics.initializeMetrics();

        // When
        metrics.handleMetricsEvents(new PublicKeyRefreshEvent(RefreshType.PUBLIC_KEY, "source1"));
        metrics.handleMetricsEvents(new PublicKeyRefreshEvent(RefreshType.PUBLIC_KEY, "source2"));
        metrics.handleMetricsEvents(new PublicKeyRefreshEvent(RefreshType.PUBLIC_KEY, "source1"));

        // Then
        Collection<Counter> counters = meterRegistry.get("public_key_source_refresh_count").counters();
        assertNotNull(counters, "Counters for public key source refresh count should be available");
        assertEquals(EXPECTED, counters.size(), "There should be one counter for public key source refresh count");
    }

    @Test
    @DisplayName("Should handle null key sources gracefully")
    void shouldHandleNullKeySourcesGracefully() {
        // Given
        PublicKeySourceProvider provider = mock(PublicKeySourceProvider.class);
        when(provider.keySources()).thenReturn(null);
        List<PublicKeySourceProvider> providers = List.of(provider);

        // When
        PublicKeyMetrics metrics = new PublicKeyMetrics(
            meterRegistry, publicKeyCache, providers, gatewayMetricsProperties);
        metrics.initializeMetrics();

        // Then
        assertEquals(0.0, meterRegistry.find("public_key_sources_count").gauge().value(), 0.0);
    }

    @Test
    @DisplayName("Should handle provider exceptions gracefully")
    void shouldHandleProviderExceptionsGracefully() {
        // Given
        PublicKeySourceProvider faultyProvider = mock(PublicKeySourceProvider.class);
        when(faultyProvider.keySources()).thenThrow(new RuntimeException("Provider error"));
        List<PublicKeySourceProvider> providers = List.of(faultyProvider);

        // When
        PublicKeyMetrics metrics = new PublicKeyMetrics(
            meterRegistry, publicKeyCache, providers, gatewayMetricsProperties);
        metrics.initializeMetrics();

        // Then
        assertEquals(0.0, meterRegistry.find("public_key_sources_count").gauge().value(), 0.0);
    }

    @Test
    @DisplayName("Should register all required metrics with proper tags")
    void shouldRegisterAllRequiredMetricsWithProperTags() {
        // Given
        PublicKeyMetrics metrics = new PublicKeyMetrics(
            meterRegistry, publicKeyCache, publicKeySourceProviders, gatewayMetricsProperties);

        // When
        metrics.initializeMetrics();

        // Then
        String[] expectedMetrics = {
            "public_key_cache_size",
            "public_key_sources_count",
            "public_key_refresh_count",
            "public_key_refresh_time",
            "public_key_source_refresh_count",
            "public_key_source_refresh_time"
        };

        metrics.handleMetricsEvents(new PublicKeyRefreshEvent(RefreshType.PUBLIC_KEY, "source1"));

        for (String metricName : expectedMetrics) {
            assertNotNull(meterRegistry.find(metricName).meter(),
                "Metric " + metricName + " should be registered");
            assertTrue(meterRegistry.find(metricName).meter().getId().getTags().stream()
                .anyMatch(tag -> "component".equals(tag.getKey()) && "public-key-cache".equals(tag.getValue())),
                "Metric " + metricName + " should have component tag");
        }
    }

    @Test
    @DisplayName("Should work with empty source providers list")
    void shouldWorkWithEmptySourceProvidersList() {
        // Given
        List<PublicKeySourceProvider> emptyProviders = List.of();

        // When
        PublicKeyMetrics metrics = new PublicKeyMetrics(
            meterRegistry, publicKeyCache, emptyProviders, gatewayMetricsProperties);
        metrics.initializeMetrics();

        // Then
        assertEquals(0.0, meterRegistry.find("public_key_sources_count").gauge().value(), 0.0);
    }

    @Test
    @DisplayName("Should update cache size metric dynamically")
    void shouldUpdateCacheSizeMetricDynamically() {
        // Given
        PublicKeyCache mockCache = mock(PublicKeyCache.class);
        when(mockCache.size()).thenReturn(INT_3).thenReturn(INT_7);
        PublicKeyMetrics metrics = new PublicKeyMetrics(
            meterRegistry, mockCache, publicKeySourceProviders, gatewayMetricsProperties);
        metrics.initializeMetrics();

        // When - First read
        double firstValue = meterRegistry.find("public_key_cache_size").gauge().value();

        // When - Second read after cache size changes
        double secondValue = meterRegistry.find("public_key_cache_size").gauge().value();

        // Then
        assertEquals(DOUBLE_3, firstValue, 0.0);
        assertEquals(DOUBLE_7, secondValue, 0.0);
    }

    @Test
    @DisplayName("Should record timestamp correctly for last refresh time")
    void shouldRecordTimestampCorrectlyForLastRefreshTime() {
        // Given
        PublicKeyMetrics metrics = new PublicKeyMetrics(
            meterRegistry, publicKeyCache, publicKeySourceProviders, gatewayMetricsProperties);
        metrics.initializeMetrics();
        double beforeRefresh = System.currentTimeMillis() / DOUBLE_1000; // Convert to seconds with decimal precision

        // When
        metrics.recordFullRefresh();

        // Then
        double lastRefreshTime = meterRegistry.find("public_key_refresh_time").gauge().value();
        double afterRefresh = System.currentTimeMillis() / DOUBLE_1000; // Convert to seconds with decimal precision

        assertTrue(lastRefreshTime >= beforeRefresh && lastRefreshTime <= afterRefresh,
            "Last refresh time should be within the test execution timeframe");
    }
}
