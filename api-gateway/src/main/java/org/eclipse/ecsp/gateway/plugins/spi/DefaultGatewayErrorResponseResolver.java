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

package org.eclipse.ecsp.gateway.plugins.spi;

import org.eclipse.ecsp.gateway.exceptions.ApiGatewayException;
import org.eclipse.ecsp.gateway.exceptions.RequestValidationException;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link GatewayErrorResponseResolver}.
 *
 * <p>Supports two response formats via {@link ErrorResponseFormat}:
 * <ul>
 * <li>{@link ErrorResponseFormat#OBJECT} (default):
 * 
 * <pre>{@code {"code": "...", "message": "..."}}</pre>
 * 
 * </li>
 * <li>{@link ErrorResponseFormat#LIST}:
 * 
 * <pre>{@code [{"detailedErrorCode": "...", "message": "..."}]}</pre>
 * 
 * </li>
 * </ul>
 *
 * <p>This bean is registered only when no other
 * {@link GatewayErrorResponseResolver} bean
 * is present in the Spring application context
 * ({@code @ConditionalOnMissingBean}).
 */
public class DefaultGatewayErrorResponseResolver implements GatewayErrorResponseResolver {

    /**
     * Enum defining the error response format.
     */
    public enum ErrorResponseFormat {
        /** Standard object format: {"code": "...", "message": "..."}. */
        OBJECT,
        /** List format: [{"detailedErrorCode": "...", "message": "..."}]. */
        LIST
    }

    private final ErrorResponseFormat format;

    /**
     * Constructs a DefaultGatewayErrorResponseResolver using the standard OBJECT
     * format.
     */
    public DefaultGatewayErrorResponseResolver() {
        this(ErrorResponseFormat.OBJECT);
    }

    /**
     * Constructs a DefaultGatewayErrorResponseResolver with the specified
     * {@link ErrorResponseFormat}.
     *
     * @param format the error response format to use (OBJECT or LIST)
     */
    public DefaultGatewayErrorResponseResolver(ErrorResponseFormat format) {
        this.format = format != null ? format : ErrorResponseFormat.OBJECT;
    }
    
    /**
     * Prepares the error response map based on the provided Throwable.
     *
     * @param throwable The Throwable that caused the error.
     * @return A Map containing the error message and code.
     */
    public Map<String, String> prepareResponse(Throwable throwable) {
        String responseMessage = GatewayConstants.INTERNAL_SERVER_ERROR;
        String errorResponseCode = GatewayConstants.API_GATEWAY_ERROR;
        if (throwable instanceof NoResourceFoundException) {
            responseMessage = GatewayConstants.REQUEST_NOT_FOUND;
        } else if (throwable instanceof RequestValidationException requestValidationException) {
            responseMessage = requestValidationException.getMessage();
            errorResponseCode = requestValidationException.getErrorCode();
            // Optional: Enrich default message with openapi ValidationResults here,
            // current impl populates the Exception's message with a list of failures.
        } else if (throwable instanceof ApiGatewayException apiGatewayException) {
            responseMessage = apiGatewayException.getMessage();
            errorResponseCode = apiGatewayException.getErrorCode();
        } else if (throwable instanceof ResponseStatusException responseStatusException
                && responseStatusException.getReason() != null) {
            responseMessage = responseStatusException.getReason();

        }
        return Map.of(GatewayConstants.MESSAGE, responseMessage, GatewayConstants.CODE, errorResponseCode);
    }
    
    /**
     * Determines the HTTP status code based on the provided Throwable.
     *
     * @param throwable The Throwable that caused the error.
     * @return The HttpStatusCode corresponding to the error.
     */
    public HttpStatusCode determineHttpStatus(Throwable throwable) {
        if (throwable instanceof NoResourceFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (throwable instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getStatusCode();
        }
        if (throwable instanceof ApiGatewayException apiGatewayException) {
            return apiGatewayException.getStatusCode();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @Override
    public ResponseEntity<Object> buildResponse(Throwable throwable, ServerWebExchange exchange) {
        Map<String, String> errorMap = prepareResponse(throwable);
        HttpStatusCode statusCode = determineHttpStatus(throwable);
        Object body;
        
        if (format == ErrorResponseFormat.LIST) {
            Map<String, String> listElement = new LinkedHashMap<>();
            listElement.put("detailedErrorCode", errorMap.get(GatewayConstants.CODE));
            listElement.put("message", errorMap.get(GatewayConstants.MESSAGE));
            body = List.of(listElement);
        } else {
            body = new HashMap<>(errorMap);
        }
        
        return new ResponseEntity<>(body, statusCode);
    }
}
