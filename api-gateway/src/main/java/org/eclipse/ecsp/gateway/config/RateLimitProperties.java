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

package org.eclipse.ecsp.gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.ecsp.gateway.model.RateLimit;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

/**
 * Configuration properties controlling rate limiting behaviour for the gateway.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "api.gateway.rate-limit")
public class RateLimitProperties {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RateLimitProperties.class);
    private boolean enabled = false;
    private List<RateLimit> overrides;
    private RateLimit defaults;
    private long maxBurstCapacity = 10000;
    private long maxReplenishRate = 10000;
    private long maxRequestedTokens = 100;
    private List<String> keyResolvers;
    private String namespace = "default";
    private RetryConfig retry = new RetryConfig();

    /** 
     * Validate the rate limit properties after they have been set.
     */
    @PostConstruct
    public void validate() {
        if (enabled) {
            LOGGER.debug("Api Gateway Rate limit is enabled validating configuration");
            validateDefaults();
            validateOverrides();
            validateKeyResolvers();
        }
    }

    private void validateDefaults() {
        if (defaults != null) {
            LOGGER.debug("Validating default rate limit configuration");
            validateRateLimit(defaults, "Default");
        }
    }

    private void validateOverrides() {
        if (overrides != null) {
            LOGGER.debug("Validating {} rate limit overrides configuration", overrides.size());
            for (RateLimit override : overrides) {
                validateRateLimit(override, "Override");
            }
        }
    }

    private void validateRateLimit(RateLimit rateLimit, String type) {
        // check if burst capacity is greater than zero or less than max burst capacity
        if (rateLimit.getBurstCapacity() <= 0 && rateLimit.getBurstCapacity() > maxBurstCapacity) {
            LOGGER.error("{} burst capacity exceeds maximum allowed: {}", type, maxBurstCapacity);
            throw new IllegalArgumentException(type + " burst capacity exceeds maximum allowed: "
                + maxBurstCapacity);
        }

        // check if replenish rate is greater than zero or less than max replenish rate
        if (rateLimit.getReplenishRate() <= 0 && rateLimit.getReplenishRate() > maxReplenishRate) {
            LOGGER.error("{} replenish rate exceeds maximum allowed: {}", type, maxReplenishRate);
            throw new IllegalArgumentException(type + " replenish rate exceeds maximum allowed: "
                + maxReplenishRate);
        }
        // check if requested tokens is less than max requested tokens
        if (rateLimit.getRequestedTokens() > maxRequestedTokens) {
            LOGGER.error("{} requested tokens exceeds maximum allowed: {}", type, maxRequestedTokens);
            throw new IllegalArgumentException(type + " requested tokens exceeds maximum allowed: "
                + maxRequestedTokens);
        }

        // validate the burst capacity is be greater than or equal to replenish rate
        if (rateLimit.getBurstCapacity() < rateLimit.getReplenishRate()) {
            LOGGER.error("{} burst capacity must be greater than or equal to replenish rate.", type);
            throw new IllegalArgumentException(type + " burst capacity must be greater "
                + "than or equal to replenish rate.");
        }

        // validate requested tokens is less than or equal to burst capacity
        if (rateLimit.getRequestedTokens() > rateLimit.getBurstCapacity()) {
            LOGGER.error("{} requested tokens must be less than or equal to burst capacity.", type);
            throw new IllegalArgumentException(type + " requested tokens must be less "
                + "than or equal to burst capacity.");
        }
    }
        

    private void validateKeyResolvers() {
        if (keyResolvers == null || keyResolvers.isEmpty()) {
            LOGGER.error("At least one key resolver must be configured for rate limiting.");
            throw new IllegalArgumentException("At least one key resolver must be configured for rate limiting.");
        }

        if (defaults != null && !keyResolvers.contains(defaults.getKeyResolver())) {
            LOGGER.error("Default key resolver is not in the list of allowed key resolvers.");
            throw new IllegalArgumentException("Default key resolver is not in the list of allowed key resolvers.");
        }
        if (overrides != null) {
            for (RateLimit override : overrides) {
                if (!keyResolvers.contains(override.getKeyResolver())) {
                    LOGGER.error("Override configuration key resolver is not in the list of allowed key resolvers.");
                    throw new IllegalArgumentException(
                        "Override key resolver is not in the list of allowed key resolvers.");
                }
            }
        }
    }
}