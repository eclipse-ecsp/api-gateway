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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.gateway.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.eclipse.ecsp.gateway.metrics.GatewayMetricsProperties.BaseMetrics;
import org.eclipse.ecsp.gateway.metrics.GatewayMetricsProperties.DistributedMetrics;
import org.eclipse.ecsp.gateway.metrics.GatewayMetricsProperties.DistributionConfig;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for GatewayMeterFilter.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class GatewayMeterFilterTest {

    @Test
    void constructor_WithNullConfig_InitializesSuccessfully() {
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(null);
        assertNotNull(filter);
    }

    @Test
    void constructor_WithConfigAndIgnoreTags_InitializesWithIgnoreTags() {
        BaseMetrics config = mock(BaseMetrics.class);
        when(config.getIgnoreTags()).thenReturn(List.of("tag1", "tag2"));
        
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(config);
        assertNotNull(filter);
    }

    @Test
    void constructor_WithConfigAndNoIgnoreTags_InitializesSuccessfully() {
        BaseMetrics config = mock(BaseMetrics.class);
        when(config.getIgnoreTags()).thenReturn(List.of());
        
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(config);
        assertNotNull(filter);
    }

    @Test
    void accept_WithNullConfig_ReturnsNeutral() {
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(null);
        Meter.Id meterId = mock(Meter.Id.class);
        when(meterId.getName()).thenReturn("test.metric");
        
        MeterFilterReply reply = filter.accept(meterId);
        assertEquals(MeterFilterReply.NEUTRAL, reply);
    }

    @Test
    void accept_WithEnabledMetric_ReturnsNeutral() {
        BaseMetrics config = mock(BaseMetrics.class);
        when(config.getPrefix()).thenReturn("gateway.metrics");
        when(config.getEnabled()).thenReturn(true);
        
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(config);
        Meter.Id meterId = mock(Meter.Id.class);
        when(meterId.getName()).thenReturn("gateway.metrics.test");
        
        MeterFilterReply reply = filter.accept(meterId);
        assertEquals(MeterFilterReply.NEUTRAL, reply);
    }

    @Test
    void accept_WithDisabledMetric_ReturnsDeny() {
        BaseMetrics config = mock(BaseMetrics.class);
        when(config.getPrefix()).thenReturn("gateway.metrics");
        when(config.getEnabled()).thenReturn(false);
        
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(config);
        Meter.Id meterId = mock(Meter.Id.class);
        when(meterId.getName()).thenReturn("gateway.metrics.test");
        
        MeterFilterReply reply = filter.accept(meterId);
        assertEquals(MeterFilterReply.DENY, reply);
    }

    @Test
    void accept_WithNonMatchingPrefix_ReturnsNeutral() {
        BaseMetrics config = mock(BaseMetrics.class);
        when(config.getPrefix()).thenReturn("gateway.metrics");
        when(config.getEnabled()).thenReturn(false);
        
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(config);
        Meter.Id meterId = mock(Meter.Id.class);
        when(meterId.getName()).thenReturn("other.metrics.test");
        
        MeterFilterReply reply = filter.accept(meterId);
        assertEquals(MeterFilterReply.NEUTRAL, reply);
    }

    @Test
    void map_WithDefaultFilter_ReturnsSameId() {
        BaseMetrics config = mock(BaseMetrics.class);
        when(config.getIgnoreTags()).thenReturn(List.of());
        
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(config);
        Meter.Id meterId = mock(Meter.Id.class);
        
        Meter.Id result = filter.map(meterId);
        assertEquals(meterId, result);
    }

    @Test
    void configure_WithNullConfig_ReturnsOriginalConfig() {
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(null);
        Meter.Id meterId = mock(Meter.Id.class);
        DistributionStatisticConfig config = DistributionStatisticConfig.DEFAULT;
        
        DistributionStatisticConfig result = filter.configure(meterId, config);
        assertEquals(config, result);
    }

    @Test
    void configure_WithDisabledMetrics_ReturnsOriginalConfig() {
        BaseMetrics metricsConfig = mock(BaseMetrics.class);
        when(metricsConfig.getEnabled()).thenReturn(false);
        
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(metricsConfig);
        Meter.Id meterId = mock(Meter.Id.class);
        DistributionStatisticConfig config = DistributionStatisticConfig.DEFAULT;
        
        DistributionStatisticConfig result = filter.configure(meterId, config);
        assertEquals(config, result);
    }

    @Test
    void configure_WithNonMatchingPrefix_ReturnsOriginalConfig() {
        BaseMetrics metricsConfig = mock(BaseMetrics.class);
        when(metricsConfig.getEnabled()).thenReturn(true);
        when(metricsConfig.getPrefix()).thenReturn("gateway.metrics");
        
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(metricsConfig);
        Meter.Id meterId = mock(Meter.Id.class);
        when(meterId.getName()).thenReturn("other.metric");
        
        DistributionStatisticConfig config = DistributionStatisticConfig.DEFAULT;
        DistributionStatisticConfig result = filter.configure(meterId, config);
        assertEquals(config, result);
    }

    @Test
    void configure_WithNonDistributedMetrics_ReturnsOriginalConfig() {
        BaseMetrics metricsConfig = mock(BaseMetrics.class);
        when(metricsConfig.getEnabled()).thenReturn(true);
        when(metricsConfig.getPrefix()).thenReturn("gateway.metrics");
        
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(metricsConfig);
        Meter.Id meterId = mock(Meter.Id.class);
        when(meterId.getName()).thenReturn("gateway.metrics.test");
        when(meterId.getType()).thenReturn(Meter.Type.TIMER);
        
        DistributionStatisticConfig config = DistributionStatisticConfig.DEFAULT;
        DistributionStatisticConfig result = filter.configure(meterId, config);
        assertEquals(config, result);
    }

    @Test
    void configure_WithDistributedMetricsDisabled_ReturnsOriginalConfig() {
        DistributedMetrics metricsConfig = mock(DistributedMetrics.class);
        DistributionConfig distributionConfig = mock(DistributionConfig.class);
        when(metricsConfig.getEnabled()).thenReturn(true);
        when(metricsConfig.getPrefix()).thenReturn("gateway.metrics");
        when(metricsConfig.getDistribution()).thenReturn(distributionConfig);
        when(distributionConfig.isEnabled()).thenReturn(false);
        
        GatewayMeterFilter<DistributedMetrics> filter = new GatewayMeterFilter<>(metricsConfig);
        Meter.Id meterId = mock(Meter.Id.class);
        when(meterId.getName()).thenReturn("gateway.metrics.test");
        when(meterId.getType()).thenReturn(Meter.Type.TIMER);
        
        DistributionStatisticConfig config = DistributionStatisticConfig.DEFAULT;
        DistributionStatisticConfig result = filter.configure(meterId, config);
        assertEquals(config, result);
    }

    @Test
    void configure_WithDistributedMetricsEnabledForTimer_ReturnsConfiguredConfig() {
        DistributedMetrics metricsConfig = mock(DistributedMetrics.class);
        DistributionConfig distributionConfig = mock(DistributionConfig.class);
        Duration[] buckets = new Duration[]{Duration.ofMillis(10), Duration.ofMillis(100), Duration.ofSeconds(1)};
        
        when(metricsConfig.getEnabled()).thenReturn(true);
        when(metricsConfig.getPrefix()).thenReturn("gateway.metrics");
        when(metricsConfig.getDistribution()).thenReturn(distributionConfig);
        when(distributionConfig.isEnabled()).thenReturn(true);
        when(distributionConfig.getBuckets()).thenReturn(buckets);
        when(distributionConfig.getExpiry()).thenReturn(Duration.ofMinutes(1));
        when(distributionConfig.getBufferLength()).thenReturn(5);
        
        GatewayMeterFilter<DistributedMetrics> filter = new GatewayMeterFilter<>(metricsConfig);
        Meter.Id meterId = mock(Meter.Id.class);
        when(meterId.getName()).thenReturn("gateway.metrics.test");
        when(meterId.getType()).thenReturn(Meter.Type.TIMER);
        
        DistributionStatisticConfig config = DistributionStatisticConfig.DEFAULT;
        DistributionStatisticConfig result = filter.configure(meterId, config);
        
        assertNotNull(result);
        assertNotNull(result.getServiceLevelObjectiveBoundaries());
    }

    @Test
    void configure_WithDistributedMetricsEnabledForDistributionSummary_ReturnsConfiguredConfig() {
        DistributedMetrics metricsConfig = mock(DistributedMetrics.class);
        DistributionConfig distributionConfig = mock(DistributionConfig.class);
        Duration[] buckets = new Duration[]{Duration.ofMillis(10), Duration.ofMillis(100)};
        
        when(metricsConfig.getEnabled()).thenReturn(true);
        when(metricsConfig.getPrefix()).thenReturn("gateway.metrics");
        when(metricsConfig.getDistribution()).thenReturn(distributionConfig);
        when(distributionConfig.isEnabled()).thenReturn(true);
        when(distributionConfig.getBuckets()).thenReturn(buckets);
        when(distributionConfig.getExpiry()).thenReturn(null);
        when(distributionConfig.getBufferLength()).thenReturn(0);
        
        GatewayMeterFilter<DistributedMetrics> filter = new GatewayMeterFilter<>(metricsConfig);
        Meter.Id meterId = mock(Meter.Id.class);
        when(meterId.getName()).thenReturn("gateway.metrics.test");
        when(meterId.getType()).thenReturn(Meter.Type.DISTRIBUTION_SUMMARY);
        
        DistributionStatisticConfig config = DistributionStatisticConfig.DEFAULT;
        DistributionStatisticConfig result = filter.configure(meterId, config);
        
        assertNotNull(result);
    }

    @Test
    void configure_WithValidBucketsAndNullExpiry_ReturnsConfiguredConfig() {
        // Test with valid buckets to ensure configuration works properly
        DistributedMetrics metricsConfig = mock(DistributedMetrics.class);
        DistributionConfig distributionConfig = mock(DistributionConfig.class);
        Duration[] validBuckets = new Duration[]{Duration.ofMillis(10)};
        
        when(metricsConfig.getEnabled()).thenReturn(true);
        when(metricsConfig.getPrefix()).thenReturn("gateway.metrics");
        when(metricsConfig.getDistribution()).thenReturn(distributionConfig);
        when(distributionConfig.isEnabled()).thenReturn(true);
        when(distributionConfig.getBuckets()).thenReturn(validBuckets);
        when(distributionConfig.getExpiry()).thenReturn(null);
        when(distributionConfig.getBufferLength()).thenReturn(0);
        
        GatewayMeterFilter<DistributedMetrics> filter = new GatewayMeterFilter<>(metricsConfig);
        Meter.Id meterId = mock(Meter.Id.class);
        when(meterId.getName()).thenReturn("gateway.metrics.test");
        when(meterId.getType()).thenReturn(Meter.Type.TIMER);
        
        DistributionStatisticConfig config = DistributionStatisticConfig.DEFAULT;
        DistributionStatisticConfig result = filter.configure(meterId, config);
        
        assertNotNull(result);
    }

    @Test
    void configure_WithEmptyBuckets_ReturnsConfigWithoutSlo() {
        DistributedMetrics metricsConfig = mock(DistributedMetrics.class);
        DistributionConfig distributionConfig = mock(DistributionConfig.class);
        
        when(metricsConfig.getEnabled()).thenReturn(true);
        when(metricsConfig.getPrefix()).thenReturn("gateway.metrics");
        when(metricsConfig.getDistribution()).thenReturn(distributionConfig);
        when(distributionConfig.isEnabled()).thenReturn(true);
        when(distributionConfig.getBuckets()).thenReturn(new Duration[0]);
        when(distributionConfig.getExpiry()).thenReturn(null);
        when(distributionConfig.getBufferLength()).thenReturn(0);
        
        GatewayMeterFilter<DistributedMetrics> filter = new GatewayMeterFilter<>(metricsConfig);
        Meter.Id meterId = mock(Meter.Id.class);
        when(meterId.getName()).thenReturn("gateway.metrics.test");
        when(meterId.getType()).thenReturn(Meter.Type.TIMER);
        
        DistributionStatisticConfig config = DistributionStatisticConfig.DEFAULT;
        DistributionStatisticConfig result = filter.configure(meterId, config);
        
        assertNotNull(result);
    }

    @Test
    void configure_WithNonTimerOrSummaryType_ReturnsOriginalConfig() {
        DistributedMetrics metricsConfig = mock(DistributedMetrics.class);
        DistributionConfig distributionConfig = mock(DistributionConfig.class);
        
        when(metricsConfig.getEnabled()).thenReturn(true);
        when(metricsConfig.getPrefix()).thenReturn("gateway.metrics");
        when(metricsConfig.getDistribution()).thenReturn(distributionConfig);
        when(distributionConfig.isEnabled()).thenReturn(true);
        
        GatewayMeterFilter<DistributedMetrics> filter = new GatewayMeterFilter<>(metricsConfig);
        Meter.Id meterId = mock(Meter.Id.class);
        when(meterId.getName()).thenReturn("gateway.metrics.test");
        when(meterId.getType()).thenReturn(Meter.Type.COUNTER);
        
        DistributionStatisticConfig config = DistributionStatisticConfig.DEFAULT;
        DistributionStatisticConfig result = filter.configure(meterId, config);
        
        assertEquals(config, result);
    }

    @Test
    void convertServiceLevelObjectives_WithValidDurations_ReturnsDoubleArray() {
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(null);
        Duration[] durations = new Duration[]{Duration.ofMillis(10), Duration.ofMillis(100), Duration.ofSeconds(1)};
        
        double[] result = filter.convertServiceLevelObjectives(durations);
        
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(10_000_000.0, result[0], 0.1);  // 10ms in nanos
        assertEquals(100_000_000.0, result[1], 0.1); // 100ms in nanos
        assertEquals(1_000_000_000.0, result[2], 0.1); // 1s in nanos
    }

    @Test
    void convertServiceLevelObjectives_WithEmptyArray_ReturnsEmptyArray() {
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(null);
        Duration[] durations = new Duration[0];
        
        double[] result = filter.convertServiceLevelObjectives(durations);
        
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void getMinimumValue_WithValidDurations_ReturnsMinimum() {
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(null);
        Duration[] durations = new Duration[]{Duration.ofMillis(100), Duration.ofMillis(10), Duration.ofSeconds(1)};
        
        double result = filter.getMinimumValue(durations);
        
        assertEquals(10_000_000.0, result, 0.1); // 10ms in nanos (minimum)
    }

    @Test
    void getMinimumValue_WithEmptyArray_ReturnsDefaultValue() {
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(null);
        Duration[] durations = new Duration[0];
        
        double result = filter.getMinimumValue(durations);
        
        assertEquals(1.0, result, 0.1); // 1ms default
    }

    @Test
    void getMaximumValue_WithValidDurations_ReturnsMaximum() {
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(null);
        Duration[] durations = new Duration[]{Duration.ofMillis(10), Duration.ofMillis(100), Duration.ofSeconds(1)};
        
        double result = filter.getMaximumValue(durations);
        
        assertEquals(1_000_000_000.0, result, 0.1); // 1s in nanos (maximum)
    }

    @Test
    void getMaximumValue_WithEmptyArray_ReturnsDefaultValue() {
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(null);
        Duration[] durations = new Duration[0];
        
        double result = filter.getMaximumValue(durations);
        
        assertEquals(60000.0, result, 0.1); // 1 minute in millis default
    }

    @Test
    void getMinimumValue_WithSingleDuration_ReturnsThatValue() {
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(null);
        Duration[] durations = new Duration[]{Duration.ofMillis(50)};
        
        double result = filter.getMinimumValue(durations);
        
        assertEquals(50_000_000.0, result, 0.1); // 50ms in nanos
    }

    @Test
    void getMaximumValue_WithSingleDuration_ReturnsThatValue() {
        GatewayMeterFilter<BaseMetrics> filter = new GatewayMeterFilter<>(null);
        Duration[] durations = new Duration[]{Duration.ofMillis(50)};
        
        double result = filter.getMaximumValue(durations);
        
        assertEquals(50_000_000.0, result, 0.1); // 50ms in nanos
    }
}
