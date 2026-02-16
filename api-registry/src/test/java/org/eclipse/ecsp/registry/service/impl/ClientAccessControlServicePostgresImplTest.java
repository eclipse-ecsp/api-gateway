package org.eclipse.ecsp.registry.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlRequestDto;
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlResponseDto;
import org.eclipse.ecsp.registry.common.entity.GatewayClientAccessControl;
import org.eclipse.ecsp.registry.mapper.ClientAccessControlMapper;
import org.eclipse.ecsp.registry.repository.ClientAccessControlRepository;
import org.eclipse.ecsp.registry.service.ClientAccessControlEventPublisher;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ClientAccessControlServicePostgresImpl.
 */
@ExtendWith(MockitoExtension.class)
class ClientAccessControlServicePostgresImplTest {

    private static final int MAX_BULK_SIZE_EXCEEDED = 101;

    @Mock
    private ClientAccessControlRepository repository;

    @Mock
    private ClientAccessControlMapper mapper;

    @Mock
    private ClientAccessControlEventPublisher eventPublisher;

    @InjectMocks
    private ClientAccessControlServicePostgresImpl service;

    private ClientAccessControlRequestDto request;
    private GatewayClientAccessControl entity;
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

        entity = GatewayClientAccessControl.builder()
                .id(1L)
                .clientId("test-client-123")
                .tenant("test-tenant")
                .description("Test client")
                .isActive(true)
                .isDeleted(false)
                .allowRules(List.of("user-service:*", "payment-service:charge"))
                .build();

        response = ClientAccessControlResponseDto.builder()
                .id(1L)
                .clientId("test-client-123")
                .tenant("test-tenant")
                .description("Test client")
                .isActive(true)
                .isDeleted(false)
                .allow(List.of("user-service:*", "payment-service:charge"))
                .build();
    }

    // =====================
    // Bulk Create Tests
    // =====================

    @Test
    void testBulkCreate_Success() {
        when(repository.existsByClientIdAndIsDeletedFalse(anyString())).thenReturn(false);
        when(mapper.requestDtoToEntity(any())).thenReturn(entity);
        when(repository.saveAll(anyList())).thenReturn(List.of(entity));
        when(mapper.entityToResponseDto(any(GatewayClientAccessControl.class))).thenReturn(response);

        List<ClientAccessControlResponseDto> results = service.bulkCreate(List.of(request));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getClientId()).isEqualTo("test-client-123");
        verify(repository).saveAll(anyList());
    }

    @Test
    void testBulkCreate_ExceedsMaxSize() {
        List<ClientAccessControlRequestDto> requests = java.util.stream.IntStream
            .range(0, MAX_BULK_SIZE_EXCEEDED)
            .mapToObj(i -> request)
            .toList();

        assertThatThrownBy(() -> service.bulkCreate(requests))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limited to 100 clients");
    }

    @Test
    void testBulkCreate_DuplicateClientId() {
        when(repository.existsByClientIdAndIsDeletedFalse("test-client-123")).thenReturn(true);

        assertThatThrownBy(() -> service.bulkCreate(List.of(request)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    // =====================
    // Get By ID Tests
    // =====================

    @Test
    void testGetById_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.entityToResponseDto(entity)).thenReturn(response);

        ClientAccessControlResponseDto result = service.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getClientId()).isEqualTo("test-client-123");
    }

    @Test
    void testGetById_NotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not found");
    }

    // =====================
    // Update Tests
    // =====================

    @Test
    void testUpdate_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);
        when(mapper.entityToResponseDto(entity)).thenReturn(response);

        ClientAccessControlResponseDto result = service.update(1L, request);

        assertThat(result.getClientId()).isEqualTo("test-client-123");
        verify(repository).save(any());
    }

    // =====================
    // Delete Tests
    // =====================

    @Test
    void testDelete_SoftDelete() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.delete(1L, false);

        verify(repository).save(argThat(e -> e.getIsDeleted() == true));
        verify(repository, never()).delete(any(GatewayClientAccessControl.class));
    }

    @Test
    void testDelete_PermanentDelete() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.delete(1L, true);

        verify(repository).delete(any(GatewayClientAccessControl.class));
        verify(repository, never()).save(any());
    }

    // =====================
    // Get All Tests
    // =====================

    @Test
    void testGetAll_ActiveOnly() {
        when(repository.findByIsActiveAndIsDeletedFalse(true)).thenReturn(List.of(entity));
        when(mapper.entityToResponseDto(any(GatewayClientAccessControl.class))).thenReturn(response);

        List<ClientAccessControlResponseDto> results = service.getAll(false);

        assertThat(results).hasSize(1);
        verify(repository).findByIsActiveAndIsDeletedFalse(true);
    }

    @Test
    void testGetAll_IncludeInactive() {
        when(repository.findAllNotDeleted()).thenReturn(List.of(entity));
        when(mapper.entityToResponseDto(any(GatewayClientAccessControl.class))).thenReturn(response);

        List<ClientAccessControlResponseDto> results = service.getAll(true);

        assertThat(results).hasSize(1);
        verify(repository).findAllNotDeleted();
    }
}
