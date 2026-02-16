package org.eclipse.ecsp.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for YAML-based client access control overrides.
 *
 * <p>
 * Binds to: api.gateway.client-access-control.overrides
 *
 * <p>
 * YAML overrides take precedence over database configurations per FR-039-043.
 * Use case: Emergency access changes without database modification.
 *
 * <p>
 * Example application.yml:
 * <pre>
 * api:
 *   gateway:
 *     client-access-control:
 *       overrides:
 *         - clientId: "emergency-client"
 *           tenant: "ops-team"
 *           active: true
 *           allow:
 *             - "*:*"
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "api.gateway.client-access-control")
public class ClientAccessControlYamlProperties {

    /**
     * List of client access control overrides from YAML.
     * Empty by default (no overrides).
     */
    private List<YamlOverride> overrides = new ArrayList<>();

    /**
     * Single YAML override entry.
     */
    @Data
    public static class YamlOverride {
        /**
         * Client identifier (must match database clientId to override).
         */
        private String clientId;

        /**
         * Tenant/organization name.
         */
        private String tenant;

        /**
         * Description (optional).
         */
        private String description;

        /**
         * Active status flag.
         */
        private boolean active = true;

        /**
         * Allow rules in service:route format.
         * Example: ["user-service:*", "!payment-service:refund"]
         */
        private List<String> allow = new ArrayList<>();
    }
}
