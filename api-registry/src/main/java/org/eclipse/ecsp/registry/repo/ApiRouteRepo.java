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

import org.eclipse.ecsp.registry.entity.ApiRouteEntity;
import java.util.List;
import java.util.Optional;

/**
 * ApiRouteRepo to update the route definitions to DB.
 */
public interface ApiRouteRepo {

    /**
     * Finds an ApiRouteEntity by its ID.
     *
     * @param id the ID of the ApiRouteEntity
     * @return an Optional containing the found ApiRouteEntity, or empty if not found
     */
    Optional<ApiRouteEntity> findById(String id);

    /**
     * Finds all ApiRouteEntities.
     *
     * @return a list of all ApiRouteEntities
     */
    List<ApiRouteEntity> findAll();

    /**
     * Saves an ApiRouteEntity to the database.
     *
     * @param entity the ApiRouteEntity to save
     * @return the saved ApiRouteEntity
     */
    ApiRouteEntity save(ApiRouteEntity entity);

    /**
     * Deletes an ApiRouteEntity from the database.
     *
     * @param entity the ApiRouteEntity to delete
     */
    void delete(ApiRouteEntity entity);
}
