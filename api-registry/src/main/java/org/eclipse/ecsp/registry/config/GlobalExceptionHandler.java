package org.eclipse.ecsp.registry.config;

import org.eclipse.ecsp.registry.dto.ErrorResponseDto;
import org.eclipse.ecsp.registry.dto.GenericResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

/**
 * Global exception handler to manage exceptions across the application.
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
}
