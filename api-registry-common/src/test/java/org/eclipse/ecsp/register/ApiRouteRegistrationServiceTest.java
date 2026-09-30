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

import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.register.model.RouteSnapshotResult;
import org.eclipse.ecsp.utils.RegistryCommonTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for ApiRouteRegistrationService.
 */
@ExtendWith(MockitoExtension.class)
class ApiRouteRegistrationServiceTest {

    @Mock
    ApiRoutesLoader apiRoutesLoader;
    
    @Mock
    private RestTemplate restTemplate;
    
    private ApiRouteRegistrationService apiRouteRegistrationService;
    
    @BeforeEach
    void setUp() {
        apiRouteRegistrationService = new ApiRouteRegistrationService(apiRoutesLoader, restTemplate);
    }

    @Test
    void testRegister() throws Exception {
        List<RouteDefinition> routeDefinitionList = new ArrayList<>();
        routeDefinitionList.add(RegistryCommonTestUtil.getRouteDefination());
        when(apiRoutesLoader.getApiRoutes()).thenReturn(routeDefinitionList);
        apiRouteRegistrationService.register();
        verify(restTemplate, Mockito.atLeastOnce())
                .postForEntity(Mockito.anyString(), Mockito.any(), Mockito.any());
    }

    @Test
    void reconcileModeSendsOneSnapshotAndNoLegacyPosts() throws Exception {
        when(apiRoutesLoader.getApiRoutes()).thenReturn(List.of(serviceRoute()));
        reconcileMode(3L);
        whenSnapshotPut().thenReturn(ResponseEntity.ok(new RouteSnapshotResult("demo-service", 3, "id", 1, 0, 0, 0)));

        apiRouteRegistrationService.register();

        verify(restTemplate).exchange(eq("http://registry:7000/api/v1/routes/services/demo-service/snapshot"),
                eq(HttpMethod.PUT), any(HttpEntity.class), eq(RouteSnapshotResult.class));
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    void reconcileModeRequiresPositiveGeneration() throws Exception {
        when(apiRoutesLoader.getApiRoutes()).thenReturn(List.of(serviceRoute()));
        reconcileMode(0L);

        assertThrows(IllegalStateException.class, () -> apiRouteRegistrationService.register());
    }

    @Test
    void rejectedSnapshotFailsStartupWithTheRegistryReason() throws Exception {
        when(apiRoutesLoader.getApiRoutes()).thenReturn(List.of(serviceRoute()));
        reconcileMode(3L);
        byte[] body = "{\"error\":\"generation_conflict\"}".getBytes(StandardCharsets.UTF_8);
        whenSnapshotPut().thenThrow(HttpClientErrorException.create(
                HttpStatus.CONFLICT, "Conflict", HttpHeaders.EMPTY, body, StandardCharsets.UTF_8));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> apiRouteRegistrationService.register());
        assertTrue(ex.getMessage().contains("409") && ex.getMessage().contains("generation_conflict"));
    }

    private OngoingStubbing<ResponseEntity<RouteSnapshotResult>> whenSnapshotPut() {
        return when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
                eq(RouteSnapshotResult.class)));
    }

    private void reconcileMode(long generation) {
        ReflectionTestUtils.setField(apiRouteRegistrationService, "registryServiceName", "http://registry");
        ReflectionTestUtils.setField(apiRouteRegistrationService, "registrationMode", "reconcile");
        ReflectionTestUtils.setField(apiRouteRegistrationService, "registrationGeneration", generation);
    }

    private static RouteDefinition serviceRoute() {
        RouteDefinition route = RegistryCommonTestUtil.getRouteDefination();
        route.setService("demo-service");
        return route;
    }
}
