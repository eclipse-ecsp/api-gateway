package org.eclipse.ecsp.registry.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating/updating Client Access Control configurations.
 *
 * <p>
 * Used in:
 * - POST /v1/config/client-access-control (bulk creation)
 * - PUT /v1/config/client-access-control/{id} (update)
 *
 * @see <a href="contracts.md">API Contracts Documentation</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientAccessControlRequestDto {

    /**
     * Client identifier extracted from JWT claims.
     * Required field.
     * Length: 3-128 characters.
     * Pattern: Alphanumeric with ._- allowed.
     */
    @JsonProperty("clientId")
    private String clientId;

    /**
     * Human-readable description of the client.
     * Optional field.
     * Max length: 512 characters.
     */
    @JsonProperty("description")
    private String description;

    /**
     * Tenant/organization name.
     * Required field.
     * Max length: 256 characters.
     */
    @JsonProperty("tenant")
    private String tenant;

    /**
     * Active status flag.
     * Optional field.
     * Default: true if omitted.
     */
    @JsonProperty("isActive")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Array of allow/deny rules in service:route format.
     * Optional field.
     *
     * <p>
     * Format examples:
     * - Allow rules: ["user-service:*", "payment-service:process"]
     * - Deny rules: ["!user-service:ban-user"]
     * - Wildcards: ["*:*", "user-service:get-*"]
     *
     * <p>
     * Empty/null treated as deny-by-default.
     */
    @JsonProperty("allow")
    private List<String> allow;
}
