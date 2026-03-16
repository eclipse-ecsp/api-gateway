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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Domain model representing client access configuration in the gateway cache.
 *
 * <p>This is a gateway-side representation optimized for fast rule validation.
 * Cached in ConcurrentHashMap for O(1) lookup by clientId.
 *
 * @see AccessRule
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientAccessConfig {

    /**
     * Client identifier (cache key).
     * Extracted from JWT claims.
     */
    private String clientId;

    /**
     * Tenant/organization name.
     * Used for logging and monitoring.
     */
    private String tenant;

    /**
     * Active status flag.
     * Inactive clients are denied access regardless of rules.
     */
    private boolean active;

    /**
     * Parsed access rules for this client.
     * Pre-parsed for performance optimization.
     *
     * <p>Empty list means deny-by-default.
     */
    private List<AccessRule> rules;

    /**
     * Last update timestamp.
     * Used for cache staleness detection.
     */
    private Instant lastUpdated;

    /**
     * Configuration source.
     * Values: "DATABASE", "YAML_OVERRIDE"
     * Used for precedence logging.
     */
    private String source;
}
