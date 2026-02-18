package org.eclipse.ecsp.registry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Response DTO for Client Access Control configurations.
 *
 * <p>Used in:
 * - GET /v1/config/client-access-control (list all)
 * - GET /v1/config/client-access-control/{id} (get by ID)
 * - Response after create/update operations
 *
 * @see <a href="contracts.md">API Contracts Documentation</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientAccessControlResponseDto {

    /**
     * Client identifier (primary key).
     */
    @JsonProperty("clientId")
    private String clientId;

    /**
     * Human-readable description.
     */
    @JsonProperty("description")
    private String description;

    /**
     * Tenant/organization.
     */
    @JsonProperty("tenant")
    private String tenant;

    /**
     * Active status flag.
     */
    @JsonProperty("isActive")
    private Boolean isActive;

    /**
     * Allow/deny rules array.
     */
    @JsonProperty("allow")
    private List<String> allow;
}
