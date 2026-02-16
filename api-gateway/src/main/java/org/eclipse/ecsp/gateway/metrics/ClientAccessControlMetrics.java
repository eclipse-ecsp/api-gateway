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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Metrics service for Client Access Control feature.
 *
 * <p>
 * Exposes Prometheus metrics for monitoring and observability:
 * - Request validation counters (checked, allowed, denied)
 * - Cache performance counters (hit, miss, size)
 * - Performance histograms (validation and refresh duration)
 * - YAML override usage counter
 *
 * <p>
 * Metrics prefix: client_access_control
 *
 * @author AI Assistant
 */
@Component
@Slf4j
public class ClientAccessControlMetrics {

    private static final String METRICS_PREFIX = "client_access_control";
    private static final int MAX_TAG_LENGTH = 100;
    private static final String TAG_CLIENT_ID = "client_id";
    private static final String TAG_SERVICE = "service";
    private static final String TAG_ROUTE = "route";
    private static final String TAG_REASON = "reason";

    private final MeterRegistry meterRegistry;

    /**
     * Constructor for ClientAccessControlMetrics.
     *
     * @param meterRegistry Micrometer meter registry
     */
    public ClientAccessControlMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        log.info("ClientAccessControlMetrics initialized with prefix: {}", METRICS_PREFIX);
    }

    /**
     * Record a request that was checked for access control.
     *
     * @param clientId Client identifier
     */
    public void recordRequestChecked(String clientId) {
        Counter.builder(METRICS_PREFIX + ".requests_checked_total")
                .tag(TAG_CLIENT_ID, sanitize(clientId))
                .description("Total number of requests checked for access control")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record a request that was allowed.
     *
     * @param clientId Client identifier
     * @param service Service name
     * @param route Route path
     */
    public void recordRequestAllowed(String clientId, String service, String route) {
        Counter.builder(METRICS_PREFIX + ".requests_allowed_total")
                .tag(TAG_CLIENT_ID, sanitize(clientId))
                .tag(TAG_SERVICE, sanitize(service))
                .tag(TAG_ROUTE, sanitize(route))
                .description("Total number of requests allowed by access control")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record a request that was denied.
     *
     * @param clientId Client identifier
     * @param service Service name
     * @param route Route path
     * @param reason Denial reason (e.g., "NO_CONFIG", "DENIED_BY_RULE", "NO_RULES")
     */
    public void recordRequestDenied(String clientId, String service, String route, String reason) {
        Counter.builder(METRICS_PREFIX + ".requests_denied_total")
                .tag(TAG_CLIENT_ID, sanitize(clientId))
                .tag(TAG_SERVICE, sanitize(service))
                .tag(TAG_ROUTE, sanitize(route))
                .tag(TAG_REASON, sanitize(reason))
                .description("Total number of requests denied by access control")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record a cache hit.
     *
     * @param clientId Client identifier
     */
    public void recordCacheHit(String clientId) {
        Counter.builder(METRICS_PREFIX + ".cache_hit_total")
                .tag(TAG_CLIENT_ID, sanitize(clientId))
                .description("Total number of cache hits")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record a cache miss.
     *
     * @param clientId Client identifier
     */
    public void recordCacheMiss(String clientId) {
        Counter.builder(METRICS_PREFIX + ".cache_miss_total")
                .tag(TAG_CLIENT_ID, sanitize(clientId))
                .description("Total number of cache misses")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Register a gauge for current cache size.
     *
     * @param cacheSizeSupplier Supplier providing current cache size
     */
    public void registerCacheSizeGauge(Supplier<Number> cacheSizeSupplier) {
        Gauge.builder(METRICS_PREFIX + ".cache_size", cacheSizeSupplier)
                .description("Current number of cached client configurations")
                .register(meterRegistry);
        log.info("Registered cache_size gauge");
    }

    /**
     * Record validation duration.
     *
     * @param duration Duration of validation operation
     * @param clientId Client identifier
     */
    public void recordValidationDuration(Duration duration, String clientId) {
        Timer.builder(METRICS_PREFIX + ".validation_duration_seconds")
                .tag(TAG_CLIENT_ID, sanitize(clientId))
                .description("Duration of access control validation in seconds")
                .register(meterRegistry)
                .record(duration);
    }

    /**
     * Record configuration refresh duration.
     *
     * @param duration Duration of refresh operation
     */
    public void recordConfigRefreshDuration(Duration duration) {
        Timer.builder(METRICS_PREFIX + ".config_refresh_duration_seconds")
                .description("Duration of configuration refresh in seconds")
                .register(meterRegistry)
                .record(duration);
    }

    /**
     * Record a YAML override hit.
     *
     * @param clientId Client identifier
     */
    public void recordYamlOverrideHit(String clientId) {
        Counter.builder(METRICS_PREFIX + ".yaml_override_hit_total")
                .tag(TAG_CLIENT_ID, sanitize(clientId))
                .description("Total number of times YAML override was applied")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Sanitize metric tag value to prevent cardinality explosion.
     *
     * @param value Tag value
     * @return Sanitized value or "unknown" if null/empty
     */
    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        // Limit length to prevent cardinality issues
        if (value.length() > MAX_TAG_LENGTH) {
            return value.substring(0, MAX_TAG_LENGTH);
        }
        return value;
    }
}
