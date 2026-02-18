package org.eclipse.ecsp.registry.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.registry.dto.ClientAccessControlRequestDto;
import org.eclipse.ecsp.registry.dto.ClientAccessControlResponseDto;
import org.eclipse.ecsp.registry.entity.ClientAccessControlEntity;
import org.eclipse.ecsp.registry.events.EventPublisherContext;
import org.eclipse.ecsp.registry.events.data.ClientAccessControlEventData;
import org.eclipse.ecsp.registry.exception.DuplicateClientException;
import org.eclipse.ecsp.registry.mapper.ClientAccessControlMapper;
import org.eclipse.ecsp.registry.repo.ClientAccessControlRepository;
import org.eclipse.ecsp.registry.service.ClientAccessControlService;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL implementation of ClientAccessControlService.
 *
 * <p>Primary implementation using JPA repository for PostgreSQL database.
 * Provides transactional CRUD operations with bulk create support.
 */
@Service
public class ClientAccessControlServiceImpl implements ClientAccessControlService {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ClientAccessControlServiceImpl.class);
    private static final int MAX_BULK_CREATE_SIZE = 100;
    private static final String CLIENT_NOT_FOUND_MSG = "Client access control configuration not found for clientId: %s";

    private final ClientAccessControlRepository repository;
    private final ClientAccessControlMapper mapper;
    private final EventPublisherContext eventPublisher;

    /**
     * Constructor to initialize dependencies.
     *
     * @param repository the ClientAccessControlRepository
     * @param mapper the ClientAccessControlMapper
     * @param eventPublisher the EventPublisherContext
     */
    public ClientAccessControlServiceImpl(ClientAccessControlRepository repository,
                                          ClientAccessControlMapper mapper,
                                          EventPublisherContext eventPublisher) {
        this.repository = repository;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public List<ClientAccessControlResponseDto> bulkCreate(List<ClientAccessControlRequestDto> requests) {
        // FR-014: Validate bulk size limit
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Request list cannot be null or empty");
        }
        if (requests.size() > MAX_BULK_CREATE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("Bulk create limited to %d clients, received %d", 
                            MAX_BULK_CREATE_SIZE, requests.size()));
        }

        // FR-025: Validate client ID uniqueness
        List<String> clientIds = requests.stream()
                .map(ClientAccessControlRequestDto::getClientId)
                .toList();

        long duplicateCount = clientIds.stream()
                .distinct()
                .count();
        if (duplicateCount != clientIds.size()) {
            throw new IllegalArgumentException("Duplicate client IDs in request");
        }

        // Check existing client IDs (active)
        List<String> duplicates = new ArrayList<>();
        for (String clientId : clientIds) {
            if (repository.existsByClientIdAndIsDeletedFalse(clientId)) {
                LOGGER.error("Client ID already exists: {}", clientId);
                duplicates.add(clientId);
            }
        }
        
        if (!duplicates.isEmpty()) {
            throw new DuplicateClientException(
                    "Client ID(s) already exist.",
                    duplicates);
        }

        // FR-015: Atomic transaction - all succeed or all fail (@Transactional handles rollback)
        List<ClientAccessControlEntity> entities = new ArrayList<>();
        for (ClientAccessControlRequestDto request : requests) {
            ClientAccessControlEntity entity;
            
            // Check if client ID exists as soft-deleted and restore it
            Optional<ClientAccessControlEntity> softDeleted = 
                    repository.findByClientIdAndIsDeletedTrue(request.getClientId());
            
            if (softDeleted.isPresent()) {
                // Restore soft-deleted record
                entity = softDeleted.get();
                entity.setIsDeleted(false);
                entity.setDeletedAt(null);
                // Update fields from request
                entity.setDescription(request.getDescription());
                entity.setTenant(request.getTenant());
                entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE);
                entity.setAllow(request.getAllow());
                entity.setUpdatedAt(OffsetDateTime.now());
                LOGGER.info("Restoring soft-deleted client access control: clientId={}", request.getClientId());
            } else {
                // Create new record
                entity = mapper.requestDtoToEntity(request);
                entity.setIsDeleted(false);
                entity.setCreatedAt(OffsetDateTime.now());
                entity.setUpdatedAt(OffsetDateTime.now());
            }
            entities.add(entity);
        }

        List<ClientAccessControlEntity> savedEntities = repository.saveAll(entities);

        LOGGER.info("Bulk created {} client access control configurations", savedEntities.size());

        // Publish event for cache refresh (reuse clientIds from earlier)
        eventPublisher.publishEvent(new ClientAccessControlEventData(clientIds));

        return savedEntities.stream()
                .map(mapper::entityToResponseDto)
                .toList();
    }

    @Override
    public List<ClientAccessControlResponseDto> getAll(boolean includeInactive) {
        List<ClientAccessControlEntity> entities;

        if (includeInactive) {
            entities = repository.findByIsDeletedFalse();
        } else {
            entities = repository.findByIsActiveAndIsDeletedFalse(true);
        }

        LOGGER.debug("Retrieved {} client configurations (includeInactive={})", entities.size(), includeInactive);

        return entities.stream()
                .map(mapper::entityToResponseDto)
                .toList();
    }

    @Override
    public ClientAccessControlResponseDto getById(String id) {
        ClientAccessControlEntity entity = repository.findByClientIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(CLIENT_NOT_FOUND_MSG, id)));

        return mapper.entityToResponseDto(entity);
    }

    @Override
    public ClientAccessControlResponseDto getByClientId(String clientId) {
        ClientAccessControlEntity entity = repository.findByClientIdAndIsDeletedFalse(clientId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(CLIENT_NOT_FOUND_MSG, clientId)));

        return mapper.entityToResponseDto(entity);
    }

    @Override
    public ClientAccessControlResponseDto update(String clientId, ClientAccessControlRequestDto request) {
        ClientAccessControlEntity entity = repository.findByClientIdAndIsDeletedFalse(clientId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(CLIENT_NOT_FOUND_MSG, clientId)));

        // Check if changing client ID would create duplicate
        if (!entity.getClientId().equals(request.getClientId())
            && repository.existsByClientIdAndIsDeletedFalse(request.getClientId())) {
            throw new DuplicateClientException(
                    "Duplicate client IDs detected.",
                    List.of(request.getClientId()));
        }

        // Update mutable fields
        if (StringUtils.isNotEmpty(request.getClientId())) {
            entity.setClientId(request.getClientId());
            // Update id to match clientId
            entity.setId(request.getClientId());
        }
        if (StringUtils.isNotEmpty(request.getDescription())) {
            entity.setDescription(request.getDescription());
        }
        if (StringUtils.isNotEmpty(request.getTenant())) {
            entity.setTenant(request.getTenant());
        }

        boolean isActiveChanged = entity.getIsActive() != null 
                && !entity.getIsActive().equals(request.getIsActive());
        if (isActiveChanged) {
            LOGGER.info("Changing active status for clientId={} from {} to {}", 
                    entity.getClientId(), entity.getIsActive(), request.getIsActive());
            entity.setIsActive(request.getIsActive() == null || request.getIsActive());
        }


        
        entity.setAllow(request.getAllow() != null ? request.getAllow() : entity.getAllow());
        entity.setUpdatedAt(OffsetDateTime.now());

        ClientAccessControlEntity savedEntity = repository.save(entity);

        LOGGER.info("Updated client access control: clientId={}", savedEntity.getClientId());

        // Publish event for cache refresh
        eventPublisher.publishEvent(new ClientAccessControlEventData(List.of(savedEntity.getClientId())));

        return mapper.entityToResponseDto(savedEntity);
    }

    @Override
    public void delete(String clientId, boolean permanent) {
        ClientAccessControlEntity entity = repository.findByClientIdAndIsDeletedFalse(clientId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(CLIENT_NOT_FOUND_MSG, clientId)));

        if (permanent) {
            repository.delete(entity);
            LOGGER.info("Permanently deleted client access control: clientId={}", clientId);
        } else {
            entity.setIsDeleted(true);
            entity.setDeletedAt(OffsetDateTime.now());
            entity.setUpdatedAt(OffsetDateTime.now());
            repository.save(entity);
            LOGGER.info("Soft deleted client access control: clientId={}", clientId);
        }

        // Publish event for cache refresh
        eventPublisher.publishEvent(new ClientAccessControlEventData(List.of(clientId)));
    }
}
