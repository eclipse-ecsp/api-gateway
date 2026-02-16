package org.eclipse.ecsp.registry.service;

import org.eclipse.ecsp.registry.common.dto.ClientAccessControlFilterDto;
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlRequestDto;
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Service interface for Client Access Control management.
 *
 * <p>
 * Provides business logic for CRUD operations on client access configurations.
 * Implementations handle database-specific logic (PostgreSQL or MongoDB).
 */
public interface ClientAccessControlService {

    /**
     * Bulk create client access control configurations.
     *
     * <p>
     * Requirements:
     * - Maximum 100 clients per request (FR-014)
     * - Atomic transaction: all succeed or all fail (FR-015)
     * - Validate client ID uniqueness (FR-025)
     * - Validate request DTO constraints (min length, pattern, etc.)
     *
     * @param requests List of client configurations to create
     * @return List of created configurations
     * @throws IllegalArgumentException if > 100 clients
     * @throws org.springframework.dao.DataIntegrityViolationException if duplicate clientId
     * @throws javax.validation.ValidationException if DTO validation fails
     */
    List<ClientAccessControlResponseDto> bulkCreate(List<ClientAccessControlRequestDto> requests);

    /**
     * Get all client access control configurations.
     *
     * @param includeInactive Whether to include inactive clients
     * @return List of all client configurations
     */
    List<ClientAccessControlResponseDto> getAll(boolean includeInactive);

    /**
     * Get client access control configuration by ID.
     *
     * @param id Configuration ID
     * @return Configuration details
     * @throws javax.persistence.EntityNotFoundException if not found
     */
    ClientAccessControlResponseDto getById(Long id);

    /**
     * Update client access control configuration.
     *
     * @param id Configuration ID
     * @param request Updated configuration
     * @return Updated configuration
     * @throws javax.persistence.EntityNotFoundException if not found
     * @throws javax.validation.ValidationException if DTO validation fails
     */
    ClientAccessControlResponseDto update(Long id, ClientAccessControlRequestDto request);

    /**
     * Delete client access control configuration.
     *
     * @param id Configuration ID
     * @param permanent Whether to permanently delete or soft delete
     * @throws javax.persistence.EntityNotFoundException if not found
     */
    void delete(Long id, boolean permanent);

    /**
     * Filter client access control configurations with pagination.
     *
     * <p>
     * Supports:
     * - Partial match on clientId, tenant (LIKE '%value%')
     * - Exact match on isActive, isDeleted
     * - Date range filters (createdAfter, createdBefore)
     * - Pagination (page, size, sort)
     *
     * @param filter Filter criteria and pagination
     * @return Page of matching configurations
     */
    Page<ClientAccessControlResponseDto> filter(ClientAccessControlFilterDto filter);

    /**
     * Get client access control configuration by client ID.
     *
     * @param clientId Client identifier
     * @return Configuration details
     * @throws javax.persistence.EntityNotFoundException if not found
     */
    ClientAccessControlResponseDto getByClientId(String clientId);
}
