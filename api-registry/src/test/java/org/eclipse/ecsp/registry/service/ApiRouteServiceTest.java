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
import org.eclipse.ecsp.registry.utils.RegistryTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.Optional;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for ApiRouteService.
 */
@ExtendWith(SpringExtension.class)
class ApiRouteServiceTest {

    private ApiRouteService apiRouteService;

    @Mock
    private ApiRouteRepo apiRouteRepo;

    @BeforeEach
    void beforeEach() {
        initMocks(this);
        apiRouteService = new ApiRouteService(apiRouteRepo);
    }

    @Test
    void testCreateOrUpdate() {
        ApiRouteEntity apiRouteEntity = RegistryTestUtil.getApiRouteEntity();
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        Mockito.when(apiRouteRepo.save(Mockito.any())).thenReturn(apiRouteEntity);
        apiRouteService.createOrUpdate(routeDefinition);
        Mockito.when(apiRouteRepo.findById(Mockito.anyString())).thenReturn(Optional.of(new ApiRouteEntity()));
        Assertions.assertNotNull(apiRouteService.createOrUpdate(routeDefinition));
    }

    @Test
    void testCreateOrUpdateException() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> apiRouteService.createOrUpdate(null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> apiRouteService.createOrUpdate(new RouteDefinition()));
    }

    @Test
    void testList() {
        Assertions.assertNotNull(apiRouteService.list());
    }

    @Test
    void testRead() {
        Mockito.when(apiRouteRepo.findById(Mockito.anyString())).thenReturn(Optional.of(new ApiRouteEntity()));
        apiRouteService.read("routeId");
        Mockito.verify(apiRouteRepo, Mockito.atLeastOnce()).findById(Mockito.anyString());
    }

    @Test
    void testReadException() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> apiRouteService.read(null));
    }

    @Test
    void testDelete() {
        Mockito.when(apiRouteRepo.findById(Mockito.anyString())).thenReturn(Optional.of(new ApiRouteEntity()));
        apiRouteService.delete("routeId");
        Mockito.verify(apiRouteRepo, Mockito.atLeastOnce()).findById(Mockito.anyString());
    }

    @Test
    void testDeleteException() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> apiRouteService.delete(null));
    }

}
