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
import io.micrometer.core.instrument.Tags;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.annotations.ConditionOnPublicKeyMetricsEnabled;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.eclipse.ecsp.gateway.utils.GatewayConstants.COMPONENT;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.DEFAULT_SOURCE_REFRESH_COUNT_METRIC;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.DEFAULT_SOURCE_REFRESH_TIME_METRIC;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.PUBLIC_KEY_CACHE;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.REFRESH_SOURCE_COUNT;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.REFRESH_SOURCE_TIME;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.TYPE;

/**
 * Records refresh-related metrics for public key cache operations.
 * Handles both full cache refreshes and individual source refreshes.
 */
@Component
@ConditionOnPublicKeyMetricsEnabled
public class PublicKeyRefreshMetricsRecorder {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PublicKeyRefreshMetricsRecorder.class);
    private static final double MILLISECONDS_TO_SECONDS_DIVISOR = 1000.0;

    private final MeterRegistry meterRegistry;
    private final MetricConfigurationAccessor configAccessor;

    // Track source-specific refresh times
    private final Map<String, AtomicLong> sourceRefreshTimes = new ConcurrentHashMap<>();

    public PublicKeyRefreshMetricsRecorder(MeterRegistry meterRegistry,
                                          GatewayMetricsProperties gatewayMetricsProperties) {
        this.meterRegistry = meterRegistry;
        this.configAccessor = new MetricConfigurationAccessor(gatewayMetricsProperties);
    }

    /**
     * Register refresh-related metrics.
     * Since full cache refresh only happens at startup, we only register source-specific metrics.
     */
    public void registerRefreshMetrics() {
        LOGGER.debug("PublicKeyRefreshMetricsRecorder initialized - only source-specific metrics will be tracked");
    }

    /**
     * Record an individual public key source refresh event.
     */
    public void recordSourceRefresh(String sourceId) {
        if (StringUtils.isBlank(sourceId)) {
            LOGGER.warn("Cannot record source refresh for null or empty source ID");
            return;
        }

        recordSourceRefreshCount(sourceId);
        recordSourceRefreshTime(sourceId);
    }

    /**
     * Record source refresh count metric.
     */
    private void recordSourceRefreshCount(String sourceId) {
        String metricName = configAccessor.getMetricName(
                REFRESH_SOURCE_COUNT, DEFAULT_SOURCE_REFRESH_COUNT_METRIC);

        try {
            meterRegistry.counter(metricName,
                    Tags.of(COMPONENT, PUBLIC_KEY_CACHE,
                            TYPE, "source-refresh",
                            "sourceId", sourceId))
                    .increment();
            LOGGER.debug("Recorded source refresh count for: {}", sourceId);
        } catch (Exception e) {
            LOGGER.warn("Failed to record source refresh count for {}: {}", sourceId, e);
        }
    }

    /**
     * Record source refresh time metric.
     */
    private void recordSourceRefreshTime(String sourceId) {
        try {
            long currentTimeMillis = System.currentTimeMillis();
            
            // Get or create AtomicLong for this source
            AtomicLong sourceRefreshTime = sourceRefreshTimes.computeIfAbsent(sourceId, 
                k -> {
                    AtomicLong atomicLong = new AtomicLong(0);
                    // Register gauge for this source when first created
                    registerSourceRefreshTimeGauge(k, atomicLong);
                    return atomicLong;
                });
            
            // Update the refresh time
            sourceRefreshTime.set(currentTimeMillis);
            
            LOGGER.debug("Recorded source refresh time for: {}", sourceId);
        } catch (Exception e) {
            LOGGER.warn("Failed to record source refresh time for {}: {}", sourceId, e);
        }
    }

    /**
     * Register gauge for individual source refresh time.
     */
    private void registerSourceRefreshTimeGauge(String sourceId, AtomicLong refreshTime) {
        String refreshTimeMetricName = configAccessor.getMetricName(
                REFRESH_SOURCE_TIME, DEFAULT_SOURCE_REFRESH_TIME_METRIC);

        Gauge.builder(refreshTimeMetricName, refreshTime,
                atomicLong -> atomicLong.get() > 0 ? atomicLong.get() / MILLISECONDS_TO_SECONDS_DIVISOR : 0.0)
                .description("Timestamp of last refresh for public key source (seconds since epoch)")
                .tags(Tags.of(COMPONENT, PUBLIC_KEY_CACHE,
                        TYPE, "source-refresh-time",
                        "sourceId", sourceId))
                .register(meterRegistry);
        
        LOGGER.debug("Registered source refresh time gauge for sourceId: {}", sourceId);
    }

}
