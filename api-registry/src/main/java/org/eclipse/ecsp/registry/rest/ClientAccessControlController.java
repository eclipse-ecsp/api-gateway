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

import jakarta.validation.Valid;
import org.eclipse.ecsp.registry.dto.BulkCreateResponseDto;
import org.eclipse.ecsp.registry.dto.ClientAccessControlRequestDto;
import org.eclipse.ecsp.registry.dto.ClientAccessControlResponseDto;
import org.eclipse.ecsp.registry.dto.CreateClientAccessControlGroup;
import org.eclipse.ecsp.registry.dto.GenericResponseDto;
import org.eclipse.ecsp.registry.service.ClientAccessControlService;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * REST controller for Client Access Control management.
 *
 * <p>Provides endpoints for CRUD operations on client access configurations.
 * All endpoints return JSON responses and require proper authentication/authorization.
 *
 * <p>Base path: /v1/config/client-access-control
 *
 * @see ClientAccessControlService
 */
@RestController
@RequestMapping("/v1/config/client-access-control")
@Validated
public class ClientAccessControlController {
    /**
     * Constructor with dependencies.
     *
     * @param service the client access control service
     */
    public ClientAccessControlController(ClientAccessControlService service) {
        this.service = service;
    }

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ClientAccessControlController.class);
    private final ClientAccessControlService service;

    /**
     * Bulk create client access control configurations.
     *
     * @param requests List of client configurations (max 100)
     * @return Response with message and list of created client IDs with 201 Created status
     */
    @PostMapping
    @Validated(CreateClientAccessControlGroup.class) 
    public ResponseEntity<BulkCreateResponseDto> bulkCreate(
            @RequestBody
            List<@Valid ClientAccessControlRequestDto> requests) {
        LOGGER.info("Bulk create request received: {} clients", requests.size());
        List<ClientAccessControlResponseDto> responses = service.bulkCreate(requests);
        
        // Build response with message and created client IDs
        List<String> createdClientIds = responses.stream()
                .map(ClientAccessControlResponseDto::getClientId)
                .toList();
        BulkCreateResponseDto response = new BulkCreateResponseDto(
                createdClientIds.size() + " client(s) created successfully.",
                createdClientIds
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all client access control configurations.
     *
     * @param includeInactive Whether to include inactive clients (default: false)
     * @return List of all client configurations
     */
    @GetMapping
    public ResponseEntity<List<ClientAccessControlResponseDto>> getAll(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        LOGGER.debug("Get all request received: includeInactive={}", includeInactive);
        List<ClientAccessControlResponseDto> responses = service.getAll(includeInactive);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get client access control configuration by client ID.
     *
     * @param clientId Client identifier
     * @return Configuration details
     */
    @GetMapping("/{clientId}")
    public ResponseEntity<ClientAccessControlResponseDto> getByClientId(
            @PathVariable String clientId) {
        LOGGER.debug("Get by client ID request received: clientId={}", clientId);
        ClientAccessControlResponseDto response = service.getByClientId(clientId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update client access control configuration.
     *
     * @param clientId Client ID
     * @param request Updated configuration
     * @return Updated configuration
     */
    @PutMapping("/{clientId}")
    public ResponseEntity<ClientAccessControlResponseDto> update(
            @PathVariable String clientId,
            @RequestBody ClientAccessControlRequestDto request) {
        LOGGER.info("Update request received: clientId={}", clientId);
        ClientAccessControlResponseDto response = service.update(clientId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete client access control configuration.
     *
     * @param clientId Client ID
     * @param permanent Whether to permanently delete (true) or soft delete (false, default)
     * @return Success message with 200 OK status
     */
    @DeleteMapping("/{clientId}")
    public ResponseEntity<GenericResponseDto> delete(
            @PathVariable String clientId,
            @RequestParam(defaultValue = "false") boolean permanent) {
        LOGGER.info("Delete request received: clientId={}, permanent={}", clientId, permanent);
        service.delete(clientId, permanent);
        
        GenericResponseDto response = new GenericResponseDto();
        response.setMessage("Client deleted successfully.");
        return ResponseEntity.ok(response);
    }
}
