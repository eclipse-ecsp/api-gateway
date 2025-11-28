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

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Rate Limit Configurations.
 */
public interface RateLimitConfigRepository {

    /**
     * Finds a RateLimitConfigEntity by its ID.
     *
     * @param id the ID of the RateLimitConfigEntity
     * @return an Optional containing the found RateLimitConfigEntity, or empty if not found
     */
    Optional<RateLimitConfigEntity> findById(String id);

    /**
     * Finds all RateLimitConfigEntities.
     *
     * @return a list of all RateLimitConfigEntities
     */
    List<RateLimitConfigEntity> findAll();

    /**
     * Saves a RateLimitConfigEntity to the database.
     *
     * @param entity the RateLimitConfigEntity to save
     * @return the saved RateLimitConfigEntity
     */
    RateLimitConfigEntity save(RateLimitConfigEntity entity);

    /**
     * Deletes a RateLimitConfigEntity from the database.
     *
     * @param entity the RateLimitConfigEntity to delete
     */
    void delete(RateLimitConfigEntity entity);


    /**
     * Saves a list of RateLimitConfigEntities to the database.
     *
     * @param entities the list of RateLimitConfigEntities to save
     * @return the list of saved RateLimitConfigEntities
     */
    List<RateLimitConfigEntity> saveAll(List<RateLimitConfigEntity> entities);


    /**
     * Finds a RateLimitConfigEntity by its service name.
     *
     * @param service the service name of the RateLimitConfigEntity
     * @return an Optional containing the found RateLimitConfigEntity, or empty if not found
     */
    Optional<RateLimitConfigEntity> findByService(String service);
}
