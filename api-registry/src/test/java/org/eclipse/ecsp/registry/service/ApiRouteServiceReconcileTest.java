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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.eclipse.ecsp.register.SnapshotDigestUtil;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.register.model.RouteSnapshotRequest;
import org.eclipse.ecsp.register.model.RouteSnapshotResult;
import org.eclipse.ecsp.registry.config.ChecksumProperties;
import org.eclipse.ecsp.registry.entity.ApiRouteEntity;
import org.eclipse.ecsp.registry.events.EventPublisherContext;
import org.eclipse.ecsp.registry.repo.ApiRouteRepo;
import org.eclipse.ecsp.registry.utils.ApiRouteUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * Tests for {@link ApiRouteService#reconcile(String, RouteSnapshotRequest)} (route snapshot
 * reconciliation).
 */
class ApiRouteServiceReconcileTest {

    private static final String SERVICE = "demo-service";

    private ApiRouteService apiRouteService;

    @Mock
    private ApiRouteRepo apiRouteRepo;

    @Mock
    private EventPublisherContext routeEventPublisher;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Mock
    private ChecksumService checksumService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);
        ChecksumProperties checksumProperties = new ChecksumProperties();
        checksumProperties.setEnabled(false);
        apiRouteService = new ApiRouteService(
                apiRouteRepo, Optional.of(routeEventPublisher), checksumProperties, checksumService, meterRegistry);
        when(apiRouteRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private RouteDefinition route(String id) {
        RouteDefinition route = new RouteDefinition();
        route.setId(id);
        route.setService(SERVICE);
        route.setUri(URI.create("http://" + SERVICE + ":8080/"));
        route.setMetadata(new HashMap<>());
        return route;
    }

    private ApiRouteEntity entity(String id, boolean active) {
        ApiRouteEntity entity = new ApiRouteEntity();
        entity.setId(id);
        entity.setService(SERVICE);
        entity.setActive(active);
        RouteDefinition route = route(id);
        entity.setRoute(route);
        return entity;
    }

    private RouteSnapshotRequest snapshot(long generation, RouteDefinition... routes) {
        List<RouteDefinition> routeList = List.of(routes);
        return new RouteSnapshotRequest(generation, SnapshotDigestUtil.compute(generation, routeList), routeList);
    }

    @Test
    void initialSnapshotCreatesAllDesiredRoutesWithoutRetirement() {
        when(apiRouteRepo.findAllByService(SERVICE)).thenReturn(new ArrayList<>());
        when(apiRouteRepo.findById(anyString())).thenReturn(Optional.empty());

        RouteSnapshotResult result = apiRouteService.reconcile(SERVICE, snapshot(1, route("A"), route("B")));

        assertEquals(2, result.created());
        assertEquals(0, result.retired());
        verify(routeEventPublisher, times(1)).publishEvent(any());
    }

    @Test
    void absentRouteIsRetiredAndDesiredRouteIsUpserted() {
        List<ApiRouteEntity> existing = new ArrayList<>(List.of(entity("A", true), entity("B", true)));
        when(apiRouteRepo.findAllByService(SERVICE)).thenReturn(existing);
        when(apiRouteRepo.findById("A")).thenReturn(Optional.of(entity("A", true)));

        RouteSnapshotResult result = apiRouteService.reconcile(SERVICE, snapshot(2, route("A")));

        assertEquals(1, result.retired());
        verify(apiRouteRepo).save(argThatIdEquals("B"));
    }

    @Test
    void retiredRouteIsMarkedInactiveAndFlaggedRetired() {
        ApiRouteEntity toRetire = entity("B", true);
        List<ApiRouteEntity> existing = new ArrayList<>(List.of(entity("A", true), toRetire));
        when(apiRouteRepo.findAllByService(SERVICE)).thenReturn(existing);
        when(apiRouteRepo.findById("A")).thenReturn(Optional.of(entity("A", true)));

        apiRouteService.reconcile(SERVICE, snapshot(2, route("A")));

        assertEquals(Boolean.FALSE, toRetire.getActive());
        assertEquals(Boolean.TRUE, toRetire.getRoute().getMetadata()
                .get("x-registration-retired"));
    }

    @Test
    void alreadyRetiredRouteIsNotRetiredAgain() {
        ApiRouteEntity alreadyRetired = entity("B", false);
        alreadyRetired.getRoute().getMetadata().put("x-registration-retired", Boolean.TRUE);
        List<ApiRouteEntity> existing = new ArrayList<>(List.of(entity("A", true), alreadyRetired));
        when(apiRouteRepo.findAllByService(SERVICE)).thenReturn(existing);

        RouteSnapshotResult result = apiRouteService.reconcile(SERVICE, snapshot(2, route("A")));

        assertEquals(0, result.retired());
    }

    @Test
    void healthInactiveAbsentRouteIsRetiredSoRecoveryCannotReactivateIt() {
        ApiRouteEntity healthInactive = entity("B", false);
        when(apiRouteRepo.findAllByService(SERVICE))
                .thenReturn(new ArrayList<>(List.of(entity("A", true), healthInactive)));

        RouteSnapshotResult result = apiRouteService.reconcile(SERVICE, snapshot(2, route("A")));

        assertEquals(1, result.retired());
        assertEquals(Boolean.TRUE, healthInactive.getRoute().getMetadata().get("x-registration-retired"));
    }

    @Test
    void snapshotIdIsAnOpaqueTokenAndNotRecomputed() {
        when(apiRouteRepo.findAllByService(SERVICE)).thenReturn(new ArrayList<>());
        RouteSnapshotRequest request = new RouteSnapshotRequest(1, "client-token", List.of(route("A")));

        RouteSnapshotResult result = apiRouteService.reconcile(SERVICE, request);

        assertEquals(1, result.created());
        assertEquals("client-token", result.snapshotId());
    }

    @Test
    void rejectsLowerGenerationThanAlreadyApplied() {
        ApiRouteEntity existing = entity("A", true);
        existing.getRoute().getMetadata().put("x-registration-generation", 5L);
        when(apiRouteRepo.findAllByService(SERVICE)).thenReturn(new ArrayList<>(List.of(existing)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> apiRouteService.reconcile(SERVICE, snapshot(3, route("A"))));
        assertEquals(409, ex.getStatusCode().value());
        verify(meterRegistry).counter("api.route.snapshot.total", "outcome", "stale_snapshot");
    }

    @Test
    void rejectsSameGenerationWithDifferentContent() {
        ApiRouteEntity existing = entity("A", true);
        existing.getRoute().getMetadata().put("x-registration-generation", 5L);
        existing.getRoute().getMetadata().put("x-registration-snapshot-id", "sha256:previous");
        when(apiRouteRepo.findAllByService(SERVICE)).thenReturn(new ArrayList<>(List.of(existing)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> apiRouteService.reconcile(SERVICE, snapshot(5, route("A"), route("B"))));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void repeatingIdenticalSnapshotIsIdempotent() {
        when(apiRouteRepo.findAllByService(SERVICE)).thenReturn(new ArrayList<>());
        when(apiRouteRepo.findById(anyString())).thenReturn(Optional.empty());

        // Each call builds its own request/route object graph, matching how two independent
        // HTTP requests are each freshly deserialized server-side; reconcile() itself must not
        // mutate shared/cached state in a way that breaks a legitimate identical retry.
        RouteSnapshotResult first = apiRouteService.reconcile(SERVICE, snapshot(1, route("A")));
        RouteSnapshotResult second = apiRouteService.reconcile(SERVICE, snapshot(1, route("A")));

        assertEquals(1, first.created());
        assertEquals(1, second.created());
    }

    @Test
    void rejectsRouteIdOwnedByAnotherService() {
        when(apiRouteRepo.findAllByService(SERVICE)).thenReturn(new ArrayList<>());
        ApiRouteEntity foreign = entity("shared-id", true);
        foreign.setService("other-service");
        when(apiRouteRepo.findById("shared-id")).thenReturn(Optional.of(foreign));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> apiRouteService.reconcile(SERVICE, snapshot(1, route("shared-id"))));
        assertEquals(409, ex.getStatusCode().value());
        assertEquals("route_id_conflict", ex.getReason());
    }

    @Test
    void rowWithoutServiceIsConflictNotNullPointer() {
        when(apiRouteRepo.findAllByService(SERVICE)).thenReturn(new ArrayList<>());
        ApiRouteEntity legacyRow = entity("legacy-id", true);
        legacyRow.setService(null);
        when(apiRouteRepo.findById("legacy-id")).thenReturn(Optional.of(legacyRow));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> apiRouteService.reconcile(SERVICE, snapshot(1, route("legacy-id"))));
        assertEquals("route_id_conflict", ex.getReason());
    }

    @Test
    void generationOnlyChangeRefreshesWatermarkWithoutRouteChangeOrEvent() {
        ChecksumProperties enabled = new ChecksumProperties();
        enabled.setEnabled(true);
        final ApiRouteService service = new ApiRouteService(
                apiRouteRepo, Optional.of(routeEventPublisher), enabled, checksumService, meterRegistry);
        when(checksumService.compute(any())).thenAnswer(invocation -> {
            RouteDefinition hashed = invocation.getArgument(0);
            assertNull(hashed.getMetadata().get("x-registration-generation"), "checksum must precede stamping");
            return Optional.of("same-content");
        });
        ApiRouteEntity existing = entity("A", true);
        existing.setChecksum("same-content");
        existing.getRoute().getMetadata().put("x-registration-generation", 1L);
        existing.getRoute().getMetadata().put("x-registration-snapshot-id", "s1");
        when(apiRouteRepo.findAllByService(SERVICE)).thenReturn(new ArrayList<>(List.of(existing)));

        RouteSnapshotResult result = service.reconcile(SERVICE, snapshot(2, route("A")));

        assertEquals(1, result.unchanged());
        assertEquals(0, result.updated());
        assertEquals(2L, existing.getRoute().getMetadata().get("x-registration-generation"));
        verify(apiRouteRepo, times(1)).save(existing);
        verify(routeEventPublisher, never()).publishEvent(any());
    }

    @Test
    void resetClearsGenerationSoLowerGenerationIsAcceptedAgain() {
        ApiRouteEntity locked = entity("A", true);
        locked.getRoute().getMetadata().put("x-registration-generation", Long.MAX_VALUE);
        locked.getRoute().getMetadata().put("x-registration-snapshot-id", "sabotage");
        when(apiRouteRepo.findAllByService(SERVICE)).thenReturn(new ArrayList<>(List.of(locked)));

        assertEquals(1, apiRouteService.resetReconciliationGeneration(SERVICE));
        RouteSnapshotResult result = apiRouteService.reconcile(SERVICE, snapshot(3, route("A")));

        assertEquals(3L, result.generation());
    }

    @Test
    void legacyUpsertOfReconciledRouteIsAcceptedWithoutScanningTheService() {
        ApiRouteEntity reconciled = entity("A", true);
        reconciled.getRoute().getMetadata().put("x-registration-generation", 1L);
        when(apiRouteRepo.findById("A")).thenReturn(Optional.of(reconciled));

        assertDoesNotThrow(() -> apiRouteService.createOrUpdate(route("A")));

        verify(apiRouteRepo).save(reconciled);
        verify(apiRouteRepo, never()).findAllByService(anyString());
    }

    @Test
    void legacyUpsertReactivatesRouteRetiredBySnapshotWhenServiceRollsBackToOldClient() {
        ApiRouteEntity retired = entity("A", false);
        retired.getRoute().getMetadata().put("x-registration-generation", 2L);
        retired.getRoute().getMetadata().put("x-registration-retired", Boolean.TRUE);
        when(apiRouteRepo.findById("A")).thenReturn(Optional.of(retired));

        apiRouteService.createOrUpdate(route("A"));

        assertTrue(retired.getActive());
        assertFalse(ApiRouteUtil.isRetired(retired));
    }

    @Test
    void rejectsEmptyRouteSet() {
        RouteSnapshotRequest empty = new RouteSnapshotRequest(1, "sha256:anything", new ArrayList<>());
        assertThrows(Exception.class, () -> apiRouteService.reconcile(SERVICE, empty));
    }

    @Test
    void desiredUpsertFailureAbortsBeforeRetirement() {
        List<ApiRouteEntity> existing = new ArrayList<>(List.of(entity("B", true)));
        when(apiRouteRepo.findAllByService(SERVICE)).thenReturn(existing);
        when(apiRouteRepo.findById("A")).thenReturn(Optional.empty());
        when(apiRouteRepo.save(argThatIdEquals("A"))).thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class, () -> apiRouteService.reconcile(SERVICE, snapshot(2, route("A"))));

        verify(apiRouteRepo, never()).save(argThatIdEquals("B"));
    }

    @Test
    void legacyUpsertOfNewRouteIsAcceptedAfterServiceAdoptsReconciliation() {
        ApiRouteEntity reconciled = entity("A", true);
        reconciled.getRoute().getMetadata().put("x-registration-generation", 1L);
        when(apiRouteRepo.findAllByService(SERVICE)).thenReturn(new ArrayList<>(List.of(reconciled)));
        when(apiRouteRepo.findById("C")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> apiRouteService.createOrUpdate(route("C")));

        verify(apiRouteRepo).save(argThatIdEquals("C"));
    }

    @Test
    void legacyUpsertStillWorksBeforeServiceAdoptsReconciliation() {
        when(apiRouteRepo.findAllByService(SERVICE)).thenReturn(new ArrayList<>());
        when(apiRouteRepo.findById(anyString())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> apiRouteService.createOrUpdate(route("C")));
    }

    private static ApiRouteEntity argThatIdEquals(String id) {
        return Mockito.argThat(e -> e != null && id.equals(e.getId()));
    }
}
