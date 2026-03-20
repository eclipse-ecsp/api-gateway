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

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.datadog.DatadogProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusProperties;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;
import java.util.List;

/**
 * GatewayMetricsProperty which maps the api gateway metrics configuration.
 *
 * @author Abhishek Kumar
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "api.gateway.metrics")
public class GatewayMetricsProperties {
    private DistributedMetrics serverRequests;
    private DistributedMetrics backendRequests;
    private DistributedMetrics httpClientRequests;
    private DistributedMetrics gatewayRequests;
    private SecurityMetrics securityMetrics;
    private PublicKeyCacheMetrics publicKeyCache;
    private DatadogProperties datadog;
    private PrometheusProperties prometheus;

    /**
     * base metrics configuration.
     */
    @Setter
    @Getter
    public static class BaseMetrics {
        private String prefix;
        private Boolean enabled;
        private List<String> ignoreTags;
    }

    /**
     * security metrics configuration.
     */
    @Setter
    @Getter
    public static class SecurityMetrics extends BaseMetrics {
        private String securityFilterName;
    }

    /**
     * public key cache metrics configuration.
     */
    @Setter
    @Getter
    public static class PublicKeyCacheMetrics extends BaseMetrics {
        private CacheSize cacheSize;
        private KeySources keySources;
        private RefreshSourceCount refreshSourceCount;
        private RefreshSourceTime refreshSourceTime;

        /**
         * cache size metrics configuration.
         */
        @Setter
        @Getter
        public static class CacheSize {
            private String name = "public_key_cache_size";
        }

        /**
         * key sources metrics configuration.
         */
        @Setter
        @Getter
        public static class KeySources {
            private String name = "public_key_sources_count";
        }

        /**
         * refresh source count metrics configuration.
         */
        @Setter
        @Getter
        public static class RefreshSourceCount {
            private String name = "public_key_refresh_source_count";
        }

        /**
         * refresh duration metrics configuration.
         */
        @Setter
        @Getter
        public static class RefreshSourceTime {
            private String name = "public_key_source_refresh_time";
        }
    }

    /**
     * metrics distribution configuration.
     */
    @Getter
    @Setter
    public static class DistributedMetrics extends BaseMetrics {
        private DistributionConfig distribution;
    }

    /**
     * metrics distribution configuration.
     */
    @Getter
    @Setter
    public static class DistributionConfig {
        private boolean enabled;
        private Duration[] buckets = new Duration[]{Duration.ofMillis(1),
                Duration.ofSeconds(5),
                Duration.ofSeconds(10),
                Duration.ofMinutes(1)
        };
        private Duration expiry = Duration.ofMinutes(5); // retain the data for 5 min
        private int bufferLength = 10; // splits the window into 10 parts
    }
}
