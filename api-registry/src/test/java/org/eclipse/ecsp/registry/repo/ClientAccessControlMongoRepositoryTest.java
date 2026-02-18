package org.eclipse.ecsp.registry.repo;

import org.eclipse.ecsp.nosqldao.IgniteQuery;
import org.eclipse.ecsp.registry.entity.ClientAccessControlEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClientAccessControlMongoRepository}.
 *
 * <p>Verifies MongoDB repository operations using Ignite query framework
 * for CRUD operations with soft delete support.
 */
@ExtendWith(MockitoExtension.class)
class ClientAccessControlMongoRepositoryTest {

    @Mock
    private ClientAccessControlDaoImpl clientAccessControlDao;

    @InjectMocks
    private ClientAccessControlMongoRepository mongoRepository;

    @Captor
    private ArgumentCaptor<IgniteQuery> queryCaptor;

    private ClientAccessControlEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new ClientAccessControlEntity();
        testEntity.setId("client1");
        testEntity.setClientId("client1");
        testEntity.setIsDeleted(false);
        testEntity.setIsActive(true);
    }

    /**
     * Given an existing client ID (not deleted).
     * When findByClientIdAndIsDeletedFalse is called.
     * Then returns entity with correct query.
     */
    @Test
    void findByClientIdAndIsDeletedFalse_ExistingClient_ReturnsEntity() {
        final List<ClientAccessControlEntity> entities = Arrays.asList(testEntity);
        when(clientAccessControlDao.find(any(IgniteQuery.class))).thenReturn(entities);

        Optional<ClientAccessControlEntity> result = mongoRepository.findByClientIdAndIsDeletedFalse("client1");

        assertTrue(result.isPresent());
        assertEquals(testEntity, result.get());
        verify(clientAccessControlDao, times(1)).find(queryCaptor.capture());
        IgniteQuery query = queryCaptor.getValue();
        assertEquals(1, query.getPageNumber());
        assertEquals(1, query.getPageSize());
    }

    /**
     * Given a non-existing client ID.
     * When findByClientIdAndIsDeletedFalse is called.
     * Then returns empty optional.
     */
    @Test
    void findByClientIdAndIsDeletedFalse_NonExistingClient_ReturnsEmpty() {
        when(clientAccessControlDao.find(any(IgniteQuery.class))).thenReturn(new ArrayList<>());

        Optional<ClientAccessControlEntity> result = mongoRepository.findByClientIdAndIsDeletedFalse("nonexistent");

        assertFalse(result.isPresent());
        verify(clientAccessControlDao, times(1)).find(any(IgniteQuery.class));
    }

    /**
     * Given active status filter true.
     * When findByIsActiveAndIsDeletedFalse is called.
     * Then returns active entities with correct query.
     */
    @Test
    void findByIsActiveAndIsDeletedFalse_ActiveTrue_ReturnsActiveEntities() {
        final List<ClientAccessControlEntity> entities = Arrays.asList(testEntity);
        when(clientAccessControlDao.find(any(IgniteQuery.class))).thenReturn(entities);

        List<ClientAccessControlEntity> result = mongoRepository.findByIsActiveAndIsDeletedFalse(true);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
        verify(clientAccessControlDao, times(1)).find(queryCaptor.capture());
        IgniteQuery query = queryCaptor.getValue();
        assertEquals(1, query.getPageNumber());
        assertEquals(Integer.MAX_VALUE, query.getPageSize());
    }

    /**
     * Given active status filter false.
     * When findByIsActiveAndIsDeletedFalse is called.
     * Then returns inactive entities.
     */
    @Test
    void findByIsActiveAndIsDeletedFalse_ActiveFalse_ReturnsInactiveEntities() {
        testEntity.setIsActive(false);
        final List<ClientAccessControlEntity> entities = Arrays.asList(testEntity);
        when(clientAccessControlDao.find(any(IgniteQuery.class))).thenReturn(entities);

        List<ClientAccessControlEntity> result = mongoRepository.findByIsActiveAndIsDeletedFalse(false);

        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsActive());
        verify(clientAccessControlDao, times(1)).find(any(IgniteQuery.class));
    }

    /**
     * Given non-deleted entities exist.
     * When findByIsDeletedFalse is called.
     * Then returns all non-deleted entities.
     */
    @Test
    void findByIsDeletedFalse_NonDeletedEntitiesExist_ReturnsAllNonDeleted() {
        final List<ClientAccessControlEntity> entities = Arrays.asList(testEntity);
        when(clientAccessControlDao.find(any(IgniteQuery.class))).thenReturn(entities);

        List<ClientAccessControlEntity> result = mongoRepository.findByIsDeletedFalse();

        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsDeleted());
        verify(clientAccessControlDao, times(1)).find(queryCaptor.capture());
        IgniteQuery query = queryCaptor.getValue();
        assertEquals(1, query.getPageNumber());
        assertEquals(Integer.MAX_VALUE, query.getPageSize());
    }

    /**
     * Given no non-deleted entities.
     * When findByIsDeletedFalse is called.
     * Then returns empty list.
     */
    @Test
    void findByIsDeletedFalse_NoNonDeletedEntities_ReturnsEmptyList() {
        when(clientAccessControlDao.find(any(IgniteQuery.class))).thenReturn(new ArrayList<>());

        List<ClientAccessControlEntity> result = mongoRepository.findByIsDeletedFalse();

        assertTrue(result.isEmpty());
        verify(clientAccessControlDao, times(1)).find(any(IgniteQuery.class));
    }

    /**
     * Given an existing client ID (not deleted).
     * When existsByClientIdAndIsDeletedFalse is called.
     * Then returns true.
     */
    @Test
    void existsByClientIdAndIsDeletedFalse_ExistingClient_ReturnsTrue() {
        final List<ClientAccessControlEntity> entities = Arrays.asList(testEntity);
        when(clientAccessControlDao.find(any(IgniteQuery.class))).thenReturn(entities);

        boolean result = mongoRepository.existsByClientIdAndIsDeletedFalse("client1");

        assertTrue(result);
        verify(clientAccessControlDao, times(1)).find(queryCaptor.capture());
        IgniteQuery query = queryCaptor.getValue();
        assertEquals(1, query.getPageNumber());
        assertEquals(1, query.getPageSize());
    }

    /**
     * Given a non-existing client ID.
     * When existsByClientIdAndIsDeletedFalse is called.
     * Then returns false.
     */
    @Test
    void existsByClientIdAndIsDeletedFalse_NonExistingClient_ReturnsFalse() {
        when(clientAccessControlDao.find(any(IgniteQuery.class))).thenReturn(new ArrayList<>());

        boolean result = mongoRepository.existsByClientIdAndIsDeletedFalse("nonexistent");

        assertFalse(result);
        verify(clientAccessControlDao, times(1)).find(any(IgniteQuery.class));
    }

    /**
     * Given a valid entity.
     * When save is called.
     * Then delegates to DAO and returns saved entity.
     */
    @Test
    void save_ValidEntity_DelegatesToDaoAndReturnsSaved() {
        when(clientAccessControlDao.save(testEntity)).thenReturn(testEntity);

        ClientAccessControlEntity result = mongoRepository.save(testEntity);

        assertEquals(testEntity, result);
        verify(clientAccessControlDao, times(1)).save(testEntity);
    }

    /**
     * Given a list of entities.
     * When saveAll is called.
     * Then delegates to DAO and returns saved entities.
     */
    @Test
    void saveAll_ValidEntities_DelegatesToDaoAndReturnsList() {
        final List<ClientAccessControlEntity> entities = Arrays.asList(testEntity);
        final ClientAccessControlEntity[] entitiesArray = {testEntity};
        when(clientAccessControlDao.saveAll(any(ClientAccessControlEntity[].class))).thenReturn(entities);

        List<ClientAccessControlEntity> result = mongoRepository.saveAll(entities);

        assertEquals(1, result.size());
        assertEquals(testEntity, result.get(0));
        verify(clientAccessControlDao, times(1)).saveAll(any(ClientAccessControlEntity[].class));
    }

    /**
     * Given an entity to delete.
     * When delete is called.
     * Then delegates to DAO for deletion.
     */
    @Test
    void delete_ValidEntity_DelegatesToDao() {
        mongoRepository.delete(testEntity);

        verify(clientAccessControlDao, times(1)).delete(testEntity);
    }

    /**
     * Given a soft-deleted client ID.
     * When findByClientIdAndIsDeletedTrue is called.
     * Then returns deleted entity.
     */
    @Test
    void findByClientIdAndIsDeletedTrue_SoftDeletedClient_ReturnsEntity() {
        testEntity.setIsDeleted(true);
        final List<ClientAccessControlEntity> entities = Arrays.asList(testEntity);
        when(clientAccessControlDao.find(any(IgniteQuery.class))).thenReturn(entities);

        Optional<ClientAccessControlEntity> result = mongoRepository.findByClientIdAndIsDeletedTrue("client1");

        assertTrue(result.isPresent());
        assertTrue(result.get().getIsDeleted());
        verify(clientAccessControlDao, times(1)).find(queryCaptor.capture());
        IgniteQuery query = queryCaptor.getValue();
        assertNotNull(query);
        assertEquals(1, query.getPageNumber());
        assertEquals(1, query.getPageSize());
    }

    /**
     * Given no soft-deleted client.
     * When findByClientIdAndIsDeletedTrue is called.
     * Then returns empty optional.
     */
    @Test
    void findByClientIdAndIsDeletedTrue_NoSoftDeletedClient_ReturnsEmpty() {
        when(clientAccessControlDao.find(any(IgniteQuery.class))).thenReturn(new ArrayList<>());

        Optional<ClientAccessControlEntity> result = mongoRepository.findByClientIdAndIsDeletedTrue("client1");

        assertFalse(result.isPresent());
        verify(clientAccessControlDao, times(1)).find(any(IgniteQuery.class));
    }
}
