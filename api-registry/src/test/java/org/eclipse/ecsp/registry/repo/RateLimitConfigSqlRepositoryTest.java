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

import org.eclipse.ecsp.registry.entity.RateLimitConfigEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link RateLimitConfigSqlRepository}.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitConfigSqlRepositoryTest {

    private static final int INT_300 = 300;

    private static final int INT_2 = 2;

    private static final int INT_200 = 200;

    private static final int INT_150 = 150;

    private static final int INT_100 = 100;

    @Mock
    private RateLimitConfigJpaRepository jpaRepository;

    private RateLimitConfigSqlRepository repository;

    @BeforeEach
    void setUp() {
        repository = new RateLimitConfigSqlRepository(jpaRepository);
    }

    @Test
    void findByIdWithExistingEntityReturnsEntity() {
        RateLimitConfigEntity entity = new RateLimitConfigEntity();
        entity.setId("test-id-1");
        entity.setService("test-service");
        entity.setReplenishRate(INT_100);
        entity.setBurstCapacity(INT_200);

        when(jpaRepository.findById("test-id-1")).thenReturn(Optional.of(entity));

        Optional<RateLimitConfigEntity> result = repository.findById("test-id-1");

        assertTrue(result.isPresent());
        assertEquals("test-id-1", result.get().getId());
        assertEquals("test-service", result.get().getService());
        verify(jpaRepository).findById("test-id-1");
    }

    @Test
    void findByIdWithNonExistingEntityReturnsEmpty() {
        when(jpaRepository.findById(anyString())).thenReturn(Optional.empty());

        Optional<RateLimitConfigEntity> result = repository.findById("non-existing-id");

        assertFalse(result.isPresent());
        verify(jpaRepository).findById("non-existing-id");
    }

    @Test
    void findAllWithMultipleEntitiesReturnsAllEntities() {
        RateLimitConfigEntity entity1 = new RateLimitConfigEntity();
        entity1.setId("test-id-1");
        entity1.setService("service-1");
        entity1.setReplenishRate(INT_100);
        entity1.setBurstCapacity(INT_200);

        RateLimitConfigEntity entity2 = new RateLimitConfigEntity();
        entity2.setId("test-id-2");
        entity2.setService("service-2");
        entity2.setReplenishRate(INT_150);
        entity2.setBurstCapacity(INT_300);

        when(jpaRepository.findAll()).thenReturn(Arrays.asList(entity1, entity2));

        List<RateLimitConfigEntity> result = repository.findAll();

        assertNotNull(result);
        assertEquals(INT_2, result.size());
        verify(jpaRepository).findAll();
    }

    @Test
    void findAllWithEmptyDatabaseReturnsEmptyList() {
        when(jpaRepository.findAll()).thenReturn(List.of());

        List<RateLimitConfigEntity> result = repository.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(jpaRepository).findAll();
    }

    @Test
    void saveWithNewEntityPersistsEntity() {
        RateLimitConfigEntity entity = new RateLimitConfigEntity();
        entity.setId("test-id-1");
        entity.setService("test-service");
        entity.setReplenishRate(INT_100);
        entity.setBurstCapacity(INT_200);

        when(jpaRepository.save(any(RateLimitConfigEntity.class))).thenReturn(entity);

        RateLimitConfigEntity saved = repository.save(entity);

        assertNotNull(saved);
        assertEquals("test-id-1", saved.getId());
        verify(jpaRepository).save(entity);
    }

    @Test
    void saveWithExistingEntityUpdatesEntity() {
        RateLimitConfigEntity entity = new RateLimitConfigEntity();
        entity.setId("test-id-1");
        entity.setService("test-service");
        entity.setReplenishRate(INT_150);
        entity.setBurstCapacity(INT_200);

        when(jpaRepository.save(any(RateLimitConfigEntity.class))).thenReturn(entity);

        RateLimitConfigEntity updated = repository.save(entity);

        assertEquals(INT_150, updated.getReplenishRate());
        verify(jpaRepository).save(entity);
    }

    @Test
    void deleteWithExistingEntityRemovesEntity() {
        RateLimitConfigEntity entity = new RateLimitConfigEntity();
        entity.setId("test-id-1");
        entity.setService("test-service");
        entity.setReplenishRate(INT_100);
        entity.setBurstCapacity(INT_200);

        repository.delete(entity);

        verify(jpaRepository).delete(entity);
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    void saveAllWithMultipleEntitiesPersistsAllEntities() {
        RateLimitConfigEntity entity1 = new RateLimitConfigEntity();
        entity1.setId("test-id-1");
        entity1.setService("service-1");
        entity1.setReplenishRate(INT_100);
        entity1.setBurstCapacity(INT_200);

        RateLimitConfigEntity entity2 = new RateLimitConfigEntity();
        entity2.setId("test-id-2");
        entity2.setService("service-2");
        entity2.setReplenishRate(INT_150);
        entity2.setBurstCapacity(300);

        List<RateLimitConfigEntity> entities = Arrays.asList(entity1, entity2);
        when(jpaRepository.saveAll(any())).thenReturn(entities);

        List<RateLimitConfigEntity> saved = repository.saveAll(entities);

        assertNotNull(saved);
        assertEquals(INT_2, saved.size());
        verify(jpaRepository).saveAll(entities);
    }

    @Test
    void findByServiceWithExistingServiceReturnsEntity() {
        RateLimitConfigEntity entity = new RateLimitConfigEntity();
        entity.setId("test-id-1");
        entity.setService("test-service");
        entity.setReplenishRate(INT_100);
        entity.setBurstCapacity(INT_200);

        when(jpaRepository.findByService("test-service")).thenReturn(Optional.of(entity));

        Optional<RateLimitConfigEntity> result = repository.findByService("test-service");

        assertTrue(result.isPresent());
        assertEquals("test-service", result.get().getService());
        verify(jpaRepository).findByService("test-service");
    }

    @Test
    void findByServiceWithNonExistingServiceReturnsEmpty() {
        when(jpaRepository.findByService(anyString())).thenReturn(Optional.empty());

        Optional<RateLimitConfigEntity> result = repository.findByService("non-existing-service");

        assertFalse(result.isPresent());
        verify(jpaRepository).findByService("non-existing-service");
    }
}
