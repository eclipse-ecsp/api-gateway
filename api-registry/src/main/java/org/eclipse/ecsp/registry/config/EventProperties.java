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

package org.eclipse.ecsp.registry.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for route change event publishing.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "api-registry.events")
public class EventProperties {

    /**
     * Default constructor.
     */
    public EventProperties() {
        // Default constructor
    }

    private boolean enabled = true;
    private RedisConfig redis = new RedisConfig();
    private RetryConfig retry = new RetryConfig();

    /**
     * Redis-specific configuration.
     */
    @Getter
    @Setter
    public static class RedisConfig {
        private String channel = "route-updates";
        private long debounceDelayMs = 5000;
        private int connectionTimeout = 5000;
        private int readTimeout = 3000;
    }

    /**
     * Retry configuration.
     */
    @Getter
    @Setter
    public static class RetryConfig {
        private int maxAttempts = 3;
        private long backoffMs = 1000L;
        private boolean enabled = true;
    }
}
