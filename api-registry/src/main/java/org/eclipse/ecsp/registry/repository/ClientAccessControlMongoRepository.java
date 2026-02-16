package org.eclipse.ecsp.registry.repository;

import org.eclipse.ecsp.registry.common.document.GatewayClientAccessControlDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for GatewayClientAccessControlDocument.
 *
 * <p>
 * Provides CRUD operations and custom queries for client access control in MongoDB.
 * Soft delete pattern: is_deleted flag instead of physical DELETE.
 */
@Repository
public interface ClientAccessControlMongoRepository
    extends MongoRepository<GatewayClientAccessControlDocument, String> {

    /**
     * Find by client ID (not deleted).
     *
     * @param clientId Client identifier
     * @return Optional document, empty if not found or deleted
     */
    Optional<GatewayClientAccessControlDocument> findByClientIdAndIsDeletedFalse(String clientId);

    /**
     * Find all active clients (not deleted).
     *
     * @param isActive Active status filter
     * @return List of documents matching criteria
     */
    List<GatewayClientAccessControlDocument> findByIsActiveAndIsDeletedFalse(boolean isActive);

    /**
     * Find all non-deleted clients.
     *
     * @param isDeleted Deleted status filter (should be false)
     * @return List of documents
     */
    List<GatewayClientAccessControlDocument> findByIsDeletedFalse(boolean isDeleted);

    /**
     * Find by tenant (not deleted).
     *
     * @param tenant Tenant name
     * @param isDeleted Deleted status filter (should be false)
     * @return List of documents for tenant
     */
    List<GatewayClientAccessControlDocument> findByTenantAndIsDeletedFalse(String tenant, boolean isDeleted);

    /**
     * Check if client ID exists (not deleted).
     *
     * @param clientId Client identifier
     * @param isDeleted Deleted status filter (should be false)
     * @return true if exists
     */
    boolean existsByClientIdAndIsDeletedFalse(String clientId, boolean isDeleted);
}
