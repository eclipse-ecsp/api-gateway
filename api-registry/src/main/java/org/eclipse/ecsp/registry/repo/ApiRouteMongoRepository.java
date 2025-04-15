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

import org.eclipse.ecsp.registry.condition.ConditionalOnNoSqlDatabase;
import org.eclipse.ecsp.registry.entity.ApiRouteEntity;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * ApiRouteRepo to update the route definitions to NoSQL DB.
 */
@ConditionalOnNoSqlDatabase
@Repository("apiRouteMongoRepository")
public class ApiRouteMongoRepository implements ApiRouteRepo {

    private final ApiRouteMongoDaoImpl apiRouteMongoDao;

    /**
     * Constructor to initialize the ApiRouteMongoDaoImpl.
     *
     * @param apiRouteMongoDao the ApiRouteMongoDaoImpl
     */
    public ApiRouteMongoRepository(ApiRouteMongoDaoImpl apiRouteMongoDao) {
        this.apiRouteMongoDao = apiRouteMongoDao;
    }

    @Override
    public Optional<ApiRouteEntity> findById(String id) {
        return Optional.ofNullable(apiRouteMongoDao.findById(id));
    }

    @Override
    public List<ApiRouteEntity> findAll() {
        return apiRouteMongoDao.findAll();
    }

    @Override
    public ApiRouteEntity save(ApiRouteEntity entity) {
        return apiRouteMongoDao.save(entity);
    }

    @Override
    public void delete(ApiRouteEntity entity) {
        apiRouteMongoDao.delete(entity);
    }
}
