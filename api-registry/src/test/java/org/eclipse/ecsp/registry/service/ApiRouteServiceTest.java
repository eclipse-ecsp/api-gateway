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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.registry.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.eclipse.ecsp.register.model.RouteDefinition;
import org.eclipse.ecsp.registry.config.ChecksumProperties;
import org.eclipse.ecsp.registry.entity.ApiRouteEntity;
import org.eclipse.ecsp.registry.events.EventPublisherContext;
import org.eclipse.ecsp.registry.repo.ApiRouteRepo;
import org.eclipse.ecsp.registry.utils.RegistryTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for ApiRouteService.
 */
@ExtendWith(SpringExtension.class)
class ApiRouteServiceTest {

    private ApiRouteService apiRouteService;

    @Mock
    private ApiRouteRepo apiRouteRepo;

    @Mock
    private EventPublisherContext routeEventPublisher;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    private ChecksumProperties checksumPropertiesEnabled;
    private ChecksumProperties checksumPropertiesDisabled;

    @Mock
    private ChecksumService checksumService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        when(meterRegistry.counter(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(counter);
        checksumPropertiesEnabled = new ChecksumProperties();
        checksumPropertiesEnabled.setEnabled(true);
        checksumPropertiesDisabled = new ChecksumProperties();
        checksumPropertiesDisabled.setEnabled(false);
        when(checksumService.compute(Mockito.any()))
                .thenReturn(Optional.of("abc123checksum"));
        apiRouteService = new ApiRouteService(
                apiRouteRepo, Optional.of(routeEventPublisher),
                checksumPropertiesEnabled, checksumService, meterRegistry);
    }

    @Test
    void testCreateOrUpdate() {
        ApiRouteEntity apiRouteEntity = RegistryTestUtil.getApiRouteEntity();
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        when(apiRouteRepo.save(Mockito.any())).thenReturn(apiRouteEntity);
        apiRouteService.createOrUpdate(routeDefinition);
        when(apiRouteRepo.findById(Mockito.anyString())).thenReturn(Optional.of(new ApiRouteEntity()));
        Assertions.assertNotNull(apiRouteService.createOrUpdate(routeDefinition));
    }

    @Test
    void testCreateOrUpdateException() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> apiRouteService.createOrUpdate(null));
        RouteDefinition rd = new RouteDefinition();
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> apiRouteService.createOrUpdate(rd));
    }

    @Test
    void testList() {
        Assertions.assertNotNull(apiRouteService.list());
    }

    @Test
    void testRead() {
        when(apiRouteRepo.findById(Mockito.anyString())).thenReturn(Optional.of(new ApiRouteEntity()));
        apiRouteService.read("routeId");
        verify(apiRouteRepo, Mockito.atLeastOnce()).findById(Mockito.anyString());
    }

    @Test
    void testReadException() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> apiRouteService.read(null));
    }

    @Test
    void testDelete() {
        when(apiRouteRepo.findById(Mockito.anyString())).thenReturn(Optional.of(new ApiRouteEntity()));
        apiRouteService.delete("routeId");
        verify(apiRouteRepo, Mockito.atLeastOnce()).findById(Mockito.anyString());
    }

    @Test
    void testDeleteException() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> apiRouteService.delete(null));
    }

    @Test
    void testCreateOrUpdatePublishesEvent() {
        ApiRouteEntity apiRouteEntity = RegistryTestUtil.getApiRouteEntity();
        apiRouteEntity.setService("test-service");
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        routeDefinition.setService("test-service");
        when(apiRouteRepo.save(Mockito.any())).thenReturn(apiRouteEntity);

        apiRouteService.createOrUpdate(routeDefinition);

        verify(routeEventPublisher, times(1)).publishEvent(Mockito.any());
    }

    @Test
    void testDeletePublishesEvent() {
        ApiRouteEntity apiRouteEntity = new ApiRouteEntity();
        apiRouteEntity.setService("test-service");
        when(apiRouteRepo.findById(Mockito.anyString())).thenReturn(Optional.of(apiRouteEntity));

        apiRouteService.delete("routeId");

        verify(routeEventPublisher, times(1)).publishEvent(Mockito.any());
    }

    @Test
    void testCreateOrUpdateWithoutEventPublisher() {
        ApiRouteService serviceWithoutPublisher = new ApiRouteService(
                apiRouteRepo, Optional.empty(),
                checksumPropertiesEnabled, checksumService, meterRegistry);
        ApiRouteEntity apiRouteEntity = RegistryTestUtil.getApiRouteEntity();
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        when(apiRouteRepo.save(Mockito.any())).thenReturn(apiRouteEntity);

        Assertions.assertDoesNotThrow(() -> serviceWithoutPublisher.createOrUpdate(routeDefinition));
    }

    /**
     * Test purpose          - Verify createOrUpdate with null service name does not publish event.
     * Test data             - Route definition with null service.
     * Test expected result  - No event published.
     * Test type             - Negative.
     */
    @Test
    void testCreateOrUpdateNullServiceNoEventPublished() {
        ApiRouteEntity apiRouteEntity = RegistryTestUtil.getApiRouteEntity();
        apiRouteEntity.setService(null);
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        routeDefinition.setService(null);
        when(apiRouteRepo.save(Mockito.any())).thenReturn(apiRouteEntity);

        apiRouteService.createOrUpdate(routeDefinition);

        verify(routeEventPublisher, Mockito.never()).publishEvent(Mockito.any());
    }

    /**
     * Test purpose          - Verify delete with null service name does not publish event.
     * Test data             - Route entity with null service.
     * Test expected result  - No event published.
     * Test type             - Negative.
     */
    @Test
    void testDeleteNullServiceNoEventPublished() {
        ApiRouteEntity apiRouteEntity = new ApiRouteEntity();
        apiRouteEntity.setService(null);
        when(apiRouteRepo.findById(Mockito.anyString())).thenReturn(Optional.of(apiRouteEntity));

        apiRouteService.delete("routeId");

        verify(routeEventPublisher, Mockito.never()).publishEvent(Mockito.any());
    }

    /**
     * Test purpose          - Verify read throws exception when route not found.
     * Test data             - Non-existent route ID.
     * Test expected result  - IllegalArgumentException thrown.
     * Test type             - Negative.
     */
    @Test
    void testReadRouteNotFoundThrowsException() {
        when(apiRouteRepo.findById(Mockito.anyString())).thenReturn(Optional.empty());

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> apiRouteService.read("nonExistentId"));
    }

    /**
     * Test purpose          - Verify delete throws exception when route not found.
     * Test data             - Non-existent route ID.
     * Test expected result  - IllegalArgumentException thrown.
     * Test type             - Negative.
     */
    @Test
    void testDeleteRouteNotFoundThrowsException() {
        when(apiRouteRepo.findById(Mockito.anyString())).thenReturn(Optional.empty());

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> apiRouteService.delete("nonExistentId"));
    }

    /**
     * Test purpose          - Verify createOrUpdate with apiDocs flag.
     * Test data             - Route definition with apiDocs set to true.
     * Test expected result  - Entity saved with apiDocs flag.
     * Test type             - Positive.
     */
    @Test
    void testCreateOrUpdateWithApiDocs() {
        ApiRouteEntity apiRouteEntity = RegistryTestUtil.getApiRouteEntity();
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        routeDefinition.setApiDocs(true);
        when(apiRouteRepo.save(Mockito.any())).thenReturn(apiRouteEntity);

        apiRouteService.createOrUpdate(routeDefinition);

        verify(apiRouteRepo, times(1)).save(Mockito.argThat(entity ->
            Boolean.TRUE.equals(entity.getApiDocs())
        ));
    }

    /**
     * Test purpose          - Verify createOrUpdate with apiDocs false does not set flag.
     * Test data             - Route definition with apiDocs set to false.
     * Test expected result  - Entity saved without apiDocs flag.
     * Test type             - Positive.
     */
    @Test
    void testCreateOrUpdateWithApiDocsFalse() {
        ApiRouteEntity apiRouteEntity = RegistryTestUtil.getApiRouteEntity();
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        routeDefinition.setApiDocs(false);
        when(apiRouteRepo.save(Mockito.any())).thenReturn(apiRouteEntity);

        apiRouteService.createOrUpdate(routeDefinition);

        verify(apiRouteRepo, times(1)).save(Mockito.any());
    }

    /**
     * Test purpose          - Verify list returns empty list when no routes exist.
     * Test data             - Empty repository.
     * Test expected result  - Empty list returned.
     * Test type             - Positive.
     */
    @Test
    void testListEmptyRepository() {
        when(apiRouteRepo.findAll()).thenReturn(Collections.emptyList());

        List<RouteDefinition> result = apiRouteService.list();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    /**
     * Test purpose          - Verify delete without event publisher succeeds.
     * Test data             - Route entity without event publisher.
     * Test expected result  - Route deleted successfully.
     * Test type             - Positive.
     */
    @Test
    void testDeleteWithoutEventPublisher() {
        ApiRouteService serviceWithoutPublisher = new ApiRouteService(
                apiRouteRepo, Optional.empty(),
                checksumPropertiesEnabled, checksumService, meterRegistry);
        ApiRouteEntity apiRouteEntity = new ApiRouteEntity();
        apiRouteEntity.setService("test-service");
        when(apiRouteRepo.findById(Mockito.anyString())).thenReturn(Optional.of(apiRouteEntity));

        Assertions.assertDoesNotThrow(() -> serviceWithoutPublisher.delete("routeId"));
    }

    /**
     * Test purpose          - Verify UNCHANGED route skips DB write and returns existing entity.
     * Test data             - Existing entity with matching checksum.
     * Test expected result  - Repository save NOT called; existing route returned.
     * Test type             - Positive.
     */
    @Test
    void testCreateOrUpdateUnchangedRouteSkipsDbWrite() {
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        ApiRouteEntity existing = RegistryTestUtil.getApiRouteEntity();
        existing.setChecksum("abc123checksum");
        when(apiRouteRepo.findById(routeDefinition.getId())).thenReturn(Optional.of(existing));
        when(checksumService.compute(routeDefinition)).thenReturn(Optional.of("abc123checksum"));

        RouteDefinition result = apiRouteService.createOrUpdate(routeDefinition);

        verify(apiRouteRepo, Mockito.never()).save(Mockito.any());
        Assertions.assertNotNull(result);
    }

    /**
     * Test purpose          - Verify UPDATED route triggers DB write when checksum differs.
     * Test data             - Existing entity with different checksum.
     * Test expected result  - Repository save called once.
     * Test type             - Positive.
     */
    @Test
    void testCreateOrUpdateUpdatedRouteWritesToDb() {
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        ApiRouteEntity existing = RegistryTestUtil.getApiRouteEntity();
        existing.setChecksum("old-checksum");
        when(apiRouteRepo.findById(routeDefinition.getId())).thenReturn(Optional.of(existing));
        when(checksumService.compute(routeDefinition)).thenReturn(Optional.of("new-checksum"));
        when(apiRouteRepo.save(Mockito.any())).thenReturn(existing);

        apiRouteService.createOrUpdate(routeDefinition);

        verify(apiRouteRepo, times(1)).save(Mockito.any());
    }

    /**
     * Test purpose          - Verify NEW route (no existing entity) triggers DB write.
     * Test data             - No existing entity in repository.
     * Test expected result  - Repository save called once.
     * Test type             - Positive.
     */
    @Test
    void testCreateOrUpdateNewRouteWritesToDb() {
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        ApiRouteEntity saved = RegistryTestUtil.getApiRouteEntity();
        when(apiRouteRepo.findById(routeDefinition.getId())).thenReturn(Optional.empty());
        when(checksumService.compute(routeDefinition)).thenReturn(Optional.of("new-checksum"));
        when(apiRouteRepo.save(Mockito.any())).thenReturn(saved);

        RouteDefinition result = apiRouteService.createOrUpdate(routeDefinition);

        verify(apiRouteRepo, times(1)).save(Mockito.any());
        Assertions.assertNotNull(result);
    }

    /**
     * Test purpose          - Verify checksum failure treats route as UPDATED (FR-07).
     * Test data             - Existing entity; checksum computation returns empty.
     * Test expected result  - Repository save called; route registered.
     * Test type             - Positive.
     */
    @Test
    void testCreateOrUpdateChecksumFailureTreatedAsUpdated() {
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        ApiRouteEntity existing = RegistryTestUtil.getApiRouteEntity();
        existing.setChecksum("old-checksum");
        when(apiRouteRepo.findById(routeDefinition.getId())).thenReturn(Optional.of(existing));
        when(checksumService.compute(routeDefinition)).thenReturn(Optional.empty());
        when(apiRouteRepo.save(Mockito.any())).thenReturn(existing);

        Assertions.assertDoesNotThrow(() -> apiRouteService.createOrUpdate(routeDefinition));

        verify(apiRouteRepo, times(1)).save(Mockito.any());
    }

    /**
     * Test purpose          - Verify null stored checksum (post-migration first write) treated as UPDATED.
     * Test data             - Existing entity with null checksum.
     * Test expected result  - Repository save called; route registered.
     * Test type             - Positive.
     */
    @Test
    void testCreateOrUpdateNullStoredChecksumTreatedAsUpdated() {
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        ApiRouteEntity existing = RegistryTestUtil.getApiRouteEntity();
        existing.setChecksum(null);
        when(apiRouteRepo.findById(routeDefinition.getId())).thenReturn(Optional.of(existing));
        when(checksumService.compute(routeDefinition)).thenReturn(Optional.of("new-checksum"));
        when(apiRouteRepo.save(Mockito.any())).thenReturn(existing);

        Assertions.assertDoesNotThrow(() -> apiRouteService.createOrUpdate(routeDefinition));

        verify(apiRouteRepo, times(1)).save(Mockito.any());
    }

    /**
     * Test purpose          - Verify change-detection disabled path saves unconditionally (FR-08).
     * Test data             - Change-detection disabled; route definition provided.
     * Test expected result  - Repository save called regardless of checksum.
     * Test type             - Positive.
     */
    @Test
    void testCreateOrUpdateChangeDetectionDisabledSavesUnconditionally() {
        ApiRouteService serviceDisabled = new ApiRouteService(
                apiRouteRepo, Optional.of(routeEventPublisher),
                checksumPropertiesDisabled, checksumService, meterRegistry);
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        ApiRouteEntity saved = RegistryTestUtil.getApiRouteEntity();
        when(apiRouteRepo.findById(routeDefinition.getId())).thenReturn(Optional.empty());
        when(apiRouteRepo.save(Mockito.any())).thenReturn(saved);

        serviceDisabled.createOrUpdate(routeDefinition);

        verify(checksumService, Mockito.never()).compute(Mockito.any());
        verify(apiRouteRepo, times(1)).save(Mockito.any());
    }

    /**
     * Test purpose          - Verify delete with a stored checksum emits DELETED event with checksum.
     * Test data             - Entity with a known checksum.
     * Test expected result  - Event published; repository delete called.
     * Test type             - Positive.
     */
    @Test
    void testDeleteWithStoredChecksumPublishesEvent() {
        ApiRouteEntity apiRouteEntity = new ApiRouteEntity();
        apiRouteEntity.setId("route-1");
        apiRouteEntity.setService("svc");
        apiRouteEntity.setChecksum("stored-checksum");
        when(apiRouteRepo.findById("route-1")).thenReturn(Optional.of(apiRouteEntity));

        apiRouteService.delete("route-1");

        verify(apiRouteRepo, times(1)).delete(apiRouteEntity);
        verify(routeEventPublisher, times(1)).publishEvent(Mockito.any());
    }

    /**
     * Test purpose          - Verify UNCHANGED route increments the 'unchanged' metric counter.
     * Test data             - Existing entity with matching checksum; meterRegistry mocked.
     * Test expected result  - Counter incremented for 'unchanged' change type.
     * Test type             - Positive.
     */
    @Test
    void testUnchangedRouteIncrementsMetric() {
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        ApiRouteEntity existing = RegistryTestUtil.getApiRouteEntity();
        existing.setChecksum("abc123checksum");
        when(apiRouteRepo.findById(routeDefinition.getId())).thenReturn(Optional.of(existing));
        when(checksumService.compute(routeDefinition)).thenReturn(Optional.of("abc123checksum"));

        apiRouteService.createOrUpdate(routeDefinition);

        verify(counter, times(1)).increment();
    }
}
