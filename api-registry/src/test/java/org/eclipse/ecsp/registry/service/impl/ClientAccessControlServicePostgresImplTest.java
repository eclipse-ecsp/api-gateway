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
import org.eclipse.ecsp.registry.mapper.ClientAccessControlMapper;
import org.eclipse.ecsp.registry.repo.ClientAccessControlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ClientAccessControlServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class ClientAccessControlServicePostgresImplTest {

    @Mock
    private ClientAccessControlRepository repository;

    @Mock
    private ClientAccessControlMapper mapper;

    @Mock
    private EventPublisherContext eventPublisher;

    @InjectMocks
    private ClientAccessControlServiceImpl service;

    private ClientAccessControlRequestDto request;
    private ClientAccessControlEntity entity;
    private ClientAccessControlResponseDto response;

    @BeforeEach
    void setUp() {
        request = ClientAccessControlRequestDto.builder()
                .clientId("test-client-123")
                .tenant("test-tenant")
                .description("Test client")
                .isActive(true)
                .allow(List.of("user-service:*", "payment-service:charge"))
                .build();

        entity = ClientAccessControlEntity.builder()
                .id("test-client-123")
                .clientId("test-client-123")
                .tenant("test-tenant")
                .description("Test client")
                .isActive(true)
                .isDeleted(false)
                .allow(List.of("user-service:*", "payment-service:charge"))
                .build();

        response = ClientAccessControlResponseDto.builder()
                .clientId("test-client-123")
                .tenant("test-tenant")
                .description("Test client")
                .isActive(true)
                .allow(List.of("user-service:*", "payment-service:charge"))
                .build();
    }

    @Test
    void testBulkCreateSuccess() {
        when(repository.existsByClientIdAndIsDeletedFalse(anyString())).thenReturn(false);
        when(repository.findByClientIdAndIsDeletedTrue(anyString())).thenReturn(Optional.empty());
        when(mapper.requestDtoToEntity(any(ClientAccessControlRequestDto.class))).thenReturn(entity);
        when(repository.saveAll(any())).thenReturn(List.of(entity));
        when(mapper.entityToResponseDto(any(ClientAccessControlEntity.class))).thenReturn(response);
        when(eventPublisher.publishEvent(any(ClientAccessControlEventData.class))).thenReturn(true);

        List<ClientAccessControlResponseDto> results = service.bulkCreate(List.of(request));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getClientId()).isEqualTo("test-client-123");
        verify(eventPublisher, times(1)).publishEvent(any(ClientAccessControlEventData.class));
    }

    @Test
    void testGetByIdSuccess() {
        when(repository.findByClientIdAndIsDeletedFalse("test-client-123")).thenReturn(Optional.of(entity));
        when(mapper.entityToResponseDto(entity)).thenReturn(response);

        ClientAccessControlResponseDto result = service.getById("test-client-123");

        assertThat(result.getClientId()).isEqualTo("test-client-123");
    }

    @Test
    void testGetByIdNotFound() {
        when(repository.findByClientIdAndIsDeletedFalse("test-client-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("test-client-123"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void testGetAllActiveOnly() {
        when(repository.findByIsActiveAndIsDeletedFalse(true)).thenReturn(List.of(entity));
        when(mapper.entityToResponseDto(any(ClientAccessControlEntity.class))).thenReturn(response);

        List<ClientAccessControlResponseDto> results = service.getAll(false);

        assertThat(results).hasSize(1);
        verify(repository).findByIsActiveAndIsDeletedFalse(true);
    }

    @Test
    void testGetAllIncludeInactive() {
        when(repository.findByIsDeletedFalse()).thenReturn(List.of(entity));
        when(mapper.entityToResponseDto(any(ClientAccessControlEntity.class))).thenReturn(response);

        List<ClientAccessControlResponseDto> results = service.getAll(true);

        assertThat(results).hasSize(1);
        verify(repository).findByIsDeletedFalse();
    }

    @Test
    void testUpdateSuccess() {
        when(repository.findByClientIdAndIsDeletedFalse("test-client-123")).thenReturn(Optional.of(entity));
        when(repository.save(any(ClientAccessControlEntity.class))).thenReturn(entity);
        when(mapper.entityToResponseDto(entity)).thenReturn(response);
        when(eventPublisher.publishEvent(any(ClientAccessControlEventData.class))).thenReturn(true);

        ClientAccessControlResponseDto result = service.update("test-client-123", request);

        assertThat(result.getClientId()).isEqualTo("test-client-123");
        verify(repository).save(any(ClientAccessControlEntity.class));
        verify(eventPublisher).publishEvent(any(ClientAccessControlEventData.class));
    }

    @Test
    void testDeleteSoftDelete() {
        when(repository.findByClientIdAndIsDeletedFalse("test-client-123")).thenReturn(Optional.of(entity));
        when(eventPublisher.publishEvent(any(ClientAccessControlEventData.class))).thenReturn(true);

        service.delete("test-client-123", false);

        verify(repository).save(any(ClientAccessControlEntity.class));
        verify(repository, never()).delete(any(ClientAccessControlEntity.class));
        verify(eventPublisher).publishEvent(any(ClientAccessControlEventData.class));
    }

    @Test
    void testDeletePermanentDelete() {
        when(repository.findByClientIdAndIsDeletedFalse("test-client-123")).thenReturn(Optional.of(entity));
        when(eventPublisher.publishEvent(any(ClientAccessControlEventData.class))).thenReturn(true);

        service.delete("test-client-123", true);

        verify(repository).delete(any(ClientAccessControlEntity.class));
        verify(repository, never()).save(any(ClientAccessControlEntity.class));
        verify(eventPublisher).publishEvent(any(ClientAccessControlEventData.class));
    }
}
