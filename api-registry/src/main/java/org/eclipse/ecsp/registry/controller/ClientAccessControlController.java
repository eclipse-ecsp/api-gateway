package org.eclipse.ecsp.registry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlFilterDto;
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlRequestDto;
import org.eclipse.ecsp.registry.common.dto.ClientAccessControlResponseDto;
import org.eclipse.ecsp.registry.service.ClientAccessControlService;
import org.springframework.data.domain.Page;
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
 * <p>
 * Provides endpoints for CRUD operations on client access configurations.
 * All endpoints return JSON responses and require proper authentication/authorization.
 *
 * <p>
 * Base path: /api/registry/client-access-control
 *
 * @see ClientAccessControlService
 */
@RestController
@RequestMapping("/api/registry/client-access-control")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Client Access Control", description = "APIs for managing client access control configurations")
public class ClientAccessControlController {

    private final ClientAccessControlService service;

    /**
     * Bulk create client access control configurations.
     *
     * <p>
     * POST /api/registry/client-access-control
     *
     * @param requests List of client configurations (max 100)
     * @return List of created configurations with 201 Created status
     */
    @PostMapping
    @Operation(
            summary = "Bulk create client access control configurations",
            description = "Creates multiple client access control configurations in a single atomic transaction. "
                    + "Maximum 100 clients per request. All succeed or all fail atomically."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Configurations created successfully",
            content = @Content(schema = @Schema(implementation = ClientAccessControlResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request: validation errors, duplicate client IDs, or > 100 clients",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict: client ID already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<List<ClientAccessControlResponseDto>> bulkCreate(
            @RequestBody @Valid List<@Valid ClientAccessControlRequestDto> requests) {
        log.info("Bulk create request received: {} clients", requests.size());
        List<ClientAccessControlResponseDto> responses = service.bulkCreate(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Get all client access control configurations.
     *
     * <p>
     * GET /api/registry/client-access-control?includeInactive={boolean}
     *
     * @param includeInactive Whether to include inactive clients (default: false)
     * @return List of all client configurations
     */
    @GetMapping
    @Operation(
            summary = "Get all client access control configurations",
            description = "Retrieves all client access control configurations. "
                    + "By default, only active clients are returned."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Configurations retrieved successfully",
            content = @Content(schema = @Schema(implementation = ClientAccessControlResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<List<ClientAccessControlResponseDto>> getAll(
            @Parameter(description = "Include inactive clients")
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.debug("Get all request received: includeInactive={}", includeInactive);
        List<ClientAccessControlResponseDto> responses = service.getAll(includeInactive);
        return ResponseEntity.ok(responses);
    }

    /**
     * Filter client access control configurations with pagination.
     *
     * <p>
     * POST /api/registry/client-access-control/filter
     *
     * @param filter Filter criteria and pagination parameters
     * @return Page of matching configurations
     */
    @PostMapping("/filter")
    @Operation(
            summary = "Filter client access control configurations",
            description = "Filters client access control configurations with pagination support. "
                    + "Supports partial matching on clientId and tenant, exact matching on isActive, "
                    + "and date range filters."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Configurations filtered successfully",
            content = @Content(schema = @Schema(implementation = Page.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid filter criteria",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<Page<ClientAccessControlResponseDto>> filter(
            @RequestBody @Valid ClientAccessControlFilterDto filter) {
        log.debug("Filter request received: {}", filter);
        Page<ClientAccessControlResponseDto> page = service.filter(filter);
        return ResponseEntity.ok(page);
    }

    /**
     * Get client access control configuration by ID.
     *
     * <p>
     * GET /api/registry/client-access-control/{id}
     *
     * @param id Configuration ID
     * @return Configuration details
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get client access control configuration by ID",
            description = "Retrieves a specific client access control configuration by its ID."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Configuration retrieved successfully",
            content = @Content(schema = @Schema(implementation = ClientAccessControlResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Configuration not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<ClientAccessControlResponseDto> getById(
            @Parameter(description = "Configuration ID")
            @PathVariable Long id) {
        log.debug("Get by ID request received: id={}", id);
        ClientAccessControlResponseDto response = service.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get client access control configuration by client ID.
     *
     * <p>
     * GET /api/registry/client-access-control/client/{clientId}
     *
     * @param clientId Client identifier
     * @return Configuration details
     */
    @GetMapping("/client/{clientId}")
    @Operation(
            summary = "Get client access control configuration by client ID",
            description = "Retrieves a specific client access control configuration by client ID."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Configuration retrieved successfully",
            content = @Content(schema = @Schema(implementation = ClientAccessControlResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Configuration not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<ClientAccessControlResponseDto> getByClientId(
            @Parameter(description = "Client identifier")
            @PathVariable String clientId) {
        log.debug("Get by client ID request received: clientId={}", clientId);
        ClientAccessControlResponseDto response = service.getByClientId(clientId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update client access control configuration.
     *
     * <p>
     * PUT /api/registry/client-access-control/{id}
     *
     * @param id Configuration ID
     * @param request Updated configuration
     * @return Updated configuration
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Update client access control configuration",
            description = "Updates an existing client access control configuration. "
                    + "All fields in the request will replace existing values."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Configuration updated successfully",
            content = @Content(schema = @Schema(implementation = ClientAccessControlResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request: validation errors or duplicate client ID",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Configuration not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict: client ID already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<ClientAccessControlResponseDto> update(
            @Parameter(description = "Configuration ID")
            @PathVariable Long id,
            @RequestBody @Valid ClientAccessControlRequestDto request) {
        log.info("Update request received: id={}, clientId={}", id, request.getClientId());
        ClientAccessControlResponseDto response = service.update(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete client access control configuration.
     *
     * <p>
     * DELETE /api/registry/client-access-control/{id}?permanent={boolean}
     *
     * @param id Configuration ID
     * @param permanent Whether to permanently delete (true) or soft delete (false, default)
     * @return No content (204)
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete client access control configuration",
            description = "Deletes a client access control configuration. "
                    + "By default, performs a soft delete (sets is_deleted flag). "
                    + "Use permanent=true for physical deletion."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Configuration deleted successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Configuration not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Configuration ID")
            @PathVariable Long id,
            @Parameter(description = "Permanent delete flag")
            @RequestParam(defaultValue = "false") boolean permanent) {
        log.info("Delete request received: id={}, permanent={}", id, permanent);
        service.delete(id, permanent);
        return ResponseEntity.noContent().build();
    }

    /**
     * Error response schema for OpenAPI documentation.
     */
    @Schema(description = "Error response")
    private static class ErrorResponse {
        @Schema(description = "HTTP status code")
        private int status;

        @Schema(description = "Error message")
        private String message;

        @Schema(description = "Timestamp")
        private String timestamp;

        @Schema(description = "Request path")
        private String path;
    }
}
