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

package org.eclipse.ecsp.registry.rest;

import org.eclipse.ecsp.registry.dto.GenericResponseDto;
import org.eclipse.ecsp.registry.dto.RateLimitConfigDto;
import org.eclipse.ecsp.registry.service.RateLimitConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test class for RateLimitConfigController.
 *
 * @author Abhishek Kumar
 */
@ExtendWith(SpringExtension.class)
class RateLimitConfigControllerTest {

    private static final int REPLENISH_RATE_100 = 100;
    private static final int BURST_CAPACITY_200 = 200;
    private static final int REPLENISH_RATE_50 = 50;
    private static final int BURST_CAPACITY_100 = 100;
    private static final int REPLENISH_RATE_150 = 150;
    private static final int BURST_CAPACITY_300 = 300;
    private static final int REPLENISH_RATE_75 = 75;
    private static final int BURST_CAPACITY_150 = 150;
    private static final int BURST_CAPACITY_250 = 250;
    private static final int NEGATIVE_REPLENISH_RATE = -10;
    private static final int EXPECTED_SIZE_2 = 2;

    private RateLimitConfigController rateLimitConfigController;

    @Mock
    private RateLimitConfigService rateLimitConfigService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rateLimitConfigController = new RateLimitConfigController(rateLimitConfigService);
    }

    // ==================== getRateLimitConfigs Tests ====================

    @Test
    void testGetRateLimitConfigs_Success_ReturnsConfigs() {
        // Arrange
        RateLimitConfigDto dto1 = createRouteDto("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        RateLimitConfigDto dto2 = createServiceDto("service1", REPLENISH_RATE_50, BURST_CAPACITY_100);
        List<RateLimitConfigDto> expectedConfigs = Arrays.asList(dto1, dto2);

        when(rateLimitConfigService.getRateLimitConfigs()).thenReturn(expectedConfigs);

        // Act
        List<RateLimitConfigDto> result = rateLimitConfigController.getRateLimitConfigs();

        // Assert
        assertNotNull(result);
        assertEquals(EXPECTED_SIZE_2, result.size());
        assertEquals("route1", result.get(0).getRouteId());
        assertEquals("service1", result.get(1).getService());
        verify(rateLimitConfigService, times(1)).getRateLimitConfigs();
    }

    @Test
    void testGetRateLimitConfigs_EmptyList_ReturnsEmptyList() {
        // Arrange
        when(rateLimitConfigService.getRateLimitConfigs()).thenReturn(Collections.emptyList());

        // Act
        List<RateLimitConfigDto> result = rateLimitConfigController.getRateLimitConfigs();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(rateLimitConfigService, times(1)).getRateLimitConfigs();
    }

    @Test
    void testGetRateLimitConfigs_ServiceThrowsException_PropagatesException() {
        // Arrange
        when(rateLimitConfigService.getRateLimitConfigs())
                .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> rateLimitConfigController.getRateLimitConfigs());
        verify(rateLimitConfigService, times(1)).getRateLimitConfigs();
    }

    // ==================== addOrUpdateRateLimitConfigs Tests ====================

    @Test
    void testAddOrUpdateRateLimitConfigs_Success_SingleConfig() {
        // Arrange
        RateLimitConfigDto inputDto = createRouteDto("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        List<RateLimitConfigDto> inputConfigs = Collections.singletonList(inputDto);

        RateLimitConfigDto outputDto = createRouteDto("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        List<RateLimitConfigDto> expectedOutput = Collections.singletonList(outputDto);

        when(rateLimitConfigService.addOrUpdateRateLimitConfigs(anyList())).thenReturn(expectedOutput);

        // Act
        List<RateLimitConfigDto> result = rateLimitConfigController.addOrUpdateRateLimitConfigs(inputConfigs);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("route1", result.get(0).getRouteId());
        assertEquals(REPLENISH_RATE_100, result.get(0).getReplenishRate());
        verify(rateLimitConfigService, times(1)).addOrUpdateRateLimitConfigs(inputConfigs);
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_Success_MultipleConfigs() {
        // Arrange
        RateLimitConfigDto dto1 = createRouteDto("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        RateLimitConfigDto dto2 = createServiceDto("service1", REPLENISH_RATE_50, BURST_CAPACITY_100);
        List<RateLimitConfigDto> inputConfigs = Arrays.asList(dto1, dto2);

        when(rateLimitConfigService.addOrUpdateRateLimitConfigs(anyList())).thenReturn(inputConfigs);

        // Act
        List<RateLimitConfigDto> result = rateLimitConfigController.addOrUpdateRateLimitConfigs(inputConfigs);

        // Assert
        assertNotNull(result);
        assertEquals(EXPECTED_SIZE_2, result.size());
        verify(rateLimitConfigService, times(1)).addOrUpdateRateLimitConfigs(inputConfigs);
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_InvalidData_ThrowsException() {
        // Arrange
        RateLimitConfigDto invalidDto = new RateLimitConfigDto();
        invalidDto.setRouteId("route1");
        invalidDto.setService("service1"); // Both set - invalid
        invalidDto.setReplenishRate(REPLENISH_RATE_100);
        invalidDto.setBurstCapacity(BURST_CAPACITY_200);
        List<RateLimitConfigDto> configs = Collections.singletonList(invalidDto);

        when(rateLimitConfigService.addOrUpdateRateLimitConfigs(anyList()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                        "Either routeId or service should be present, not both"));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigController.addOrUpdateRateLimitConfigs(configs));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(rateLimitConfigService, times(1)).addOrUpdateRateLimitConfigs(configs);
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_EmptyList_Success() {
        // Arrange
        List<RateLimitConfigDto> emptyList = Collections.emptyList();
        when(rateLimitConfigService.addOrUpdateRateLimitConfigs(emptyList)).thenReturn(emptyList);

        // Act
        List<RateLimitConfigDto> result = rateLimitConfigController.addOrUpdateRateLimitConfigs(emptyList);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(rateLimitConfigService, times(1)).addOrUpdateRateLimitConfigs(emptyList);
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_DuplicateRouteIds_ThrowsException() {
        // Arrange
        RateLimitConfigDto dto1 = createRouteDto("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        RateLimitConfigDto dto2 = createRouteDto("route1", REPLENISH_RATE_150, BURST_CAPACITY_250);
        List<RateLimitConfigDto> configs = Arrays.asList(dto1, dto2);

        when(rateLimitConfigService.addOrUpdateRateLimitConfigs(anyList()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                        "Duplicate routeId entries found: route1"));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigController.addOrUpdateRateLimitConfigs(configs));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Duplicate routeId"));
    }

    // ==================== updateRateLimitConfig Tests ====================

    @Test
    void testUpdateRateLimitConfig_Success() {
        // Arrange
        String id = "route1";
        RateLimitConfigDto inputDto = createRouteDto("route1", REPLENISH_RATE_150, BURST_CAPACITY_300);
        RateLimitConfigDto outputDto = createRouteDto("route1", REPLENISH_RATE_150, BURST_CAPACITY_300);

        when(rateLimitConfigService.updateRateLimitConfig(eq(id), any(RateLimitConfigDto.class)))
                .thenReturn(outputDto);

        // Act
        RateLimitConfigDto result = rateLimitConfigController.updateRateLimitConfig(id, inputDto);

        // Assert
        assertNotNull(result);
        assertEquals("route1", result.getRouteId());
        assertEquals(REPLENISH_RATE_150, result.getReplenishRate());
        assertEquals(BURST_CAPACITY_300, result.getBurstCapacity());
        verify(rateLimitConfigService, times(1)).updateRateLimitConfig(id, inputDto);
    }

    @Test
    void testUpdateRateLimitConfig_NotFound_ThrowsException() {
        // Arrange
        String id = "nonexistent";
        RateLimitConfigDto updateDto = createRouteDto("route1", REPLENISH_RATE_150, BURST_CAPACITY_300);

        when(rateLimitConfigService.updateRateLimitConfig(eq(id), any(RateLimitConfigDto.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Rate limit configuration not found for id: " + id));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigController.updateRateLimitConfig(id, updateDto));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("not found"));
        verify(rateLimitConfigService, times(1)).updateRateLimitConfig(id, updateDto);
    }

    @Test
    void testUpdateRateLimitConfig_InvalidData_ThrowsException() {
        // Arrange
        String id = "route1";
        RateLimitConfigDto invalidDto = createRouteDto("route1", NEGATIVE_REPLENISH_RATE, BURST_CAPACITY_200);

        when(rateLimitConfigService.updateRateLimitConfig(eq(id), any(RateLimitConfigDto.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                        "Replenish rate and burst capacity must be positive integers"));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigController.updateRateLimitConfig(id, invalidDto));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(rateLimitConfigService, times(1)).updateRateLimitConfig(id, invalidDto);
    }

    @Test
    void testUpdateRateLimitConfig_ServiceConfig_Success() {
        // Arrange
        String id = "service1";
        RateLimitConfigDto inputDto = createServiceDto("service1", REPLENISH_RATE_75, BURST_CAPACITY_150);
        RateLimitConfigDto outputDto = createServiceDto("service1", REPLENISH_RATE_75, BURST_CAPACITY_150);

        when(rateLimitConfigService.updateRateLimitConfig(eq(id), any(RateLimitConfigDto.class)))
                .thenReturn(outputDto);

        // Act
        RateLimitConfigDto result = rateLimitConfigController.updateRateLimitConfig(id, inputDto);

        // Assert
        assertNotNull(result);
        assertEquals("service1", result.getService());
        assertEquals(REPLENISH_RATE_75, result.getReplenishRate());
        verify(rateLimitConfigService, times(1)).updateRateLimitConfig(id, inputDto);
    }

    @Test
    void testUpdateRateLimitConfig_WithHeaderType_Success() {
        // Arrange
        RateLimitConfigDto inputDto = createRouteDto("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        inputDto.setKeyResolver("HEADER");
        inputDto.setArgs(Map.of("headerName", "X-API-Key"));
        inputDto.setIncludeHeaders(true);

        RateLimitConfigDto outputDto = createRouteDto("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        outputDto.setKeyResolver("HEADER");
        outputDto.setArgs(Map.of("headerName", "X-API-Key"));
        outputDto.setIncludeHeaders(true);

        when(rateLimitConfigService.updateRateLimitConfig(eq("route1"), any(RateLimitConfigDto.class)))
                .thenReturn(outputDto);

        // Act
        RateLimitConfigDto result = rateLimitConfigController.updateRateLimitConfig("route1", inputDto);

        // Assert
        assertNotNull(result);
        assertEquals("HEADER", result.getKeyResolver());
        assertEquals("X-API-Key", result.getArgs().get("headerName"));
        assertTrue(result.isIncludeHeaders());
    }

    // ==================== deleteRateLimitConfig Tests ====================

    @Test
    void testDeleteRateLimitConfig_Success() {
        // Arrange
        String id = "route1";
        doNothing().when(rateLimitConfigService).deleteRateLimitConfig(id);

        // Act
        ResponseEntity<GenericResponseDto> response = rateLimitConfigController.deleteRateLimitConfig(id);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage() != null && !response.getBody().getMessage().isEmpty());
        assertEquals("Rate limit configuration deleted successfully", response.getBody().getMessage());
        verify(rateLimitConfigService, times(1)).deleteRateLimitConfig(id);
    }

    @Test
    void testDeleteRateLimitConfig_NotFound_ThrowsException() {
        // Arrange
        String id = "nonexistent";
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Rate limit configuration not found for id: " + id))
                .when(rateLimitConfigService).deleteRateLimitConfig(id);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> rateLimitConfigController.deleteRateLimitConfig(id));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("not found"));
        verify(rateLimitConfigService, times(1)).deleteRateLimitConfig(id);
    }

    @Test
    void testDeleteRateLimitConfig_ServiceConfig_Success() {
        // Arrange
        String id = "service1";
        doNothing().when(rateLimitConfigService).deleteRateLimitConfig(id);

        // Act
        ResponseEntity<GenericResponseDto> response = rateLimitConfigController.deleteRateLimitConfig(id);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(rateLimitConfigService, times(1)).deleteRateLimitConfig(id);
    }

    @Test
    void testDeleteRateLimitConfig_DatabaseError_PropagatesException() {
        // Arrange
        doThrow(new RuntimeException("Database connection error"))
                .when(rateLimitConfigService).deleteRateLimitConfig("route1");

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> rateLimitConfigController.deleteRateLimitConfig("route1"));
        verify(rateLimitConfigService, times(1)).deleteRateLimitConfig("route1");
    }

    // ==================== Helper Methods ====================

    private RateLimitConfigDto createRouteDto(String routeId, long replenishRate, long burstCapacity) {
        RateLimitConfigDto dto = new RateLimitConfigDto();
        dto.setRouteId(routeId);
        dto.setReplenishRate(replenishRate);
        dto.setBurstCapacity(burstCapacity);
        dto.setIncludeHeaders(false);
        dto.setKeyResolver("CLIENT_IP");
        return dto;
    }

    private RateLimitConfigDto createServiceDto(String service, long replenishRate, long burstCapacity) {
        RateLimitConfigDto dto = new RateLimitConfigDto();
        dto.setService(service);
        dto.setReplenishRate(replenishRate);
        dto.setBurstCapacity(burstCapacity);
        dto.setIncludeHeaders(false);
        dto.setKeyResolver("CLIENT_IP");
        return dto;
    }
}
