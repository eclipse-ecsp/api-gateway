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

import org.eclipse.ecsp.gateway.annotations.ConditionOnPublicKeyMetricsEnabled;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;

/**
 * Encapsulates metric configuration access logic.
 * Reduces complexity and eliminates repetitive configuration extraction code.
 */
@Component
@ConditionOnPublicKeyMetricsEnabled
public class MetricConfigurationAccessor {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(MetricConfigurationAccessor.class);

    private final GatewayMetricsProperties gatewayMetricsProperties;

    public MetricConfigurationAccessor(GatewayMetricsProperties gatewayMetricsProperties) {
        this.gatewayMetricsProperties = gatewayMetricsProperties;
    }

    /**
     * Gets the metric name from configuration or returns the default.
     */
    public String getMetricName(String metricType, String defaultName) {
        return Optional.ofNullable(gatewayMetricsProperties)
                .map(GatewayMetricsProperties::getPublicKeyCache)
                .map(config -> getConfiguredMetricName(config, metricType))
                .orElseGet(() -> {
                    LOGGER.debug("Using default metric name for {}: {}", metricType, defaultName);
                    return defaultName;
                });
    }

    /**
     * Gets the configured metric name for the specified type.
     */
    private String getConfiguredMetricName(
            GatewayMetricsProperties.PublicKeyCacheMetrics config, String metricType) {

        Function<GatewayMetricsProperties.PublicKeyCacheMetrics, String> nameExtractor =
                getNameExtractor(metricType);

        if (nameExtractor != null) {
            try {
                return nameExtractor.apply(config);
            } catch (Exception e) {
                LOGGER.warn("Error extracting configured name for metric type {}: {}",
                        metricType, e);
            }
        }

        return null;
    }

    /**
     * Get the appropriate name extractor function for the metric type.
     */
    private Function<GatewayMetricsProperties.PublicKeyCacheMetrics, String> getNameExtractor(
            String metricType) {
        return switch (metricType) {
            case GatewayConstants.CACHE_SIZE -> config -> Optional.ofNullable(config.getCacheSize())
                    .map(GatewayMetricsProperties.PublicKeyCacheMetrics.CacheSize::getName)
                    .orElse(null);
            case GatewayConstants.KEY_SOURCES -> config -> Optional.ofNullable(config.getKeySources())
                    .map(GatewayMetricsProperties.PublicKeyCacheMetrics.KeySources::getName)
                    .orElse(null);
            case GatewayConstants.REFRESH_SOURCE_COUNT -> config -> Optional.ofNullable(config.getRefreshSourceCount())
                    .map(GatewayMetricsProperties.PublicKeyCacheMetrics.RefreshSourceCount::getName)
                    .orElse(null);
            case GatewayConstants.REFRESH_SOURCE_TIME -> config -> Optional.ofNullable(config.getRefreshSourceTime())
                    .map(GatewayMetricsProperties.PublicKeyCacheMetrics.RefreshSourceTime::getName)
                    .orElse(null);
            default -> {
                LOGGER.warn("Unknown metric type: {}", metricType);
                yield null;
            }
        };
    }
}
