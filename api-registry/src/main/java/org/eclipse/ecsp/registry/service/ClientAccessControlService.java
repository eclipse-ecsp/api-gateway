package org.eclipse.ecsp.registry.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.eclipse.ecsp.registry.dto.ClientAccessControlRequestDto;
import org.eclipse.ecsp.registry.dto.ClientAccessControlResponseDto;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.List;

/**
 * Service interface for Client Access Control management.
 *
 * <p>Provides business logic for CRUD operations on client access configurations.
 * Implementations handle database-specific logic (PostgreSQL or MongoDB).
 */
public interface ClientAccessControlService {

    /**
     * Bulk create client access control configurations.
     *
     * <p>Requirements:
     * - Maximum 100 clients per request (FR-014)
     * - Atomic transaction: all succeed or all fail (FR-015)
     * - Validate client ID uniqueness (FR-025)
     * - Validate request DTO constraints (min length, pattern, etc.)
     *
     * @param requests List of client configurations to create
     * @return List of created configurations
     * @throws IllegalArgumentException if > 100 clients
     * @throws DataIntegrityViolationException if duplicate clientId
     * @throws ValidationException if DTO validation fails
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
     * @param clientId Client identifier
     * @return Configuration details
     * @throws EntityNotFoundException if not found
     */
    ClientAccessControlResponseDto getById(String clientId);

    /**
     * Update client access control configuration.
     *
     * @param clientId Client identifier
     * @param request Updated configuration
     * @return Updated configuration
     * @throws EntityNotFoundException if not found
     * @throws ValidationException if DTO validation fails
     */
    ClientAccessControlResponseDto update(String clientId, ClientAccessControlRequestDto request);

    /**
     * Delete client access control configuration.
     *
     * @param clientId Client identifier
     * @param permanent Whether to permanently delete or soft delete
     * @throws EntityNotFoundException if not found
     */
    void delete(String clientId, boolean permanent);

    /**
     * Get client access control configuration by client ID.
     *
     * @param clientId Client identifier
     * @return Configuration details
     * @throws EntityNotFoundException if not found
     */
    ClientAccessControlResponseDto getByClientId(String clientId);
}
