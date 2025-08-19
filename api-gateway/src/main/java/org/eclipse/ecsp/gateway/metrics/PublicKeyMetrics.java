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
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.cache.PublicKeyCache;
import org.eclipse.ecsp.gateway.events.PublicKeyRefreshEvent;
import org.eclipse.ecsp.gateway.events.PublicKeyRefreshEvent.RefreshType;
import org.eclipse.ecsp.gateway.plugins.keysources.PublicKeySourceProvider;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.CACHE_SIZE;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.COMPONENT;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.DEFAULT_CACHE_SIZE_METRIC;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.DEFAULT_KEY_SOURCES_METRIC;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.DEFAULT_LAST_REFRESH_TIME_METRIC;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.DEFAULT_REFRESH_COUNT_METRIC;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.DEFAULT_SOURCE_REFRESH_COUNT_METRIC;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.DEFAULT_SOURCE_REFRESH_TIME_METRIC;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.KEY_SOURCES;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.PUBLIC_KEY_CACHE;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.REFRESH_COUNT;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.REFRESH_SOURCE_COUNT;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.REFRESH_SOURCE_TIME;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.REFRESH_TIME;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.TYPE;

/**
 * {@link PublicKeyMetrics} exposes public key cache metrics for monitoring.
 * Provides real-time metrics for cache size and configured key sources count.
 *
 * <p>This component automatically registers metrics for:
 * <ul>
 *   <li>Cache size - current number of cached public keys</li>
 *   <li>Key sources count - number of configured key source providers</li>
 *   <li>Refresh events - tracking of full and source-specific refreshes</li>
 * </ul>
 *
 * @author Abhishek Kumar
 */
@Component
@ConditionalOnProperty(name = "api.gateway.metrics.public-key.enabled",
        havingValue = "true", matchIfMissing = true)
public class PublicKeyMetrics {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PublicKeyMetrics.class);
    private static final double MILLISECONDS_TO_SECONDS_DIVISOR = 1000.0;
    public static final int MINUS_ONE = -1;

    private final MeterRegistry meterRegistry;
    private final PublicKeyCache publicKeyCache;
    private final List<PublicKeySourceProvider> sourceProviders;
    private final MetricConfigurationAccessor configAccessor;

    // Metrics for tracking refresh events
    private final AtomicLong lastFullRefreshTime = new AtomicLong(0);
    private final AtomicLong fullRefreshCount = new AtomicLong(0);

    // Cached key sources count to avoid expensive recalculation
    private final AtomicInteger cachedKeySourcesCount = new AtomicInteger(MINUS_ONE);

    /**
     * PublicKeyMetrics constructor.
     *
     * @param meterRegistry meter registry for exposing metrics
     * @param publicKeyCache public key cache instance
     * @param sourceProviders list of public key source providers
     * @param gatewayMetricsProperties gateway metrics configuration properties
     */
    public PublicKeyMetrics(MeterRegistry meterRegistry,
                            PublicKeyCache publicKeyCache,
                            List<PublicKeySourceProvider> sourceProviders,
                            GatewayMetricsProperties gatewayMetricsProperties) {
        this.meterRegistry = meterRegistry;
        this.publicKeyCache = publicKeyCache;
        this.sourceProviders = sourceProviders;
        this.configAccessor = new MetricConfigurationAccessor(gatewayMetricsProperties);
    }

    /**
     * Initialize the metrics gauges after bean construction.
     * Sets up real-time monitoring of cache size and key sources count.
     */
    @PostConstruct
    public void initializeMetrics() {
        LOGGER.info("Initializing public key cache metrics");

        try {
            registerCacheMetrics();
            registerRefreshMetrics();
            LOGGER.info("Public key cache metrics initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize public key cache metrics", e);
            throw new IllegalStateException("Could not initialize metrics", e);
        }
    }

    /**
     * Register cache-related metrics (size and key sources count).
     */
    private void registerCacheMetrics() {
        final String cacheMetricsName = configAccessor.getMetricName(
                CACHE_SIZE, DEFAULT_CACHE_SIZE_METRIC);
        final String keySourcesMetricsName = configAccessor.getMetricName(
                KEY_SOURCES, DEFAULT_KEY_SOURCES_METRIC);

        // Register gauge for cache size
        Gauge.builder(cacheMetricsName, publicKeyCache, PublicKeyCache::size)
                .description("Number of public keys currently cached")
                .tags(createBaseTags(CACHE_SIZE))
                .register(meterRegistry);

        // Register gauge for key sources count
        Gauge.builder(keySourcesMetricsName, this, PublicKeyMetrics::getKeySourcesCount)
                .description("Number of configured public key sources")
                .tags(createBaseTags(KEY_SOURCES))
                .register(meterRegistry);

        LOGGER.debug("Registered cache metrics: {}, {}", cacheMetricsName, keySourcesMetricsName);
    }

    /**
     * Register refresh-related metrics (count and timestamp).
     */
    private void registerRefreshMetrics() {
        final String refreshCountName = configAccessor.getMetricName(
                REFRESH_COUNT, DEFAULT_REFRESH_COUNT_METRIC);
        final String lastRefreshTimeName = configAccessor.getMetricName(
                REFRESH_TIME, DEFAULT_LAST_REFRESH_TIME_METRIC);

        // Register gauge for last full refresh time (timestamp in seconds since epoch)
        Gauge.builder(lastRefreshTimeName, lastFullRefreshTime, 
                atomicLong -> atomicLong.get() / MILLISECONDS_TO_SECONDS_DIVISOR)
                .description("Timestamp of last full public key refresh (seconds since epoch)")
                .tags(createBaseTags(REFRESH_TIME))
                .register(meterRegistry);

        // Register gauge for full refresh count
        Gauge.builder(refreshCountName, fullRefreshCount, AtomicLong::get)
                .description("Total number of full public key cache refreshes")
                .tags(createBaseTags(GatewayConstants.REFRESH_COUNT))
                .register(meterRegistry);

        LOGGER.debug("Registered refresh metrics: {}, {}", refreshCountName, lastRefreshTimeName);
    }

    /**
     * Create base tags for metrics.
     *
     * @param type the metric type
     * @return configured tags
     */
    private Tags createBaseTags(String type) {
        return Tags.of(
                COMPONENT, PUBLIC_KEY_CACHE,
                TYPE, type
        );
    }

    /**
     * Gets the total count of configured public key sources across all providers.
     * Uses caching to avoid expensive recalculation on every metric collection.
     *
     * @return total number of key sources
     */
    private int getKeySourcesCount() {
        // Return cached value if available and valid
        int cached = cachedKeySourcesCount.get();
        if (cached >= 0) {
            return cached;
        }

        // Calculate and cache the result
        int totalSources = calculateKeySourcesCount();
        cachedKeySourcesCount.set(totalSources);
        return totalSources;
    }

    /**
     * Calculate the actual count of key sources from all providers.
     *
     * @return calculated count of key sources
     */
    private int calculateKeySourcesCount() {
        try {
            int totalSources = sourceProviders.stream()
                    .mapToInt(this::getSafeKeySourceCount)
                    .sum();

            LOGGER.debug("Calculated total configured public key sources: {}", totalSources);
            return totalSources;
        } catch (Exception e) {
            LOGGER.error("Error calculating key sources count", e);
            return 0;
        }
    }

    /**
     * Safely get key source count from a provider.
     *
     * @param provider the provider to query
     * @return count of key sources or 0 if error occurs
     */
    private int getSafeKeySourceCount(PublicKeySourceProvider provider) {
        try {
            List<?> sources = provider.keySources();
            return sources != null ? sources.size() : 0;
        } catch (Exception e) {
            LOGGER.warn("Error getting key sources from provider {}: {}",
                    provider.getClass().getSimpleName(), e);
            return 0;
        }
    }

    /**
     * Invalidate the cached key sources count.
     * Call this method when key sources configuration changes.
     */
    public void invalidateKeySourcesCache() {
        cachedKeySourcesCount.set(MINUS_ONE);
        LOGGER.debug("Invalidated key sources count cache");
    }

    /**
     * Record a full public key cache refresh event.
     * This should be called when the entire cache is refreshed from all sources.
     */
    public void recordFullRefresh() {
        fullRefreshCount.incrementAndGet();
        lastFullRefreshTime.set(System.currentTimeMillis());

        // Invalidate cache since sources might have changed
        invalidateKeySourcesCache();

        LOGGER.debug("Recorded full public key cache refresh. Total count: {}", fullRefreshCount.get());
    }

    /**
     * Record an individual public key source refresh event.
     * This should be called when a specific key source (e.g., JWKS endpoint) is refreshed.
     *
     * @param sourceId identifier of the refreshed source
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
     *
     * @param sourceId the source identifier
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
     *
     * @param sourceId the source identifier
     */
    private void recordSourceRefreshTime(String sourceId) {
        String refreshTimeMetricName = configAccessor.getMetricName(
                REFRESH_SOURCE_TIME, DEFAULT_SOURCE_REFRESH_TIME_METRIC);

        try {
            double syncTime = System.currentTimeMillis() / MILLISECONDS_TO_SECONDS_DIVISOR;
            meterRegistry.gauge(refreshTimeMetricName,
                    Tags.of(COMPONENT, PUBLIC_KEY_CACHE,
                            TYPE, "source-refresh-time",
                            "sourceId", sourceId),
                    syncTime);
            LOGGER.debug("Recorded source refresh time for: {}", sourceId);
        } catch (Exception e) {
            LOGGER.warn("Failed to record source refresh time for {}: {}", sourceId, e);
        }
    }

    /**
     * Handle PublicKeyRefreshEvent to update metrics.
     * This method listens for public key refresh events and updates the metrics accordingly.
     *
     * @param publicKeyRefreshEvent the event containing refresh details
     */
    @EventListener(PublicKeyRefreshEvent.class)
    public void handleMetricsEvents(PublicKeyRefreshEvent publicKeyRefreshEvent) {
        if (publicKeyRefreshEvent == null) {
            LOGGER.warn("Received null PublicKeyRefreshEvent, ignoring");
            return;
        }

        LOGGER.info("Handling PublicKeyRefreshEvent: {}", publicKeyRefreshEvent);

        try {
            handleRefreshEventByType(publicKeyRefreshEvent);
        } catch (Exception e) {
            LOGGER.error("Error handling PublicKeyRefreshEvent: {}", publicKeyRefreshEvent, e);
        }
    }

    /**
     * Handle refresh event based on its type.
     *
     * @param event the refresh event to handle
     */
    private void handleRefreshEventByType(PublicKeyRefreshEvent event) {
        RefreshType refreshType = event.getRefreshType();

        if (RefreshType.ALL_KEYS.equals(refreshType)) {
            recordFullRefresh();
        } else if (RefreshType.PUBLIC_KEY.equals(refreshType)) {
            String sourceId = event.getSourceId();
            if (sourceId != null) {
                recordSourceRefresh(sourceId);
            } else {
                LOGGER.warn("Received source refresh event without source ID, ignoring");
            }
        } else {
            LOGGER.debug("Unhandled refresh type: {}", refreshType);
        }
    }

    /**
     * Helper class to encapsulate metric configuration access logic.
     * Reduces complexity and eliminates repetitive configuration extraction code.
     */
    private static class MetricConfigurationAccessor {
        private final GatewayMetricsProperties gatewayMetricsProperties;

        /**
         * Constructor for MetricConfigurationAccessor.
         *
         * @param gatewayMetricsProperties the gateway metrics properties
         */
        MetricConfigurationAccessor(GatewayMetricsProperties gatewayMetricsProperties) {
            this.gatewayMetricsProperties = gatewayMetricsProperties;
        }

        /**
         * Gets the metric name from configuration or returns the default.
         *
         * @param metricType the type of metric
         * @param defaultName the default metric name
         * @return the configured or default metric name
         */
        String getMetricName(String metricType, String defaultName) {
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
         *
         * @param config the public key cache metrics configuration
         * @param metricType the type of metric
         * @return the configured metric name or null if not configured
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
         *
         * @param metricType the metric type
         * @return function to extract the configured name, or null if unknown type
         */
        private Function<GatewayMetricsProperties.PublicKeyCacheMetrics, String> getNameExtractor(
                String metricType) {
            return switch (metricType) {
                case CACHE_SIZE -> config -> Optional.ofNullable(config.getCacheSize())
                        .map(GatewayMetricsProperties.PublicKeyCacheMetrics.CacheSize::getName)
                        .orElse(null);
                case KEY_SOURCES -> config -> Optional.ofNullable(config.getKeySources())
                        .map(GatewayMetricsProperties.PublicKeyCacheMetrics.KeySources::getName)
                        .orElse(null);
                case REFRESH_COUNT -> config -> Optional.ofNullable(config.getRefreshCount())
                        .map(GatewayMetricsProperties.PublicKeyCacheMetrics.RefreshCount::getName)
                        .orElse(null);
                case REFRESH_TIME -> config -> Optional.ofNullable(config.getRefreshTime())
                        .map(GatewayMetricsProperties.PublicKeyCacheMetrics.RefreshTime::getName)
                        .orElse(null);
                case REFRESH_SOURCE_COUNT -> config -> Optional.ofNullable(config.getRefreshSourceCount())
                        .map(GatewayMetricsProperties.PublicKeyCacheMetrics.RefreshSourceCount::getName)
                        .orElse(null);
                case REFRESH_SOURCE_TIME -> config -> Optional.ofNullable(config.getRefreshSourceTime())
                        .map(GatewayMetricsProperties.PublicKeyCacheMetrics.RefreshSourceTime::getName)
                        .orElse(null);
                default -> {
                    LOGGER.warn("Unknown metric type: {}", metricType);
                    yield null;
                }
            };
        }
    }
}
