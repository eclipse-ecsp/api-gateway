package org.eclipse.ecsp.registry.repository;

import org.eclipse.ecsp.registry.common.entity.GatewayClientAccessControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for GatewayClientAccessControl entity (PostgreSQL).
 *
 * <p>
 * Provides CRUD operations and custom queries for client access control management.
 * Soft delete pattern: is_deleted flag instead of physical DELETE.
 */
@Repository
public interface ClientAccessControlRepository extends JpaRepository<GatewayClientAccessControl, Long>, 
                                                        JpaSpecificationExecutor<GatewayClientAccessControl> {

    /**
     * Find by client ID (not deleted).
     *
     * @param clientId Client identifier
     * @return Optional entity, empty if not found or deleted
     */
    Optional<GatewayClientAccessControl> findByClientIdAndIsDeletedFalse(String clientId);

    /**
     * Find all active clients (not deleted).
     *
     * @param isActive Active status filter
     * @return List of entities matching criteria
     */
    List<GatewayClientAccessControl> findByIsActiveAndIsDeletedFalse(boolean isActive);

    /**
     * Find all non-deleted clients.
     *
     * @return List of entities
     */
    @Query("SELECT c FROM GatewayClientAccessControl c WHERE c.isDeleted = false")
    List<GatewayClientAccessControl> findAllNotDeleted();

    /**
     * Find by tenant (not deleted).
     *
     * @param tenant Tenant name
     * @return List of entities for tenant
     */
    List<GatewayClientAccessControl> findByTenantAndIsDeletedFalse(String tenant);

    /**
     * Check if client ID exists (not deleted).
     *
     * @param clientId Client identifier
     * @return true if exists
     */
    boolean existsByClientIdAndIsDeletedFalse(String clientId);
}
