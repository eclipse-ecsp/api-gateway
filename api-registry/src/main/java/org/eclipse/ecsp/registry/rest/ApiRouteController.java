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

package org.eclipse.ecsp.registry.rest;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.registry.service.ApiRouteService;
import org.eclipse.ecsp.registry.utils.RegistryConstants;
import org.eclipse.ecsp.security.Scopes;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * Controller to create,delete the routes.
 */
@RestController
public class ApiRouteController {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ApiRouteController.class);

    private final ApiRouteService apiRouteService;

    /**
     * Constructor to initialize the ApiRouteController.
     *
     * @param apiRouteService the ApiRouteService
     */
    public ApiRouteController(ApiRouteService apiRouteService) {
        this.apiRouteService = apiRouteService;
    }

    /**
     * register an api route.
     *
     * @param apiRoute the api route definition.
     * @return the route definition.
     */
    @Hidden
    @PostMapping(path = "/api/v1/routes",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = RegistryConstants.REGISTRY, scopes = {Scopes.Fields.SYSTEM_MANAGE})
    public RouteDefinition create(@RequestBody @Validated RouteDefinition apiRoute) {
        LOGGER.info("Upsert Route: {}", apiRoute);
        return apiRouteService.createOrUpdate(apiRoute);
    }

    /**
     * get all the api routes.
     *
     * @return list of routes.
     */
    @Hidden
    @GetMapping(path = "/api/v1/routes", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = RegistryConstants.REGISTRY, scopes = {Scopes.Fields.SYSTEM_READ})
    public List<RouteDefinition> list() {
        LOGGER.info("List Routes");
        return apiRouteService.list();
    }

    /**
     * get the api route by name.
     *
     * @param name the route name.
     * @return the route definition.
     */
    @Hidden
    @GetMapping(path = "/api/v1/routes/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = RegistryConstants.REGISTRY, scopes = {Scopes.Fields.SYSTEM_READ})
    public RouteDefinition get(@PathVariable String name) {
        LOGGER.info("Read Route: {}", name);
        return apiRouteService.read(name);
    }

    /**
     * delete the api route by name.
     *
     * @param name the route name.
     */
    @Hidden
    @DeleteMapping(path = "/api/v1/routes/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = RegistryConstants.REGISTRY, scopes = {Scopes.Fields.SYSTEM_MANAGE})
    public void delete(@PathVariable String name) {
        LOGGER.info("Delete Route: {}", name);
        apiRouteService.delete(name);
    }
}