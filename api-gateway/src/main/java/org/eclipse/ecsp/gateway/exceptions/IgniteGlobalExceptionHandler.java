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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * Global exception handler for the Ignite API Gateway.
 *
 * <p>This class extends AbstractErrorWebExceptionHandler to handle exceptions
 * that occur during request processing and return appropriate error responses.
 *
 * @author Abhishek Kumar
 */
@Component
@Order(Integer.MIN_VALUE)
public class IgniteGlobalExceptionHandler extends AbstractErrorWebExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IgniteGlobalExceptionHandler.class);
    private static final String MESSAGE = "message";
    private static final String CODE = "code";
    private static final String API_GATEWAY_ERROR = "api.gateway.error";

    /**
     * Constructs an IgniteGlobalExceptionHandler with the specified parameters.
     *
     * @param errorAttributes    The ErrorAttributes to use for error handling.
     * @param resources          The WebProperties.Resources to use for resource handling.
     * @param applicationContext The ApplicationContext to use for context-related operations.
     * @param configurer         The ServerCodecConfigurer to use for codec configuration.
     */
    public IgniteGlobalExceptionHandler(final ErrorAttributes errorAttributes,
                                        final WebProperties.Resources resources,
                                        final ApplicationContext applicationContext,
                                        final ServerCodecConfigurer configurer) {
        super(errorAttributes, resources, applicationContext);
        setMessageReaders(configurer.getReaders());
        setMessageWriters(configurer.getWriters());
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

        ErrorAttributeOptions options = ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE);
        Map<String, Object> errorPropertiesMap = getErrorAttributes(request, options);
        Throwable throwable = getError(request);
        HttpStatusCode httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        errorPropertiesMap.clear();
        // prepare the response based on the ApiGatewayException
        if (throwable instanceof ApiGatewayException apiGatewayException) {
            errorPropertiesMap.put(CODE, apiGatewayException.getErrorCode());
            errorPropertiesMap.put(MESSAGE, apiGatewayException.getMessage());
            httpStatus = apiGatewayException.getStatusCode();
        } else if (throwable instanceof NoResourceFoundException) {
            errorPropertiesMap.put(CODE, API_GATEWAY_ERROR);
            errorPropertiesMap.put(MESSAGE, "Request not found");
            httpStatus = HttpStatus.NOT_FOUND;
        } else if (throwable instanceof ResponseStatusException responseStatusException) {
            errorPropertiesMap.put(CODE, API_GATEWAY_ERROR);
            errorPropertiesMap.put(MESSAGE, determineMessage(responseStatusException, request.path()));
            httpStatus = responseStatusException.getStatusCode();
        } else {
            errorPropertiesMap.put(CODE, API_GATEWAY_ERROR);
            errorPropertiesMap.put(MESSAGE, determineMessage(throwable, request.path()));
        }
        return ServerResponse.status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(errorPropertiesMap);
    }

    /**
     * Determines the error message based on the provided Throwable and URI.
     *
     * @param throwable The Throwable that caused the error.
     * @param uri       The URI of the request that caused the error.
     * @return A String containing the error message.
     */
    private String determineMessage(Throwable throwable, String uri) {
        if (throwable instanceof ResponseStatusException responseStatusException) {
            LOGGER.error("ResponseStatusException during request processing: {}",
                    responseStatusException.getReason(), responseStatusException);
            return responseStatusException.getReason();
        }

        LOGGER.error("Unexpected error occurred : {} , sending Internal Server Error response to api: {}",
                throwable, uri);
        return "Internal Server Error";
    }
}
