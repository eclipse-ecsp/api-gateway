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
    
    Optional<ClientAccessControlEntity> findByClientIdAndIsDeletedFalse(String clientId);
    
    List<ClientAccessControlEntity> findByIsActiveAndIsDeletedFalse(boolean isActive);
    
    List<ClientAccessControlEntity> findByIsDeletedFalse();
    
    boolean existsByClientIdAndIsDeletedFalse(String clientId);
    
    Optional<ClientAccessControlEntity> findByClientIdAndIsDeletedTrue(String clientId);
}
