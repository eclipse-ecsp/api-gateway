package org.eclipse.ecsp.registry.service.impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlFilterDto;
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlRequestDto;
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlResponseDto;
import org.eclipse.ecsp.registry.common.entity.GatewayClientAccessControl;
import org.eclipse.ecsp.registry.mapper.ClientAccessControlMapper;
import org.eclipse.ecsp.registry.repository.ClientAccessControlRepository;
import org.eclipse.ecsp.registry.service.ClientAccessControlEventPublisher;
import org.eclipse.ecsp.registry.service.ClientAccessControlService;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL implementation of ClientAccessControlService.
 *
 * <p>
 * Primary implementation using JPA repository for PostgreSQL database.
 * Provides transactional CRUD operations with bulk create support.
 */
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class ClientAccessControlServicePostgresImpl implements ClientAccessControlService {

    private static final int MAX_BULK_CREATE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ClientAccessControlRepository repository;
    private final ClientAccessControlMapper mapper;
    private final ClientAccessControlEventPublisher eventPublisher;

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

        // Check existing client IDs
        for (String clientId : clientIds) {
            if (repository.existsByClientIdAndIsDeletedFalse(clientId)) {
                throw new IllegalArgumentException(
                        String.format("Client ID already exists: %s", clientId));
            }
        }

        // FR-015: Atomic transaction - all succeed or all fail (@Transactional handles rollback)
        List<GatewayClientAccessControl> entities = new ArrayList<>();
        for (ClientAccessControlRequestDto request : requests) {
            GatewayClientAccessControl entity = mapper.requestDtoToEntity(request);
            entity.setIsDeleted(false);
            entity.setCreatedAt(OffsetDateTime.now());
            entity.setUpdatedAt(OffsetDateTime.now());
            entities.add(entity);
        }

        List<GatewayClientAccessControl> savedEntities = repository.saveAll(entities);

        log.info("Bulk created {} client access control configurations", savedEntities.size());

        // Publish event for cache refresh (reuse clientIds from earlier)
        eventPublisher.publishEvent("CREATE", clientIds);

        return savedEntities.stream()
                .map(mapper::entityToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientAccessControlResponseDto> getAll(boolean includeInactive) {
        List<GatewayClientAccessControl> entities;

        if (includeInactive) {
            entities = repository.findAllNotDeleted();
        } else {
            entities = repository.findByIsActiveAndIsDeletedFalse(true);
        }

        log.debug("Retrieved {} client configurations (includeInactive={})", entities.size(), includeInactive);

        return entities.stream()
                .map(mapper::entityToResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClientAccessControlResponseDto getById(Long id) {
        GatewayClientAccessControl entity = repository.findById(id)
                .filter(e -> !e.getIsDeleted())
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Client access control not found with id: %d", id)));

        return mapper.entityToResponseDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientAccessControlResponseDto getByClientId(String clientId) {
        GatewayClientAccessControl entity = repository.findByClientIdAndIsDeletedFalse(clientId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Client access control not found with clientId: %s", clientId)));

        return mapper.entityToResponseDto(entity);
    }

    @Override
    @Transactional
    public ClientAccessControlResponseDto update(Long id, ClientAccessControlRequestDto request) {
        GatewayClientAccessControl entity = repository.findById(id)
                .filter(e -> !e.getIsDeleted())
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Client access control not found with id: %d", id)));

        // Check if changing client ID would create duplicate
        if (!entity.getClientId().equals(request.getClientId())) {
            if (repository.existsByClientIdAndIsDeletedFalse(request.getClientId())) {
                throw new IllegalArgumentException(
                        String.format("Client ID already exists: %s", request.getClientId()));
            }
        }

        // Update mutable fields
        entity.setClientId(request.getClientId());
        entity.setDescription(request.getDescription());
        entity.setTenant(request.getTenant());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        entity.setAllowRules(request.getAllow() != null ? request.getAllow() : List.of());
        entity.setUpdatedAt(OffsetDateTime.now());

        GatewayClientAccessControl savedEntity = repository.save(entity);

        log.info("Updated client access control: id={}, clientId={}", id, savedEntity.getClientId());

        // Publish event for cache refresh
        eventPublisher.publishEvent("UPDATE", List.of(savedEntity.getClientId()));

        return mapper.entityToResponseDto(savedEntity);
    }

    @Override
    @Transactional
    public void delete(Long id, boolean permanent) {
        GatewayClientAccessControl entity = repository.findById(id)
                .filter(e -> !e.getIsDeleted())
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Client access control not found with id: %d", id)));

        String clientId = entity.getClientId();

        if (permanent) {
            repository.delete(entity);
            log.info("Permanently deleted client access control: id={}, clientId={}", id, clientId);
        } else {
            entity.setIsDeleted(true);
            entity.setDeletedAt(OffsetDateTime.now());
            entity.setUpdatedAt(OffsetDateTime.now());
            repository.save(entity);
            log.info("Soft deleted client access control: id={}, clientId={}", id, clientId);
        }

        // Publish event for cache refresh
        eventPublisher.publishEvent("DELETE", List.of(clientId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientAccessControlResponseDto> filter(ClientAccessControlFilterDto filter) {
        // Parse pagination parameters
        int page = filter.getPage() != null ? filter.getPage() : 0;
        int size = filter.getSize() != null ? filter.getSize() : DEFAULT_PAGE_SIZE;
        if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE; // Enforce max page size
        }

        // Parse sort parameter (format: "field,direction")
        Sort sort = parseSort(filter.getSort());
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        // Build dynamic query using Specification
        Specification<GatewayClientAccessControl> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude deleted records
            predicates.add(cb.isFalse(root.get("isDeleted")));

            // Client ID filter (partial match)
            if (filter.getClientId() != null && !filter.getClientId().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("clientId")),
                        "%" + filter.getClientId().toLowerCase() + "%"
                ));
            }

            // Tenant filter (partial match)
            if (filter.getTenant() != null && !filter.getTenant().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("tenant")),
                        "%" + filter.getTenant().toLowerCase() + "%"
                ));
            }

            // Active status filter (exact match)
            if (filter.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), filter.getIsActive()));
            }

            // Created date range filters
            if (filter.getCreatedAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        OffsetDateTime.from(filter.getCreatedAfter())
                ));
            }
            if (filter.getCreatedBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt"),
                        OffsetDateTime.from(filter.getCreatedBefore())
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<GatewayClientAccessControl> entityPage = repository.findAll(spec, pageRequest);

        List<ClientAccessControlResponseDto> dtos = entityPage.getContent().stream()
                .map(mapper::entityToResponseDto)
                .toList();

        log.debug("Filtered {} client configurations (page={}, size={})", entityPage.getTotalElements(), page, size);

        return new PageImpl<>(dtos, pageRequest, entityPage.getTotalElements());
    }

    /**
     * Parse sort parameter string into Sort object.
     *
     * @param sortParam Format: "field,direction" (e.g., "createdAt,desc")
     * @return Sort object
     */
    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt"); // Default sort
        }

        String[] parts = sortParam.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direction, field);
    }
}
