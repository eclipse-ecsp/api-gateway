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
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ClientAccessControlMetrics.
 *
 * <p>Tests all metric recording methods:
 * - Request counters (checked, allowed, denied)
 * - Cache counters (hit, miss)
 * - Cache size gauge
 * - Duration timers (validation, refresh)
 * - YAML override counter
 */
class ClientAccessControlMetricsTest {

    private static final String CLIENT_ID = "test_client";
    private static final String SERVICE = "user-service";
    private static final String ROUTE = "/users";
    private static final String REASON = "DENIED_BY_RULE";
    private static final int CACHE_SIZE = 42;
    private static final int NEW_CACHE_SIZE = 100;
    private static final long ONE_HUNDRED_MS = 100;
    private static final long FIVE_HUNDRED_MS = 500;
    private static final int LONG_CLIENT_ID_LENGTH = 150;
    private static final int MAX_TAG_LENGTH = 100;
    private static final double EXPECTED_COUNTER_AFTER_TWO_INCREMENTS = 2.0;
    private static final double EXPECTED_ONE_HUNDRED_MS = 0.1;
    private static final double EXPECTED_FIVE_HUNDRED_MS = 0.5;
    private static final double TOLERANCE = 0.01;

    private MeterRegistry meterRegistry;
    private ClientAccessControlMetrics metrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metrics = new ClientAccessControlMetrics(meterRegistry);
    }

    @Test
    void testRecordRequestChecked() {
        // When: Record request checked
        metrics.recordRequestChecked(CLIENT_ID);

        // Then: Counter should increment
        Counter counter = meterRegistry.find("client_access_control.requests_checked_total")
                .tag("client_id", CLIENT_ID)
                .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);

        // When: Record again
        metrics.recordRequestChecked(CLIENT_ID);

        // Then: Counter should increment
        assertThat(counter.count()).isEqualTo(EXPECTED_COUNTER_AFTER_TWO_INCREMENTS);
    }

    @Test
    void testRecordRequestAllowed() {
        // When: Record request allowed
        metrics.recordRequestAllowed(CLIENT_ID, SERVICE, ROUTE);

        // Then: Counter should exist with all tags
        Counter counter = meterRegistry.find("client_access_control.requests_allowed_total")
                .tag("client_id", CLIENT_ID)
                .tag("service", SERVICE)
                .tag("route", ROUTE)
                .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void testRecordRequestDenied() {
        // When: Record request denied
        metrics.recordRequestDenied(CLIENT_ID, SERVICE, ROUTE, REASON);

        // Then: Counter should exist with all tags including reason
        Counter counter = meterRegistry.find("client_access_control.requests_denied_total")
                .tag("client_id", CLIENT_ID)
                .tag("service", SERVICE)
                .tag("route", ROUTE)
                .tag("reason", REASON)
                .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void testRecordCacheHit() {
        // When: Record cache hit
        metrics.recordCacheHit(CLIENT_ID);

        // Then: Counter should increment
        Counter counter = meterRegistry.find("client_access_control.cache_hit_total")
                .tag("client_id", CLIENT_ID)
                .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void testRecordCacheMiss() {
        // When: Record cache miss
        metrics.recordCacheMiss(CLIENT_ID);

        // Then: Counter should increment
        Counter counter = meterRegistry.find("client_access_control.cache_miss_total")
                .tag("client_id", CLIENT_ID)
                .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void testRegisterCacheSizeGauge() {
        // Given: Cache size supplier
        AtomicInteger cacheSize = new AtomicInteger(CACHE_SIZE);

        // When: Register gauge
        metrics.registerCacheSizeGauge(cacheSize::get);

        // Then: Gauge should reflect current value
        Gauge gauge = meterRegistry.find("client_access_control.cache_size")
                .gauge();
        
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(CACHE_SIZE);

        // When: Cache size changes
        cacheSize.set(NEW_CACHE_SIZE);

        // Then: Gauge should reflect new value
        assertThat(gauge.value()).isEqualTo(NEW_CACHE_SIZE);
    }

    @Test
    void testRecordValidationDuration() {
        // Given: 100ms duration
        Duration duration = Duration.ofMillis(ONE_HUNDRED_MS);

        // When: Record validation duration
        metrics.recordValidationDuration(duration, CLIENT_ID);

        // Then: Timer should record the duration
        Timer timer = meterRegistry.find("client_access_control.validation_duration_seconds")
                .tag("client_id", CLIENT_ID)
                .timer();
        
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.SECONDS))
                .isCloseTo(EXPECTED_ONE_HUNDRED_MS, org.assertj.core.data.Offset.offset(TOLERANCE));
    }

    @Test
    void testRecordConfigRefreshDuration() {
        // Given: 500ms duration
        Duration duration = Duration.ofMillis(FIVE_HUNDRED_MS);

        // When: Record refresh duration
        metrics.recordConfigRefreshDuration(duration);

        // Then: Timer should record the duration
        Timer timer = meterRegistry.find("client_access_control.config_refresh_duration_seconds")
                .timer();
        
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.SECONDS))
                .isCloseTo(EXPECTED_FIVE_HUNDRED_MS, org.assertj.core.data.Offset.offset(TOLERANCE));
    }

    @Test
    void testRecordYamlOverrideHit() {
        // When: Record YAML override hit
        metrics.recordYamlOverrideHit(CLIENT_ID);

        // Then: Counter should increment
        Counter counter = meterRegistry.find("client_access_control.yaml_override_hit_total")
                .tag("client_id", CLIENT_ID)
                .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void testSanitizationNullValue() {
        // When: Record with null client ID
        metrics.recordRequestChecked(null);

        // Then: Should use "unknown" as tag value
        Counter counter = meterRegistry.find("client_access_control.requests_checked_total")
                .tag("client_id", "unknown")
                .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void testSanitizationEmptyValue() {
        // When: Record with empty client ID
        metrics.recordRequestChecked("");

        // Then: Should use "unknown" as tag value
        Counter counter = meterRegistry.find("client_access_control.requests_checked_total")
                .tag("client_id", "unknown")
                .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void testSanitizationLongValue() {
        // Given: Very long client ID (>100 chars)
        String longClientId = "a".repeat(LONG_CLIENT_ID_LENGTH);

        // When: Record with long client ID
        metrics.recordRequestChecked(longClientId);

        // Then: Should truncate to 100 chars
        Counter counter = meterRegistry.find("client_access_control.requests_checked_total")
                .tag("client_id", longClientId.substring(0, MAX_TAG_LENGTH))
                .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
