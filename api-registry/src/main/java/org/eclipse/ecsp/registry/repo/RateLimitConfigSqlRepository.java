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
import org.eclipse.ecsp.registry.entity.RateLimitConfigEntity;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL-backed implementation of {@link RateLimitConfigRepository}.
 * Delegates to {@link RateLimitConfigJpaRepository}.
 */
@ConditionalOnSqlDatabase
@Repository("rateLimitConfigSqlRepository")
public class RateLimitConfigSqlRepository implements RateLimitConfigRepository {

    private final RateLimitConfigJpaRepository jpaRepository;

    public RateLimitConfigSqlRepository(RateLimitConfigJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<RateLimitConfigEntity> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<RateLimitConfigEntity> findAll() {
        List<RateLimitConfigEntity> entities = new ArrayList<>();
        jpaRepository.findAll().forEach(entities::add);
        return entities;
    }

    @Override
    public RateLimitConfigEntity save(RateLimitConfigEntity entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public void delete(RateLimitConfigEntity entity) {
        jpaRepository.delete(entity);
    }

    @Override
    public List<RateLimitConfigEntity> saveAll(List<RateLimitConfigEntity> entities) {
        Iterable<RateLimitConfigEntity> savedEntities = jpaRepository.saveAll(entities);
        List<RateLimitConfigEntity> result = new ArrayList<>();
        savedEntities.forEach(result::add);
        return result;
    }

    @Override
    public Optional<RateLimitConfigEntity> findByService(String service) {
        return jpaRepository.findByService(service);
    }
}
