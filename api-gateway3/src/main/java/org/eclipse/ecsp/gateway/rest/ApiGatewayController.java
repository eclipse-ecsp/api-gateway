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

package org.eclipse.ecsp.gateway.rest;

import org.eclipse.ecsp.gateway.model.ApiService;
import org.eclipse.ecsp.gateway.model.Health;
import org.eclipse.ecsp.gateway.service.IgniteRouteLocator;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Set;
import java.util.TreeSet;

/**
 * Controller tot get the api docs of microservices.
 */
@RestController
public class ApiGatewayController {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ApiGatewayController.class);

    @Value("${api.registered.services}")
    private String registeredServices;

    private final IgniteRouteLocator igniteRouteLocator;

    /**
     * Constructor to initialize the ApiGatewayController.
     *
     * @param igniteRouteLocator the IgniteRouteLocator
     */
    public ApiGatewayController(IgniteRouteLocator igniteRouteLocator) {
        this.igniteRouteLocator = igniteRouteLocator;
    }

    /**
     * to get the health of the api-gateway.
     *
     * @return returns the health status
     */
    @GetMapping(path = "/api-gateway/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<Health> health() {
        return Mono.just(new Health("UP"));
    }

    /**
     * to refresh the routes.
     */
    @GetMapping(path = "/api-gateway/reload", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reload() {
        LOGGER.info("Refresh Routes");
        if (igniteRouteLocator != null) {
            igniteRouteLocator.refreshRoutes();
        }
    }

    /**
     * to get the Api Docs.
     *
     * @return returns the Api routes
     */
    @GetMapping(path = "/v3/api-docs/swagger-config", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Flux<ApiService> getApiDocs() {
        if (igniteRouteLocator != null && igniteRouteLocator.getApiDocRoutes() != null) {
            LOGGER.debug("Swagger-Config-API Response: {}", igniteRouteLocator.getApiDocRoutes());
            return Flux.fromIterable(igniteRouteLocator.getApiDocRoutes());
        } else {
            Set<ApiService> apiDocRoutes = new TreeSet<>();
            String[] services = registeredServices.split(",");
            for (String service : services) {
                String path = "/v3/api-docs/" + service;
                ApiService apiservice = new ApiService(service, path, "Api-Docs of " + service);
                apiDocRoutes.add(apiservice);
            }
            return Flux.fromIterable(apiDocRoutes);
        }
    }

}