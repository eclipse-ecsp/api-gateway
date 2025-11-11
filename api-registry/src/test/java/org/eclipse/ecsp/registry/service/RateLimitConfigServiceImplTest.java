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

package org.eclipse.ecsp.registry.service;

import org.eclipse.ecsp.registry.dto.RateLimitConfigDto;
import org.eclipse.ecsp.registry.entity.RateLimitConfigEntity;
import org.eclipse.ecsp.registry.repo.RateLimitConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test class for RateLimitConfigServiceImpl.
 *
 * @author Abhishek Kumar
 */
@ExtendWith(SpringExtension.class)
class RateLimitConfigServiceImplTest {

    private static final int REPLENISH_RATE_100 = 100;
    private static final int BURST_CAPACITY_200 = 200;
    private static final int REPLENISH_RATE_50 = 50;
    private static final int BURST_CAPACITY_100 = 100;
    private static final int REPLENISH_RATE_150 = 150;
    private static final int BURST_CAPACITY_300 = 300;
    private static final int REPLENISH_RATE_200 = 200;
    private static final int BURST_CAPACITY_250 = 250;
    private static final int NEGATIVE_REPLENISH_RATE = -10;
    private static final int NEGATIVE_BURST_CAPACITY = -50;
    private static final int EXPECTED_SIZE_2 = 2;

    private RateLimitConfigServiceImpl rateLimitConfigService;

    @Mock
    private RateLimitConfigRepository rateLimitConfigRepository;

    @Captor
    private ArgumentCaptor<RateLimitConfigEntity> entityCaptor;

    @Captor
    private ArgumentCaptor<List<RateLimitConfigEntity>> entityListCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rateLimitConfigService = new RateLimitConfigServiceImpl(rateLimitConfigRepository);
    }

    // ==================== addOrUpdateRateLimitConfigs Tests ====================

    @Test
    void testAddOrUpdateRateLimitConfigs_Success_WithRouteId() {
        // Arrange
        RateLimitConfigDto dto = createValidRouteDto("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        RateLimitConfigEntity entity = createRouteEntity("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        when(rateLimitConfigRepository.saveAll(anyList())).thenReturn(Collections.singletonList(entity));

        final List<RateLimitConfigDto> dtos = Collections.singletonList(dto);

        // Act
        List<RateLimitConfigDto> result = rateLimitConfigService.addOrUpdateRateLimitConfigs(dtos);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("route1", result.get(0).getRouteId());
        assertEquals(REPLENISH_RATE_100, result.get(0).getReplenishRate());
        assertEquals(BURST_CAPACITY_200, result.get(0).getBurstCapacity());
        verify(rateLimitConfigRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_Success_WithService() {
        // Arrange
        RateLimitConfigDto dto = createValidServiceDto("service1", REPLENISH_RATE_50, BURST_CAPACITY_100);
        RateLimitConfigEntity entity = createServiceEntity("service1", REPLENISH_RATE_50, BURST_CAPACITY_100);
        when(rateLimitConfigRepository.saveAll(anyList())).thenReturn(Collections.singletonList(entity));

        final List<RateLimitConfigDto> dtos = Collections.singletonList(dto);

        // Act
        List<RateLimitConfigDto> result = rateLimitConfigService.addOrUpdateRateLimitConfigs(dtos);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("service1", result.get(0).getService());
        assertEquals(REPLENISH_RATE_50, result.get(0).getReplenishRate());
        verify(rateLimitConfigRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_MultipleConfigs_Success() {
        // Arrange
        RateLimitConfigDto dto1 = createValidRouteDto("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        RateLimitConfigDto dto2 = createValidServiceDto("service1", REPLENISH_RATE_50, BURST_CAPACITY_100);
        RateLimitConfigEntity entity1 = createRouteEntity("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        RateLimitConfigEntity entity2 = createServiceEntity("service1", REPLENISH_RATE_50, BURST_CAPACITY_100);
        when(rateLimitConfigRepository.saveAll(anyList())).thenReturn(Arrays.asList(entity1, entity2));

        final List<RateLimitConfigDto> dtos = Arrays.asList(dto1, dto2);

        // Act
        List<RateLimitConfigDto> result = rateLimitConfigService.addOrUpdateRateLimitConfigs(dtos);

        // Assert
        assertEquals(EXPECTED_SIZE_2, result.size());
        verify(rateLimitConfigRepository, times(1)).saveAll(entityListCaptor.capture());
        assertEquals(EXPECTED_SIZE_2, entityListCaptor.getValue().size());
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_BothRouteIdAndService_ThrowsException() {
        // Arrange
        RateLimitConfigDto dto = new RateLimitConfigDto();
        dto.setRouteId("route1");
        dto.setService("service1");
        dto.setReplenishRate(REPLENISH_RATE_100);
        dto.setBurstCapacity(BURST_CAPACITY_200);
        dto.setRateLimitType(RateLimitConfigDto.RateLimitType.CLIENT_IP);

        final List<RateLimitConfigDto> dtos = Collections.singletonList(dto);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigService.addOrUpdateRateLimitConfigs(dtos));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Either routeId or service should be present, not both"));
        verify(rateLimitConfigRepository, never()).saveAll(anyList());
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_NeitherRouteIdNorService_ThrowsException() {
        // Arrange
        RateLimitConfigDto dto = new RateLimitConfigDto();
        dto.setReplenishRate(REPLENISH_RATE_100);
        dto.setBurstCapacity(BURST_CAPACITY_200);
        dto.setRateLimitType(RateLimitConfigDto.RateLimitType.CLIENT_IP);

        List<RateLimitConfigDto> dtos = Collections.singletonList(dto);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigService.addOrUpdateRateLimitConfigs(dtos));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Either routeId or service should be present"));
        verify(rateLimitConfigRepository, never()).saveAll(anyList());
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_NegativeReplenishRate_ThrowsException() {
        // Arrange
        RateLimitConfigDto dto = createValidRouteDto("route1", NEGATIVE_REPLENISH_RATE, BURST_CAPACITY_200);

        List<RateLimitConfigDto> dtos = Collections.singletonList(dto);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigService.addOrUpdateRateLimitConfigs(dtos));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("must be positive integers"));
        verify(rateLimitConfigRepository, never()).saveAll(anyList());
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_ZeroReplenishRate_ThrowsException() {
        // Arrange
        RateLimitConfigDto dto = createValidRouteDto("route1", 0, BURST_CAPACITY_200);
        List<RateLimitConfigDto> dtos = Collections.singletonList(dto);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigService.addOrUpdateRateLimitConfigs(dtos));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(rateLimitConfigRepository, never()).saveAll(anyList());
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_NegativeBurstCapacity_ThrowsException() {
        // Arrange
        RateLimitConfigDto dto = createValidRouteDto("route1", REPLENISH_RATE_100, NEGATIVE_BURST_CAPACITY);
        List<RateLimitConfigDto> dtos = Collections.singletonList(dto);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigService.addOrUpdateRateLimitConfigs(dtos));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("must be positive integers"));
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_BurstCapacityLessThanReplenishRate_ThrowsException() {
        // Arrange
        RateLimitConfigDto dto = createValidRouteDto("route1", REPLENISH_RATE_200, BURST_CAPACITY_100);
        List<RateLimitConfigDto> dtos = Collections.singletonList(dto);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigService.addOrUpdateRateLimitConfigs(dtos));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Burst capacity must be greater than or equal to replenish rate"));
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_BurstCapacityEqualToReplenishRate_Success() {
        // Arrange
        RateLimitConfigDto dto = createValidRouteDto("route1", REPLENISH_RATE_100, BURST_CAPACITY_100);
        List<RateLimitConfigDto> dtos = Collections.singletonList(dto);

        RateLimitConfigEntity entity = createRouteEntity("route1", REPLENISH_RATE_100, BURST_CAPACITY_100);
        when(rateLimitConfigRepository.saveAll(anyList())).thenReturn(Collections.singletonList(entity));

        // Act
        List<RateLimitConfigDto> result = rateLimitConfigService.addOrUpdateRateLimitConfigs(dtos);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(rateLimitConfigRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_DuplicateRouteIds_ThrowsException() {
        // Arrange
        RateLimitConfigDto dto1 = createValidRouteDto("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        RateLimitConfigDto dto2 = createValidRouteDto("route1", REPLENISH_RATE_150, BURST_CAPACITY_300);
        final List<RateLimitConfigDto> dtos = Arrays.asList(dto1, dto2);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigService.addOrUpdateRateLimitConfigs(dtos));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Duplicate routeId entries found"));
        assertTrue(exception.getReason().contains("route1"));
        verify(rateLimitConfigRepository, never()).saveAll(anyList());
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_DuplicateServices_ThrowsException() {
        // Arrange
        RateLimitConfigDto dto1 = createValidServiceDto("service1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        RateLimitConfigDto dto2 = createValidServiceDto("service1", REPLENISH_RATE_150, BURST_CAPACITY_250);
        List<RateLimitConfigDto> dtos = Arrays.asList(dto1, dto2);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigService.addOrUpdateRateLimitConfigs(dtos));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Duplicate service entries found"));
        assertTrue(exception.getReason().contains("service1"));
        verify(rateLimitConfigRepository, never()).saveAll(anyList());
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_WithHeaderType_Success() {
        // Arrange
        RateLimitConfigDto dto = createValidRouteDto("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        dto.setRateLimitType(RateLimitConfigDto.RateLimitType.HEADER);
        dto.setHeaderName("X-API-Key");
        dto.setIncludeHeaders(true);
        RateLimitConfigEntity entity = createRouteEntity("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        entity.setRateLimitType(RateLimitConfigEntity.RateLimitType.HEADER);
        entity.setHeaderName("X-API-Key");
        entity.setIncludeHeaders(true);
        when(rateLimitConfigRepository.saveAll(anyList())).thenReturn(Collections.singletonList(entity));

        final List<RateLimitConfigDto> dtos = Collections.singletonList(dto);

        // Act
        List<RateLimitConfigDto> result = rateLimitConfigService.addOrUpdateRateLimitConfigs(dtos);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(RateLimitConfigDto.RateLimitType.HEADER, result.get(0).getRateLimitType());
        assertEquals("X-API-Key", result.get(0).getHeaderName());
        assertTrue(result.get(0).isIncludeHeaders());
    }

    // ==================== updateRateLimitConfig Tests ====================

    @Test
    void testUpdateRateLimitConfig_Success_RouteId() {
        // Arrange
        String routeId = "route1";

        RateLimitConfigEntity existingEntity = createRouteEntity(routeId, REPLENISH_RATE_100, BURST_CAPACITY_200);
        RateLimitConfigEntity updatedEntity = createRouteEntity(routeId, REPLENISH_RATE_150, BURST_CAPACITY_300);

        when(rateLimitConfigRepository.findById(routeId)).thenReturn(Optional.of(existingEntity));
        when(rateLimitConfigRepository.save(any(RateLimitConfigEntity.class))).thenReturn(updatedEntity);
        RateLimitConfigDto updateDto = createValidRouteDto(routeId, REPLENISH_RATE_150, BURST_CAPACITY_300);

        // Act
        RateLimitConfigDto result = rateLimitConfigService.updateRateLimitConfig(routeId, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(REPLENISH_RATE_150, result.getReplenishRate());
        assertEquals(BURST_CAPACITY_300, result.getBurstCapacity());
        verify(rateLimitConfigRepository, times(1)).findById(routeId);
        verify(rateLimitConfigRepository, times(1)).save(entityCaptor.capture());
        
        RateLimitConfigEntity capturedEntity = entityCaptor.getValue();
        assertEquals(REPLENISH_RATE_150, capturedEntity.getReplenishRate());
        assertEquals(BURST_CAPACITY_300, capturedEntity.getBurstCapacity());
    }

    @Test
    void testUpdateRateLimitConfig_Success_Service() {
        // Arrange
        String serviceId = "service1";
        RateLimitConfigDto updateDto = createValidServiceDto(serviceId, REPLENISH_RATE_150, BURST_CAPACITY_300);

        RateLimitConfigEntity existingEntity = createServiceEntity(serviceId, REPLENISH_RATE_100, BURST_CAPACITY_200);
        RateLimitConfigEntity updatedEntity = createServiceEntity(serviceId, REPLENISH_RATE_150, BURST_CAPACITY_300);

        when(rateLimitConfigRepository.findById(serviceId)).thenReturn(Optional.of(existingEntity));
        when(rateLimitConfigRepository.save(any(RateLimitConfigEntity.class))).thenReturn(updatedEntity);

        // Act
        RateLimitConfigDto result = rateLimitConfigService.updateRateLimitConfig(serviceId, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(REPLENISH_RATE_150, result.getReplenishRate());
        assertEquals(BURST_CAPACITY_300, result.getBurstCapacity());
        verify(rateLimitConfigRepository, times(1)).save(any(RateLimitConfigEntity.class));
    }

    @Test
    void testUpdateRateLimitConfig_NotFound_ThrowsException() {
        // Arrange
        String id = "nonexistent";
        RateLimitConfigDto updateDto = createValidRouteDto("route1", REPLENISH_RATE_150, BURST_CAPACITY_300);

        when(rateLimitConfigRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigService.updateRateLimitConfig(id, updateDto));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Rate limit configuration not found"));
        verify(rateLimitConfigRepository, never()).save(any());
    }

    @Test
    void testUpdateRateLimitConfig_BlankId_ThrowsException() {
        // Arrange
        String id = "";
        RateLimitConfigDto updateDto = createValidRouteDto("route1", REPLENISH_RATE_150, BURST_CAPACITY_300);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigService.updateRateLimitConfig(id, updateDto));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Id cannot be blank"));
        verify(rateLimitConfigRepository, never()).findById(any());
    }

    @Test
    void testUpdateRateLimitConfig_NullId_ThrowsException() {
        // Arrange
        RateLimitConfigDto updateDto = createValidRouteDto("route1", REPLENISH_RATE_150, BURST_CAPACITY_300);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigService.updateRateLimitConfig(null, updateDto));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(rateLimitConfigRepository, never()).findById(any());
    }

    @Test
    void testUpdateRateLimitConfig_InvalidData_BothRouteIdAndService_ThrowsException() {
        // Arrange
        String id = "route1";
        RateLimitConfigDto updateDto = new RateLimitConfigDto();
        updateDto.setRouteId("route1");
        updateDto.setService("service1");
        updateDto.setReplenishRate(REPLENISH_RATE_100);
        updateDto.setBurstCapacity(BURST_CAPACITY_200);
        updateDto.setRateLimitType(RateLimitConfigDto.RateLimitType.CLIENT_IP);

        RateLimitConfigEntity existingEntity = createRouteEntity(id, REPLENISH_RATE_100, BURST_CAPACITY_200);
        when(rateLimitConfigRepository.findById(id)).thenReturn(Optional.of(existingEntity));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigService.updateRateLimitConfig(id, updateDto));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(rateLimitConfigRepository, never()).save(any());
    }

    @Test
    void testUpdateRateLimitConfig_InvalidReplenishRate_ThrowsException() {
        // Arrange
        String id = "route1";
        RateLimitConfigDto updateDto = createValidRouteDto("route1", NEGATIVE_REPLENISH_RATE, BURST_CAPACITY_200);

        RateLimitConfigEntity existingEntity = createRouteEntity(id, REPLENISH_RATE_100, BURST_CAPACITY_200);
        when(rateLimitConfigRepository.findById(id)).thenReturn(Optional.of(existingEntity));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigService.updateRateLimitConfig(id, updateDto));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(rateLimitConfigRepository, never()).save(any());
    }

    // ==================== getRateLimitConfigs Tests ====================

    @Test
    void testGetRateLimitConfigs_Success() {
        // Arrange
        RateLimitConfigEntity entity1 = createRouteEntity("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        RateLimitConfigEntity entity2 = createServiceEntity("service1", REPLENISH_RATE_50, BURST_CAPACITY_100);
        List<RateLimitConfigEntity> entities = Arrays.asList(entity1, entity2);

        when(rateLimitConfigRepository.findAll()).thenReturn(entities);

        // Act
        List<RateLimitConfigDto> result = rateLimitConfigService.getRateLimitConfigs();

        // Assert
        assertNotNull(result);
        assertEquals(EXPECTED_SIZE_2, result.size());
        verify(rateLimitConfigRepository, times(1)).findAll();
    }

    @Test
    void testGetRateLimitConfigs_EmptyList_ReturnsEmptyList() {
        // Arrange
        when(rateLimitConfigRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<RateLimitConfigDto> result = rateLimitConfigService.getRateLimitConfigs();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(rateLimitConfigRepository, times(1)).findAll();
    }

    @Test
    void testGetRateLimitConfigs_ConvertsEntityToDto_Correctly() {
        // Arrange
        RateLimitConfigEntity entity = createRouteEntity("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        entity.setIncludeHeaders(true);
        entity.setRateLimitType(RateLimitConfigEntity.RateLimitType.HEADER);
        entity.setHeaderName("X-Custom-Header");

        when(rateLimitConfigRepository.findAll()).thenReturn(Collections.singletonList(entity));

        // Act
        List<RateLimitConfigDto> result = rateLimitConfigService.getRateLimitConfigs();

        // Assert
        assertEquals(1, result.size());
        RateLimitConfigDto dto = result.get(0);
        assertEquals("route1", dto.getRouteId());
        assertEquals(REPLENISH_RATE_100, dto.getReplenishRate());
        assertEquals(BURST_CAPACITY_200, dto.getBurstCapacity());
        assertTrue(dto.isIncludeHeaders());
        assertEquals(RateLimitConfigDto.RateLimitType.HEADER, dto.getRateLimitType());
        assertEquals("X-Custom-Header", dto.getHeaderName());
    }

    // ==================== deleteRateLimitConfig Tests ====================

    @Test
    void testDeleteRateLimitConfig_Success() {
        // Arrange
        String id = "route1";
        RateLimitConfigEntity entity = createRouteEntity(id, REPLENISH_RATE_100, BURST_CAPACITY_200);

        when(rateLimitConfigRepository.findById(id)).thenReturn(Optional.of(entity));
        doNothing().when(rateLimitConfigRepository).delete(entity);

        // Act
        rateLimitConfigService.deleteRateLimitConfig(id);

        // Assert
        verify(rateLimitConfigRepository, times(1)).findById(id);
        verify(rateLimitConfigRepository, times(1)).delete(entity);
    }

    @Test
    void testDeleteRateLimitConfig_NotFound_ThrowsException() {
        // Arrange
        String id = "nonexistent";
        when(rateLimitConfigRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigService.deleteRateLimitConfig(id));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Rate limit configuration not found"));
        verify(rateLimitConfigRepository, never()).delete(any());
    }

    // ==================== Helper Methods ====================

    private RateLimitConfigDto createValidRouteDto(String routeId, long replenishRate, long burstCapacity) {
        RateLimitConfigDto dto = new RateLimitConfigDto();
        dto.setRouteId(routeId);
        dto.setReplenishRate(replenishRate);
        dto.setBurstCapacity(burstCapacity);
        dto.setIncludeHeaders(false);
        dto.setRateLimitType(RateLimitConfigDto.RateLimitType.CLIENT_IP);
        return dto;
    }

    private RateLimitConfigDto createValidServiceDto(String service, long replenishRate, long burstCapacity) {
        RateLimitConfigDto dto = new RateLimitConfigDto();
        dto.setService(service);
        dto.setReplenishRate(replenishRate);
        dto.setBurstCapacity(burstCapacity);
        dto.setIncludeHeaders(false);
        dto.setRateLimitType(RateLimitConfigDto.RateLimitType.CLIENT_IP);
        return dto;
    }

    private RateLimitConfigEntity createRouteEntity(String routeId, long replenishRate, long burstCapacity) {
        RateLimitConfigEntity entity = new RateLimitConfigEntity();
        entity.setId(routeId);
        entity.setRouteId(routeId);
        entity.setReplenishRate(replenishRate);
        entity.setBurstCapacity(burstCapacity);
        entity.setIncludeHeaders(false);
        entity.setRateLimitType(RateLimitConfigEntity.RateLimitType.CLIENT_IP);
        return entity;
    }

    private RateLimitConfigEntity createServiceEntity(String service, long replenishRate, long burstCapacity) {
        RateLimitConfigEntity entity = new RateLimitConfigEntity();
        entity.setId(service);
        entity.setService(service);
        entity.setReplenishRate(replenishRate);
        entity.setBurstCapacity(burstCapacity);
        entity.setIncludeHeaders(false);
        entity.setRateLimitType(RateLimitConfigEntity.RateLimitType.CLIENT_IP);
        return entity;
    }
}
