package org.eclipse.ecsp.registry.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response DTO for Client Access Control configurations.
 *
 * <p>
 * Used in:
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
     * Internal database identifier.
     */
    @JsonProperty("id")
    private Long id;

    /**
     * Client identifier.
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
     * Soft delete flag (included only if includeInactive=true).
     */
    @JsonProperty("isDeleted")
    private Boolean isDeleted;

    /**
     * Allow/deny rules array.
     */
    @JsonProperty("allow")
    private List<String> allow;

    /**
     * Creation timestamp.
     */
    @JsonProperty("createdAt")
    private OffsetDateTime createdAt;

    /**
     * Last update timestamp.
     */
    @JsonProperty("updatedAt")
    private OffsetDateTime updatedAt;

    /**
     * Deletion timestamp (included only if deleted).
     */
    @JsonProperty("deletedAt")
    private OffsetDateTime deletedAt;
}
