package org.eclipse.ecsp.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Client Access Control feature.
 *
 * <p>
 * Loaded from application.yml under 'api.gateway.client-access-control.*'
 *
 * <p>
 * Example configuration:
 * <pre>
 * api:
 *   gateway:
 *     client-access-control:
 *       enabled: true
 *       claim-names:
 *         - clientId
 *         - azp
 *         - client_id
 *         - cid
 *       skip-paths:
 *         - "/v3/api-docs/**"
 *         - "/actuator/**"
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "api.gateway.client-access-control")
@Data
public class ClientAccessControlProperties {

    /**
     * Enable or disable client access control filtering globally.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Ordered list of JWT claim names to try for extracting client ID.
     * The first non-empty claim value is used.
     *
     * <p>
     * Default order supports multiple IDPs:
     * - clientId: Custom claim
     * - azp: Azure AD authorized party
     * - client_id: OAuth2/OIDC standard
     * - cid: Alternative client identifier
     */
    private List<String> claimNames = List.of("clientId", "azp", "client_id", "cid");

    /**
     * Ant-style path patterns to skip validation.
     * Useful for health checks, API documentation, actuator endpoints.
     *
     * <p>
     * Examples:
     * - "/v3/api-docs/**" - OpenAPI documentation
     * - "/actuator/**" - Spring Boot actuator
     * - "/health" - Health check endpoint
     */
    private List<String> skipPaths = new ArrayList<>();

    /**
     * Polling interval in seconds for fallback mode (when Redis unavailable).
     * Default: 30 seconds
     */
    private int pollingIntervalSeconds = 30;

    /**
     * Fail-fast if Redis is unavailable at startup.
     * Default: true
     */
    private boolean failFastOnRedisUnavailable = true;
}
