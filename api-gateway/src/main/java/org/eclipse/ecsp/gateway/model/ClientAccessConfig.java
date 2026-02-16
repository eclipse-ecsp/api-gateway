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
 * <p>
 * This is a gateway-side representation optimized for fast rule validation.
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
     * <p>
     * Empty list means deny-by-default.
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
