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

package org.eclipse.ecsp.gateway.exceptions;

import org.eclipse.ecsp.gateway.plugins.spi.GatewayErrorResponseResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.webflux.autoconfigure.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Global exception handler for the Ignite API Gateway.
 *
 * <p>This class extends AbstractErrorWebExceptionHandler to handle exceptions
 * that occur during request processing and return appropriate error responses.
 *
 * @author Abhishek Kumar
 */
@Component
@Order(-2)
public class IgniteGlobalExceptionHandler extends AbstractErrorWebExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IgniteGlobalExceptionHandler.class);

    private final GatewayErrorResponseResolver errorResponseResolver;

    /**
     * Constructs an IgniteGlobalExceptionHandler with the specified parameters.
     *
     * @param errorAttributes      The ErrorAttributes to use for error handling.
     * @param resources            The WebProperties.Resources to use for resource
     *                             handling.
     * @param applicationContext   The ApplicationContext to use for context-related
     *                             operations.
     * @param configurer           The ServerCodecConfigurer to use for codec
     *                             configuration.
     * @param errorResponseResolver The SPI for building error response bodies and
     *                             determining
     *                             HTTP status codes. Falls back to the default
     *                             implementation
     *                             if no custom bean is registered.
     */
    public IgniteGlobalExceptionHandler(final ErrorAttributes errorAttributes,
            final WebProperties.Resources resources,
            final ApplicationContext applicationContext,
            final ServerCodecConfigurer configurer,
            final GatewayErrorResponseResolver errorResponseResolver) {
        super(errorAttributes, resources, applicationContext);
        setMessageReaders(configurer.getReaders());
        setMessageWriters(configurer.getWriters());
        this.errorResponseResolver = errorResponseResolver;
    }

    /**
     * Configures the routing function for error handling.
     *
     * @param errorAttributes The ErrorAttributes to use for error handling.
     * @return A RouterFunction that handles errors.
     */
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    /**
     * Renders the error response based on the provided ServerRequest.
     *
     * @param request The ServerRequest that caused the error.
     * @return A Mono containing the ServerResponse with the error details.
     */
    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable throwable = getError(request);
        ResponseEntity<Object> responseEntity = errorResponseResolver.buildResponse(throwable, request.exchange());
        LOGGER.error("Error occurred while processing request: {}", throwable.getMessage(), throwable);
        Object responseBody = responseEntity.getBody() != null ? responseEntity.getBody() : "";
        return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(responseBody);
    }
}
