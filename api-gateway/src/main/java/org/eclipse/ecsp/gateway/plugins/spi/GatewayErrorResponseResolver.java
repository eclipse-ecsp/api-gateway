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

import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;

/**
 * SPI for building the JSON error response body and determining the HTTP status
 * code.
 *
 * <p>Register a Spring bean implementing this interface to fully customise the
 * error
 * JSON shape returned by the API Gateway for any exception — including
 * {@link org.eclipse.ecsp.gateway.exceptions.ApiGatewayException},
 * {@link org.eclipse.ecsp.gateway.exceptions.RequestValidationException},
 * and any other {@link Throwable}.
 *
 * <p>The default implementation delegates to the existing static
 * {@code IgniteGlobalExceptionHandler.prepareResponse()} and
 * {@code IgniteGlobalExceptionHandler.determineHttpStatus()} methods,
 * preserving the existing {@code {"message": "...", "code": "..."}} error
 * shape.
 *
 * <p>If no custom bean is present, the gateway automatically uses
 * {@link DefaultGatewayErrorResponseResolver} via
 * {@code @ConditionalOnMissingBean}.
 *
 * <p><strong>Tip:</strong> To handle
 * {@link org.eclipse.ecsp.gateway.exceptions.RequestValidationException}
 * separately and return field-level error details, use {@code instanceof} in
 * the
 * {@link #buildResponse(Throwable, ServerWebExchange)} method:
 * 
 * <pre>{@code
 * if (throwable instanceof RequestValidationException rve) {
 *     return Map.of("field", rve.getFieldName(), "message", rve.getMessage());
 * }
 * }</pre>
 */
public interface GatewayErrorResponseResolver {

    /**
     * Build the corresponding HTTP response representing the error.
     *
     * <p>The returned {@link ResponseEntity} wraps both the HTTP status code
     * and the response body logic that will be serialised to JSON.
     *
     * @param throwable the exception that caused the error; never null
     * @param exchange  the server web exchange that triggered the error; never null
     * @return a non-null ResponseEntity encompassing status code and body
     */
    ResponseEntity<Object> buildResponse(Throwable throwable, ServerWebExchange exchange);
}
