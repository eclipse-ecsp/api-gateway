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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.registry.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.ecsp.registry.dto.RateLimitConfigDto;
import org.eclipse.ecsp.registry.entity.RateLimitConfigEntity;
import org.eclipse.ecsp.registry.repo.RateLimitConfigRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test class for RateLimitConfigController.
 * Tests the full request/response cycle with actual database interactions.
 *
 * @author Abhishek Kumar
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractRateLimitConfigControllerIntegrationTest {

    private static final int REPLENISH_RATE_100 = 100;
    private static final int BURST_CAPACITY_200 = 200;
    private static final int REPLENISH_RATE_50 = 50;
    private static final int BURST_CAPACITY_100 = 100;
    private static final int REPLENISH_RATE_75 = 75;
    private static final int BURST_CAPACITY_150 = 150;
    private static final int REPLENISH_RATE_150 = 150;
    private static final int BURST_CAPACITY_300 = 300;
    private static final int NEGATIVE_REPLENISH_RATE = -10;
    private static final int EXPECTED_SIZE_0 = 0;
    private static final int EXPECTED_SIZE_1 = 1;
    private static final int EXPECTED_SIZE_2 = 2;
    private static final int EXPECTED_SIZE_3 = 3;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitConfigRepository rateLimitConfigRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        clearDatabase();
    }

    @AfterEach
    void tearDown() {
        // Clean up database after each test
        clearDatabase();
    }

    private void clearDatabase() {
        try {
            List<RateLimitConfigEntity> allEntities = rateLimitConfigRepository.findAll();
            for (RateLimitConfigEntity entity : allEntities) {
                rateLimitConfigRepository.delete(entity);
            }
        } catch (Exception e) {
            // Ignore if repository is not available
        }
    }

    // ==================== GET /v1/config/rate-limits Tests ====================

    @Test
    void testGetRateLimitConfigs_EmptyDatabase_ReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/v1/config/rate-limits")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(EXPECTED_SIZE_0)));
    }

    @Test
    void testGetRateLimitConfigs_WithData_ReturnsConfigs() throws Exception {
        // Arrange - Insert test data
        createAndSaveRouteEntity("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        createAndSaveServiceEntity("service1", REPLENISH_RATE_50, BURST_CAPACITY_100);

        // Act & Assert
        mockMvc.perform(get("/v1/config/rate-limits")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(EXPECTED_SIZE_2)))
                .andExpect(jsonPath("$[*].routeId", hasItem("route1")))
                .andExpect(jsonPath("$[*].service", hasItem("service1")));
    }

    // ==================== POST /v1/config/rate-limits Tests ====================

    @Test
    void testAddOrUpdateRateLimitConfigs_SingleRouteConfig_Success() throws Exception {
        // Arrange
        RateLimitConfigDto dto = createRouteDto("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        List<RateLimitConfigDto> configs = Arrays.asList(dto);
        String requestBody = objectMapper.writeValueAsString(configs);

        // Act & Assert
        mockMvc.perform(post("/v1/config/rate-limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(EXPECTED_SIZE_1)))
                .andExpect(jsonPath("$[0].routeId", is("route1")))
                .andExpect(jsonPath("$[0].replenishRate", is(REPLENISH_RATE_100)))
                .andExpect(jsonPath("$[0].burstCapacity", is(BURST_CAPACITY_200)));

        // Verify database
        Optional<RateLimitConfigEntity> savedEntity = rateLimitConfigRepository.findById("route1");
        assertTrue(savedEntity.isPresent());
        assertEquals(REPLENISH_RATE_100, savedEntity.get().getReplenishRate());
        assertEquals(BURST_CAPACITY_200, savedEntity.get().getBurstCapacity());
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_SingleServiceConfig_Success() throws Exception {
        // Arrange
        RateLimitConfigDto dto = createServiceDto("service1", REPLENISH_RATE_50, BURST_CAPACITY_100);
        List<RateLimitConfigDto> configs = Arrays.asList(dto);
        String requestBody = objectMapper.writeValueAsString(configs);

        // Act & Assert
        mockMvc.perform(post("/v1/config/rate-limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(EXPECTED_SIZE_1)))
                .andExpect(jsonPath("$[0].service", is("service1")))
                .andExpect(jsonPath("$[0].replenishRate", is(REPLENISH_RATE_50)));

        // Verify database
        Optional<RateLimitConfigEntity> savedEntity = rateLimitConfigRepository.findById("service1");
        assertTrue(savedEntity.isPresent());
        assertEquals("service1", savedEntity.get().getService());
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_MultipleConfigs_Success() throws Exception {
        // Arrange
        RateLimitConfigDto dto1 = createRouteDto("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        RateLimitConfigDto dto2 = createServiceDto("service1", REPLENISH_RATE_50, BURST_CAPACITY_100);
        RateLimitConfigDto dto3 = createRouteDto("route2", REPLENISH_RATE_75, BURST_CAPACITY_150);
        List<RateLimitConfigDto> configs = Arrays.asList(dto1, dto2, dto3);
        String requestBody = objectMapper.writeValueAsString(configs);

        // Act & Assert
        mockMvc.perform(post("/v1/config/rate-limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(EXPECTED_SIZE_3)));

        // Verify database
        List<RateLimitConfigEntity> allConfigs = rateLimitConfigRepository.findAll();
        assertEquals(EXPECTED_SIZE_3, allConfigs.size());
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_BothRouteIdAndService_Returns400() throws Exception {
        // Arrange
        RateLimitConfigDto dto = new RateLimitConfigDto();
        dto.setRouteId("route1");
        dto.setService("service1");
        dto.setReplenishRate(REPLENISH_RATE_100);
        dto.setBurstCapacity(BURST_CAPACITY_200);
        dto.setRateLimitType(RateLimitConfigDto.RateLimitType.CLIENT_IP);

        List<RateLimitConfigDto> configs = Arrays.asList(dto);
        String requestBody = objectMapper.writeValueAsString(configs);

        // Act & Assert
        mockMvc.perform(post("/v1/config/rate-limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        // Verify database is empty
        List<RateLimitConfigEntity> allConfigs = rateLimitConfigRepository.findAll();
        assertEquals(EXPECTED_SIZE_0, allConfigs.size());
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_NegativeReplenishRate_Returns400() throws Exception {
        // Arrange
        RateLimitConfigDto dto = createRouteDto("route1", NEGATIVE_REPLENISH_RATE, BURST_CAPACITY_200);
        List<RateLimitConfigDto> configs = Arrays.asList(dto);
        String requestBody = objectMapper.writeValueAsString(configs);

        // Act & Assert
        mockMvc.perform(post("/v1/config/rate-limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_BurstLessThanReplenish_Returns400() throws Exception {
        // Arrange
        RateLimitConfigDto dto = createRouteDto("route1", BURST_CAPACITY_200, REPLENISH_RATE_100);
        List<RateLimitConfigDto> configs = Arrays.asList(dto);
        String requestBody = objectMapper.writeValueAsString(configs);

        // Act & Assert
        mockMvc.perform(post("/v1/config/rate-limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_DuplicateRouteIds_Returns400() throws Exception {
        // Arrange
        RateLimitConfigDto dto1 = createRouteDto("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        RateLimitConfigDto dto2 = createRouteDto("route1", REPLENISH_RATE_150, BURST_CAPACITY_300);
        List<RateLimitConfigDto> configs = Arrays.asList(dto1, dto2);
        String requestBody = objectMapper.writeValueAsString(configs);

        // Act & Assert
        mockMvc.perform(post("/v1/config/rate-limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddOrUpdateRateLimitConfigs_UpdateExisting_Success() throws Exception {
        // Arrange - Create initial config
        createAndSaveRouteEntity("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);

        // Update with new values
        RateLimitConfigDto updateDto = createRouteDto("route1", REPLENISH_RATE_150, BURST_CAPACITY_300);
        List<RateLimitConfigDto> configs = Arrays.asList(updateDto);
        String requestBody = objectMapper.writeValueAsString(configs);

        // Act & Assert
        mockMvc.perform(post("/v1/config/rate-limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].replenishRate", is(REPLENISH_RATE_150)))
                .andExpect(jsonPath("$[0].burstCapacity", is(BURST_CAPACITY_300)));

        // Verify database
        Optional<RateLimitConfigEntity> updatedEntity = rateLimitConfigRepository.findById("route1");
        assertTrue(updatedEntity.isPresent());
        assertEquals(REPLENISH_RATE_150, updatedEntity.get().getReplenishRate());
    }

    // ==================== PUT /v1/config/rate-limits/{id} Tests ====================

    @Test
    void testUpdateRateLimitConfig_ExistingRoute_Success() throws Exception {
        // Arrange
        createAndSaveRouteEntity("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);

        RateLimitConfigDto updateDto = createRouteDto("route1", REPLENISH_RATE_150, BURST_CAPACITY_300);
        String requestBody = objectMapper.writeValueAsString(updateDto);

        // Act & Assert
        mockMvc.perform(put("/v1/config/rate-limits/route1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId", is("route1")))
                .andExpect(jsonPath("$.replenishRate", is(REPLENISH_RATE_150)))
                .andExpect(jsonPath("$.burstCapacity", is(BURST_CAPACITY_300)));

        // Verify database
        Optional<RateLimitConfigEntity> updatedEntity = rateLimitConfigRepository.findById("route1");
        assertTrue(updatedEntity.isPresent());
        assertEquals(REPLENISH_RATE_150, updatedEntity.get().getReplenishRate());
    }

    @Test
    void testUpdateRateLimitConfig_ExistingService_Success() throws Exception {
        // Arrange
        createAndSaveServiceEntity("service1", REPLENISH_RATE_50, BURST_CAPACITY_100);

        RateLimitConfigDto updateDto = createServiceDto("service1", REPLENISH_RATE_75, BURST_CAPACITY_150);
        String requestBody = objectMapper.writeValueAsString(updateDto);

        // Act & Assert
        mockMvc.perform(put("/v1/config/rate-limits/service1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service", is("service1")))
                .andExpect(jsonPath("$.replenishRate", is(REPLENISH_RATE_75)));
    }

    @Test
    void testUpdateRateLimitConfig_NonExistent_Returns404() throws Exception {
        // Arrange
        RateLimitConfigDto updateDto = createRouteDto("nonexistent", REPLENISH_RATE_100, BURST_CAPACITY_200);
        String requestBody = objectMapper.writeValueAsString(updateDto);

        // Act & Assert
        mockMvc.perform(put("/v1/config/rate-limits/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateRateLimitConfig_InvalidData_Returns400() throws Exception {
        // Arrange
        createAndSaveRouteEntity("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);

        RateLimitConfigDto updateDto = createRouteDto("route1", NEGATIVE_REPLENISH_RATE, BURST_CAPACITY_200);
        String requestBody = objectMapper.writeValueAsString(updateDto);

        // Act & Assert
        mockMvc.perform(put("/v1/config/rate-limits/route1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateRateLimitConfig_WithHeaderType_Success() throws Exception {
        // Arrange
        createAndSaveRouteEntity("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);

        RateLimitConfigDto updateDto = createRouteDto("route1", REPLENISH_RATE_150, BURST_CAPACITY_300);
        updateDto.setRateLimitType(RateLimitConfigDto.RateLimitType.HEADER);
        updateDto.setHeaderName("X-API-Key");
        updateDto.setIncludeHeaders(true);
        String requestBody = objectMapper.writeValueAsString(updateDto);

        // Act & Assert
        mockMvc.perform(put("/v1/config/rate-limits/route1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rateLimitType", is("HEADER")))
                .andExpect(jsonPath("$.headerName", is("X-API-Key")))
                .andExpect(jsonPath("$.includeHeaders", is(true)));

        // Verify database
        Optional<RateLimitConfigEntity> updatedEntity = rateLimitConfigRepository.findById("route1");
        assertTrue(updatedEntity.isPresent());
        assertEquals(RateLimitConfigEntity.RateLimitType.HEADER, updatedEntity.get().getRateLimitType());
        assertEquals("X-API-Key", updatedEntity.get().getHeaderName());
    }

    // ==================== DELETE /v1/config/rate-limits/{id} Tests ====================

    @Test
    void testDeleteRateLimitConfig_ExistingRoute_Success() throws Exception {
        // Arrange
        createAndSaveRouteEntity("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);

        // Act & Assert
        mockMvc.perform(delete("/v1/config/rate-limits/route1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Rate limit configuration deleted successfully")));

        // Verify database
        Optional<RateLimitConfigEntity> deletedEntity = rateLimitConfigRepository.findById("route1");
        assertFalse(deletedEntity.isPresent());
    }

    @Test
    void testDeleteRateLimitConfig_ExistingService_Success() throws Exception {
        // Arrange
        createAndSaveServiceEntity("service1", REPLENISH_RATE_50, BURST_CAPACITY_100);

        // Act & Assert
        mockMvc.perform(delete("/v1/config/rate-limits/service1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify database
        Optional<RateLimitConfigEntity> deletedEntity = rateLimitConfigRepository.findById("service1");
        assertFalse(deletedEntity.isPresent());
    }

    @Test
    void testDeleteRateLimitConfig_NonExistent_Returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/v1/config/rate-limits/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteRateLimitConfig_VerifyOthersNotAffected() throws Exception {
        // Arrange
        createAndSaveRouteEntity("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        createAndSaveRouteEntity("route2", REPLENISH_RATE_150, BURST_CAPACITY_300);
        createAndSaveServiceEntity("service1", REPLENISH_RATE_50, BURST_CAPACITY_100);

        // Act - Delete route1
        mockMvc.perform(delete("/v1/config/rate-limits/route1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        // Verify - route1 is deleted but others remain
        Optional<RateLimitConfigEntity> deleted = rateLimitConfigRepository.findById("route1");
        assertFalse(deleted.isPresent());

        Optional<RateLimitConfigEntity> route2 = rateLimitConfigRepository.findById("route2");
        assertTrue(route2.isPresent());

        Optional<RateLimitConfigEntity> service1 = rateLimitConfigRepository.findById("service1");
        assertTrue(service1.isPresent());
    }

    // ==================== End-to-End Workflow Tests ====================

    @Test
    void testCompleteWorkflow_CreateUpdateDelete() throws Exception {
        // 1. Create config
        RateLimitConfigDto createDto = createRouteDto("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        String createBody = objectMapper.writeValueAsString(Arrays.asList(createDto));

        mockMvc.perform(post("/v1/config/rate-limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk());

        // 2. Get all configs - should have 1
        mockMvc.perform(get("/v1/config/rate-limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(EXPECTED_SIZE_1)));

        // 3. Update config
        RateLimitConfigDto updateDto = createRouteDto("route1", REPLENISH_RATE_150, BURST_CAPACITY_300);
        String updateBody = objectMapper.writeValueAsString(updateDto);

        mockMvc.perform(put("/v1/config/rate-limits/route1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replenishRate", is(REPLENISH_RATE_150)));

        // 4. Get all configs - should still have 1 with updated values
        mockMvc.perform(get("/v1/config/rate-limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(EXPECTED_SIZE_1)))
                .andExpect(jsonPath("$[0].replenishRate", is(REPLENISH_RATE_150)));

        // 5. Delete config
        mockMvc.perform(delete("/v1/config/rate-limits/route1"))
                .andExpect(status().isOk());

        // 6. Get all configs - should be empty
        mockMvc.perform(get("/v1/config/rate-limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(EXPECTED_SIZE_0)));
    }

    @Test
    void testWorkflow_MultipleRateLimitTypes() throws Exception {
        // Create configs with different rate limit types
        RateLimitConfigDto clientIpDto = createRouteDto("route1", REPLENISH_RATE_100, BURST_CAPACITY_200);
        clientIpDto.setRateLimitType(RateLimitConfigDto.RateLimitType.CLIENT_IP);

        RateLimitConfigDto headerDto = createRouteDto("route2", REPLENISH_RATE_150, BURST_CAPACITY_300);
        headerDto.setRateLimitType(RateLimitConfigDto.RateLimitType.HEADER);
        headerDto.setHeaderName("X-API-Key");

        RateLimitConfigDto routePathDto = createServiceDto("service1", REPLENISH_RATE_50, BURST_CAPACITY_100);
        routePathDto.setRateLimitType(RateLimitConfigDto.RateLimitType.ROUTE_PATH);

        List<RateLimitConfigDto> configs = Arrays.asList(clientIpDto, headerDto, routePathDto);
        String requestBody = objectMapper.writeValueAsString(configs);

        // Create all configs
        mockMvc.perform(post("/v1/config/rate-limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(EXPECTED_SIZE_3)));

        // Verify all types are saved correctly
        mockMvc.perform(get("/v1/config/rate-limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(EXPECTED_SIZE_3)))
                .andExpect(jsonPath("$[*].rateLimitType",
                        containsInAnyOrder("CLIENT_IP", "HEADER", "ROUTE_PATH")));
    }

    // ==================== Helper Methods ====================

    private RateLimitConfigDto createRouteDto(String routeId, long replenishRate, long burstCapacity) {
        RateLimitConfigDto dto = new RateLimitConfigDto();
        dto.setRouteId(routeId);
        dto.setReplenishRate(replenishRate);
        dto.setBurstCapacity(burstCapacity);
        dto.setIncludeHeaders(false);
        dto.setRateLimitType(RateLimitConfigDto.RateLimitType.CLIENT_IP);
        return dto;
    }

    private RateLimitConfigDto createServiceDto(String service, long replenishRate, long burstCapacity) {
        RateLimitConfigDto dto = new RateLimitConfigDto();
        dto.setService(service);
        dto.setReplenishRate(replenishRate);
        dto.setBurstCapacity(burstCapacity);
        dto.setIncludeHeaders(false);
        dto.setRateLimitType(RateLimitConfigDto.RateLimitType.CLIENT_IP);
        return dto;
    }

    private RateLimitConfigEntity createAndSaveRouteEntity(String routeId, long replenishRate, long burstCapacity) {
        RateLimitConfigEntity entity = new RateLimitConfigEntity();
        entity.setId(routeId);
        entity.setRouteId(routeId);
        entity.setReplenishRate(replenishRate);
        entity.setBurstCapacity(burstCapacity);
        entity.setIncludeHeaders(false);
        entity.setRateLimitType(RateLimitConfigEntity.RateLimitType.CLIENT_IP);
        return rateLimitConfigRepository.save(entity);
    }

    private RateLimitConfigEntity createAndSaveServiceEntity(String service, long replenishRate, long burstCapacity) {
        RateLimitConfigEntity entity = new RateLimitConfigEntity();
        entity.setId(service);
        entity.setService(service);
        entity.setReplenishRate(replenishRate);
        entity.setBurstCapacity(burstCapacity);
        entity.setIncludeHeaders(false);
        entity.setRateLimitType(RateLimitConfigEntity.RateLimitType.CLIENT_IP);
        return rateLimitConfigRepository.save(entity);
    }
}
