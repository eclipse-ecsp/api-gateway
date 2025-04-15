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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * {@link ServicesMetrics} exposes service health metrics for registered services.
 *
 * @author Abhishek Kumar
 */
@Component
@ConditionalOnProperty(name = "api-registry.metrics.enabled", matchIfMissing = true, havingValue = "true")
public class ServicesMetrics {

    @Value("${api-registry.metrics.service-health.prefix:service_health_status}")
    private String healthMetricsName;

    @Value("${api-registry.metrics.service-routes.prefix:service_route_count}")
    private String routesCountMetricsName;

    private final MeterRegistry meterRegistry;

    /**
     * ServicesMetrics constructor.
     *
     * @param meterRegistry meter registry.
     */
    public ServicesMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * handles {@link ServiceMetricsEvent} and exposes service health and count metrics.
     *
     * @param event health event.
     */
    @EventListener(ServiceMetricsEvent.class)
    public void listenHealthStatus(ServiceMetricsEvent event) {
        if (event != null && event.getServiceName() != null) {
            Tags tags = Tags.of("service", event.getServiceName());
            meterRegistry.gauge(healthMetricsName, tags, event.isStatus() ? 1 : 0);
            meterRegistry.gauge(routesCountMetricsName, tags, event.getTotalRoutes());
        }
    }
}
