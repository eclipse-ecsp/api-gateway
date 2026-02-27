/********************************************************************************
 * Copyright (c) 2023-24 Harman International
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and\
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.registry.mapper;

import org.eclipse.ecsp.registry.dto.ClientAccessControlRequestDto;
import org.eclipse.ecsp.registry.dto.ClientAccessControlResponseDto;
import org.eclipse.ecsp.registry.entity.ClientAccessControlEntity;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * Manual mapper for Client Access Control entity/document to DTO conversions.
 *
 * <p>Uses Spring BeanUtils for simple field copying and Jackson ObjectMapper for JSON operations.
 * Implementation follows user decision to avoid MapStruct complexity.
 */
@Component
public class ClientAccessControlMapper {
    /**
     * Default constructor.
     */
    public ClientAccessControlMapper() {
        // Default constructor
    }

    /**
     * Map JPA entity to response DTO.
     *
     * @param entity Source JPA entity
     * @return Response DTO
     */
    public ClientAccessControlResponseDto entityToResponseDto(ClientAccessControlEntity entity) {
        if (entity == null) {
            return null;
        }

        ClientAccessControlResponseDto dto = new ClientAccessControlResponseDto();
        BeanUtils.copyProperties(entity, dto);
        
        // Map allow (entity) to allow (DTO) - direct mapping
        dto.setAllow(entity.getAllow());
        
        return dto;
    }

    /**
     * Map request DTO to JPA entity.
     *
     * @param dto Source request DTO
     * @return JPA entity
     */
    public ClientAccessControlEntity requestDtoToEntity(ClientAccessControlRequestDto dto) {
        if (dto == null) {
            return null;
        }

        ClientAccessControlEntity entity = new ClientAccessControlEntity();
        BeanUtils.copyProperties(dto, entity);
        
        // Set id to same value as clientId
        entity.setId(dto.getClientId());
        
        // Map allow (DTO) to allow (entity)
        entity.setAllow(dto.getAllow());
        
        // Initialize timestamps
        LocalDateTime now = LocalDateTime.now();
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
    public void updateEntityFromRequestDto(ClientAccessControlEntity entity, ClientAccessControlRequestDto dto) {
        if (entity == null || dto == null) {
            return;
        }

        // Update only mutable fields
        entity.setDescription(dto.getDescription());
        entity.setTenant(dto.getTenant());
        entity.setIsActive(dto.getIsActive());
        entity.setAllow(dto.getAllow());
        entity.setUpdatedAt(LocalDateTime.now());
    }
}
