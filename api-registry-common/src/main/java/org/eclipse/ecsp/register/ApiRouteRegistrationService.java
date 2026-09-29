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
import org.eclipse.ecsp.register.model.RouteSnapshotRequest;
import org.eclipse.ecsp.register.model.RouteSnapshotResult;
import org.eclipse.ecsp.security.Scopes;
import org.eclipse.ecsp.utils.ObjectMapperUtil;
import org.eclipse.ecsp.utils.RegistryCommonConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

    @Value("${api.registry.registration-mode:upsert}")
    private String registrationMode;

    @Value("${api.registry.registration-generation:0}")
    private long registrationGeneration;

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
                LOGGER.warn("Failed to register application: {}", ex);
            }
        });
        if (RegistryCommonConstants.REGISTRATION_MODE_RECONCILE.equalsIgnoreCase(registrationMode)) {
            registerSnapshot(apiRoutes);
        } else {
            registerApiRoutes(apiRoutes);
        }
        LOGGER.info("APIs loaded to registry...!");
    }

    private void registerApiRoutes(final List<RouteDefinition> apiRoutes) {
        apiRoutes.forEach(route -> {
            LOGGER.info("Registering route: {}", route);
            HttpEntity<RouteDefinition> entity = new HttpEntity<>(route, registryHeaders());
            String apiRegistryUrl = registryServiceName
                    + RegistryCommonConstants.COLON
                    + RegistryCommonConstants.PORT
                    + RegistryCommonConstants.POST_ENDPOINT;
            LOGGER.info("Registry POST-API: {}", apiRegistryUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(apiRegistryUrl, entity, String.class);
            if (response == null || !response.getStatusCode().is2xxSuccessful()) {
                LOGGER.warn("Error while adding route: {}", route);
            }
        });
    }

    /**
     * Submits the complete, de-duplicated desired route set for the service as one authoritative
     * snapshot (route snapshot reconciliation). Unlike {@link #registerApiRoutes(List)}, absence
     * from this set is meaningful: api-registry retires any previously-active route for the
     * service that this snapshot does not include.
     *
     * <p>Never falls back to {@link #registerApiRoutes(List)} on failure &mdash; doing so would
     * recreate the exact stale-route bug this mode exists to fix. The caller must fix the
     * configuration/deployment and retry; a retry with identical content is idempotent.
     *
     * @param apiRoutes the complete desired route set for this deployment
     */
    private void registerSnapshot(final List<RouteDefinition> apiRoutes) {
        if (registrationGeneration <= 0) {
            throw new IllegalStateException("api.registry.registration-generation must be a positive value "
                    + "when api.registry.registration-mode=reconcile");
        }
        if (apiRoutes.isEmpty()) {
            LOGGER.warn("No routes to reconcile; skipping snapshot registration");
            return;
        }
        String service = apiRoutes.get(0).getService();
        String snapshotId = SnapshotDigestUtil.compute(registrationGeneration, apiRoutes);
        RouteSnapshotRequest request = new RouteSnapshotRequest(registrationGeneration, snapshotId, apiRoutes);
        HttpEntity<RouteSnapshotRequest> entity = new HttpEntity<>(request, registryHeaders());
        String snapshotUrl = registryServiceName
                + RegistryCommonConstants.COLON
                + RegistryCommonConstants.PORT
                + RegistryCommonConstants.ROUTES_SERVICES_ENDPOINT_PREFIX
                + service
                + RegistryCommonConstants.SNAPSHOT_ENDPOINT_SUFFIX;
        LOGGER.info("Registry PUT-Snapshot-API: {}, generation={}, snapshotId={}",
                snapshotUrl, registrationGeneration, snapshotId);
        ResponseEntity<RouteSnapshotResult> response =
                restTemplate.exchange(snapshotUrl, HttpMethod.PUT, entity, RouteSnapshotResult.class);
        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            LOGGER.warn("Error while reconciling route snapshot for service: {}", service);
            return;
        }
        LOGGER.info("Route snapshot reconciled: {}", response.getBody());
    }

    private HttpHeaders registryHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(RegistryCommonConstants.USER_ID, "1");
        headers.set(RegistryCommonConstants.SCOPE, Scopes.Fields.SYSTEM_MANAGE);
        return headers;
    }

}
