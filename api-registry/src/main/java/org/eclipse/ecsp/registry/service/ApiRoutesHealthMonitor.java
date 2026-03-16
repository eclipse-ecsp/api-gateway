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

import org.eclipse.ecsp.registry.entity.ApiRouteEntity;
import org.eclipse.ecsp.registry.events.EventPublisherContext;
import org.eclipse.ecsp.registry.events.data.ServiceHealthEventData;
import org.eclipse.ecsp.registry.metrics.ServiceMetricsEvent;
import org.eclipse.ecsp.registry.repo.ApiRouteRepo;
import org.eclipse.ecsp.registry.utils.RegistryConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * ApiRoutesHealthMonitor.
 */
@Service
@EnableScheduling
@EnableAsync
public class ApiRoutesHealthMonitor {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ApiRoutesHealthMonitor.class);
    private final Map<String, String> services = new HashMap<>();
    private final Map<String, List<ApiRouteEntity>> serviceApiRoutes = new HashMap<>();
    private final ApiRouteRepo apiRouteRepo;
    private final RestTemplate restTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final EventPublisherContext routeEventPublisher;

    /**
     * Constructor to initialize the ApiRoutesHealthMonitor.
     *
     * @param apiRouteRepo   the ApiRouteRepo
     * @param restTemplate   the RestTemplate
     * @param eventPublisher the ApplicationEventPublisher
     * @param routeEventPublisher the EventPublisherContext
     */
    public ApiRoutesHealthMonitor(ApiRouteRepo apiRouteRepo,
                                  RestTemplate restTemplate,
                                  ApplicationEventPublisher eventPublisher,
                                  Optional<EventPublisherContext> routeEventPublisher) {
        this.apiRouteRepo = apiRouteRepo;
        this.restTemplate = restTemplate;
        this.eventPublisher = eventPublisher;
        this.routeEventPublisher = routeEventPublisher.orElse(null);
    }

    /**
     * healthCheck is cron job to check the health status of components.
     */
    @Scheduled(cron = "${api.health.monitor}")
    @Async()
    public void healthCheck() {
        LOGGER.info("Service Health Check started...");
        services.clear();
        serviceApiRoutes.clear();
        Iterable<ApiRouteEntity> entities = apiRouteRepo.findAll();
        if (entities.spliterator().getExactSizeIfKnown() < 0) {
            LOGGER.info("Empty Routes found.");
            return;
        }

        Map<String, Boolean> previousStatus = new HashMap<>();
        Set<String> changedServices = new HashSet<>();
        // find out servers and each server apis
        entities.forEach(entity -> {
            String url = entity.getRoute().getUri().toString();
            // append with contextPath
            String contextPath = entity.getContextPath();
            if (contextPath != null && !contextPath.isEmpty()) {
                contextPath = contextPath.charAt(0) == '/' ? contextPath.substring(1) : contextPath;
                url = url + contextPath + RegistryConstants.PATH_DELIMITER;
                LOGGER.info("health check url: {}", url);
            }
            services.putIfAbsent(entity.getService(), url);
            previousStatus.putIfAbsent(entity.getService(), entity.getActive());
            // Add Api Routes
            serviceApiRoutes.computeIfAbsent(entity.getService(), k -> new ArrayList<>()).add(entity);
        });
        LOGGER.debug("Services: {}", services);
        // check health of each server
        services.forEach((name, url) -> {
            boolean currentStatus = getHealth(name, url, "actuator/health") || getHealth(name, url, "v1/health");
            Boolean prevStatus = previousStatus.get(name);
            // Track status changes
            if (prevStatus != null && !currentStatus == prevStatus) {
                changedServices.add(name);
                LOGGER.info("Service: {}, health status changed from: {} to: {}", name, prevStatus, currentStatus);
                update(name, currentStatus);
            }
        });
        // get the services which status has changed
        // Send event for all changed services at once
        if (!changedServices.isEmpty() && routeEventPublisher != null) {
            LOGGER.info("Services with health status changes: {}", changedServices);
            ServiceHealthEventData eventData = 
                new ServiceHealthEventData(new ArrayList<>(changedServices), java.util.Collections.emptyMap());
            routeEventPublisher.publishEvent(eventData);
        }
        LOGGER.info("Service Health Check completed.");
    }

    private boolean getHealth(String name, String url, String uri) {
        String healthUrl = url + uri;
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(healthUrl, String.class);
            LOGGER.debug("Url {}, Response: {}", healthUrl, resp);
            LOGGER.info("Server: {}, url: {}, status: {}", name, healthUrl, resp.getStatusCode());
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            LOGGER.warn("Server: {}, url: {}, Error: {}", name, healthUrl, e.getMessage(), e);
            // inactive all the APIs of this server
            return false;
        }
    }

    private void update(String service, Boolean active) {
        // inactive all the APIs of this server
        List<ApiRouteEntity> apiRoutes = serviceApiRoutes.get(service);
        if (apiRoutes != null && !apiRoutes.isEmpty()) {
            apiRoutes.forEach(apiRoute -> {
                if (!apiRoute.getActive().equals(active)) {
                    apiRoute.setActive(active);
                    apiRouteRepo.save(apiRoute);
                    LOGGER.warn("Updated ApiRoute: {}, active: {}", apiRoute.getId(), active);
                }
            });
        }

        //update the service metrics
        eventPublisher.publishEvent(
                new ServiceMetricsEvent(this, service, active,
                        apiRoutes != null ? apiRoutes.size() : 0));
    }
}
