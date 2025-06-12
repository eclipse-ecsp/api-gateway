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

import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

/**
 * Unit tests for the IgniteGlobalExceptionHandler class.
 * These tests verify that the exception handler correctly prepares responses
 * for various types of exceptions.
 */
class IgniteGlobalExceptionHandlerTest {

    /**
     * Tests the handling of a ResponseStatusException.
     */
    @Test
    void testHandleResponseStatusException() {
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        Map<String, String> response = IgniteGlobalExceptionHandler.prepareResponse(exception);
        Assertions.assertEquals(GatewayConstants.API_GATEWAY_ERROR, response.get("code"));
        Assertions.assertEquals("Resource not found", response.get("message"));
    }

    @Test
    void testHandleResponseStatusExceptionWithNoReason() {
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, null);
        Map<String, String> response = IgniteGlobalExceptionHandler.prepareResponse(exception);
        Assertions.assertEquals(GatewayConstants.API_GATEWAY_ERROR, response.get("code"));
        Assertions.assertEquals(GatewayConstants.INTERNAL_SERVER_ERROR, response.get("message"));
    }

    @Test
    void testHandleNoResourceFoundException() {
        NoResourceFoundException exception = new NoResourceFoundException("Resource not found");
        Map<String, String> response = IgniteGlobalExceptionHandler.prepareResponse(exception);
        Assertions.assertEquals(GatewayConstants.API_GATEWAY_ERROR, response.get("code"));
        Assertions.assertEquals(GatewayConstants.REQUEST_NOT_FOUND, response.get("message"));
    }

    @Test
    void testHandleApiGatewayException() {
        ApiGatewayException exception = new ApiGatewayException(HttpStatus.BAD_REQUEST,
                GatewayConstants.API_GATEWAY_ERROR,
                "Bad request");
        Map<String, String> response = IgniteGlobalExceptionHandler.prepareResponse(exception);
        Assertions.assertEquals(GatewayConstants.API_GATEWAY_ERROR, response.get("code"));
        Assertions.assertEquals("Bad request", response.get("message"));
    }

    @Test
    void testHandleIllegalStateException() {
        IllegalStateException exception = new IllegalStateException("Invalid request value");
        Map<String, String> response = IgniteGlobalExceptionHandler.prepareResponse(exception);
        Assertions.assertEquals(GatewayConstants.API_GATEWAY_ERROR, response.get("code"));
        Assertions.assertEquals(GatewayConstants.INTERNAL_SERVER_ERROR, response.get("message"));
    }

    @Test
    void testHandleGenericException() {
        Exception exception = new Exception("An unexpected error occurred");
        Map<String, String> response = IgniteGlobalExceptionHandler.prepareResponse(exception);
        Assertions.assertEquals(GatewayConstants.API_GATEWAY_ERROR, response.get("code"));
        Assertions.assertEquals(GatewayConstants.INTERNAL_SERVER_ERROR, response.get("message"));
    }

    @Test
    void testStatusCode() {
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        HttpStatusCode status = IgniteGlobalExceptionHandler.determineHttpStatus(exception);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, status);

        NoResourceFoundException noResourceFoundException = new NoResourceFoundException("Resource not found");
        status = IgniteGlobalExceptionHandler.determineHttpStatus(noResourceFoundException);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, status);

        ApiGatewayException apiGatewayException = new ApiGatewayException(HttpStatus.BAD_REQUEST,
                GatewayConstants.API_GATEWAY_ERROR,
                "Bad request");
        status = IgniteGlobalExceptionHandler.determineHttpStatus(apiGatewayException);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, status);

        IllegalStateException illegalStateException = new IllegalStateException("Invalid request value");
        status = IgniteGlobalExceptionHandler.determineHttpStatus(illegalStateException);
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status);

        Exception genericException = new Exception("An unexpected error occurred");
        status = IgniteGlobalExceptionHandler.determineHttpStatus(genericException);
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status);
    }

}
