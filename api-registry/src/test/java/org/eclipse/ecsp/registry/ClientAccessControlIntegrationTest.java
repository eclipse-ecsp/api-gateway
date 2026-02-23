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

package org.eclipse.ecsp.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.redis.testcontainers.RedisContainer;
import org.eclipse.ecsp.registry.dto.BulkCreateResponseDto;
import org.eclipse.ecsp.registry.dto.ClientAccessControlRequestDto;
import org.eclipse.ecsp.registry.entity.ClientAccessControlEntity;
import org.eclipse.ecsp.registry.repo.ClientAccessControlRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.fasterxml.jackson.core.JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for Client Access Control CRUD operations.
 *
 * <p>Uses TestContainers with PostgreSQL.
 * Tests the complete request flow: Controller → Service → Repository → PostgreSQL Database.
 *
 * @author Abhishek Kumar
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("java:S6813")// allow spring Autowire
class ClientAccessControlIntegrationTest {

    private static final String W_999999 = "/999999";
    private static final String CLIENT_ID_JSON_PATH = "$.clientId";
    private static final String TENANT_JSON_PATH = "$.tenant";
    private static final String IS_ACTIVE_JSON_PATH = "$.isActive";
    private static final String TENANT_AFTER = "tenant_after";
    private static final String UPDATE_CLIENT = "update_client";
    private static final String WORKFLOW_CLIENT = "workflow_client";
    private static final String USER_SERVICE = "user-service:*";
    private static final String DUPLICATE_CLIENT = "duplicate_client";
    private static final String CLIENT_DESCRIPTION = "client description";
    private static final String BASE_URL = "/v1/config/client-access-control";
    private static final int EXPECTED_TWO_ITEMS = 2;

    @SuppressWarnings("resource") // Managed by Testcontainers framework
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("api_registry_test")
            .withUsername("test")
            .withPassword("test");

    @SuppressWarnings("resource") // Managed by Testcontainers framework
    @Container
    static RedisContainer redis = new RedisContainer("redis:8-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("api-registry.database.type", () -> "sql");
        registry.add("postgres.jdbc.url", postgres::getJdbcUrl);
        registry.add("postgres.username", postgres::getUsername);
        registry.add("postgres.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientAccessControlRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper.enable(INCLUDE_SOURCE_IN_LOCATION);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        repository.deleteAll();
    }

    @AfterAll
    static void tearDown() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
            postgres.close();
        }
        if (redis != null && redis.isRunning()) {
            redis.stop();
            redis.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("PostgreSQL container should be running")
    void testPostgresContainerIsRunning() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(postgres.getDatabaseName()).isEqualTo("api_registry_test");
        assertThat(postgres.getJdbcUrl()).contains("postgresql");
    }

    @Test
    @Order(2)
    @DisplayName("POST /v1/config/client-access-control - Bulk create should succeed")
    void testBulkCreateSuccess() throws Exception {
        // Arrange
        ClientAccessControlRequestDto request1 = ClientAccessControlRequestDto.builder()
                .clientId("test_client_1")
                .description(CLIENT_DESCRIPTION)
                .tenant("tenant_a")
                .isActive(true)
                .allow(Arrays.asList(USER_SERVICE, "!user-service:ban-user"))
                .build();

        ClientAccessControlRequestDto request2 = ClientAccessControlRequestDto.builder()
                .clientId("test_client_2")
                .description(CLIENT_DESCRIPTION)
                .tenant("tenant_b")
                .isActive(true)
                .allow(List.of("*:*"))
                .build();

        List<ClientAccessControlRequestDto> requests = Arrays.asList(request1, request2);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created", hasSize(EXPECTED_TWO_ITEMS)))
                .andExpect(jsonPath("$.created[*]", containsInAnyOrder("test_client_1", "test_client_2")));
    }

    @Test
    @Order(3)
    @DisplayName("POST /v1/config/client-access-control - Duplicate clientId should return 409")
    void testBulkCreateDuplicateClientIdConflict() throws Exception {
        // Arrange - Create initial client
        ClientAccessControlEntity existing = ClientAccessControlEntity.builder()
                .id(DUPLICATE_CLIENT)
                .clientId(DUPLICATE_CLIENT)
                .tenant("tenant_a")
                .isActive(true)
                .allow(List.of(USER_SERVICE))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        repository.save(existing);

        ClientAccessControlRequestDto request = ClientAccessControlRequestDto.builder()
                .clientId(DUPLICATE_CLIENT)
                .description("description updated")
                .tenant("tenant_b")
                .isActive(true)
                .allow(List.of("*:*"))
                .build();

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(request))))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(4)
    @DisplayName("POST /v1/config/client-access-control - Invalid request should return 400")
    void testBulkCreateInvalidRequestBadRequest() throws Exception {
        // Arrange - Missing required field (clientId)
        String invalidJson = "[{\"tenant\": \"tenant_a\", \"isActive\": true, \"allow\": [\"*:*\"]}]";

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", 
                    containsString("Validation failed for one or more clients.")))
                .andExpect(jsonPath("$.errors.[*].error", 
                    containsInAnyOrder("clientId is required", "description is required")));
                
    }

    @Test
    @Order(9)
    @DisplayName("GET /v1/config/client-access-control/{id} - Get by ID should succeed")
    void testGetByIdSuccess() throws Exception {
        // Arrange
        ClientAccessControlEntity entity = createTestClient("get_by_id_client", "tenant_x", true);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/" + entity.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CLIENT_ID_JSON_PATH, is("get_by_id_client")))
                .andExpect(jsonPath(TENANT_JSON_PATH, is("tenant_x")))
                .andExpect(jsonPath(IS_ACTIVE_JSON_PATH, is(true)));
    }

    @Test
    @Order(10)
    @DisplayName("GET /v1/config/client-access-control/{id} - Non-existent ID should return 404")
    void testGetByIdNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE_URL + W_999999))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(11)
    @DisplayName("PUT /v1/config/client-access-control/{id} - Update should succeed")
    void testUpdateSuccess() throws Exception {
        // Arrange
        ClientAccessControlEntity original = createTestClient(UPDATE_CLIENT, "tenant_before", true);

        ClientAccessControlRequestDto updateRequest = ClientAccessControlRequestDto.builder()
                .clientId(UPDATE_CLIENT)
                .tenant(TENANT_AFTER)
                .isActive(false)
                .allow(Arrays.asList("service-a:*", "service-b:read"))
                .build();

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/" + original.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CLIENT_ID_JSON_PATH, is(UPDATE_CLIENT)))
                .andExpect(jsonPath(TENANT_JSON_PATH, is(TENANT_AFTER)))
                .andExpect(jsonPath(IS_ACTIVE_JSON_PATH, is(false)))
                .andExpect(jsonPath("$.allow", hasSize(EXPECTED_TWO_ITEMS)));

        // Verify database state
        Optional<ClientAccessControlEntity> updated = repository.findByClientIdAndIsDeletedFalse(original.getId());
        assertThat(updated).isNotNull();
        assertThat(updated.get().getTenant()).isEqualTo(TENANT_AFTER);
        assertThat(updated.get().getIsActive()).isFalse();
    }

    @Test
    @Order(12)
    @DisplayName("PUT /v1/config/client-access-control/{id} - Update non-existent should return 404")
    void testUpdateNotFound() throws Exception {
        // Arrange
        ClientAccessControlRequestDto updateRequest = ClientAccessControlRequestDto.builder()
                .clientId("non_existent")
                .tenant("tenant")
                .isActive(true)
                .allow(List.of("*:*"))
                .build();

        // Act & Assert
        mockMvc.perform(put(BASE_URL + W_999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(13)
    @DisplayName("DELETE /v1/config/client-access-control/{id} - Delete should succeed")
    void testDeleteSuccess() throws Exception {
        // Arrange
        ClientAccessControlEntity entity = createTestClient("delete_client", "tenant_del", true);
        String id = entity.getId();

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/" + id))
                .andExpect(status().isOk());

        // Verify deletion, permanent deletion should remove from DB, soft deletion should mark as deleted
        assertThat(repository.findByClientIdAndIsDeletedTrue(id)).isNotEmpty();
    }

    @Test
    @Order(14)
    @DisplayName("DELETE /v1/config/client-access-control/{id} - Delete non-existent should return 404")
    void testDeleteNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(delete(BASE_URL + W_999999))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(15)
    @DisplayName("Integration test - Complete CRUD workflow")
    void testCompleteCrudWorkflow() throws Exception {
        // 1. Create
        ClientAccessControlRequestDto createRequest = ClientAccessControlRequestDto.builder()
                .clientId(WORKFLOW_CLIENT)
                .description(CLIENT_DESCRIPTION)
                .tenant("workflow_tenant")
                .isActive(true)
                .allow(List.of("service-1:*"))
                .build();

        MvcResult createResult = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(createRequest))))
                .andExpect(status().isCreated())
                .andReturn();

        BulkCreateResponseDto created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                BulkCreateResponseDto.class
        );
        String id = created.getCreated().get(0);

        // 2. Read
        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CLIENT_ID_JSON_PATH, is(WORKFLOW_CLIENT)));

        // 3. Update
        ClientAccessControlRequestDto updateRequest = ClientAccessControlRequestDto.builder()
                .clientId(WORKFLOW_CLIENT)
                .tenant("workflow_tenant_updated")
                .isActive(false)
                .allow(Arrays.asList("service-1:*", "service-2:read"))
                .build();

        mockMvc.perform(put(BASE_URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CLIENT_ID_JSON_PATH, is(WORKFLOW_CLIENT)))
                .andExpect(jsonPath(TENANT_JSON_PATH, is("workflow_tenant_updated")))
                .andExpect(jsonPath(IS_ACTIVE_JSON_PATH, is(false)))
                .andExpect(jsonPath("$.allow", hasSize(EXPECTED_TWO_ITEMS)));

        // 5. Delete
        mockMvc.perform(delete(BASE_URL + "/" + id))
                .andExpect(status().is2xxSuccessful());

        // 6. Verify deletion
        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isNotFound());
    }

    // Helper methods

    private ClientAccessControlEntity createTestClient(String clientId, String tenant, boolean active) {
        ClientAccessControlEntity entity = ClientAccessControlEntity.builder()
                .id(clientId)
                .clientId(clientId)
                .tenant(tenant)
                .isActive(active)
                .allow(Arrays.asList(USER_SERVICE, "payment-service:read"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return repository.save(entity);
    }

    /**
     * Placeholder for ErrorResponse schema (referenced in controller).
     */
    static class ErrorResponse {
        private String message;
        private int status;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }
    }
}
