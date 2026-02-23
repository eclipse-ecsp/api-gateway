package org.eclipse.ecsp.registry.mapper;

import org.eclipse.ecsp.registry.dto.ClientAccessControlRequestDto;
import org.eclipse.ecsp.registry.dto.ClientAccessControlResponseDto;
import org.eclipse.ecsp.registry.entity.ClientAccessControlEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ClientAccessControlMapper.
 *
 * <p>Tests entity/document to DTO conversions and vice versa.
 */
class ClientAccessControlMapperTest {

    private static final int EXPECTED_TWO_ITEMS = 2;
    private static final String TEST_CLIENT_ID = "test_client";
    private static final String TEST_DESCRIPTION = "Test client";
    private static final String TEST_TENANT = "Test Tenant";
    private static final String NEW_CLIENT_ID = "new_client";
    private static final String NEW_DESCRIPTION = "New client";
    private static final String NEW_TENANT = "New Tenant";
    private static final String EXISTING_CLIENT_ID = "existing_client";
    private static final String OLD_DESCRIPTION = "Old description";
    private static final String OLD_TENANT = "Old Tenant";
    private static final String UPDATED_DESCRIPTION = "Updated description";
    private static final String UPDATED_TENANT = "Updated Tenant";
    private static final String SERVICE_USER = "user-service:*";
    private static final String SERVICE_PAYMENT_REFUND = "!payment-service:refund";
    private static final String SERVICE_VEHICLE = "vehicle-service:*";
    private static final String SERVICE_OLD = "old-service:*";
    private static final String SERVICE_NEW = "new-service:*";
    private static final String SERVICE_ANOTHER = "another-service:route";

    private ClientAccessControlMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ClientAccessControlMapper();
    }

    /**
     * Test purpose          - Verify entityToResponseDto with valid entity.
     * Test data             - Valid ClientAccessControlEntity with all fields populated.
     * Test expected result  - Successfully converted to ClientAccessControlResponseDto with all fields mapped.
     * Test type             - Positive.
     *
     * @throws Exception if test fails.
     */
    @Test
    void entityToResponseDtoValidEntitySuccess() {
        // GIVEN:
        LocalDateTime now = LocalDateTime.now();
        List<String> rules = Arrays.asList(SERVICE_USER, SERVICE_PAYMENT_REFUND);
        
        ClientAccessControlEntity entity = ClientAccessControlEntity.builder()
                .id(TEST_CLIENT_ID)
                .clientId(TEST_CLIENT_ID)
                .description(TEST_DESCRIPTION)
                .tenant(TEST_TENANT)
                .isActive(true)
                .isDeleted(false)
                .allow(rules)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // WHEN:
        ClientAccessControlResponseDto dto = mapper.entityToResponseDto(entity);

        // THEN:
        assertNotNull(dto);
        assertEquals(TEST_CLIENT_ID, dto.getClientId());
        assertEquals(TEST_DESCRIPTION, dto.getDescription());
        assertEquals(TEST_TENANT, dto.getTenant());
        assertTrue(dto.getIsActive());
        assertEquals(EXPECTED_TWO_ITEMS, dto.getAllow().size());
        assertEquals(SERVICE_USER, dto.getAllow().get(0));
    }

    /**
     * Test purpose          - Verify entityToResponseDto with null entity.
     * Test data             - Null entity.
     * Test expected result  - Returns null DTO.
     * Test type             - Negative.
     *
     * @throws Exception if test fails.
     */
    @Test
    void entityToResponseDtoNullEntityReturnsNull() {
        // GIVEN:
        // null entity

        // WHEN:
        ClientAccessControlResponseDto dto = mapper.entityToResponseDto(null);

        // THEN:
        assertNull(dto);
    }

    /**
     * Test purpose          - Verify requestDtoToEntity with valid request DTO.
     * Test data             - Valid ClientAccessControlRequestDto with all fields populated.
     * Test expected result  - Successfully converted to entity with timestamps initialized.
     * Test type             - Positive.
     *
     * @throws Exception if test fails.
     */
    @Test
    void requestDtoToEntityValidDtoSuccess() {
        // GIVEN:
        List<String> rules = Arrays.asList(SERVICE_VEHICLE);
        ClientAccessControlRequestDto dto = ClientAccessControlRequestDto.builder()
                .clientId(NEW_CLIENT_ID)
                .description(NEW_DESCRIPTION)
                .tenant(NEW_TENANT)
                .isActive(true)
                .allow(rules)
                .build();

        // WHEN:
        ClientAccessControlEntity entity = mapper.requestDtoToEntity(dto);

        // THEN:
        assertNotNull(entity);
        assertEquals(NEW_CLIENT_ID, entity.getClientId());
        assertEquals(NEW_DESCRIPTION, entity.getDescription());
        assertEquals(NEW_TENANT, entity.getTenant());
        assertTrue(entity.getIsActive());
        assertEquals(1, entity.getAllow().size());
        assertEquals(SERVICE_VEHICLE, entity.getAllow().get(0));
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    /**
     * Test purpose          - Verify requestDtoToEntity with null DTO.
     * Test data             - Null request DTO.
     * Test expected result  - Returns null entity.
     * Test type             - Negative.
     *
     * @throws Exception if test fails.
     */
    @Test
    void requestDtoToEntityNullDtoReturnsNull() {
        // GIVEN:
        // null DTO

        // WHEN:
        ClientAccessControlEntity entity = mapper.requestDtoToEntity(null);

        // THEN:
        assertNull(entity);
    }

    /**
     * Test purpose          - Verify updateEntityFromRequestDto updates mutable fields.
     * Test data             - Existing entity and valid update DTO.
     * Test expected result  - Mutable fields updated, immutable fields preserved.
     * Test type             - Positive.
     *
     * @throws Exception if test fails.
     */
    @Test
    void updateEntityFromRequestDtoValidInputsUpdatesMutableFields() {
        // GIVEN:
        LocalDateTime originalCreatedAt = LocalDateTime.now().minusDays(1);
        ClientAccessControlEntity entity = ClientAccessControlEntity.builder()
                .id(EXISTING_CLIENT_ID)
                .clientId(EXISTING_CLIENT_ID)
                .description(OLD_DESCRIPTION)
                .tenant(OLD_TENANT)
                .isActive(false)
                .allow(Arrays.asList(SERVICE_OLD))
                .createdAt(originalCreatedAt)
                .updatedAt(originalCreatedAt)
                .build();

        ClientAccessControlRequestDto dto = ClientAccessControlRequestDto.builder()
                .description(UPDATED_DESCRIPTION)
                .tenant(UPDATED_TENANT)
                .isActive(true)
                .allow(Arrays.asList(SERVICE_NEW, SERVICE_ANOTHER))
                .build();

        // WHEN:
        mapper.updateEntityFromRequestDto(entity, dto);

        // THEN:
        assertEquals(EXISTING_CLIENT_ID, entity.getClientId()); // Not updated
        assertEquals(UPDATED_DESCRIPTION, entity.getDescription());
        assertEquals(UPDATED_TENANT, entity.getTenant());
        assertTrue(entity.getIsActive());
        assertEquals(EXPECTED_TWO_ITEMS, entity.getAllow().size());
        assertEquals(SERVICE_NEW, entity.getAllow().get(0));
        assertEquals(originalCreatedAt, entity.getCreatedAt()); // Not updated
        assertTrue(entity.getUpdatedAt().isAfter(originalCreatedAt)); // Updated
    }

    /**
     * Test purpose          - Verify updateEntityFromRequestDto handles null inputs gracefully.
     * Test data             - Various combinations of null entity and DTO.
     * Test expected result  - No exception thrown.
     * Test type             - Negative.
     *
     * @throws Exception if test fails.
     */
    @Test
    void updateEntityFromRequestDtoNullInputsNoException() {
        // GIVEN:
        ClientAccessControlEntity entity = ClientAccessControlEntity.builder()
                .clientId(TEST_CLIENT_ID)
                .tenant(TEST_TENANT)
                .build();

        // WHEN:
        mapper.updateEntityFromRequestDto(null, null);
        mapper.updateEntityFromRequestDto(entity, null);
        mapper.updateEntityFromRequestDto(null, new ClientAccessControlRequestDto());

        // THEN:
        // No exception thrown, verify entity unchanged when updated with null DTO
        assertEquals(TEST_CLIENT_ID, entity.getClientId());
        assertEquals(TEST_TENANT, entity.getTenant());
    }

    /**
     * Test purpose          - Verify entityToResponseDto handles empty allow list.
     * Test data             - Entity with empty allow list.
     * Test expected result  - Successfully converted with empty list in DTO.
     * Test type             - Positive.
     *
     * @throws Exception if test fails.
     */
    @Test
    void entityToResponseDtoEmptyAllowListSuccess() {
        // GIVEN:
        LocalDateTime now = LocalDateTime.now();
        ClientAccessControlEntity entity = ClientAccessControlEntity.builder()
                .id(TEST_CLIENT_ID)
                .clientId(TEST_CLIENT_ID)
                .description(TEST_DESCRIPTION)
                .tenant(TEST_TENANT)
                .isActive(true)
                .allow(Collections.emptyList())
                .createdAt(now)
                .updatedAt(now)
                .build();

        // WHEN:
        ClientAccessControlResponseDto dto = mapper.entityToResponseDto(entity);

        // THEN:
        assertNotNull(dto);
        assertNotNull(dto.getAllow());
        assertTrue(dto.getAllow().isEmpty());
    }

    /**
     * Test purpose          - Verify requestDtoToEntity sets ID same as clientId.
     * Test data             - Valid request DTO with clientId.
     * Test expected result  - Entity ID equals clientId.
     * Test type             - Positive.
     *
     * @throws Exception if test fails.
     */
    @Test
    void requestDtoToEntityValidDtoSetsIdFromClientId() {
        // GIVEN:
        ClientAccessControlRequestDto dto = ClientAccessControlRequestDto.builder()
                .clientId(TEST_CLIENT_ID)
                .description(TEST_DESCRIPTION)
                .tenant(TEST_TENANT)
                .isActive(true)
                .allow(Arrays.asList(SERVICE_USER))
                .build();

        // WHEN:
        ClientAccessControlEntity entity = mapper.requestDtoToEntity(dto);

        // THEN:
        assertNotNull(entity);
        assertEquals(TEST_CLIENT_ID, entity.getId());
        assertEquals(TEST_CLIENT_ID, entity.getClientId());
    }

    /**
     * Test purpose          - Verify updateEntityFromRequestDto preserves clientId.
     * Test data             - Entity with different clientId in DTO.
     * Test expected result  - ClientId not updated.
     * Test type             - Positive.
     *
     * @throws Exception if test fails.
     */
    @Test
    void updateEntityFromRequestDtoDifferentClientIdPreservesOriginal() {
        // GIVEN:
        ClientAccessControlEntity entity = ClientAccessControlEntity.builder()
                .id(TEST_CLIENT_ID)
                .clientId(TEST_CLIENT_ID)
                .description(OLD_DESCRIPTION)
                .build();

        ClientAccessControlRequestDto dto = ClientAccessControlRequestDto.builder()
                .clientId(NEW_CLIENT_ID) // Different client ID
                .description(UPDATED_DESCRIPTION)
                .build();

        // WHEN:
        mapper.updateEntityFromRequestDto(entity, dto);

        // THEN:
        assertEquals(TEST_CLIENT_ID, entity.getClientId()); // Original preserved
    }
}
