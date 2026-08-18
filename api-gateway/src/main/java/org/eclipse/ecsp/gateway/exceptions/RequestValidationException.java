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

package org.eclipse.ecsp.gateway.exceptions;

import lombok.Getter;
import org.openapi4j.core.validation.ValidationResults;
import org.springframework.http.HttpStatusCode;

/**
 * Exception thrown when an incoming request fails structural validation, such as:
 * <ul>
 *   <li>A mandatory request header is missing or has an invalid value</li>
 *   <li>The request body does not conform to the configured OpenAPI schema</li>
 * </ul>
 *
 * <p>Extends {@link ApiGatewayException} so that all existing exception handlers
 * that catch {@code ApiGatewayException} continue to work without modification.
 *
 * <p>The additional {@link #fieldName} property allows custom
 * {@link org.eclipse.ecsp.gateway.plugins.spi.GatewayErrorResponseResolver} implementations
 * to include field-level detail in the error response, for example:
 * <pre>{@code
 * if (throwable instanceof RequestValidationException rve) {
 *     return Map.of(
 *         "field",   rve.getFieldName(),
 *         "message", rve.getMessage(),
 *         "code",    rve.getErrorCode()
 *     );
 * }
 * }</pre>
 *
 * <p>Thrown by:
 * <ul>
 *   <li>{@link org.eclipse.ecsp.gateway.plugins.filters.RequestBodyFilter} — for schema validation failures</li>
 *   <li>{@link org.eclipse.ecsp.gateway.plugins.RequestHeaderFilter} — for header validation failures</li>
 * </ul>
 */
@Getter
public class RequestValidationException extends ApiGatewayException {

    /**
     * The name of the request field (header name or body field path) that failed validation.
     * May be {@code null} if the failure is not attributable to a specific field.
     */
    private final String fieldName;

    /**
     * The original validation error message or details (e.g., from an OpenAPI validation library).
     * May be {@code null} if not available or not applicable.
     */
    private final String validationError;

    /**
     * The detailed validation results from the OpenAPI validation library.
     * May be {@code null} if the failure is not attributable to schema validation.
     */
    private final ValidationResults validationResults;

    /**
     * Constructs a RequestValidationException with the specified status, error code,
     * message, and field name.
     *
     * @param statusCode the HTTP status code to return (typically 400 BAD_REQUEST)
     * @param errorCode  the application-specific error code string
     * @param message    a human-readable description of the validation failure
     * @param fieldName  the name of the header or body field that caused the failure;
     *                   may be {@code null}
     */
    public RequestValidationException(HttpStatusCode statusCode, String errorCode,
                                       String message, String fieldName) {
        this(statusCode, errorCode, message, fieldName, null, null);
    }

    /**
     * Constructs a RequestValidationException with the specified status, error code,
     * message, field name, and the original validation error.
     *
     * @param statusCode      the HTTP status code to return (typically 400 BAD_REQUEST)
     * @param errorCode       the application-specific error code string
     * @param message         a human-readable description of the validation failure
     * @param fieldName       the name of the header or body field that caused the failure;
     *                        may be {@code null}
     * @param validationError the original validation error message or details;
     *                        may be {@code null}
     */
    public RequestValidationException(HttpStatusCode statusCode, String errorCode,
                                       String message, String fieldName, String validationError) {
        this(statusCode, errorCode, message, fieldName, validationError, null);
    }

    /**
     * Constructs a RequestValidationException with the specified status, error code,
     * message, field name, and the OpenAPI validation results.
     *
     * @param statusCode        the HTTP status code to return (typically 400 BAD_REQUEST)
     * @param errorCode         the application-specific error code string
     * @param message           a human-readable description of the validation failure
     * @param fieldName         the name of the header or body field that caused the failure;
     *                          may be {@code null}
     * @param validationError   the original validation error message or details;
     *                          may be {@code null}
     * @param validationResults the OpenAPI validation results; may be {@code null}
     */
    public RequestValidationException(HttpStatusCode statusCode, String errorCode,
                                       String message, String fieldName, String validationError,
                                       ValidationResults validationResults) {
        super(statusCode, errorCode, message);
        this.fieldName = fieldName;
        this.validationError = validationError;
        this.validationResults = validationResults;
    }
}
