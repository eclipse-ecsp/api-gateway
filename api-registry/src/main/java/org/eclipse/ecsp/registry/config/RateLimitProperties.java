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
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for rate limiting settings.
 */
@ConfigurationProperties(prefix = "api-registry.rate-limit")
@Configuration
@Getter
@Setter
public class RateLimitProperties {
    /**
     * Default constructor.
     */
    public RateLimitProperties() {
        // Default constructor
    }

    /**
     * Maximum replenish rate for rate limiting.
     */
    private int maxReplenishRate = 10000;
    
    /**
     * Maximum burst capacity for rate limiting.
     */
    private int maxBurstCapacity = 10000;
    
    /**
     * Maximum requested tokens for rate limiting.
     */
    private int maxRequestedTokens = 100;
    
    /**
     * List of key resolvers for rate limiting.
     */
    private List<String> keyResolvers = new ArrayList<>();
}
