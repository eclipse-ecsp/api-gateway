package org.eclipse.ecsp.registry.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.ecsp.registry.common.document.GatewayClientAccessControlDocument;
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlRequestDto;
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlResponseDto;
import org.eclipse.ecsp.registry.common.entity.GatewayClientAccessControl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ClientAccessControlMapper.
 *
 * <p>
 * Tests entity/document to DTO conversions and vice versa.
 */
class ClientAccessControlMapperTest {

    private static final int EXPECTED_TWO_ITEMS = 2;
    private static final long SECONDS_IN_HOUR = 3600L;

    private ClientAccessControlMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        mapper = new ClientAccessControlMapper(objectMapper);
    }

    @Test
    void testEntityToResponseDto() {
        // Arrange
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<String> rules = Arrays.asList("user-service:*", "!payment-service:refund");
        
        GatewayClientAccessControl entity = GatewayClientAccessControl.builder()
                .id(1L)
                .clientId("test_client")
                .description("Test client")
                .tenant("Test Tenant")
                .isActive(true)
                .isDeleted(false)
                .allowRules(rules)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Act
        ClientAccessControlResponseDto dto = mapper.entityToResponseDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("test_client", dto.getClientId());
        assertEquals("Test client", dto.getDescription());
        assertEquals("Test Tenant", dto.getTenant());
        assertTrue(dto.getIsActive());
        assertFalse(dto.getIsDeleted());
        assertEquals(EXPECTED_TWO_ITEMS, dto.getAllow().size());
        assertEquals("user-service:*", dto.getAllow().get(0));
        assertNotNull(dto.getCreatedAt());
        assertNotNull(dto.getUpdatedAt());
    }

    @Test
    void testEntityToResponseDto_NullEntity() {
        // Act
        ClientAccessControlResponseDto dto = mapper.entityToResponseDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testRequestDtoToEntity() {
        // Arrange
        List<String> rules = Arrays.asList("vehicle-service:*");
        ClientAccessControlRequestDto dto = ClientAccessControlRequestDto.builder()
                .clientId("new_client")
                .description("New client")
                .tenant("New Tenant")
                .isActive(true)
                .allow(rules)
                .build();

        // Act
        GatewayClientAccessControl entity = mapper.requestDtoToEntity(dto);

        // Assert
        assertNotNull(entity);
        assertEquals("new_client", entity.getClientId());
        assertEquals("New client", entity.getDescription());
        assertEquals("New Tenant", entity.getTenant());
        assertTrue(entity.getIsActive());
        assertEquals(1, entity.getAllowRules().size());
        assertEquals("vehicle-service:*", entity.getAllowRules().get(0));
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void testRequestDtoToEntity_NullDto() {
        // Act
        GatewayClientAccessControl entity = mapper.requestDtoToEntity(null);

        // Assert
        assertNull(entity);
    }

    @Test
    void testUpdateEntityFromRequestDto() {
        // Arrange
        OffsetDateTime originalCreatedAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        GatewayClientAccessControl entity = GatewayClientAccessControl.builder()
                .id(1L)
                .clientId("existing_client")
                .description("Old description")
                .tenant("Old Tenant")
                .isActive(false)
                .allowRules(Arrays.asList("old-service:*"))
                .createdAt(originalCreatedAt)
                .updatedAt(originalCreatedAt)
                .build();

        ClientAccessControlRequestDto dto = ClientAccessControlRequestDto.builder()
                .description("Updated description")
                .tenant("Updated Tenant")
                .isActive(true)
                .allow(Arrays.asList("new-service:*", "another-service:route"))
                .build();

        // Act
        mapper.updateEntityFromRequestDto(entity, dto);

        // Assert
        assertEquals("existing_client", entity.getClientId()); // Not updated
        assertEquals("Updated description", entity.getDescription());
        assertEquals("Updated Tenant", entity.getTenant());
        assertTrue(entity.getIsActive());
        assertEquals(EXPECTED_TWO_ITEMS, entity.getAllowRules().size());
        assertEquals("new-service:*", entity.getAllowRules().get(0));
        assertEquals(originalCreatedAt, entity.getCreatedAt()); // Not updated
        assertTrue(entity.getUpdatedAt().isAfter(originalCreatedAt)); // Updated
    }

    @Test
    void testUpdateEntityFromRequestDto_NullInputs() {
        // Arrange
        GatewayClientAccessControl entity = GatewayClientAccessControl.builder()
                .clientId("test")
                .tenant("Test")
                .build();

        // Act - Should not throw exception
        mapper.updateEntityFromRequestDto(null, null);
        mapper.updateEntityFromRequestDto(entity, null);
        mapper.updateEntityFromRequestDto(null, new ClientAccessControlRequestDto());

        // Assert - No exception
    }

    @Test
    void testDocumentToResponseDto() {
        // Arrange
        Instant now = Instant.now();
        List<String> rules = Arrays.asList("user-service:*");
        
        GatewayClientAccessControlDocument document = GatewayClientAccessControlDocument.builder()
                .id("60d5ec49f1a2b123456789ab")
                .clientId("mongo_client")
                .description("MongoDB client")
                .tenant("Mongo Tenant")
                .isActive(true)
                .isDeleted(false)
                .allow(rules)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Act
        ClientAccessControlResponseDto dto = mapper.documentToResponseDto(document);

        // Assert
        assertNotNull(dto);
        assertNull(dto.getId());  // MongoDB ObjectId can't be mapped to Long, remains null
        assertEquals("mongo_client", dto.getClientId());
        assertEquals("MongoDB client", dto.getDescription());
        assertEquals("Mongo Tenant", dto.getTenant());
        assertTrue(dto.getIsActive());
        assertFalse(dto.getIsDeleted());
        assertEquals(1, dto.getAllow().size());
        assertNotNull(dto.getCreatedAt());
        assertNotNull(dto.getUpdatedAt());
    }

    @Test
    void testDocumentToResponseDto_NullDocument() {
        // Act
        ClientAccessControlResponseDto dto = mapper.documentToResponseDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testRequestDtoToDocument() {
        // Arrange
        List<String> rules = Arrays.asList("service:route");
        ClientAccessControlRequestDto dto = ClientAccessControlRequestDto.builder()
                .clientId("mongo_new")
                .description("New MongoDB client")
                .tenant("Mongo Tenant")
                .isActive(false)
                .allow(rules)
                .build();

        // Act
        GatewayClientAccessControlDocument document = mapper.requestDtoToDocument(dto);

        // Assert
        assertNotNull(document);
        assertEquals("mongo_new", document.getClientId());
        assertEquals("New MongoDB client", document.getDescription());
        assertEquals("Mongo Tenant", document.getTenant());
        assertFalse(document.getIsActive());
        assertFalse(document.getIsDeleted());
        assertEquals(1, document.getAllow().size());
        assertNotNull(document.getCreatedAt());
        assertNotNull(document.getUpdatedAt());
    }

    @Test
    void testRequestDtoToDocument_DefaultIsActive() {
        // Arrange
        ClientAccessControlRequestDto dto = ClientAccessControlRequestDto.builder()
                .clientId("default_active")
                .tenant("Test")
                .build();

        // Act
        GatewayClientAccessControlDocument document = mapper.requestDtoToDocument(dto);

        // Assert
        assertTrue(document.getIsActive(), "isActive should default to true if null in DTO");
        assertFalse(document.getIsDeleted());
    }

    @Test
    void testUpdateDocumentFromRequestDto() {
        // Arrange
        Instant originalCreatedAt = Instant.now().minusSeconds(SECONDS_IN_HOUR);
        GatewayClientAccessControlDocument document = GatewayClientAccessControlDocument.builder()
                .id("60d5ec49f1a2b123456789ab")
                .clientId("existing_mongo")
                .description("Old description")
                .tenant("Old Tenant")
                .isActive(false)
                .allow(Arrays.asList("old:*"))
                .createdAt(originalCreatedAt)
                .updatedAt(originalCreatedAt)
                .build();

        ClientAccessControlRequestDto dto = ClientAccessControlRequestDto.builder()
                .description("Updated description")
                .tenant("Updated Tenant")
                .isActive(true)
                .allow(Arrays.asList("new:route"))
                .build();

        // Act
        mapper.updateDocumentFromRequestDto(document, dto);

        // Assert
        assertEquals("existing_mongo", document.getClientId()); // Not updated
        assertEquals("Updated description", document.getDescription());
        assertEquals("Updated Tenant", document.getTenant());
        assertTrue(document.getIsActive());
        assertEquals(1, document.getAllow().size());
        assertEquals("new:route", document.getAllow().get(0));
        assertEquals(originalCreatedAt, document.getCreatedAt()); // Not updated
        assertTrue(document.getUpdatedAt().isAfter(originalCreatedAt)); // Updated
    }

    @Test
    void testUpdateDocumentFromRequestDto_NullInputs() {
        // Arrange
        GatewayClientAccessControlDocument document = GatewayClientAccessControlDocument.builder()
                .clientId("test")
                .tenant("Test")
                .build();

        // Act - Should not throw exception
        mapper.updateDocumentFromRequestDto(null, null);
        mapper.updateDocumentFromRequestDto(document, null);
        mapper.updateDocumentFromRequestDto(null, new ClientAccessControlRequestDto());

        // Assert - No exception
    }
}
