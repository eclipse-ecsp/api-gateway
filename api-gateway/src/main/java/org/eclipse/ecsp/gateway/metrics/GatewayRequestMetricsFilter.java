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

import io.micrometer.core.instrument.MeterRegistry;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayMetricsFilter;
import org.springframework.cloud.gateway.support.tagsprovider.GatewayTagsProvider;

import java.util.List;

/**
 * Custom Gateway Metrics Filter that extends Spring's GatewayMetricsFilter
 * with a modified execution order to ensure all requests are captured,
 * including authentication failures.
 *
 * <p>This filter runs at order -100 to execute very early in the filter chain,
 * ensuring that metrics are recorded for all requests regardless of authentication
 * or other filter outcomes.
 *
 * @author abhishek kumar
 */
public class GatewayRequestMetricsFilter extends GatewayMetricsFilter {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(GatewayRequestMetricsFilter.class);

    /**
     * Constructor that delegates to parent GatewayMetricsFilter.
     *
     * @param meterRegistry   the meter registry for recording metrics
     * @param tagsProviders   list of tag providers for metric tags
     * @param metricsPrefix   prefix for metric names
     */
    public GatewayRequestMetricsFilter(MeterRegistry meterRegistry,
                                       List<GatewayTagsProvider> tagsProviders,
                                       String metricsPrefix) {
        super(meterRegistry, tagsProviders, metricsPrefix);
        LOGGER.info("GatewayRequestMetricsFilter initialized with order: {} and prefix: {}",
                GatewayConstants.CUSTOM_METRICS_FILTER_ORDER, metricsPrefix);
    }

    /**
     * Override the order to run very early in the filter chain.
     *
     * <p>Order -100 ensures this filter runs:
     * - After AccessLogFilter (-1000)
     * - Before CacheFilter (-10)
     * - Before JwtAuthFilter (10)
     * - Much earlier than default GatewayMetricsFilter (0)
     *
     * <p>This guarantees that all requests are captured for metrics,
     * including those that fail authentication.
     *
     * @return the custom filter order (-100)
     */
    @Override
    public int getOrder() {
        return GatewayConstants.CUSTOM_METRICS_FILTER_ORDER;
    }
}
