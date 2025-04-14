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

import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.registry.service.ApiRouteService;
import org.eclipse.ecsp.registry.utils.RegistryTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.List;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for ApiRouteController.
 */
@ExtendWith(SpringExtension.class)
class ApiRouteControllerTest {

    @InjectMocks
    private ApiRouteController apiRouteController;

    @Mock
    private ApiRouteService apiRouteService;

    @BeforeEach
    void beforeEach() {
        initMocks(this);
    }

    @Test
    void testCreate() {
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        Mockito.when(apiRouteService.createOrUpdate(Mockito.any())).thenReturn(routeDefinition);
        Assertions.assertNotNull(apiRouteController.create(routeDefinition));
        Mockito.verify(apiRouteService, Mockito.atLeastOnce()).createOrUpdate(Mockito.any());
    }

    @Test
    void testList() {
        Mockito.doReturn(List.of()).when(apiRouteService).list();
        Assertions.assertNotNull(apiRouteController.list());
        Mockito.verify(apiRouteService, Mockito.atLeastOnce()).list();
    }

    @Test
    void testGet() {
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        Mockito.when(apiRouteService.read(Mockito.any())).thenReturn(routeDefinition);
        Assertions.assertNotNull(apiRouteController.get("routeId"));
        Mockito.verify(apiRouteService, Mockito.atLeastOnce()).read(Mockito.anyString());
    }

    @Test
    void testDelete() {
        apiRouteController.delete("routeId");
        Mockito.verify(apiRouteService, Mockito.atLeastOnce()).delete(Mockito.anyString());
    }
}
