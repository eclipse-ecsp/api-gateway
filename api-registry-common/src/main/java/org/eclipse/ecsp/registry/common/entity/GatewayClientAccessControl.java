package org.eclipse.ecsp.registry.common.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * JPA Entity for Client Access Control configuration.
 *
 * <p>
 * Maps to PostgreSQL table 'gateway_client_access_control'.
 * Stores client access control configurations with allow/deny rules for service and route access validation.
 *
 * @see <a href="data-model.md">Data Model Documentation</a>
 */
@Entity
@Table(name = "gateway_client_access_control")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayClientAccessControl {

    /**
     * Primary key - unique identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * Client identifier extracted from JWT claims (unique).
     * Length: 3-128 characters.
     */
    @Column(name = "client_id", nullable = false, unique = true, length = 128)
    private String clientId;

    /**
     * Human-readable description of the client.
     * Max length: 512 characters.
     */
    @Column(name = "description", length = 512)
    private String description;

    /**
     * Tenant/organization of the client.
     * Max length: 256 characters.
     */
    @Column(name = "tenant", nullable = false, length = 256)
    private String tenant;

    /**
     * Active status flag for client validation.
     * Default: true.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Soft delete flag for audit compliance.
     * Default: false.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * JSON array of allow/deny rules in service:route format.
     * Stored as JSONB in PostgreSQL.
     *
     * <p>
     * Examples:
     * - Allow rules: ["user-service:*", "payment-service:process"]
     * - Deny rules: ["!user-service:ban-user"]
     * - Wildcards: ["*:*", "user-service:get-*"]
     *
     * <p>
     * Empty/null treated as deny-by-default.
     */
    @Type(JsonBinaryType.class)
    @Column(name = "allow_rules", columnDefinition = "jsonb")
    private List<String> allowRules;

    /**
     * Record creation timestamp.
     * Auto-populated on insert.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Last update timestamp.
     * Auto-populated on update.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Soft deletion timestamp (NULL if not deleted).
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
