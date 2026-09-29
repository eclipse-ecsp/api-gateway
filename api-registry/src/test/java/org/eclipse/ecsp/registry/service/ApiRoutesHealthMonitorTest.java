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
import org.eclipse.ecsp.registry.events.EventPublisherContext;
import org.eclipse.ecsp.registry.repo.ApiRouteRepo;
import org.eclipse.ecsp.registry.utils.RegistryTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for ApiRoutesHealthMonitor.
 */
@ExtendWith(SpringExtension.class)
class ApiRoutesHealthMonitorTest {

    private ApiRoutesHealthMonitor apiRoutesHealthMonitor;

    @Mock
    private ApiRouteRepo apiRouteRepo;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private EventPublisherContext routeEventPublisher;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        apiRoutesHealthMonitor = new ApiRoutesHealthMonitor(apiRouteRepo, 
                restTemplate, eventPublisher, Optional.of(routeEventPublisher));
    }

    @Test
    void healthCheckTest() {
        // Scenario 1: No API routes found
        when(apiRouteRepo.findAll()).thenReturn(new ArrayList<>());
        apiRoutesHealthMonitor.healthCheck();
        verify(restTemplate, never()).getForEntity(Mockito.anyString(), Mockito.eq(String.class));

        // Scenario 2: API routes found, health check succeeds
        ApiRouteEntity apiRouteEntity = RegistryTestUtil.getApiRouteEntity();
        ArrayList<ApiRouteEntity> apiRouteEntities = new ArrayList<>();
        apiRouteEntities.add(apiRouteEntity);
        when(apiRouteRepo.findAll()).thenReturn(apiRouteEntities);
        when(restTemplate.getForEntity(Mockito.anyString(), Mockito.any()))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));
        apiRoutesHealthMonitor.healthCheck();
        verify(restTemplate, Mockito.atLeastOnce()).getForEntity(Mockito.anyString(), Mockito.eq(String.class));

        // Scenario 3: API routes found, health check fails
        when(restTemplate.getForEntity(Mockito.anyString(), Mockito.any()))
                .thenThrow(new RuntimeException("Health check failed"));
        apiRoutesHealthMonitor.healthCheck();
        verify(restTemplate, Mockito.atLeastOnce()).getForEntity(Mockito.anyString(), Mockito.eq(String.class));
    }

    @Test
    void retiredRouteIsNeverReactivatedOrDeactivatedByHealthMonitor() {
        ApiRouteEntity normalRoute = RegistryTestUtil.getApiRouteEntity();
        normalRoute.setId("normal-route");
        normalRoute.setActive(Boolean.TRUE);

        ApiRouteEntity retiredRoute = RegistryTestUtil.getApiRouteEntity();
        retiredRoute.setId("retired-route");
        retiredRoute.setActive(Boolean.TRUE);
        RouteDefinition retiredDefinition = RegistryTestUtil.getRouteDefination();
        retiredDefinition.getMetadata().put("x-registration-retired", Boolean.TRUE);
        retiredRoute.setRoute(retiredDefinition);

        ArrayList<ApiRouteEntity> entities = new ArrayList<>(List.of(normalRoute, retiredRoute));
        when(apiRouteRepo.findAll()).thenReturn(entities);

        // First health check establishes the healthy baseline for both routes' shared service.
        when(restTemplate.getForEntity(Mockito.anyString(), Mockito.any()))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));
        apiRoutesHealthMonitor.healthCheck();

        // Second health check: the service becomes unhealthy, triggering update(service, false).
        when(restTemplate.getForEntity(Mockito.anyString(), Mockito.any()))
                .thenThrow(new RuntimeException("down"));
        apiRoutesHealthMonitor.healthCheck();

        verify(apiRouteRepo).save(Mockito.argThat(e -> "normal-route".equals(e.getId())));
        verify(apiRouteRepo, never()).save(Mockito.argThat(e -> "retired-route".equals(e.getId())));
    }

}