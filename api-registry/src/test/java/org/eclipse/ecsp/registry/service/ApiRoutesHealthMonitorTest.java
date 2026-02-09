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

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        apiRoutesHealthMonitor = new ApiRoutesHealthMonitor(apiRouteRepo, restTemplate, eventPublisher);
    }

    @Test
    void healthCheckTest() {
        // Scenario 1: No API routes found
        Mockito.when(apiRouteRepo.findAll()).thenReturn(new ArrayList<>());
        apiRoutesHealthMonitor.healthCheck();
        Mockito.verify(restTemplate, Mockito.never()).getForEntity(Mockito.anyString(), Mockito.eq(String.class));

        // Scenario 2: API routes found, health check succeeds
        ApiRouteEntity apiRouteEntity = RegistryTestUtil.getApiRouteEntity();
        ArrayList<ApiRouteEntity> apiRouteEntities = new ArrayList<>();
        apiRouteEntities.add(apiRouteEntity);
        Mockito.when(apiRouteRepo.findAll()).thenReturn(apiRouteEntities);
        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.any()))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));
        apiRoutesHealthMonitor.healthCheck();
        Mockito.verify(restTemplate, Mockito.atLeastOnce()).getForEntity(Mockito.anyString(), Mockito.eq(String.class));

        // Scenario 3: API routes found, health check fails
        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.any()))
                .thenThrow(new RuntimeException("Health check failed"));
        apiRoutesHealthMonitor.healthCheck();
        Mockito.verify(restTemplate, Mockito.atLeastOnce()).getForEntity(Mockito.anyString(), Mockito.eq(String.class));
    }

}