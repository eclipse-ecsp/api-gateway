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

package org.eclipse.ecsp.registry.config;

import jakarta.persistence.EntityNotFoundException;
import org.eclipse.ecsp.registry.dto.ConflictResponseDto;
import org.eclipse.ecsp.registry.dto.ErrorResponseDto;
import org.eclipse.ecsp.registry.dto.GenericResponseDto;
import org.eclipse.ecsp.registry.dto.ValidationErrorResponseDto;
import org.eclipse.ecsp.registry.exception.DuplicateClientException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Global exception handler to manage exceptions across the application.
 * Handles various exception types and returns appropriate error responses per design document.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ResponseStatusException and returns a structured error response.
     *
     * @param ex the ResponseStatusException thrown
     * @return ResponseEntity containing the error response and appropriate HTTP status
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDto> handleResponseStatusException(ResponseStatusException ex) {
        ErrorResponseDto errorResponse = new ErrorResponseDto();
        errorResponse.setError(ex.getReason());
        return new ResponseEntity<>(errorResponse, ex.getStatusCode());
    }

    /**
     * Handles DuplicateClientException (409 Conflict).
     * Returns response with message and list of duplicate client IDs.
     *
     * @param ex the DuplicateClientException thrown
     * @return ResponseEntity with 409 Conflict status
     */
    @ExceptionHandler(DuplicateClientException.class)
    public ResponseEntity<ConflictResponseDto> handleDuplicateClientException(DuplicateClientException ex) {
        ConflictResponseDto errorResponse = new ConflictResponseDto(
                ex.getMessage(),
                ex.getDuplicateClientIds()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handles DataIntegrityViolationException (409 Conflict).
     * Thrown when database constraints are violated (e.g., unique constraint on client_id).
     *
     * @param ex the DataIntegrityViolationException thrown
     * @return ResponseEntity with 409 Conflict status
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ConflictResponseDto> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex) {
        // Extract client ID from exception message if possible, otherwise return generic message
        String message = "Duplicate client IDs detected.";
        if (ex.getMessage() != null && ex.getMessage().contains("client_id")) {
            message = "Client ID already exists in the database.";
        }
        
        ConflictResponseDto errorResponse = new ConflictResponseDto(
                message,
                Collections.emptyList() // Cannot determine specific duplicates from DB exception
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handles Jakarta Bean Validation errors (400 Bad Request).
     * Returns response with message and list of field-level validation errors.
     *
     * @param ex the MethodArgumentNotValidException thrown
     * @return ResponseEntity with 400 Bad Request status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponseDto> handleValidationException(MethodArgumentNotValidException ex) {
        List<ValidationErrorResponseDto.FieldError> errors = new ArrayList<>();
        
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            ValidationErrorResponseDto.FieldError error = ValidationErrorResponseDto.FieldError.builder()
                    .field(fieldError.getField())
                    .error(fieldError.getDefaultMessage())
                    .build();
            errors.add(error);
        }

        ValidationErrorResponseDto errorResponse = ValidationErrorResponseDto.builder()
                .message("Validation failed for one or more clients.")
                .errors(errors)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles IllegalArgumentException (400 Bad Request).
     * Used for business validation errors like exceeding max bulk size.
     *
     * @param ex the IllegalArgumentException thrown
     * @return ResponseEntity with 400 Bad Request status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GenericResponseDto> handleIllegalArgumentException(IllegalArgumentException ex) {
        GenericResponseDto errorResponse = new GenericResponseDto();
        errorResponse.setMessage(ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles EntityNotFoundException (404 Not Found).
     * Returns response with error message.
     *
     * @param ex the EntityNotFoundException thrown
     * @return ResponseEntity with 404 Not Found status
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<GenericResponseDto> handleEntityNotFoundException(EntityNotFoundException ex) {
        GenericResponseDto errorResponse = new GenericResponseDto();
        errorResponse.setMessage(ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles all other exceptions (500 Internal Server Error).
     * Returns generic error message to avoid exposing internal details.
     *
     * @param ex the Exception thrown
     * @return ResponseEntity with 500 Internal Server Error status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponseDto> handleGenericException(Exception ex) {
        GenericResponseDto errorResponse = new GenericResponseDto();
        errorResponse.setMessage("Internal server error occurred while processing the request.");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
