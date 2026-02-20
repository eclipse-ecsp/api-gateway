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

package org.eclipse.ecsp.registry.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.eclipse.ecsp.registry.dto.ClientAccessControlRequestDto;
import org.eclipse.ecsp.registry.dto.ClientAccessControlResponseDto;
import org.eclipse.ecsp.registry.entity.ClientAccessControlEntity;
import org.eclipse.ecsp.registry.events.EventPublisherContext;
import org.eclipse.ecsp.registry.events.data.ClientAccessControlEventData;
import org.eclipse.ecsp.registry.exception.DuplicateClientException;
import org.eclipse.ecsp.registry.mapper.ClientAccessControlMapper;
import org.eclipse.ecsp.registry.repo.ClientAccessControlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClientAccessControlServiceImpl}.
 *
 * <p>Verifies service layer business logic including validation,
 * bulk operations, soft delete, and event publishing.
 */
@ExtendWith(MockitoExtension.class)
class ClientAccessControlServiceImplTest {

    @Mock
    private ClientAccessControlRepository repository;

    @Mock
    private ClientAccessControlMapper mapper;

    @Mock
    private EventPublisherContext eventPublisher;

    @InjectMocks
    private ClientAccessControlServiceImpl service;

    @Captor
    private ArgumentCaptor<ClientAccessControlEventData> eventDataCaptor;

    @Captor
    private ArgumentCaptor<List<ClientAccessControlEntity>> entitiesCaptor;

    private ClientAccessControlRequestDto requestDto;
    private ClientAccessControlEntity entity;
    private ClientAccessControlResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = new ClientAccessControlRequestDto();
        requestDto.setClientId("client1");
        requestDto.setDescription("Test client");
        requestDto.setTenant("tenant1");
        requestDto.setIsActive(true);

        entity = new ClientAccessControlEntity();
        entity.setId("client1");
        entity.setClientId("client1");
        entity.setDescription("Test client");
        entity.setTenant("tenant1");
        entity.setIsActive(true);
        entity.setIsDeleted(false);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        responseDto = new ClientAccessControlResponseDto();
        responseDto.setClientId("client1");
        responseDto.setDescription("Test client");
        responseDto.setTenant("tenant1");
        responseDto.setIsActive(true);
    }

    /**
     * Given a valid bulk create request.
     * When bulkCreate is called.
     * Then creates all entities and publishes event.
     */
    @Test
    void bulkCreate_ValidRequest_CreatesAllEntitiesAndPublishesEvent() {
        when(repository.existsByClientIdAndIsDeletedFalse("client1")).thenReturn(false);
        when(mapper.requestDtoToEntity(requestDto)).thenReturn(entity);
        when(repository.saveAll(anyList())).thenReturn(Arrays.asList(entity));
        when(mapper.entityToResponseDto(entity)).thenReturn(responseDto);

        List<ClientAccessControlResponseDto> result = service.bulkCreate(Arrays.asList(requestDto));

        assertEquals(1, result.size());
        assertEquals("client1", result.get(0).getClientId());
        verify(repository, times(1)).saveAll(anyList());
        verify(eventPublisher, times(1)).publishEvent(any(ClientAccessControlEventData.class));
    }

    /**
     * Given a null request list.
     * When bulkCreate is called.
     * Then throws IllegalArgumentException.
     */
    @Test
    void bulkCreate_NullRequest_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.bulkCreate(null)
        );

        assertEquals("Request list cannot be null or empty", exception.getMessage());
        verify(repository, never()).saveAll(anyList());
        verify(eventPublisher, never()).publishEvent(any());
    }

    /**
     * Given an empty request list.
     * When bulkCreate is called.
     * Then throws IllegalArgumentException.
     */
    @Test
    void bulkCreate_EmptyRequest_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.bulkCreate(new ArrayList<>())
        );

        assertEquals("Request list cannot be null or empty", exception.getMessage());
        verify(repository, never()).saveAll(anyList());
    }

    /**
     * Given bulk request exceeds size limit.
     * When bulkCreate is called.
     * Then throws IllegalArgumentException.
     */
    @Test
    void bulkCreate_ExceedsSizeLimit_ThrowsIllegalArgumentException() {
        final List<ClientAccessControlRequestDto> requests = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            requests.add(requestDto);
        }

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.bulkCreate(requests)
        );

        assertTrue(exception.getMessage().contains("Bulk create limited to 100 clients"));
        verify(repository, never()).saveAll(anyList());
    }

    /**
     * Given duplicate client IDs in request.
     * When bulkCreate is called.
     * Then throws IllegalArgumentException.
     */
    @Test
    void bulkCreate_DuplicateClientIdsInRequest_ThrowsIllegalArgumentException() {
        final ClientAccessControlRequestDto dto1 = new ClientAccessControlRequestDto();
        dto1.setClientId("duplicate");
        final ClientAccessControlRequestDto dto2 = new ClientAccessControlRequestDto();
        dto2.setClientId("duplicate");
        final List<ClientAccessControlRequestDto> requests = Arrays.asList(dto1, dto2);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.bulkCreate(requests)
        );

        assertEquals("Duplicate client IDs in request", exception.getMessage());
        verify(repository, never()).saveAll(anyList());
    }

    /**
     * Given existing active client ID.
     * When bulkCreate is called.
     * Then throws DuplicateClientException.
     */
    @Test
    void bulkCreate_ExistingActiveClientId_ThrowsDuplicateClientException() {
        when(repository.existsByClientIdAndIsDeletedFalse("client1")).thenReturn(true);
        final List<ClientAccessControlRequestDto> requests = Arrays.asList(requestDto);

        DuplicateClientException exception = assertThrows(
                DuplicateClientException.class,
                () -> service.bulkCreate(requests));

        assertTrue(exception.getMessage().contains("Client ID(s) already exist"));
        verify(repository, never()).saveAll(anyList());
    }

    /**
     * Given soft-deleted client ID.
     * When bulkCreate is called.
     * Then restores entity and updates fields.
     */
    @Test
    void bulkCreate_SoftDeletedClientId_RestoresEntity() {
        entity.setIsDeleted(true);
        entity.setDeletedAt(OffsetDateTime.now());
        when(repository.existsByClientIdAndIsDeletedFalse("client1")).thenReturn(false);
        when(repository.findByClientIdAndIsDeletedTrue("client1")).thenReturn(Optional.of(entity));
        when(repository.saveAll(anyList())).thenReturn(Arrays.asList(entity));
        when(mapper.entityToResponseDto(entity)).thenReturn(responseDto);

        List<ClientAccessControlResponseDto> result = service.bulkCreate(Arrays.asList(requestDto));

        assertEquals(1, result.size());
        verify(repository, times(1)).saveAll(entitiesCaptor.capture());
        final ClientAccessControlEntity savedEntity = entitiesCaptor.getValue().get(0);
        assertFalse(savedEntity.getIsDeleted());
        verify(eventPublisher, times(1)).publishEvent(any(ClientAccessControlEventData.class));
    }

    /**
     * Given includeInactive is false.
     * When getAll is called.
     * Then returns only active entities.
     */
    @Test
    void getAll_IncludeInactiveFalse_ReturnsOnlyActive() {
        when(repository.findByIsActiveAndIsDeletedFalse(true)).thenReturn(Arrays.asList(entity));
        when(mapper.entityToResponseDto(entity)).thenReturn(responseDto);

        List<ClientAccessControlResponseDto> result = service.getAll(false);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
        verify(repository, times(1)).findByIsActiveAndIsDeletedFalse(true);
        verify(repository, never()).findByIsDeletedFalse();
    }

    /**
     * Given includeInactive is true.
     * When getAll is called.
     * Then returns all non-deleted entities.
     */
    @Test
    void getAll_IncludeInactiveTrue_ReturnsAllNonDeleted() {
        when(repository.findByIsDeletedFalse()).thenReturn(Arrays.asList(entity));
        when(mapper.entityToResponseDto(entity)).thenReturn(responseDto);

        List<ClientAccessControlResponseDto> result = service.getAll(true);

        assertEquals(1, result.size());
        verify(repository, times(1)).findByIsDeletedFalse();
        verify(repository, never()).findByIsActiveAndIsDeletedFalse(any(Boolean.class));
    }

    /**
     * Given existing client ID.
     * When getByClientId is called.
     * Then returns response DTO.
     */
    @Test
    void getByClientId_ExistingClientId_ReturnsResponseDto() {
        when(repository.findByClientIdAndIsDeletedFalse("client1")).thenReturn(Optional.of(entity));
        when(mapper.entityToResponseDto(entity)).thenReturn(responseDto);

        ClientAccessControlResponseDto result = service.getByClientId("client1");

        assertNotNull(result);
        assertEquals("client1", result.getClientId());
        verify(repository, times(1)).findByClientIdAndIsDeletedFalse("client1");
    }

    /**
     * Given non-existing client ID.
     * When getByClientId is called.
     * Then throws EntityNotFoundException.
     */
    @Test
    void getByClientId_NonExistingClientId_ThrowsEntityNotFoundException() {
        when(repository.findByClientIdAndIsDeletedFalse("nonexistent")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> service.getByClientId("nonexistent")
        );

        assertTrue(exception.getMessage().contains("Client access control configuration not found"));
        verify(repository, times(1)).findByClientIdAndIsDeletedFalse("nonexistent");
    }

    /**
     * Given valid update request.
     * When update is called.
     * Then updates entity and publishes event.
     */
    @Test
    void update_ValidRequest_UpdatesEntityAndPublishesEvent() {
        requestDto.setDescription("Updated description");
        when(repository.findByClientIdAndIsDeletedFalse("client1")).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.entityToResponseDto(entity)).thenReturn(responseDto);

        ClientAccessControlResponseDto result = service.update("client1", requestDto);

        assertNotNull(result);
        verify(repository, times(1)).save(entity);
        verify(eventPublisher, times(1)).publishEvent(eventDataCaptor.capture());
        final ClientAccessControlEventData eventData = eventDataCaptor.getValue();
        assertEquals(1, eventData.getClientIds().size());
        assertTrue(eventData.getClientIds().contains("client1"));
    }

    /**
     * Given update with duplicate client ID.
     * When update is called.
     * Then throws DuplicateClientException.
     */
    @Test
    void update_DuplicateClientId_ThrowsDuplicateClientException() {
        requestDto.setClientId("client2");
        when(repository.findByClientIdAndIsDeletedFalse("client1")).thenReturn(Optional.of(entity));
        when(repository.existsByClientIdAndIsDeletedFalse("client2")).thenReturn(true);
        final String clientId = "client1";
        final ClientAccessControlRequestDto dto = requestDto;

        DuplicateClientException exception = assertThrows(
                DuplicateClientException.class,
                () -> service.update(clientId, dto));

        assertTrue(exception.getMessage().contains("Duplicate client IDs detected"));
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    /**
     * Given update for non-existing client.
     * When update is called.
     * Then throws EntityNotFoundException.
     */
    @Test
    void update_NonExistingClient_ThrowsEntityNotFoundException() {
        when(repository.findByClientIdAndIsDeletedFalse("nonexistent")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> service.update("nonexistent", requestDto)
        );

        assertTrue(exception.getMessage().contains("Client access control configuration not found"));
        verify(repository, never()).save(any());
    }

    /**
     * Given soft delete request.
     * When delete is called with permanent false.
     * Then soft deletes entity and publishes event.
     */
    @Test
    void delete_SoftDelete_MarksAsDeletedAndPublishesEvent() {
        when(repository.findByClientIdAndIsDeletedFalse("client1")).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        service.delete("client1", false);

        assertTrue(entity.getIsDeleted());
        assertNotNull(entity.getDeletedAt());
        verify(repository, times(1)).save(entity);
        verify(repository, never()).delete(any());
        verify(eventPublisher, times(1)).publishEvent(any(ClientAccessControlEventData.class));
    }

    /**
     * Given permanent delete request.
     * When delete is called with permanent true.
     * Then permanently deletes entity and publishes event.
     */
    @Test
    void delete_PermanentDelete_DeletesEntityAndPublishesEvent() {
        when(repository.findByClientIdAndIsDeletedFalse("client1")).thenReturn(Optional.of(entity));

        service.delete("client1", true);

        verify(repository, times(1)).delete(entity);
        verify(repository, never()).save(any());
        verify(eventPublisher, times(1)).publishEvent(any(ClientAccessControlEventData.class));
    }

    /**
     * Given delete for non-existing client.
     * When delete is called.
     * Then throws EntityNotFoundException.
     */
    @Test
    void delete_NonExistingClient_ThrowsEntityNotFoundException() {
        when(repository.findByClientIdAndIsDeletedFalse("nonexistent")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> service.delete("nonexistent", false)
        );

        assertTrue(exception.getMessage().contains("Client access control configuration not found"));
        verify(repository, never()).delete(any());
        verify(repository, never()).save(any());
    }

    /**
     * Given existing client ID.
     * When getById is called.
     * Then returns response DTO.
     */
    @Test
    void getById_ExistingId_ReturnsResponseDto() {
        when(repository.findByClientIdAndIsDeletedFalse("client1")).thenReturn(Optional.of(entity));
        when(mapper.entityToResponseDto(entity)).thenReturn(responseDto);

        ClientAccessControlResponseDto result = service.getById("client1");

        assertNotNull(result);
        assertEquals("client1", result.getClientId());
        verify(repository, times(1)).findByClientIdAndIsDeletedFalse("client1");
    }
}
