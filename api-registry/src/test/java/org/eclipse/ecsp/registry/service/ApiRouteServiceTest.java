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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.Optional;

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

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        apiRouteService = new ApiRouteService(apiRouteRepo, Optional.of(routeEventPublisher));
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
        RouteDefinition rd = new RouteDefinition();
        try {
            apiRouteService.createOrUpdate(rd);
            Assertions.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(true);
        }
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

    @Test
    void testCreateOrUpdatePublishesEvent() {
        // Arrange
        ApiRouteEntity apiRouteEntity = RegistryTestUtil.getApiRouteEntity();
        apiRouteEntity.setService("test-service");
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        routeDefinition.setService("test-service");
        Mockito.when(apiRouteRepo.save(Mockito.any())).thenReturn(apiRouteEntity);

        // Act
        apiRouteService.createOrUpdate(routeDefinition);

        // Assert
        Mockito.verify(routeEventPublisher, Mockito.times(1)).publishEvent(Mockito.any());
    }

    @Test
    void testDeletePublishesEvent() {
        // Arrange
        ApiRouteEntity apiRouteEntity = new ApiRouteEntity();
        apiRouteEntity.setService("test-service");
        Mockito.when(apiRouteRepo.findById(Mockito.anyString())).thenReturn(Optional.of(apiRouteEntity));

        // Act
        apiRouteService.delete("routeId");

        // Assert
        Mockito.verify(routeEventPublisher, Mockito.times(1)).publishEvent(Mockito.any());
    }

    @Test
    void testCreateOrUpdateWithoutEventPublisher() {
        // Arrange - create service without event publisher
        ApiRouteService serviceWithoutPublisher = new ApiRouteService(apiRouteRepo, Optional.empty());
        ApiRouteEntity apiRouteEntity = RegistryTestUtil.getApiRouteEntity();
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        Mockito.when(apiRouteRepo.save(Mockito.any())).thenReturn(apiRouteEntity);

        // Act & Assert - should not throw exception
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
        // Arrange
        ApiRouteEntity apiRouteEntity = RegistryTestUtil.getApiRouteEntity();
        apiRouteEntity.setService(null);
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        routeDefinition.setService(null);
        Mockito.when(apiRouteRepo.save(Mockito.any())).thenReturn(apiRouteEntity);

        // Act
        apiRouteService.createOrUpdate(routeDefinition);

        // Assert
        Mockito.verify(routeEventPublisher, Mockito.never()).publishEvent(Mockito.any());
    }

    /**
     * Test purpose          - Verify delete with null service name does not publish event.
     * Test data             - Route entity with null service.
     * Test expected result  - No event published.
     * Test type             - Negative.
     */
    @Test
    void testDeleteNullServiceNoEventPublished() {
        // Arrange
        ApiRouteEntity apiRouteEntity = new ApiRouteEntity();
        apiRouteEntity.setService(null);
        Mockito.when(apiRouteRepo.findById(Mockito.anyString())).thenReturn(Optional.of(apiRouteEntity));

        // Act
        apiRouteService.delete("routeId");

        // Assert
        Mockito.verify(routeEventPublisher, Mockito.never()).publishEvent(Mockito.any());
    }

    /**
     * Test purpose          - Verify read throws exception when route not found.
     * Test data             - Non-existent route ID.
     * Test expected result  - IllegalArgumentException thrown.
     * Test type             - Negative.
     */
    @Test
    void testReadRouteNotFoundThrowsException() {
        // Arrange
        Mockito.when(apiRouteRepo.findById(Mockito.anyString())).thenReturn(Optional.empty());

        // Act & Assert
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
        // Arrange
        Mockito.when(apiRouteRepo.findById(Mockito.anyString())).thenReturn(Optional.empty());

        // Act & Assert
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
        // Arrange
        ApiRouteEntity apiRouteEntity = RegistryTestUtil.getApiRouteEntity();
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        routeDefinition.setApiDocs(true);
        Mockito.when(apiRouteRepo.save(Mockito.any())).thenReturn(apiRouteEntity);

        // Act
        apiRouteService.createOrUpdate(routeDefinition);

        // Assert
        Mockito.verify(apiRouteRepo, Mockito.times(1)).save(Mockito.argThat(entity -> 
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
        // Arrange
        ApiRouteEntity apiRouteEntity = RegistryTestUtil.getApiRouteEntity();
        RouteDefinition routeDefinition = RegistryTestUtil.getRouteDefination();
        routeDefinition.setApiDocs(false);
        Mockito.when(apiRouteRepo.save(Mockito.any())).thenReturn(apiRouteEntity);

        // Act
        apiRouteService.createOrUpdate(routeDefinition);

        // Assert
        Mockito.verify(apiRouteRepo, Mockito.times(1)).save(Mockito.any());
    }

    /**
     * Test purpose          - Verify list returns empty list when no routes exist.
     * Test data             - Empty repository.
     * Test expected result  - Empty list returned.
     * Test type             - Positive.
     */
    @Test
    void testListEmptyRepository() {
        // Arrange
        Mockito.when(apiRouteRepo.findAll()).thenReturn(java.util.Collections.emptyList());

        // Act
        java.util.List<RouteDefinition> result = apiRouteService.list();

        // Assert
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
        // Arrange
        ApiRouteService serviceWithoutPublisher = new ApiRouteService(apiRouteRepo, Optional.empty());
        ApiRouteEntity apiRouteEntity = new ApiRouteEntity();
        apiRouteEntity.setService("test-service");
        Mockito.when(apiRouteRepo.findById(Mockito.anyString())).thenReturn(Optional.of(apiRouteEntity));

        // Act & Assert
        Assertions.assertDoesNotThrow(() -> serviceWithoutPublisher.delete("routeId"));
    }


}
