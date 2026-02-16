package org.eclipse.ecsp.registry.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Filter DTO for querying Client Access Control configurations.
 *
 * <p>
 * Used in:
 * - POST /v1/config/client-access-control/filter (filter with criteria)
 *
 * <p>
 * All fields are optional for flexible filtering.
 *
 * @see <a href="contracts.md">API Contracts Documentation</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientAccessControlFilterDto {

    /**
     * Partial match on client ID (case-insensitive).
     * Optional filter.
     */
    @JsonProperty("clientId")
    private String clientId;

    /**
     * Partial match on tenant (case-insensitive).
     * Optional filter.
     */
    @JsonProperty("tenant")
    private String tenant;

    /**
     * Filter by active status.
     * Optional filter.
     */
    @JsonProperty("isActive")
    private Boolean isActive;

    /**
     * Filter by deleted status.
     * Optional filter.
     */
    @JsonProperty("isDeleted")
    private Boolean isDeleted;

    /**
     * Filter clients created after this timestamp.
     * Optional filter.
     */
    @JsonProperty("createdAfter")
    private OffsetDateTime createdAfter;

    /**
     * Filter clients created before this timestamp.
     * Optional filter.
     */
    @JsonProperty("createdBefore")
    private OffsetDateTime createdBefore;

    /**
     * Page number (zero-indexed).
     * Default: 0.
     */
    @JsonProperty("page")
    @Builder.Default
    private Integer page = 0;

    /**
     * Page size.
     * Min: 1, Max: 100, Default: 20.
     */
    @JsonProperty("size")
    @Builder.Default
    private Integer size = 20;

    /**
     * Sort field and direction.
     * Format: "field,direction" (e.g., "clientId,asc", "createdAt,desc").
     * Default: "createdAt,desc".
     */
    @JsonProperty("sort")
    @Builder.Default
    private String sort = "createdAt,desc";
}
