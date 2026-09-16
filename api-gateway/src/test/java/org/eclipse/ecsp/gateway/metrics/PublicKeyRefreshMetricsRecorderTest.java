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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicKeyRefreshMetricsRecorderTest {

    private MeterRegistry meterRegistry;
    private GatewayMetricsProperties gatewayMetricsProperties;
    private PublicKeyRefreshMetricsRecorder recorder;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        gatewayMetricsProperties = new GatewayMetricsProperties();
        recorder = new PublicKeyRefreshMetricsRecorder(meterRegistry, gatewayMetricsProperties);
    }

    @Test
    @DisplayName("Should initialize refresh metrics without error")
    void testRegisterRefreshMetrics() {
        assertDoesNotThrow(() -> recorder.registerRefreshMetrics());
    }

    @Test
    @DisplayName("Should record source refresh metrics for valid source ID")
    void testRecordSourceRefresh() {
        recorder.recordSourceRefresh("source-1");
        Counter counter = meterRegistry.find("public_key_source_refresh_count")
                .tag("sourceId", "source-1")
                .counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    @DisplayName("Should ignore source refresh when source ID is blank or null")
    void testRecordSourceRefreshBlankOrNull() {
        recorder.recordSourceRefresh(null);
        recorder.recordSourceRefresh("");
        recorder.recordSourceRefresh("   ");
        assertNull(meterRegistry.find("public_key_source_refresh_count").counter());
    }

    @Test
    @DisplayName("Should record unknown kid metric")
    void testRecordUnknownKid() {
        recorder.recordUnknownKid("source-jwks");
        Counter counter = meterRegistry.find("api_gateway_jwks_unknown_kid_total")
                .tag("sourceId", "source-jwks")
                .counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    @DisplayName("Should ignore unknown kid when source ID is blank")
    void testRecordUnknownKidBlank() {
        recorder.recordUnknownKid(null);
        recorder.recordUnknownKid("");
        assertNull(meterRegistry.find("api_gateway_jwks_unknown_kid_total").counter());
    }

    @Test
    @DisplayName("Should record forced refresh metric with outcome")
    void testRecordForcedRefreshWithOutcome() {
        recorder.recordForcedRefresh("source-jwks", "success");
        recorder.recordForcedRefresh("source-jwks", "failure");
        recorder.recordForcedRefresh("source-jwks", "suppressed");

        Counter successCounter = meterRegistry.find("api_gateway_jwks_forced_refresh_total")
                .tag("sourceId", "source-jwks")
                .tag("outcome", "success")
                .counter();
        assertNotNull(successCounter);
        assertEquals(1.0, successCounter.count());

        Counter failureCounter = meterRegistry.find("api_gateway_jwks_forced_refresh_total")
                .tag("sourceId", "source-jwks")
                .tag("outcome", "failure")
                .counter();
        assertNotNull(failureCounter);
        assertEquals(1.0, failureCounter.count());

        Counter suppressedCounter = meterRegistry.find("api_gateway_jwks_forced_refresh_total")
                .tag("sourceId", "source-jwks")
                .tag("outcome", "suppressed")
                .counter();
        assertNotNull(suppressedCounter);
        assertEquals(1.0, suppressedCounter.count());
    }

    @Test
    @DisplayName("Should record forced refresh metric without outcome tag when outcome is null")
    void testRecordForcedRefreshWithoutOutcome() {
        recorder.recordForcedRefresh("source-jwks", null);

        Counter counter = meterRegistry.find("api_gateway_jwks_forced_refresh_total")
                .tag("sourceId", "source-jwks")
                .counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    @DisplayName("Should ignore forced refresh when source ID is blank")
    void testRecordForcedRefreshBlank() {
        recorder.recordForcedRefresh(null, "success");
        recorder.recordForcedRefresh("  ", "success");
        assertNull(meterRegistry.find("api_gateway_jwks_forced_refresh_total").counter());
    }

    @Test
    @DisplayName("Should record kid recovered metric")
    void testRecordKidRecovered() {
        recorder.recordKidRecovered("source-jwks");
        Counter counter = meterRegistry.find("api_gateway_jwks_kid_recovered_total")
                .tag("sourceId", "source-jwks")
                .counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    @DisplayName("Should ignore kid recovered when source ID is blank")
    void testRecordKidRecoveredBlank() {
        recorder.recordKidRecovered(null);
        recorder.recordKidRecovered(" ");
        assertNull(meterRegistry.find("api_gateway_jwks_kid_recovered_total").counter());
    }

    @Test
    @DisplayName("Should handle exception from meter registry gracefully")
    void testRecordCounterExceptionHandling() {
        MeterRegistry faultyRegistry = mock(MeterRegistry.class);
        when(faultyRegistry.counter(anyString(), any(Tags.class)))
                .thenThrow(new RuntimeException("Registry error"));

        PublicKeyRefreshMetricsRecorder faultyRecorder =
                new PublicKeyRefreshMetricsRecorder(faultyRegistry, gatewayMetricsProperties);

        assertDoesNotThrow(() -> faultyRecorder.recordUnknownKid("source-1"));
        assertDoesNotThrow(() -> faultyRecorder.recordForcedRefresh("source-1", "success"));
        assertDoesNotThrow(() -> faultyRecorder.recordKidRecovered("source-1"));
    }
}
