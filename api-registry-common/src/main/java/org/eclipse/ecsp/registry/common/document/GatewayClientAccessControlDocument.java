package org.eclipse.ecsp.registry.common.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB Document for Client Access Control configuration.
 *
 * <p>
 * Collection: 'gateway_client_access_control'.
 * Alternative storage to PostgreSQL with same business logic.
 *
 * @see <a href="data-model.md">Data Model Documentation</a>
 */
@Document(collection = "gateway_client_access_control")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayClientAccessControlDocument {

    /**
     * MongoDB unique identifier.
     */
    @Id
    private String id;

    /**
     * Client identifier (unique).
     * Length: 3-128 characters.
     * Pattern: Alphanumeric with ._- allowed.
     */
    @Indexed(unique = true)
    private String clientId;

    /**
     * Human-readable description of the client.
     * Max length: 512 characters.
     */
    private String description;

    /**
     * Tenant/organization identifier.
     * Max length: 256 characters.
     */
    @Indexed
    private String tenant;

    /**
     * Active status flag.
     * Default: true.
     */
    @Indexed
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Soft delete flag.
     * Default: false.
     */
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Array of allow/deny rules in service:route format.
     *
     * <p>
     * Examples:
     * - ["user-service:*", "!user-service:ban-user"]
     * - ["vehicle-service:get-vehicle-info"]
     */
    private List<String> allow;

    /**
     * Creation timestamp.
     */
    private Instant createdAt;

    /**
     * Last update timestamp.
     */
    @Indexed
    private Instant updatedAt;

    /**
     * Deletion timestamp (null if not deleted).
     */
    private Instant deletedAt;
}
