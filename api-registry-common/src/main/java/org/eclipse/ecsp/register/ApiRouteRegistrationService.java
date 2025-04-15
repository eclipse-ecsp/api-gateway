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

package org.eclipse.ecsp.register;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.security.Scopes;
import org.eclipse.ecsp.utils.Constants;
import org.eclipse.ecsp.utils.ObjectMapperUtil;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Register the routes with Api Gateway.
 */
@Service
@ConditionalOnProperty(value = "api.registry.enabled", havingValue = "true", matchIfMissing = false)
public class ApiRouteRegistrationService {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ApiRouteRegistrationService.class);
    private static final ObjectMapper MAPPER = ObjectMapperUtil.getObjectMapper();

    @Value("${api.registry.service_name}")
    private String registryServiceName;

    private final ApiRoutesLoader apiRoutesLoader;
    private final RestTemplate restTemplate;

    /**
     * Constructor to initialize the ApiRouteRegistrationService.
     *
     * @param apiRoutesLoader the ApiRoutesLoader
     * @param restTemplate  the RestTemplate
     */
    public ApiRouteRegistrationService(ApiRoutesLoader apiRoutesLoader, RestTemplate restTemplate) {
        this.apiRoutesLoader = apiRoutesLoader;
        this.restTemplate = restTemplate;
    }

    /**
     * registers the routes to the Api Gateway.
     *
     * @throws URISyntaxException throws wxception
     */
    @EventListener(classes = {ApplicationReadyEvent.class, ContextRefreshedEvent.class})
    public void register() throws URISyntaxException {
        LOGGER.info("Loading APIs to registry...");
        List<RouteDefinition> apiRoutes = apiRoutesLoader.getApiRoutes();
        apiRoutes.forEach(route -> {
            try {
                LOGGER.info("Route : {}", MAPPER.writeValueAsString(route));
            } catch (JsonProcessingException ex) {
                LOGGER.warn("Failed to register application: {}", ex.getMessage());
            }
        });
        registerApiRoutes(apiRoutes);
        LOGGER.info("APIs loaded to registry...!");
    }

    private void registerApiRoutes(final List<RouteDefinition> apiRoutes) {
        apiRoutes.forEach(route -> {
            LOGGER.info("Registering route: {}", route);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(Constants.USER_ID, "1");
            headers.set(Constants.SCOPE, Scopes.Fields.SYSTEM_MANAGE);
            HttpEntity<RouteDefinition> entity = new HttpEntity<>(route, headers);
            String apiRegistryUrl = registryServiceName + Constants.COLON + Constants.PORT + Constants.POST_ENDPOINT;
            LOGGER.info("Registry POST-API: {}", apiRegistryUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(apiRegistryUrl, entity, String.class);
            if (response == null || !response.getStatusCode().is2xxSuccessful()) {
                LOGGER.warn("Error while adding route: {}", route);
            }
        });
    }

}
