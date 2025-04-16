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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Type;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.eclipse.ecsp.gateway.metrics.GatewayMetricsProperties.BaseMetrics;
import org.eclipse.ecsp.gateway.metrics.GatewayMetricsProperties.DistributedMetrics;
import org.eclipse.ecsp.gateway.metrics.GatewayMetricsProperties.DistributionConfig;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.util.CollectionUtils;
import java.time.Duration;
import java.util.OptionalDouble;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

/**
 * GatewayMeterFilter is a custom implementation of MeterFilter that filters and configures
 * meters based on the provided metrics configuration.
 *
 * <p>This class is responsible for filtering out meters based on their names and configuring
 * distribution statistics for specific types of meters.
 *
 * @author Abhishek Kumar
 *
 * @param <T> The type of metrics configuration.
 */
public class GatewayMeterFilter<T extends BaseMetrics> implements MeterFilter {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(GatewayMeterFilter.class);
    private final T metricsConfig;
    private final MeterFilter meterFilter;

    /**
     * Constructs a GatewayMeterFilter with the specified metrics configuration.
     *
     * @param metricsConfig The metrics configuration to use for filtering and configuring meters.
     */
    public GatewayMeterFilter(T metricsConfig) {
        this.metricsConfig = metricsConfig;
        if (metricsConfig != null && !CollectionUtils.isEmpty(metricsConfig.getIgnoreTags())) {
            this.meterFilter = MeterFilter.ignoreTags(metricsConfig.getIgnoreTags().toArray(String[]::new));
        } else {
            this.meterFilter = new MeterFilter() {
            };
        }
    }

    /**
     * Constructs a GatewayMeterFilter with the specified metrics configuration and meter filter.
     * The metrics configuration to use for filtering and configuring meters.
     *
     * @param id The meter filter to apply.
     */
    @Override
    public MeterFilterReply accept(Meter.Id id) {
        if (metricsConfig != null
                && id.getName().startsWith(metricsConfig.getPrefix())
                && Boolean.FALSE.equals(metricsConfig.getEnabled())) {
            LOGGER.info("GatewayMeterFilter#accept for meterId: {}, is disabled", id.getName());
            return MeterFilterReply.DENY;
        }
        return MeterFilterReply.NEUTRAL;
    }

    /**
     * Maps the specified meter ID to a new meter ID based on the provided meter filter.
     *
     * @param id The meter ID to map.
     * @return The mapped meter ID.
     */
    @Override
    public Meter.Id map(Meter.Id id) {
        return this.meterFilter.map(id);
    }

    /**
     * Configures the distribution statistics for the specified meter ID and distribution
     * statistic configuration.
     *
     * @param id     The meter ID to configure.
     * @param config The distribution statistic configuration to apply.
     * @return The configured distribution statistic configuration.
     */
    @Override
    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
        if (metricsConfig != null
                && metricsConfig.getEnabled()
                && id.getName().startsWith(metricsConfig.getPrefix())
                && metricsConfig instanceof DistributedMetrics dcMetricsConfig
                && dcMetricsConfig.getDistribution().isEnabled()
                && (id.getType().equals(Type.TIMER)
                || id.getType().equals(Type.DISTRIBUTION_SUMMARY))) {
            DistributionConfig dc = dcMetricsConfig.getDistribution();
            return DistributionStatisticConfig.builder()
                    // maximum value to be recorded
                    .maximumExpectedValue(getMaximumValue(dc.getBuckets()))
                    // minimum value to be recorded
                    .minimumExpectedValue(getMinimumValue(dc.getBuckets()))
                    // The SLO boundaries to include the set of histogram buckets shipped to the monitoring system.
                    .serviceLevelObjectives(dc.getBuckets() != null
                            && dc.getBuckets().length > 0
                            ? convertServiceLevelObjectives(dc.getBuckets())
                            : null)
                    //The amount of time samples are accumulated to
                    // decaying distribution statistics before they are reset and rotated
                    .expiry(dc.getExpiry() != null ? dc.getExpiry() : null)
                    // The number of histograms to keep in the ring buffer
                    .bufferLength(dc.getBufferLength() > 0 ? dc.getBufferLength() : null)
                    .build().merge(config);
        }
        return config;
    }

    /**
     * Converts an array of Duration objects to an array of double values representing the
     * service level objectives.
     *
     * @param sloDurations The array of Duration objects to convert.
     * @return An array of double values representing the service level objectives.
     */
    public double[] convertServiceLevelObjectives(Duration[] sloDurations) {
        return Stream.of(sloDurations)
                .map(Duration::toNanos)
                .flatMapToDouble(DoubleStream::of)
                .toArray();
    }

    /**
     * Converts an array of Duration objects to an array of double values representing the
     * service level objectives and get the min value.
     *
     * @param durations The array of Duration objects to convert.
     * @return An array of double values representing the service level objectives.
     */
    public double getMinimumValue(Duration[] durations) {
        OptionalDouble minValue = Stream.of(durations)
                .map(Duration::toNanos)
                .flatMapToDouble(DoubleStream::of)
                .min();
        if (minValue.isPresent()) {
            return minValue.getAsDouble();
        } else {
            return Duration.ofMillis(1).toMillis();
        }
    }

    /**
     * Converts an array of Duration objects to an array of double values representing the
     * service level objectives and get the max value.
     *
     * @param durations The array of Duration objects to convert.
     * @return An array of double values representing the service level objectives.
     */
    public double getMaximumValue(Duration[] durations) {
        OptionalDouble maxValue = Stream.of(durations)
                .map(Duration::toNanos)
                .flatMapToDouble(DoubleStream::of)
                .max();
        if (maxValue.isPresent()) {
            return maxValue.getAsDouble();
        } else {
            return Duration.ofMinutes(1).toMillis();
        }
    }
}
