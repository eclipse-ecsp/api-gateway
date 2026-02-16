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
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlFilterDto;
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlRequestDto;
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlResponseDto;
import org.eclipse.ecsp.registry.common.entity.GatewayClientAccessControl;
import org.eclipse.ecsp.registry.config.TestJpaConfiguration;
import org.eclipse.ecsp.registry.repository.ClientAccessControlRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
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
 * <p>
 * Uses TestContainers with PostgreSQL.
 * Tests the complete request flow: Controller → Service → Repository → PostgreSQL Database.
 *
 * @author AI Assistant
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(TestJpaConfiguration.class)
@Disabled(
    "TestContainers initialization issue - requires proper CI/CD environment"
)
class ClientAccessControlIntegrationTest {

    private static final String BASE_URL = "/api/registry/client-access-control";
    private static final int EXPECTED_TWO_ITEMS = 2;
    private static final int EXPECTED_FIVE_ITEMS = 5;
    private static final int EXPECTED_TEN_ITEMS = 10;
    private static final int EXPECTED_FIFTEEN_ITEMS = 15;
    private static final int ONE_ITEM = 1;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("api_registry_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        // Disable Redis auto-configuration for this test
        registry.add("spring.data.redis.repositories.enabled", () -> "false");
        registry.add(
            "spring.autoconfigure.exclude",
            () -> "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientAccessControlRepository repository;

    // Mock Redis components to avoid connection failures
    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @AfterAll
    static void tearDown() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
            postgres.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("PostgreSQL container should be running")
    void testPostgresContainer_IsRunning() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(postgres.getDatabaseName()).isEqualTo("api_registry_test");
        assertThat(postgres.getJdbcUrl()).contains("postgresql");
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/registry/client-access-control - Bulk create should succeed")
    void testBulkCreate_Success() throws Exception {
        // Arrange
        ClientAccessControlRequestDto request1 = ClientAccessControlRequestDto.builder()
                .clientId("test_client_1")
                .tenant("tenant_a")
                .isActive(true)
                .allow(Arrays.asList("user-service:*", "!user-service:ban-user"))
                .build();

        ClientAccessControlRequestDto request2 = ClientAccessControlRequestDto.builder()
                .clientId("test_client_2")
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
                .andExpect(jsonPath("$", hasSize(EXPECTED_TWO_ITEMS)))
                .andExpect(jsonPath("$[0].clientId", is("test_client_1")))
                .andExpect(jsonPath("$[0].tenant", is("tenant_a")))
                .andExpect(jsonPath("$[0].active", is(true)))
                .andExpect(jsonPath("$[0].rules", hasSize(EXPECTED_TWO_ITEMS)))
                .andExpect(jsonPath("$[1].clientId", is("test_client_2")))
                .andExpect(jsonPath("$[1].tenant", is("tenant_b")));

        // Verify database state
        assertThat(repository.count()).isEqualTo(Integer.toUnsignedLong(EXPECTED_TWO_ITEMS));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/registry/client-access-control - Duplicate clientId should return 409")
    void testBulkCreate_DuplicateClientId_Conflict() throws Exception {
        // Arrange - Create initial client
        GatewayClientAccessControl existing = GatewayClientAccessControl.builder()
                .clientId("duplicate_client")
                .tenant("tenant_a")
                .isActive(true)
                .allowRules(List.of("user-service:*"))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        repository.save(existing);

        ClientAccessControlRequestDto request = ClientAccessControlRequestDto.builder()
                .clientId("duplicate_client")
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
    @DisplayName("POST /api/registry/client-access-control - Invalid request should return 400")
    void testBulkCreate_InvalidRequest_BadRequest() throws Exception {
        // Arrange - Missing required field (clientId)
        String invalidJson = "[{\"tenant\": \"tenant_a\", \"active\": true, \"rules\": [\"*:*\"]}]";

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/registry/client-access-control - Filter by clientId should succeed")
    void testFilter_ByClientId_Success() throws Exception {
        // Arrange - Create test data
        createTestClient("filter_client_1", "tenant_a", true);
        createTestClient("filter_client_2", "tenant_a", true);
        createTestClient("other_client", "tenant_b", true);

        ClientAccessControlFilterDto filter = ClientAccessControlFilterDto.builder()
                .clientId("filter_client")
                .build();

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/filter?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(EXPECTED_TWO_ITEMS)))
                .andExpect(jsonPath("$.content[*].clientId", everyItem(containsString("filter_client"))))
                .andExpect(jsonPath("$.totalElements", is(EXPECTED_TWO_ITEMS)));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/registry/client-access-control - Filter by tenant should succeed")
    void testFilter_ByTenant_Success() throws Exception {
        // Arrange
        createTestClient("client_1", "tenant_alpha", true);
        createTestClient("client_2", "tenant_alpha", false);
        createTestClient("client_3", "tenant_beta", true);

        ClientAccessControlFilterDto filter = ClientAccessControlFilterDto.builder()
                .tenant("tenant_alpha")
                .build();

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/filter?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(EXPECTED_TWO_ITEMS)))
                .andExpect(jsonPath("$.content[*].tenant", everyItem(is("tenant_alpha"))))
                .andExpect(jsonPath("$.totalElements", is(EXPECTED_TWO_ITEMS)));
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/registry/client-access-control - Filter by active status should succeed")
    void testFilter_ByActiveStatus_Success() throws Exception {
        // Arrange
        createTestClient("active_client_1", "tenant_a", true);
        createTestClient("active_client_2", "tenant_a", true);
        createTestClient("inactive_client", "tenant_a", false);

        ClientAccessControlFilterDto filter = ClientAccessControlFilterDto.builder()
                .isActive(true)
                .build();

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/filter?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(EXPECTED_TWO_ITEMS)))
                .andExpect(jsonPath("$.content[*].active", everyItem(is(true))))
                .andExpect(jsonPath("$.totalElements", is(EXPECTED_TWO_ITEMS)));
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/registry/client-access-control - Pagination should work correctly")
    void testFilter_Pagination_Success() throws Exception {
        // Arrange - Create 15 clients
        for (int i = 1; i <= EXPECTED_FIFTEEN_ITEMS; i++) {
            createTestClient("page_client_" + i, "tenant_page", true);
        }

        ClientAccessControlFilterDto filter = ClientAccessControlFilterDto.builder()
                .tenant("tenant_page")
                .build();

        // Act & Assert - Page 0 (first 10)
        mockMvc.perform(post(BASE_URL + "/filter?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(EXPECTED_TEN_ITEMS)))
                .andExpect(jsonPath("$.totalElements", is(EXPECTED_FIFTEEN_ITEMS)))
                .andExpect(jsonPath("$.totalPages", is(EXPECTED_TWO_ITEMS)))
                .andExpect(jsonPath("$.number", is(0)));

        // Act & Assert - Page 1 (remaining 5)
        mockMvc.perform(post(BASE_URL + "/filter?page=1&size=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(EXPECTED_FIVE_ITEMS)))
                .andExpect(jsonPath("$.totalElements", is(EXPECTED_FIFTEEN_ITEMS)))
                .andExpect(jsonPath("$.number", is(1)));
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/registry/client-access-control/{id} - Get by ID should succeed")
    void testGetById_Success() throws Exception {
        // Arrange
        GatewayClientAccessControl entity = createTestClient("get_by_id_client", "tenant_x", true);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/" + entity.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId", is("get_by_id_client")))
                .andExpect(jsonPath("$.tenant", is("tenant_x")))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.id", is(entity.getId().intValue())));
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/registry/client-access-control/{id} - Non-existent ID should return 404")
    void testGetById_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(11)
    @DisplayName("PUT /api/registry/client-access-control/{id} - Update should succeed")
    void testUpdate_Success() throws Exception {
        // Arrange
        GatewayClientAccessControl original = createTestClient("update_client", "tenant_before", true);

        ClientAccessControlRequestDto updateRequest = ClientAccessControlRequestDto.builder()
                .clientId("update_client")
                .tenant("tenant_after")
                .isActive(false)
                .allow(Arrays.asList("service-a:*", "service-b:read"))
                .build();

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/" + original.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId", is("update_client")))
                .andExpect(jsonPath("$.tenant", is("tenant_after")))
                .andExpect(jsonPath("$.active", is(false)))
                .andExpect(jsonPath("$.rules", hasSize(EXPECTED_TWO_ITEMS)));

        // Verify database state
        GatewayClientAccessControl updated = repository.findById(original.getId()).orElseThrow();
        assertThat(updated.getTenant()).isEqualTo("tenant_after");
        assertThat(updated.getIsActive()).isFalse();
    }

    @Test
    @Order(12)
    @DisplayName("PUT /api/registry/client-access-control/{id} - Update non-existent should return 404")
    void testUpdate_NotFound() throws Exception {
        // Arrange
        ClientAccessControlRequestDto updateRequest = ClientAccessControlRequestDto.builder()
                .clientId("non_existent")
                .tenant("tenant")
                .isActive(true)
                .allow(List.of("*:*"))
                .build();

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(13)
    @DisplayName("DELETE /api/registry/client-access-control/{id} - Delete should succeed")
    void testDelete_Success() throws Exception {
        // Arrange
        GatewayClientAccessControl entity = createTestClient("delete_client", "tenant_del", true);
        Long id = entity.getId();

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/" + id))
                .andExpect(status().isNoContent());

        // Verify deletion
        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    @Order(14)
    @DisplayName("DELETE /api/registry/client-access-control/{id} - Delete non-existent should return 404")
    void testDelete_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(15)
    @DisplayName("Integration test - Complete CRUD workflow")
    void testCompleteCrudWorkflow() throws Exception {
        // 1. Create
        ClientAccessControlRequestDto createRequest = ClientAccessControlRequestDto.builder()
                .clientId("workflow_client")
                .tenant("workflow_tenant")
                .isActive(true)
                .allow(List.of("service-1:*"))
                .build();

        MvcResult createResult = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(createRequest))))
                .andExpect(status().isCreated())
                .andReturn();

        ClientAccessControlResponseDto[] created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                ClientAccessControlResponseDto[].class
        );
        Long id = created[0].getId();

        // 2. Read
        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId", is("workflow_client")));

        // 3. Update
        ClientAccessControlRequestDto updateRequest = ClientAccessControlRequestDto.builder()
                .clientId("workflow_client")
                .tenant("workflow_tenant_updated")
                .isActive(false)
                .allow(Arrays.asList("service-1:*", "service-2:read"))
                .build();

        mockMvc.perform(put(BASE_URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant", is("workflow_tenant_updated")))
                .andExpect(jsonPath("$.active", is(false)))
                .andExpect(jsonPath("$.rules", hasSize(EXPECTED_TWO_ITEMS)));

        // 4. Filter
        ClientAccessControlFilterDto filter = ClientAccessControlFilterDto.builder()
                .clientId("workflow_client")
                .build();

        mockMvc.perform(post(BASE_URL + "/filter?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(ONE_ITEM)))
                .andExpect(jsonPath("$.content[0].clientId", is("workflow_client")));

        // 5. Delete
        mockMvc.perform(delete(BASE_URL + "/" + id))
                .andExpect(status().isNoContent());

        // 6. Verify deletion
        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isNotFound());
    }

    // Helper methods

    private GatewayClientAccessControl createTestClient(String clientId, String tenant, boolean active) {
        GatewayClientAccessControl entity = GatewayClientAccessControl.builder()
                .clientId(clientId)
                .tenant(tenant)
                .isActive(active)
                .allowRules(Arrays.asList("user-service:*", "payment-service:read"))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
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
