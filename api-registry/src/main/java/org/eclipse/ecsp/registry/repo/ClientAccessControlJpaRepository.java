package org.eclipse.ecsp.registry.repo;

import org.eclipse.ecsp.registry.condition.ConditionalOnSqlDatabase;
import org.eclipse.ecsp.registry.entity.ClientAccessControlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for GatewayClientAccessControl entity (PostgreSQL).
 *
 * <p>Provides CRUD operations and custom queries for client access control management.
 * Soft delete pattern: is_deleted flag instead of physical DELETE.
 */
@ConditionalOnSqlDatabase
@Repository("clientAccessControlJpaRepository")
public interface ClientAccessControlJpaRepository extends JpaRepository<ClientAccessControlEntity, String> {
    /**
     * Find client access control configuration by client ID (non-deleted only).
     *
     * @param clientId the client identifier
     * @return Optional containing the entity if found
     */
    Optional<ClientAccessControlEntity> findByClientIdAndIsDeletedFalse(String clientId);
    
    /**
     * Find all client access control configurations by active status (non-deleted only).
     *
     * @param isActive the active status
     * @return List of matching entities
     */
    List<ClientAccessControlEntity> findByIsActiveAndIsDeletedFalse(boolean isActive);
    
    /**
     * Find all non-deleted client access control configurations.
     *
     * @return List of all non-deleted entities
     */
    List<ClientAccessControlEntity> findByIsDeletedFalse();
    
    /**
     * Check if a client access control configuration exists by client ID (non-deleted only).
     *
     * @param clientId the client identifier
     * @return true if exists
     */
    boolean existsByClientIdAndIsDeletedFalse(String clientId);
    
    /**
     * Find client access control configuration by client ID (deleted only).
     *
     * @param clientId the client identifier
     * @return Optional containing the deleted entity if found
     */
    Optional<ClientAccessControlEntity> findByClientIdAndIsDeletedTrue(String clientId);
}
