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
import org.eclipse.ecsp.register.model.RouteSnapshotRequest;
import org.eclipse.ecsp.register.model.RouteSnapshotResult;
import org.eclipse.ecsp.utils.ObjectMapperUtil;
import org.eclipse.ecsp.utils.RegistryCommonTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Captor
    private ArgumentCaptor<HttpEntity<RouteSnapshotRequest>> snapshot;
    
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
    void reconcileModeRegistersRoutesBeforeSendingSnapshot() throws Exception {
        RouteDefinition first = serviceRoute();
        RouteDefinition second = serviceRoute();
        second.setId("second-route");
        when(apiRoutesLoader.getApiRoutes()).thenReturn(List.of(first, second));
        reconcileMode(3L);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
            .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body("registered"));
        whenSnapshotPut().thenReturn(ResponseEntity.ok(new RouteSnapshotResult("demo-service", 3, "id", 1, 0, 0, 0)));

        apiRouteRegistrationService.register();

        InOrder order = Mockito.inOrder(restTemplate);
        order.verify(restTemplate, times(2)).postForEntity(eq("http://registry:7000/api/v1/routes"),
            any(HttpEntity.class), eq(String.class));
        order.verify(restTemplate).exchange(eq("http://registry:7000/api/v1/routes/services/demo-service/snapshot"),
            eq(HttpMethod.PUT), snapshot.capture(), eq(RouteSnapshotResult.class));
        RouteSnapshotRequest request = snapshot.getValue().getBody();
        assertEquals(List.of(first.getId(), second.getId()), request.routeIds());
        assertEquals(SnapshotDigestUtil.computeRouteIds(3L, request.routeIds()), request.snapshotId());
        String json = ObjectMapperUtil.getObjectMapper().writeValueAsString(request);
        assertTrue(json.contains("\"routeIds\""));
        assertTrue(!json.contains("\"routes\"") && !json.contains("\"uri\""));
    }

    @Test
    void failedRouteRegistrationDoesNotSendSnapshot() throws Exception {
        when(apiRoutesLoader.getApiRoutes()).thenReturn(List.of(serviceRoute()));
        reconcileMode(3L);

        assertThrows(IllegalStateException.class, () -> apiRouteRegistrationService.register());

        verify(restTemplate, never()).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
            eq(RouteSnapshotResult.class));
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
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
            .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body("registered"));
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
