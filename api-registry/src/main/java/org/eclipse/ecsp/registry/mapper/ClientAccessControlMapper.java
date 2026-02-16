package org.eclipse.ecsp.registry.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.ecsp.registry.common.document.GatewayClientAccessControlDocument;
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlRequestDto;
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlResponseDto;
import org.eclipse.ecsp.registry.common.entity.GatewayClientAccessControl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Manual mapper for Client Access Control entity/document to DTO conversions.
 *
 * <p>
 * Uses Spring BeanUtils for simple field copying and Jackson ObjectMapper for JSON operations.
 * Implementation follows user decision to avoid MapStruct complexity.
 */
@Component
public class ClientAccessControlMapper {

    private final ObjectMapper objectMapper;

    public ClientAccessControlMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Map JPA entity to response DTO.
     *
     * @param entity Source JPA entity
     * @return Response DTO
     */
    public ClientAccessControlResponseDto entityToResponseDto(GatewayClientAccessControl entity) {
        if (entity == null) {
            return null;
        }

        ClientAccessControlResponseDto dto = new ClientAccessControlResponseDto();
        BeanUtils.copyProperties(entity, dto);
        
        // Map allowRules (entity) to allow (DTO)
        dto.setAllow(entity.getAllowRules());
        
        return dto;
    }

    /**
     * Map request DTO to JPA entity.
     *
     * @param dto Source request DTO
     * @return JPA entity
     */
    public GatewayClientAccessControl requestDtoToEntity(ClientAccessControlRequestDto dto) {
        if (dto == null) {
            return null;
        }

        GatewayClientAccessControl entity = new GatewayClientAccessControl();
        BeanUtils.copyProperties(dto, entity);
        
        // Map allow (DTO) to allowRules (entity)
        entity.setAllowRules(dto.getAllow());
        
        // Initialize timestamps
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        
        return entity;
    }

    /**
     * Update existing JPA entity from request DTO.
     * Preserves id, createdAt, and other non-updatable fields.
     *
     * @param entity Target entity to update
     * @param dto Source request DTO
     */
    public void updateEntityFromRequestDto(GatewayClientAccessControl entity, ClientAccessControlRequestDto dto) {
        if (entity == null || dto == null) {
            return;
        }

        // Update only mutable fields
        entity.setDescription(dto.getDescription());
        entity.setTenant(dto.getTenant());
        entity.setIsActive(dto.getIsActive());
        entity.setAllowRules(dto.getAllow());
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    /**
     * Map MongoDB document to response DTO.
     *
     * @param document Source MongoDB document
     * @return Response DTO
     */
    public ClientAccessControlResponseDto documentToResponseDto(GatewayClientAccessControlDocument document) {
        if (document == null) {
            return null;
        }

        return ClientAccessControlResponseDto.builder()
                .id(null)  // MongoDB uses String ObjectId, ResponseDto uses Long for JPA - leave null
                .clientId(document.getClientId())
                .description(document.getDescription())
                .tenant(document.getTenant())
                .isActive(document.getIsActive())
                .isDeleted(document.getIsDeleted())
                .allow(document.getAllow())
                .createdAt(document.getCreatedAt() != null
                        ? OffsetDateTime.ofInstant(document.getCreatedAt(), ZoneOffset.UTC) : null)
                .updatedAt(document.getUpdatedAt() != null
                        ? OffsetDateTime.ofInstant(document.getUpdatedAt(), ZoneOffset.UTC) : null)
                .deletedAt(document.getDeletedAt() != null
                        ? OffsetDateTime.ofInstant(document.getDeletedAt(), ZoneOffset.UTC) : null)
                .build();
    }

    /**
     * Map request DTO to MongoDB document.
     *
     * @param dto Source request DTO
     * @return MongoDB document
     */
    public GatewayClientAccessControlDocument requestDtoToDocument(ClientAccessControlRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Instant now = Instant.now();
        
        return GatewayClientAccessControlDocument.builder()
                .clientId(dto.getClientId())
                .description(dto.getDescription())
                .tenant(dto.getTenant())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .isDeleted(false)
                .allow(dto.getAllow())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Update existing MongoDB document from request DTO.
     *
     * <p>
     * Preserves _id, createdAt, and other nonupdatable fields.
     *
     * @param document Target document to update
     * @param dto Source request DTO
     */
    public void updateDocumentFromRequestDto(
            GatewayClientAccessControlDocument document,
            ClientAccessControlRequestDto dto) {
        if (document == null || dto == null) {
            return;
        }

        // Update only mutable fields
        document.setDescription(dto.getDescription());
        document.setTenant(dto.getTenant());
        document.setIsActive(dto.getIsActive());
        document.setAllow(dto.getAllow());
        document.setUpdatedAt(Instant.now());
    }
}
