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
import org.eclipse.ecsp.gateway.annotations.ConditionOnPublicKeyMetricsEnabled;
import org.eclipse.ecsp.gateway.cache.PublicKeyCache;
import org.eclipse.ecsp.gateway.plugins.keysources.PublicKeySourceProvider;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.CACHE_SIZE;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.COMPONENT;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.DEFAULT_CACHE_SIZE_METRIC;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.DEFAULT_KEY_SOURCES_METRIC;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.KEY_SOURCES;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.PUBLIC_KEY_CACHE;
import static org.eclipse.ecsp.gateway.utils.GatewayConstants.TYPE;

/**
 * Handles registration of cache-related metrics.
 * Responsible for registering gauges for cache size and key sources count.
 */
@Component
@ConditionOnPublicKeyMetricsEnabled
public class PublicKeyCacheMetricsRegistrar {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PublicKeyCacheMetricsRegistrar.class);
    private static final int MINUS_ONE = -1;

    private final MeterRegistry meterRegistry;
    private final PublicKeyCache publicKeyCache;
    private final List<PublicKeySourceProvider> sourceProviders;
    private final MetricConfigurationAccessor configAccessor;

    // Cached key sources count to avoid expensive recalculation
    private final AtomicInteger cachedKeySourcesCount = new AtomicInteger(MINUS_ONE);

    /**
     * Constructor for PublicKeyCacheMetricsRegistrar.
     *
     * @param meterRegistry meter registry for registering metrics
     * @param publicKeyCache public key cache
     * @param sourceProviders list of public key source providers
     * @param gatewayMetricsProperties gateway metrics properties
     */
    public PublicKeyCacheMetricsRegistrar(MeterRegistry meterRegistry,
                                         PublicKeyCache publicKeyCache,
                                         List<PublicKeySourceProvider> sourceProviders,
                                         GatewayMetricsProperties gatewayMetricsProperties) {
        this.meterRegistry = meterRegistry;
        this.publicKeyCache = publicKeyCache;
        this.sourceProviders = sourceProviders;
        this.configAccessor = new MetricConfigurationAccessor(gatewayMetricsProperties);
    }

    /**
     * Register cache-related metrics.
     */
    public void registerCacheMetrics() {
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
        Gauge.builder(keySourcesMetricsName, this, PublicKeyCacheMetricsRegistrar::getKeySourcesCount)
                .description("Number of configured public key sources")
                .tags(createBaseTags(KEY_SOURCES))
                .register(meterRegistry);

        LOGGER.debug("Registered cache metrics: {}, {}", cacheMetricsName, keySourcesMetricsName);
    }

    /**
     * Gets the total count of configured public key sources.
     */
    private int getKeySourcesCount() {
        int cached = cachedKeySourcesCount.get();
        if (cached >= 0) {
            return cached;
        }

        int totalSources = calculateKeySourcesCount();
        cachedKeySourcesCount.set(totalSources);
        return totalSources;
    }

    /**
     * Calculate the actual count of key sources from all providers.
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
     */
    private int getSafeKeySourceCount(PublicKeySourceProvider provider) {
        if (provider == null) {
            LOGGER.warn("Null provider encountered when calculating key sources count");
            return 0;
        }

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
     */
    public void invalidateKeySourcesCache() {
        cachedKeySourcesCount.set(MINUS_ONE);
        LOGGER.debug("Invalidated key sources count cache");
    }

    /**
     * Create base tags for metrics.
     */
    private Tags createBaseTags(String type) {
        return Tags.of(
                COMPONENT, PUBLIC_KEY_CACHE,
                TYPE, type
        );
    }
}
