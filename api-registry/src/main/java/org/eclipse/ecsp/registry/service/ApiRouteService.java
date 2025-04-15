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

package org.eclipse.ecsp.registry.service;

import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.registry.entity.ApiRouteEntity;
import org.eclipse.ecsp.registry.repo.ApiRouteRepo;
import org.eclipse.ecsp.registry.utils.ApiRouteUtil;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * Service to create,delete the routes.
 */
@Service
public class ApiRouteService {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ApiRouteService.class);

    private final ApiRouteRepo apiRouteRepo;

    /**
     * Constructor to initialize the ApiRouteService.
     *
     * @param apiRouteRepo the ApiRouteRepo
     */
    public ApiRouteService(ApiRouteRepo apiRouteRepo) {
        this.apiRouteRepo = apiRouteRepo;
    }

    /**
     * Method create or updated the RouteDefinition.
     *
     * @param model models describes RouteDefinition object
     * @return return the RouteDefinition
     */
    public RouteDefinition createOrUpdate(RouteDefinition model) {
        if (model == null || model.getId() == null) {
            throw new IllegalArgumentException("Invalid route request");
        }
        Optional<ApiRouteEntity> entities = apiRouteRepo.findById(model.getId());
        ApiRouteEntity entity = null;
        if (entities.isPresent()) {
            // get ApiRoute object from DB
            entity = entities.get();
        } else {
            // create ApiRoute in DB
            entity = new ApiRouteEntity();
            entity.setId(model.getId());
        }
        entity.setRoute(model);
        entity.setService(model.getService());
        entity.setContextPath(model.getContextPath());
        if (Boolean.TRUE.equals(model.getApiDocs())) {
            entity.setApiDocs(model.getApiDocs());
        }
        entity.setActive(Boolean.TRUE);
        entity = apiRouteRepo.save(entity);
        LOGGER.info("Created/Updated ApiRoute: {}", entity.getId());
        return ApiRouteUtil.convert(entity);
    }

    /**
     * Method to list all the active RouteDefinitions.
     *
     * @return returns List of RouteDefinition
     */
    public List<RouteDefinition> list() {
        return ApiRouteUtil.convertActive(apiRouteRepo.findAll());
    }

    /**
     * Method to read the RouteDefinitions based on id.
     *
     * @param id id used to read RouteDefinitions
     * @return returns RouteDefinition
     */
    public RouteDefinition read(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Invalid route id");
        }
        Optional<ApiRouteEntity> result = apiRouteRepo.findById(id);
        if (result.isEmpty()) {
            throw new IllegalArgumentException("ApiRoute not found for id: " + id);
        }
        return ApiRouteUtil.convert(result.get());
    }

    /**
     * Delete the Routes based on ID.
     *
     * @param id id used to delet the RouteDefinition
     */
    public void delete(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Invalid route id");
        }
        Optional<ApiRouteEntity> result = apiRouteRepo.findById(id);
        if (result.isEmpty()) {
            throw new IllegalArgumentException("ApiRoute not found for id: " + id);
        }
        // deactivate ApiRoute in DB
        ApiRouteEntity entity = result.get();
        apiRouteRepo.delete(entity);
        LOGGER.info("Deleted ApiRoute: {}", entity.getId());
    }
}