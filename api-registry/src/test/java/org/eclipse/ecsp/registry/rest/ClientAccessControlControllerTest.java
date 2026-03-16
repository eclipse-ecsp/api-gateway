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

package org.eclipse.ecsp.registry.rest;

import jakarta.persistence.EntityNotFoundException;
import org.eclipse.ecsp.registry.dto.BulkCreateResponseDto;
import org.eclipse.ecsp.registry.dto.ClientAccessControlRequestDto;
import org.eclipse.ecsp.registry.dto.ClientAccessControlResponseDto;
import org.eclipse.ecsp.registry.dto.GenericResponseDto;
import org.eclipse.ecsp.registry.service.ClientAccessControlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ClientAccessControlController.
 *
 * <p>Tests all REST endpoints following Engineering Fundamentals Guidelines:
 * - POST / (bulk create)
 * - GET / (get all)
 * - GET /{clientId} (get by client ID)
 * - PUT /{clientId} (update)
 * - DELETE /{clientId} (delete)
 */
@ExtendWith(MockitoExtension.class)
class ClientAccessControlControllerTest {

    private static final String NON_EXISTENT = "non-existent";
    private static final String CLIENT_NOT_FOUND = "Client not found";
    private static final String CLIENT_ID_1 = "client-1";
    private static final String CLIENT_ID_2 = "client-2";
    private static final String TENANT_A = "tenant-a";
    private static final String TEST_DESCRIPTION = "Test description";
    private static final int TWO = 2;

    @Mock
    private ClientAccessControlService service;

    @InjectMocks
    private ClientAccessControlController controller;

    @BeforeEach
    void setUp() {
        // Setup if needed
    }

    // ==================== POST / (Bulk Create) ====================

    /**
     * Test purpose          - Verify bulk create with valid requests returns 201 Created.
     * Test data             - List of 2 valid ClientAccessControlRequestDto objects.
     * Test expected result  - ResponseEntity with 201 Created status and BulkCreateResponseDto.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void bulkCreateValidRequestWith2ClientsReturnCreatedStatus() {
        // GIVEN: Valid request with 2 clients
        List<ClientAccessControlRequestDto> requests = List.of(
                createRequestDto(CLIENT_ID_1, TENANT_A, true, List.of("service-a:*")),
                createRequestDto(CLIENT_ID_2, TENANT_A, true, List.of("service-b:*"))
        );

        List<ClientAccessControlResponseDto> serviceResponses = List.of(
                createResponseDto(CLIENT_ID_1, TENANT_A, true),
                createResponseDto(CLIENT_ID_2, TENANT_A, true)
        );

        when(service.bulkCreate(any())).thenReturn(serviceResponses);

        // WHEN: Controller bulkCreate is called
        ResponseEntity<BulkCreateResponseDto> response = controller.bulkCreate(requests);

        // THEN: Should return 201 Created with correct response
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TWO, response.getBody().getCreated().size());
        assertTrue(response.getBody().getMessage().contains("2 client(s) created successfully"));
        verify(service, times(1)).bulkCreate(requests);
    }

    /**
     * Test purpose          - Verify bulk create with empty list throws exception.
     * Test data             - Empty list of requests.
     * Test expected result  - Service method is called and can handle empty list.
     * Test type             - Negative.
     *
     * @throws Exception if test fails
     **/
    @Test
    void bulkCreateEmptyRequestListServiceCalled() {
        // GIVEN: Empty request list
        List<ClientAccessControlRequestDto> requests = List.of();

        when(service.bulkCreate(any())).thenReturn(List.of());

        // WHEN: Controller bulkCreate is called
        ResponseEntity<BulkCreateResponseDto> response = controller.bulkCreate(requests);

        // THEN: Should return 201 with empty result
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCreated().size());
        verify(service, times(1)).bulkCreate(requests);
    }

    /**
     * Test purpose          - Verify bulk create delegates to service correctly.
     * Test data             - Single client request.
     * Test expected result  - Service method called once with correct parameter.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void bulkCreateSingleClientServiceCalledOnce() {
        // GIVEN: Single client request
        List<ClientAccessControlRequestDto> requests = List.of(
                createRequestDto(CLIENT_ID_1, TENANT_A, true, List.of("*:*"))
        );

        List<ClientAccessControlResponseDto> serviceResponses = List.of(
                createResponseDto(CLIENT_ID_1, TENANT_A, true)
        );

        when(service.bulkCreate(requests)).thenReturn(serviceResponses);

        // WHEN: Controller bulkCreate is called
        ResponseEntity<BulkCreateResponseDto> response = controller.bulkCreate(requests);

        // THEN: Service should be called once
        assertNotNull(response);
        verify(service, times(1)).bulkCreate(requests);
    }

    // ==================== GET / (Get All) ====================

    /**
     * Test purpose          - Verify getAll returns all active clients when includeInactive is false.
     * Test data             - includeInactive = false.
     * Test expected result  - ResponseEntity with 200 OK and list of active clients.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void getAllIncludeInactiveFalseReturnActiveClientsOnly() {
        // GIVEN: Service returns 2 active clients
        List<ClientAccessControlResponseDto> responses = List.of(
                createResponseDto(CLIENT_ID_1, TENANT_A, true),
                createResponseDto(CLIENT_ID_2, TENANT_A, true)
        );

        when(service.getAll(false)).thenReturn(responses);

        // WHEN: Controller getAll is called with includeInactive=false
        ResponseEntity<List<ClientAccessControlResponseDto>> response = controller.getAll(false);

        // THEN: Should return 200 OK with active clients
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TWO, response.getBody().size());
        verify(service, times(1)).getAll(false);
    }

    /**
     * Test purpose          - Verify getAll returns all clients when includeInactive is true.
     * Test data             - includeInactive = true.
     * Test expected result  - ResponseEntity with 200 OK and list of all clients.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void getAllIncludeInactiveTrueReturnAllClients() {
        // GIVEN: Service returns active and inactive clients
        List<ClientAccessControlResponseDto> responses = List.of(
                createResponseDto(CLIENT_ID_1, TENANT_A, true),
                createResponseDto(CLIENT_ID_2, TENANT_A, false)
        );

        when(service.getAll(true)).thenReturn(responses);

        // WHEN: Controller getAll is called with includeInactive=true
        ResponseEntity<List<ClientAccessControlResponseDto>> response = controller.getAll(true);

        // THEN: Should return 200 OK with all clients
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TWO, response.getBody().size());
        verify(service, times(1)).getAll(true);
    }

    /**
     * Test purpose          - Verify getAll returns empty list when no clients exist.
     * Test data             - Empty list from service.
     * Test expected result  - ResponseEntity with 200 OK and empty list.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void getAllNoClientsReturnEmptyList() {
        // GIVEN: Service returns empty list
        when(service.getAll(anyBoolean())).thenReturn(List.of());

        // WHEN: Controller getAll is called
        ResponseEntity<List<ClientAccessControlResponseDto>> response = controller.getAll(false);

        // THEN: Should return 200 OK with empty list
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(service, times(1)).getAll(false);
    }

    // ==================== GET /{clientId} (Get By Client ID) ====================

    /**
     * Test purpose          - Verify getByClientId returns client configuration when exists.
     * Test data             - Valid client ID.
     * Test expected result  - ResponseEntity with 200 OK and client configuration.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void getByClientIdExistingClientReturnClientConfiguration() {
        // GIVEN: Client configuration exists
        ClientAccessControlResponseDto response = createResponseDto(CLIENT_ID_1, TENANT_A, true);

        when(service.getByClientId(CLIENT_ID_1)).thenReturn(response);

        // WHEN: Controller getByClientId is called
        ResponseEntity<ClientAccessControlResponseDto> result = controller.getByClientId(CLIENT_ID_1);

        // THEN: Should return 200 OK with configuration
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(CLIENT_ID_1, result.getBody().getClientId());
        verify(service, times(1)).getByClientId(CLIENT_ID_1);
    }

    /**
     * Test purpose          - Verify getByClientId throws EntityNotFoundException for non-existent client.
     * Test data             - Non-existent client ID.
     * Test expected result  - EntityNotFoundException thrown.
     * Test type             - Negative.
     *
     * @throws Exception if test fails
     **/
    @Test
    void getByClientIdNonExistentClientThrowEntityNotFoundException() {
        // GIVEN: Client does not exist
        when(service.getByClientId(NON_EXISTENT)).thenThrow(new EntityNotFoundException(CLIENT_NOT_FOUND));

        // WHEN/THEN: Should throw EntityNotFoundException
        try {
            controller.getByClientId(NON_EXISTENT);
        } catch (EntityNotFoundException e) {
            assertEquals(CLIENT_NOT_FOUND, e.getMessage());
        }

        verify(service, times(1)).getByClientId(NON_EXISTENT);
    }

    // ==================== PUT /{clientId} (Update) ====================

    /**
     * Test purpose          - Verify update with valid request returns updated configuration.
     * Test data             - Valid client ID and request DTO.
     * Test expected result  - ResponseEntity with 200 OK and updated configuration.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void updateValidRequestReturnUpdatedConfiguration() {
        // GIVEN: Valid update request
        ClientAccessControlRequestDto request = createRequestDto(CLIENT_ID_1, TENANT_A, true, List.of("updated:*"));
        ClientAccessControlResponseDto response = createResponseDto(CLIENT_ID_1, TENANT_A, true);

        when(service.update(CLIENT_ID_1, request)).thenReturn(response);

        // WHEN: Controller update is called
        ResponseEntity<ClientAccessControlResponseDto> result = controller.update(CLIENT_ID_1, request);

        // THEN: Should return 200 OK with updated configuration
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(CLIENT_ID_1, result.getBody().getClientId());
        verify(service, times(1)).update(CLIENT_ID_1, request);
    }

    /**
     * Test purpose          - Verify update throws EntityNotFoundException for non-existent client.
     * Test data             - Non-existent client ID.
     * Test expected result  - EntityNotFoundException thrown.
     * Test type             - Negative.
     *
     * @throws Exception if test fails
     **/
    @Test
    void updateNonExistentClientThrowEntityNotFoundException() {
        // GIVEN: Client does not exist
        ClientAccessControlRequestDto request = createRequestDto(CLIENT_ID_1, TENANT_A, true, List.of("*:*"));

        when(service.update(NON_EXISTENT, request))
                .thenThrow(new EntityNotFoundException(CLIENT_NOT_FOUND));

        // WHEN/THEN: Should throw EntityNotFoundException
        try {
            controller.update(NON_EXISTENT, request);
        } catch (EntityNotFoundException e) {
            assertEquals(CLIENT_NOT_FOUND, e.getMessage());
        }

        verify(service, times(1)).update(NON_EXISTENT, request);
    }

    // ==================== DELETE /{clientId} (Delete) ====================

    /**
     * Test purpose          - Verify delete with permanent=false performs soft delete.
     * Test data             - Valid client ID, permanent=false.
     * Test expected result  - ResponseEntity with 200 OK and success message.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void deleteSoftDeleteReturnSuccessMessage() {
        // GIVEN: Client exists
        doNothing().when(service).delete(CLIENT_ID_1, false);

        // WHEN: Controller delete is called with permanent=false
        ResponseEntity<GenericResponseDto> response = controller.delete(CLIENT_ID_1, false);

        // THEN: Should return 200 OK with success message
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Client deleted successfully.", response.getBody().getMessage());
        verify(service, times(1)).delete(CLIENT_ID_1, false);
    }

    /**
     * Test purpose          - Verify delete with permanent=true performs permanent delete.
     * Test data             - Valid client ID, permanent=true.
     * Test expected result  - ResponseEntity with 200 OK and success message.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void deletePermanentDeleteReturnSuccessMessage() {
        // GIVEN: Client exists
        doNothing().when(service).delete(CLIENT_ID_1, true);

        // WHEN: Controller delete is called with permanent=true
        ResponseEntity<GenericResponseDto> response = controller.delete(CLIENT_ID_1, true);

        // THEN: Should return 200 OK with success message
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Client deleted successfully.", response.getBody().getMessage());
        verify(service, times(1)).delete(CLIENT_ID_1, true);
    }

    /**
     * Test purpose          - Verify delete throws EntityNotFoundException for non-existent client.
     * Test data             - Non-existent client ID.
     * Test expected result  - EntityNotFoundException thrown.
     * Test type             - Negative.
     *
     * @throws Exception if test fails
     **/
    @Test
    void deleteNonExistentClientThrowEntityNotFoundException() {
        // GIVEN: Client does not exist
        doThrow(new EntityNotFoundException(CLIENT_NOT_FOUND))
                .when(service).delete(NON_EXISTENT, false);

        // WHEN/THEN: Should throw EntityNotFoundException
        try {
            controller.delete(NON_EXISTENT, false);
        } catch (EntityNotFoundException e) {
            assertEquals(CLIENT_NOT_FOUND, e.getMessage());
        }

        verify(service, times(1)).delete(NON_EXISTENT, false);
    }

    // ==================== Helper Methods ====================

    private ClientAccessControlRequestDto createRequestDto(
            String clientId, String tenant, boolean active, List<String> allow) {
        ClientAccessControlRequestDto dto = new ClientAccessControlRequestDto();
        dto.setClientId(clientId);
        dto.setTenant(tenant);
        dto.setDescription(TEST_DESCRIPTION);
        dto.setIsActive(active);
        dto.setAllow(allow);
        return dto;
    }

    private ClientAccessControlResponseDto createResponseDto(
            String clientId, String tenant, boolean active) {
        ClientAccessControlResponseDto dto = new ClientAccessControlResponseDto();
        dto.setClientId(clientId);
        dto.setTenant(tenant);
        dto.setDescription(TEST_DESCRIPTION);
        dto.setIsActive(active);
        dto.setAllow(List.of("service-a:*"));
        return dto;
    }
}
