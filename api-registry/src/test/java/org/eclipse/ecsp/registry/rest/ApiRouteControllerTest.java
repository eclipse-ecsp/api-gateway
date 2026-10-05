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
import org.eclipse.ecsp.register.model.RouteSnapshotRequest;
import org.eclipse.ecsp.register.model.RouteSnapshotResult;
import org.eclipse.ecsp.registry.service.ApiRouteService;
import org.eclipse.ecsp.registry.utils.RegistryTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for ApiRouteController.
 */
@ExtendWith(SpringExtension.class)
class ApiRouteControllerTest {

    private ApiRouteController apiRouteController;

    @Mock
    private ApiRouteService apiRouteService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        apiRouteController = new ApiRouteController(apiRouteService);
    }

    @Test
    void testCreate() {
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        when(apiRouteService.createOrUpdate(Mockito.any())).thenReturn(routeDefinition);
        Assertions.assertNotNull(apiRouteController.create(routeDefinition));
        verify(apiRouteService, Mockito.atLeastOnce()).createOrUpdate(Mockito.any());
    }

    @Test
    void testList() {
        doReturn(List.of()).when(apiRouteService).list();
        Assertions.assertNotNull(apiRouteController.list());
        verify(apiRouteService, Mockito.atLeastOnce()).list();
    }

    @Test
    void testGet() {
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        when(apiRouteService.read(Mockito.any())).thenReturn(routeDefinition);
        Assertions.assertNotNull(apiRouteController.get("routeId"));
        verify(apiRouteService, Mockito.atLeastOnce()).read(Mockito.anyString());
    }

    @Test
    void testDelete() {
        apiRouteController.delete("routeId");
        verify(apiRouteService, Mockito.atLeastOnce()).delete(Mockito.anyString());
    }

    @Test
    void resetSnapshotGenerationDelegatesToService() {
        apiRouteController.resetSnapshotGeneration("demo-service");
        verify(apiRouteService).resetReconciliationGeneration("demo-service");
    }

    @Test
    void idOnlySnapshotDelegatesToService() {
        RouteSnapshotRequest request = new RouteSnapshotRequest(1, "token", List.of("routeId"));
        RouteSnapshotResult result = new RouteSnapshotResult("demo-service", 1, "token", 0, 0, 1, 1);
        when(apiRouteService.reconcile("demo-service", request)).thenReturn(result);

        Assertions.assertEquals(result, apiRouteController.reconcileSnapshot("demo-service", request));

        verify(apiRouteService).reconcile("demo-service", request);
    }

    @Test
    void snapshotEndpointAcceptsIdsAndRejectsFullRoutePayload() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(apiRouteController).build();
        RouteSnapshotRequest request = new RouteSnapshotRequest(1, "token", List.of("routeId"));
        when(apiRouteService.reconcile("demo-service", request))
                .thenReturn(new RouteSnapshotResult("demo-service", 1, "token", 0, 0, 0, 1));

        mvc.perform(put("/api/v1/routes/services/demo-service/snapshot")
                .contentType("application/json")
                .content("{\"generation\":1,\"snapshotId\":\"token\",\"routeIds\":[\"routeId\"]}"))
                .andExpect(status().isOk());
        verify(apiRouteService).reconcile("demo-service", request);

        mvc.perform(put("/api/v1/routes/services/demo-service/snapshot")
                .contentType("application/json")
                .content("{\"generation\":1,\"snapshotId\":\"token\",\"routes\":[{\"id\":\"routeId\"}]}"))
                .andExpect(status().isBadRequest());
    }
}
