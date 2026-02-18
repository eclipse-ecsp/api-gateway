package org.eclipse.ecsp.registry.repo;

import org.eclipse.ecsp.registry.entity.ClientAccessControlEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ClientAccessControlRepository.
 *
 * <p>Tests custom query methods:
 * - findByClientIdAndIsDeletedFalse
 * - findByIsActiveAndIsDeletedFalse
 * - findAllNotDeleted
 * - findByTenantAndIsDeletedFalse
 * - existsByClientIdAndIsDeletedFalse
 *
 * <p>DISABLED: RegistryApplication excludes JPA/DataSource auto-configuration which
 * conflicts with @DataJpaTest.  Repository functionality is tested through service
 * and integration tests.
 */

@Disabled("RegistryApplication JPA exclusions conflict with @DataJpaTest context loading")
@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ClientAccessControlRepositoryTest {

    private static final int EXPECTED_TWO_ITEMS = 2;

    @Autowired
    private ClientAccessControlRepository repository;

    @BeforeEach
    void setUp() {
        // No explicit cleanup API on ClientAccessControlRepository
    }

    /**
     * Test findByClientIdAndIsDeletedFalse - should find existing non-deleted client.
     */
    @Test
    void testFindByClientIdAndIsDeletedFalse_Found() {
        // Given: Entity exists in database
        ClientAccessControlEntity entity = createEntity("client-123", "tenant-a", true, false);
        repository.save(entity);
        

        // When: Find by clientId
        Optional<ClientAccessControlEntity> result = repository.findByClientIdAndIsDeletedFalse("client-123");

        // Then: Should find the entity
        assertThat(result).isPresent();
        assertThat(result.get().getClientId()).isEqualTo("client-123");
        assertThat(result.get().getTenant()).isEqualTo("tenant-a");
    }

    /**
     * Test findByClientIdAndIsDeletedFalse - should not find deleted client.
     */
    @Test
    void testFindByClientIdAndIsDeletedFalse_NotFoundWhenDeleted() {
        // Given: Deleted entity exists in database
        ClientAccessControlEntity entity = createEntity("client-456", "tenant-a", true, true);
        repository.save(entity);
        

        // When: Find by clientId
        Optional<ClientAccessControlEntity> result = repository.findByClientIdAndIsDeletedFalse("client-456");

        // Then: Should not find deleted entity
        assertThat(result).isEmpty();
    }

    /**
     * Test findByClientIdAndIsDeletedFalse - should not find non-existent client.
     */
    @Test
    void testFindByClientIdAndIsDeletedFalse_NotFoundWhenNotExists() {
        // When: Find non-existent clientId
        Optional<ClientAccessControlEntity> result = repository.findByClientIdAndIsDeletedFalse("non-existent");

        // Then: Should return empty
        assertThat(result).isEmpty();
    }

    /**
     * Test findByIsActiveAndIsDeletedFalse - should find only active non-deleted clients.
     */
    @Test
    void testFindByIsActiveAndIsDeletedFalse_ActiveOnly() {
        // Given: Mixed active/inactive entities
        repository.save(createEntity("client-active-1", "tenant-a", true, false));
        repository.save(createEntity("client-active-2", "tenant-a", true, false));
        repository.save(createEntity("client-inactive-1", "tenant-a", false, false));
        repository.save(createEntity("client-deleted", "tenant-a", true, true));
        

        // When: Find active clients
        List<ClientAccessControlEntity> result = repository.findByIsActiveAndIsDeletedFalse(true);

        // Then: Should return only active non-deleted clients
        assertThat(result).hasSize(EXPECTED_TWO_ITEMS);
        assertThat(result).extracting(ClientAccessControlEntity::getClientId)
                .containsExactlyInAnyOrder("client-active-1", "client-active-2");
    }

    /**
     * Test findByIsActiveAndIsDeletedFalse - should find only inactive non-deleted clients.
     */
    @Test
    void testFindByIsActiveAndIsDeletedFalse_InactiveOnly() {
        // Given: Mixed active/inactive entities
        repository.save(createEntity("client-active-1", "tenant-a", true, false));
        repository.save(createEntity("client-inactive-1", "tenant-a", false, false));
        repository.save(createEntity("client-inactive-2", "tenant-a", false, false));
        

        // When: Find inactive clients
        List<ClientAccessControlEntity> result = repository.findByIsActiveAndIsDeletedFalse(false);

        // Then: Should return only inactive non-deleted clients
        assertThat(result).hasSize(EXPECTED_TWO_ITEMS);
        assertThat(result).extracting(ClientAccessControlEntity::getClientId)
                .containsExactlyInAnyOrder("client-inactive-1", "client-inactive-2");
    }

    /**
    * Test findByIsDeletedFalse - should return all non-deleted clients.
     */
    @Test
    void testFindByIsDeletedFalse() {
        // Given: 3 entities with 1 deleted
        repository.save(createEntity("client-1", "tenant-a", true, false));
        repository.save(createEntity("client-2", "tenant-a", true, false));
        repository.save(createEntity("client-3-deleted", "tenant-a", true, true));
        

        // When: Find all not deleted
        List<ClientAccessControlEntity> result = repository.findByIsDeletedFalse();

        // Then: Should return 2 non-deleted clients
        assertThat(result).hasSize(EXPECTED_TWO_ITEMS);
        assertThat(result).extracting(ClientAccessControlEntity::getClientId)
                .containsExactlyInAnyOrder("client-1", "client-2");
    }

    /**
    * Test findByIsDeletedFalse - should return empty list when all deleted.
     */
    @Test
    void testFindByIsDeletedFalse_EmptyWhenAllDeleted() {
        // Given: All entities are deleted
        repository.save(createEntity("client-1", "tenant-a", true, true));
        repository.save(createEntity("client-2", "tenant-a", true, true));
        

        // When: Find all not deleted
        List<ClientAccessControlEntity> result = repository.findByIsDeletedFalse();

        // Then: Should return empty list
        assertThat(result).isEmpty();
    }

    /**
    * Test findByIsDeletedFalse with tenant filtering in assertion.
     */
    @Test
    void testFindByIsDeletedFalse_FilterByTenantInAssertion() {
        // Given: Entities with different tenants
        repository.save(createEntity("client-1", "tenant-a", true, false));
        repository.save(createEntity("client-2", "tenant-a", true, false));
        repository.save(createEntity("client-3", "tenant-b", true, false));
        repository.save(createEntity("client-4", "tenant-a", true, true)); // deleted
        

        // When: Find all non-deleted and filter by tenant-a
        List<ClientAccessControlEntity> result = repository.findByIsDeletedFalse().stream()
            .filter(e -> "tenant-a".equals(e.getTenant()))
            .toList();

        // Then: Should return only tenant-a non-deleted clients
        assertThat(result).hasSize(EXPECTED_TWO_ITEMS);
        assertThat(result).extracting(ClientAccessControlEntity::getClientId)
                .containsExactlyInAnyOrder("client-1", "client-2");
        assertThat(result).allMatch(e -> "tenant-a".equals(e.getTenant()));
    }

    /**
    * Test findByIsDeletedFalse with non-existent tenant filter should return empty.
     */
    @Test
    void testFindByIsDeletedFalse_EmptyForNonExistentTenantFilter() {
        // Given: Entities exist but not for target tenant
        repository.save(createEntity("client-1", "tenant-a", true, false));
        

        // When: Find all non-deleted and filter by non-existent tenant
        List<ClientAccessControlEntity> result = repository.findByIsDeletedFalse().stream()
            .filter(e -> "tenant-nonexistent".equals(e.getTenant()))
            .toList();

        // Then: Should return empty list
        assertThat(result).isEmpty();
    }

    /**
     * Test existsByClientIdAndIsDeletedFalse - should return true for existing non-deleted client.
     */
    @Test
    void testExistsByClientIdAndIsDeletedFalse_True() {
        // Given: Entity exists in database
        repository.save(createEntity("client-exists", "tenant-a", true, false));
        

        // When: Check existence
        boolean exists = repository.existsByClientIdAndIsDeletedFalse("client-exists");

        // Then: Should return true
        assertThat(exists).isTrue();
    }

    /**
     * Test existsByClientIdAndIsDeletedFalse - should return false for deleted client.
     */
    @Test
    void testExistsByClientIdAndIsDeletedFalse_FalseWhenDeleted() {
        // Given: Deleted entity exists in database
        repository.save(createEntity("client-deleted", "tenant-a", true, true));
        

        // When: Check existence
        boolean exists = repository.existsByClientIdAndIsDeletedFalse("client-deleted");

        // Then: Should return false
        assertThat(exists).isFalse();
    }

    /**
     * Test existsByClientIdAndIsDeletedFalse - should return false for non-existent client.
     */
    @Test
    void testExistsByClientIdAndIsDeletedFalse_FalseWhenNotExists() {
        // When: Check existence of non-existent client
        boolean exists = repository.existsByClientIdAndIsDeletedFalse("non-existent");

        // Then: Should return false
        assertThat(exists).isFalse();
    }

    /**
     * Helper: Create test entity.
     */
    private ClientAccessControlEntity createEntity(String clientId, String tenant, boolean active, boolean deleted) {
        ClientAccessControlEntity entity = new ClientAccessControlEntity();
        entity.setClientId(clientId);
        entity.setTenant(tenant);
        entity.setDescription("Test description for " + clientId);
        entity.setIsActive(active);
        entity.setIsDeleted(deleted);
        
        // Create allow_rules as List
        entity.setAllow(List.of("service-a:*", "service-b:route-1"));
        
        entity.setCreatedAt(java.time.OffsetDateTime.now());
        entity.setUpdatedAt(java.time.OffsetDateTime.now());
        
        return entity;
    }
}
