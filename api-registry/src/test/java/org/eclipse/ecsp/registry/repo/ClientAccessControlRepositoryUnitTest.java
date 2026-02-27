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

package org.eclipse.ecsp.registry.repo;

import org.eclipse.ecsp.registry.entity.ClientAccessControlEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClientAccessControlSqlRepository}.
 *
 * <p>Verifies the SQL repository delegates correctly to JPA repository
 * for all CRUD operations while maintaining the repository abstraction.
 */
@ExtendWith(MockitoExtension.class)
class ClientAccessControlRepositoryUnitTest {

    @Mock
    private ClientAccessControlJpaRepository jpaRepository;

    @InjectMocks
    private ClientAccessControlSqlRepository sqlRepository;

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
     * Given a valid entity.
     * When save is called.
     * Then delegates to JPA repository and returns saved entity.
     */
    @Test
    void saveValidEntityDelegatesToJpaAndReturnsSaved() {
        when(jpaRepository.save(testEntity)).thenReturn(testEntity);

        ClientAccessControlEntity result = sqlRepository.save(testEntity);

        assertEquals(testEntity, result);
        verify(jpaRepository, times(1)).save(testEntity);
    }

    /**
     * Given a list of entities.
     * When saveAll is called.
     * Then delegates to JPA repository and returns saved entities as list.
     */
    @Test
    void saveAllValidEntitiesDelegatesToJpaAndReturnsList() {
        final List<ClientAccessControlEntity> entities = Arrays.asList(testEntity);
        when(jpaRepository.saveAll(entities)).thenReturn(entities);

        List<ClientAccessControlEntity> result = sqlRepository.saveAll(entities);

        assertEquals(1, result.size());
        assertEquals(testEntity, result.get(0));
        verify(jpaRepository, times(1)).saveAll(entities);
    }

    /**
     * Given an existing client ID (not deleted).
     * When findByClientIdAndIsDeletedFalse is called.
     * Then delegates to JPA repository and returns entity.
     */
    @Test
    void findByClientIdAndIsDeletedFalseExistingClientReturnsEntity() {
        when(jpaRepository.findByClientIdAndIsDeletedFalse("client1")).thenReturn(Optional.of(testEntity));

        Optional<ClientAccessControlEntity> result = sqlRepository.findByClientIdAndIsDeletedFalse("client1");

        assertTrue(result.isPresent());
        assertEquals(testEntity, result.get());
        verify(jpaRepository, times(1)).findByClientIdAndIsDeletedFalse("client1");
    }

    /**
     * Given a non-existing client ID.
     * When findByClientIdAndIsDeletedFalse is called.
     * Then delegates to JPA repository and returns empty.
     */
    @Test
    void findByClientIdAndIsDeletedFalseNonExistingClientReturnsEmpty() {
        when(jpaRepository.findByClientIdAndIsDeletedFalse("nonexistent")).thenReturn(Optional.empty());

        Optional<ClientAccessControlEntity> result = sqlRepository.findByClientIdAndIsDeletedFalse("nonexistent");

        assertFalse(result.isPresent());
        verify(jpaRepository, times(1)).findByClientIdAndIsDeletedFalse("nonexistent");
    }

    /**
     * Given active status filter true.
     * When findByIsActiveAndIsDeletedFalse is called.
     * Then delegates to JPA repository and returns active entities.
     */
    @Test
    void findByIsActiveAndIsDeletedFalseActiveTrueReturnsActiveEntities() {
        final List<ClientAccessControlEntity> entities = Arrays.asList(testEntity);
        when(jpaRepository.findByIsActiveAndIsDeletedFalse(true)).thenReturn(entities);

        List<ClientAccessControlEntity> result = sqlRepository.findByIsActiveAndIsDeletedFalse(true);

        assertEquals(1, result.size());
        assertEquals(testEntity, result.get(0));
        verify(jpaRepository, times(1)).findByIsActiveAndIsDeletedFalse(true);
    }

    /**
     * Given active status filter false.
     * When findByIsActiveAndIsDeletedFalse is called.
     * Then delegates to JPA repository and returns inactive entities.
     */
    @Test
    void findByIsActiveAndIsDeletedFalseActiveFalseReturnsInactiveEntities() {
        testEntity.setIsActive(false);
        final List<ClientAccessControlEntity> entities = Arrays.asList(testEntity);
        when(jpaRepository.findByIsActiveAndIsDeletedFalse(false)).thenReturn(entities);

        List<ClientAccessControlEntity> result = sqlRepository.findByIsActiveAndIsDeletedFalse(false);

        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsActive());
        verify(jpaRepository, times(1)).findByIsActiveAndIsDeletedFalse(false);
    }

    /**
     * Given non-deleted entities exist.
     * When findByIsDeletedFalse is called.
     * Then delegates to JPA repository and returns all non-deleted entities.
     */
    @Test
    void findByIsDeletedFalseNonDeletedEntitiesExistReturnsAllNonDeleted() {
        final List<ClientAccessControlEntity> entities = Arrays.asList(testEntity);
        when(jpaRepository.findByIsDeletedFalse()).thenReturn(entities);

        List<ClientAccessControlEntity> result = sqlRepository.findByIsDeletedFalse();

        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsDeleted());
        verify(jpaRepository, times(1)).findByIsDeletedFalse();
    }

    /**
     * Given no non-deleted entities.
     * When findByIsDeletedFalse is called.
     * Then delegates to JPA repository and returns empty list.
     */
    @Test
    void findByIsDeletedFalseNoNonDeletedEntitiesReturnsEmptyList() {
        when(jpaRepository.findByIsDeletedFalse()).thenReturn(new ArrayList<>());

        List<ClientAccessControlEntity> result = sqlRepository.findByIsDeletedFalse();

        assertTrue(result.isEmpty());
        verify(jpaRepository, times(1)).findByIsDeletedFalse();
    }

    /**
     * Given an existing client ID (not deleted).
     * When existsByClientIdAndIsDeletedFalse is called.
     * Then delegates to JPA repository and returns true.
     */
    @Test
    void existsByClientIdAndIsDeletedFalseExistingClientReturnsTrue() {
        when(jpaRepository.existsByClientIdAndIsDeletedFalse("client1")).thenReturn(true);

        boolean result = sqlRepository.existsByClientIdAndIsDeletedFalse("client1");

        assertTrue(result);
        verify(jpaRepository, times(1)).existsByClientIdAndIsDeletedFalse("client1");
    }

    /**
     * Given a non-existing client ID.
     * When existsByClientIdAndIsDeletedFalse is called.
     * Then delegates to JPA repository and returns false.
     */
    @Test
    void existsByClientIdAndIsDeletedFalseNonExistingClientReturnsFalse() {
        when(jpaRepository.existsByClientIdAndIsDeletedFalse("nonexistent")).thenReturn(false);

        boolean result = sqlRepository.existsByClientIdAndIsDeletedFalse("nonexistent");

        assertFalse(result);
        verify(jpaRepository, times(1)).existsByClientIdAndIsDeletedFalse("nonexistent");
    }

    /**
     * Given a soft-deleted client ID.
     * When findByClientIdAndIsDeletedTrue is called.
     * Then delegates to JPA repository and returns deleted entity.
     */
    @Test
    void findByClientIdAndIsDeletedTrueSoftDeletedClientReturnsEntity() {
        testEntity.setIsDeleted(true);
        when(jpaRepository.findByClientIdAndIsDeletedTrue("client1")).thenReturn(Optional.of(testEntity));

        Optional<ClientAccessControlEntity> result = sqlRepository.findByClientIdAndIsDeletedTrue("client1");

        assertTrue(result.isPresent());
        assertTrue(result.get().getIsDeleted());
        verify(jpaRepository, times(1)).findByClientIdAndIsDeletedTrue("client1");
    }

    /**
     * Given an entity to delete.
     * When delete is called.
     * Then delegates to JPA repository for deletion.
     */
    @Test
    void deleteValidEntityDelegatesToJpa() {
        sqlRepository.delete(testEntity);

        verify(jpaRepository, times(1)).delete(testEntity);
    }
}
