package org.eclipse.ecsp.registry.common.entity;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for GatewayClientAccessControl JPA entity.
 * Tests entity validation, builder pattern, and field constraints.
 */
class GatewayClientAccessControlTest {

    private static final int EXPECTED_TWO_RULES = 2;
    private static final int EXPECTED_THREE_RULES = 3;
    private static final int CLIENT_ID_MAX_LENGTH = 128;
    private static final int DESCRIPTION_MAX_LENGTH = 512;
    private static final int TENANT_MAX_LENGTH = 256;
    private static final int EXPECTED_TEN_ELEMENTS = 10;

    @Test
    void testEntityCreation() {
        // Arrange
        OffsetDateTime now = OffsetDateTime.now();
        List<String> rules = Arrays.asList("user-service:*", "!user-service:ban-user");

        // Act
        GatewayClientAccessControl entity = GatewayClientAccessControl.builder()
                .clientId("test_client")
                .description("Test client")
                .tenant("Test Tenant")
                .isActive(true)
                .isDeleted(false)
                .allowRules(rules)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Assert
        assertNotNull(entity);
        assertEquals("test_client", entity.getClientId());
        assertEquals("Test client", entity.getDescription());
        assertEquals("Test Tenant", entity.getTenant());
        assertTrue(entity.getIsActive());
        assertFalse(entity.getIsDeleted());
        assertEquals(EXPECTED_TWO_RULES, entity.getAllowRules().size());
        assertEquals("user-service:*", entity.getAllowRules().get(0));
        assertNull(entity.getDeletedAt());
    }

    @Test
    void testDefaultValues() {
        // Act
        GatewayClientAccessControl entity = GatewayClientAccessControl.builder()
                .clientId("default_client")
                .tenant("Default Tenant")
                .build();

        // Assert
        assertTrue(entity.getIsActive(), "isActive should default to true");
        assertFalse(entity.getIsDeleted(), "isDeleted should default to false");
    }

    @Test
    void testSoftDelete() {
        // Arrange
        OffsetDateTime now = OffsetDateTime.now();
        GatewayClientAccessControl entity = GatewayClientAccessControl.builder()
                .clientId("delete_test")
                .tenant("Test")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Act - Soft delete
        entity.setIsDeleted(true);
        entity.setDeletedAt(now);

        // Assert
        assertTrue(entity.getIsDeleted());
        assertNotNull(entity.getDeletedAt());
        assertEquals(now, entity.getDeletedAt());
    }

    @Test
    void testAllowRulesWithWildcards() {
        // Arrange
        List<String> rules = Arrays.asList(
                "*:*",                           // All services, all routes
                "vehicle-service:get-*",         // Vehicle service, routes starting with get-
                "!payment-service:refund"        // Deny refund in payment service
        );

        // Act
        GatewayClientAccessControl entity = GatewayClientAccessControl.builder()
                .clientId("wildcard_client")
                .tenant("Wildcard Test")
                .allowRules(rules)
                .build();

        // Assert
        assertEquals(EXPECTED_THREE_RULES, entity.getAllowRules().size());
        assertTrue(entity.getAllowRules().contains("*:*"));
        assertTrue(entity.getAllowRules().contains("vehicle-service:get-*"));
        assertTrue(entity.getAllowRules().contains("!payment-service:refund"));
    }

    @Test
    void testEmptyAllowRules() {
        // Act
        GatewayClientAccessControl entity = GatewayClientAccessControl.builder()
                .clientId("empty_rules")
                .tenant("Empty Rules Test")
                .allowRules(null)
                .build();

        // Assert
        assertNull(entity.getAllowRules(), "Null allow rules represent deny-by-default");
    }

    @Test
    void testClientIdUniqueness() {
        // Arrange
        String clientId = "unique_client_123";

        // Act
        GatewayClientAccessControl entity1 = GatewayClientAccessControl.builder()
                .clientId(clientId)
                .tenant("Tenant1")
                .build();

        GatewayClientAccessControl entity2 = GatewayClientAccessControl.builder()
                .clientId(clientId)
                .tenant("Tenant2")
                .build();

        // Assert
        assertEquals(entity1.getClientId(), entity2.getClientId());
        // Note: Actual uniqueness constraint is enforced at database level via unique index
    }

    @Test
    void testClientIdLengthConstraints() {
        // Test minimum length (3 characters)
        String minLengthClientId = "abc";
        GatewayClientAccessControl minEntity = GatewayClientAccessControl.builder()
                .clientId(minLengthClientId)
                .tenant("Test")
                .build();
        assertEquals(EXPECTED_THREE_RULES, minEntity.getClientId().length());

        // Test maximum length (128 characters)
        String maxLengthClientId = "a".repeat(CLIENT_ID_MAX_LENGTH);
        GatewayClientAccessControl maxEntity = GatewayClientAccessControl.builder()
                .clientId(maxLengthClientId)
                .tenant("Test")
                .build();
        assertEquals(CLIENT_ID_MAX_LENGTH, maxEntity.getClientId().length());
    }

    @Test
    void testDescriptionMaxLength() {
        // Arrange - 512 characters
        String description = "a".repeat(DESCRIPTION_MAX_LENGTH);

        // Act
        GatewayClientAccessControl entity = GatewayClientAccessControl.builder()
                .clientId("desc_test")
                .description(description)
                .tenant("Test")
                .build();

        // Assert
        assertEquals(DESCRIPTION_MAX_LENGTH, entity.getDescription().length());
    }

    @Test
    void testTenantMaxLength() {
        // Arrange - 256 characters
        String tenant = "a".repeat(TENANT_MAX_LENGTH);

        // Act
        GatewayClientAccessControl entity = GatewayClientAccessControl.builder()
                .clientId("tenant_test")
                .tenant(tenant)
                .build();

        // Assert
        assertEquals(TENANT_MAX_LENGTH, entity.getTenant().length());
    }

    @Test
    void testStateTransitions() {
        // Arrange
        OffsetDateTime now = OffsetDateTime.now();
        GatewayClientAccessControl entity = GatewayClientAccessControl.builder()
                .clientId("state_test")
                .tenant("Test")
                .isActive(true)
                .isDeleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Act & Assert - Active state
        assertTrue(entity.getIsActive());
        assertFalse(entity.getIsDeleted());
        assertNull(entity.getDeletedAt());

        // Deactivate
        entity.setIsActive(false);
        assertFalse(entity.getIsActive());
        assertFalse(entity.getIsDeleted());

        // Soft delete
        entity.setIsDeleted(true);
        entity.setDeletedAt(now);
        assertFalse(entity.getIsActive());
        assertTrue(entity.getIsDeleted());
        assertNotNull(entity.getDeletedAt());

        // Restore
        entity.setIsActive(true);
        entity.setIsDeleted(false);
        entity.setDeletedAt(null);
        assertTrue(entity.getIsActive());
        assertFalse(entity.getIsDeleted());
        assertNull(entity.getDeletedAt());
    }

    @Test
    void testTimestampOrdering() {
        // Arrange
        OffsetDateTime created = OffsetDateTime.now();
        OffsetDateTime updated = created.plusSeconds(EXPECTED_TEN_ELEMENTS);

        // Act
        GatewayClientAccessControl entity = GatewayClientAccessControl.builder()
                .clientId("timestamp_test")
                .tenant("Test")
                .createdAt(created)
                .updatedAt(updated)
                .build();

        // Assert
        assertTrue(entity.getUpdatedAt().isAfter(entity.getCreatedAt()));
    }
}
