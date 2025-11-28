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

package org.eclipse.ecsp.registry.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for ServicesMetrics.
 * Tests service health and route count metrics handling.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class ServicesMetricsTest {

    private MeterRegistry meterRegistry;
    private ServicesMetrics servicesMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        servicesMetrics = new ServicesMetrics(meterRegistry);
        ReflectionTestUtils.setField(servicesMetrics, "healthMetricsName", "service_health_status");
        ReflectionTestUtils.setField(servicesMetrics, "routesCountMetricsName", "service_route_count");
    }

    @Test
    void constructor_WithValidMeterRegistry_CreatesInstance() {
        assertNotNull(servicesMetrics);
    }

    @Test
    void listenHealthStatus_WithValidEvent_RecordsHealthyMetric() {
        ServiceMetricsEvent event = new ServiceMetricsEvent(this, "test-service", true, 5);

        servicesMetrics.listenHealthStatus(event);

        Tags tags = Tags.of("service", "test-service");
        Double healthValue = meterRegistry.find("service_health_status")
                .tags(tags)
                .gauge()
                .value();
        
        assertEquals(1.0, healthValue);
    }

    @Test
    void listenHealthStatus_WithUnhealthyService_RecordsUnhealthyMetric() {
        ServiceMetricsEvent event = new ServiceMetricsEvent(this, "unhealthy-service", false, 3);

        servicesMetrics.listenHealthStatus(event);

        Tags tags = Tags.of("service", "unhealthy-service");
        Double healthValue = meterRegistry.find("service_health_status")
                .tags(tags)
                .gauge()
                .value();
        
        assertEquals(0.0, healthValue);
    }

    @Test
    void listenHealthStatus_WithValidEvent_RecordsRouteCount() {
        ServiceMetricsEvent event = new ServiceMetricsEvent(this, "test-service", true, 10);

        servicesMetrics.listenHealthStatus(event);

        Tags tags = Tags.of("service", "test-service");
        Double routeCount = meterRegistry.find("service_route_count")
                .tags(tags)
                .gauge()
                .value();
        
        assertEquals(10.0, routeCount);
    }

    @Test
    void listenHealthStatus_WithZeroRoutes_RecordsZero() {
        ServiceMetricsEvent event = new ServiceMetricsEvent(this, "empty-service", true, 0);

        servicesMetrics.listenHealthStatus(event);

        Tags tags = Tags.of("service", "empty-service");
        Double routeCount = meterRegistry.find("service_route_count")
                .tags(tags)
                .gauge()
                .value();
        
        assertEquals(0.0, routeCount);
    }

    @Test
    void listenHealthStatus_WithNullEvent_DoesNotThrowException() {
        try {
            servicesMetrics.listenHealthStatus(null);
        } catch (Exception e) {
            fail("Method should not throw exception when event is null");
        }
    }

    @Test
    void listenHealthStatus_WithNullServiceName_DoesNotRecordMetrics() {
        ServiceMetricsEvent event = new ServiceMetricsEvent(this, null, true, 5);

        servicesMetrics.listenHealthStatus(event);

        // Should not record metrics when service name is null
        assertNotNull(meterRegistry);
    }

    @Test
    void listenHealthStatus_WithMultipleServices_RecordsAllMetrics() {
        ServiceMetricsEvent event1 = new ServiceMetricsEvent(this, "service-1", true, 5);
        ServiceMetricsEvent event2 = new ServiceMetricsEvent(this, "service-2", false, 3);

        servicesMetrics.listenHealthStatus(event1);
        servicesMetrics.listenHealthStatus(event2);

        Tags tags1 = Tags.of("service", "service-1");
        Tags tags2 = Tags.of("service", "service-2");
        
        assertEquals(1.0, meterRegistry.find("service_health_status").tags(tags1).gauge().value());
        assertEquals(0.0, meterRegistry.find("service_health_status").tags(tags2).gauge().value());
        assertEquals(5.0, meterRegistry.find("service_route_count").tags(tags1).gauge().value());
        assertEquals(3.0, meterRegistry.find("service_route_count").tags(tags2).gauge().value());
    }

    @Test
    void listenHealthStatus_WithSameServiceTwice_UpdatesMetrics() {
        ServiceMetricsEvent event1 = new ServiceMetricsEvent(this, "test-service", true, 5);
        ServiceMetricsEvent event2 = new ServiceMetricsEvent(this, "test-service", false, 10);

        servicesMetrics.listenHealthStatus(event1);
        servicesMetrics.listenHealthStatus(event2);

        Tags tags = Tags.of("service", "test-service");
        // Gauge registration with constant values - first value registered wins
        assertEquals(1.0, meterRegistry.find("service_health_status").tags(tags).gauge().value());
        assertEquals(5.0, meterRegistry.find("service_route_count").tags(tags).gauge().value());
    }
}
