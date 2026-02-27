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

package org.eclipse.ecsp.gateway.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 * Model representing Rate Limit configuration.
 *
 * @author Abhishek Kumar
 */
@Getter
@Setter
@ToString
public class RateLimit {
    /**
     * Default constructor.
     */
    public RateLimit() {
        // Default constructor
    }

    /**
     * Route identifier for the rate limit.
     */
    private String routeId;
    /**
     * Service name for the rate limit.
     */
    private String service;
    /**
     * Replenish rate for the rate limiter.
     */
    private long replenishRate;
    /**
     * Burst capacity for the rate limiter.
     */
    private long burstCapacity;
    /**
     * Key resolver to be used for rate limiting.
     */
    private String keyResolver;
    /**
     * Additional arguments for the key resolver.
     */
    private Map<String, String> args;
    /**
     * Flag to include headers in the rate limiting response.
     */
    private boolean includeHeaders = true;
    /**
     * Number of requested tokens for the rate limiter.
     */
    private long requestedTokens = 1;

    /**
     * Flag to deny requests with empty keys.
     */
    private Boolean denyEmptyKey = true;

    /**
     * Status to return when the key is empty and denyEmptyKey is true.
     */
    private String emptyKeyStatus = "400";
}
