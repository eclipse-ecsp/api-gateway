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

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Client Access Control entities.
 *
 * <p>Defines methods for CRUD operations and custom queries related to client access control.
 * Implemented by both JPA and MongoDB repositories to abstract data access layer.
 */
public interface ClientAccessControlRepository {

    /**
     * Save a client access control entity.
     *
     * @param entity the entity to save
     * @return the saved entity
     */
    ClientAccessControlEntity save(ClientAccessControlEntity entity);

    /**
     * Save a list of client access control entities in bulk.
     *
     * @param entities the list of entities to save
     * @return the list of saved entities
     */
    List<ClientAccessControlEntity> saveAll(Iterable<ClientAccessControlEntity> entities);

    /**
     * Find by client ID (not deleted).
     *
     * @param clientId Client identifier
     * @return Optional entity, empty if not found or deleted
     */
    Optional<ClientAccessControlEntity> findByClientIdAndIsDeletedFalse(String clientId);

    /**
     * Find all active clients (not deleted).
     *
     * @param isActive Active status filter
     * @return List of entities matching criteria
     */
    List<ClientAccessControlEntity> findByIsActiveAndIsDeletedFalse(boolean isActive);

    /**
     * Find all non-deleted clients.
     *
     * @return List of entities
     */
    List<ClientAccessControlEntity> findByIsDeletedFalse();

    /**
     * Check if client ID exists (not deleted).
     *
     * @param clientId Client identifier
     * @return true if exists
     */
    boolean existsByClientIdAndIsDeletedFalse(String clientId);

    /**
     * Find by client ID (soft-deleted only).
     *
     * @param clientId Client identifier
     * @return Optional entity, empty if not found or not deleted
     */
    Optional<ClientAccessControlEntity> findByClientIdAndIsDeletedTrue(String clientId);

    /**
     * Delete a client access control entity.
     *
     * @param entity the entity to delete
     */
    void delete(ClientAccessControlEntity entity);
}
