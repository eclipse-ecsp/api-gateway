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

package org.eclipse.ecsp.registry.repo;

import org.eclipse.ecsp.registry.condition.ConditionalOnSqlDatabase;
import org.eclipse.ecsp.registry.entity.ClientAccessControlEntity;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL-backed implementation of {@link ClientAccessControlRepository}.
 * Delegates to {@link ClientAccessControlJpaRepository}.
 */
@ConditionalOnSqlDatabase
@Repository("clientAccessControlSqlRepository")
public class ClientAccessControlSqlRepository implements ClientAccessControlRepository {

    private final ClientAccessControlJpaRepository jpaRepository;

    /**
     * Constructor with JPA repository dependency.
     *
     * @param jpaRepository the JPA repository for client access control
     */
    public ClientAccessControlSqlRepository(ClientAccessControlJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ClientAccessControlEntity save(ClientAccessControlEntity entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public List<ClientAccessControlEntity> saveAll(Iterable<ClientAccessControlEntity> entities) {
        Iterable<ClientAccessControlEntity> savedEntities = jpaRepository.saveAll(entities);
        List<ClientAccessControlEntity> result = new ArrayList<>();
        savedEntities.forEach(result::add);
        return result;
    }

    @Override
    public Optional<ClientAccessControlEntity> findByClientIdAndIsDeletedFalse(String clientId) {
        return jpaRepository.findByClientIdAndIsDeletedFalse(clientId);
    }

    @Override
    public List<ClientAccessControlEntity> findByIsActiveAndIsDeletedFalse(boolean isActive) {
        return jpaRepository.findByIsActiveAndIsDeletedFalse(isActive);
    }

    @Override
    public List<ClientAccessControlEntity> findByIsDeletedFalse() {
        return jpaRepository.findByIsDeletedFalse();
    }

    @Override
    public boolean existsByClientIdAndIsDeletedFalse(String clientId) {
        return jpaRepository.existsByClientIdAndIsDeletedFalse(clientId);
    }

    @Override
    public Optional<ClientAccessControlEntity> findByClientIdAndIsDeletedTrue(String clientId) {
        return jpaRepository.findByClientIdAndIsDeletedTrue(clientId);
    }

    @Override
    public void delete(ClientAccessControlEntity entity) {
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }
}
