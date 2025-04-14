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

package org.eclipse.ecsp.restclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.eclipse.ecsp.register.model.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;

/**
 * RestTemplateErrorHandlerTest.
 */
@ExtendWith(SpringExtension.class)
class RestTemplateErrorHandlerTest {

    private RestTemplateErrorHandler errorHandler;
    private static final URI URL = URI.create("http://localhost:8080/error-api");

    @BeforeEach
    public void setUp() {
        errorHandler = new RestTemplateErrorHandler();
    }

    @Test
    void testHasError() throws IOException {
        Assertions.assertTrue(errorHandler.hasError(createClientHttpResponse(HttpStatus.BAD_REQUEST, "Not Found")));
        Assertions.assertTrue(errorHandler.hasError(createClientHttpResponse(HttpStatus.INTERNAL_SERVER_ERROR, "IO")));
    }

    @Test
    void test200Response() throws IOException {
        Assertions.assertFalse(errorHandler.hasError(createClientHttpResponse(HttpStatus.OK, "OK")));
    }

    @Test
    void testHandleError404() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> errorHandler.handleError(URL, HttpMethod.GET,
                        createClientHttpResponse(HttpStatus.BAD_REQUEST, "Not Found"))
        );
    }

    @Test
    void testHandleError500() {
        Assertions.assertThrows(IOException.class,
                () -> errorHandler.handleError(URL, HttpMethod.GET,
                        createClientHttpResponse(HttpStatus.INTERNAL_SERVER_ERROR, "IO"))
        );
    }

    private ClientHttpResponse createClientHttpResponse(HttpStatus status, String message) {
        return new ClientHttpResponse() {
            @Override
            public HttpStatusCode getStatusCode() throws IOException {
                return status;
            }

            @Override
            public int getRawStatusCode() throws IOException {
                return status.value();
            }

            @Override
            public String getStatusText() throws IOException {
                return status.name();
            }

            @Override
            public void close() {
            }

            @Override
            public InputStream getBody() throws IOException {
                Response response = new Response(status.value(), message);
                response.setArgs(new HashMap<>());
                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                String json = ow.writeValueAsString(response);
                return new ByteArrayInputStream(json.getBytes());
            }

            @Override
            public HttpHeaders getHeaders() {
                return null;
            }
        };
    }

    @Test
    void handleErrorWithNullBody() throws IOException {
        handlerErrorResponseBody(null);
    }

    @Test
    void handleErrorWithEmptyBody() throws IOException {
        handlerErrorResponseBody("");
    }

    @Test
    void handleErrorWithValidJsonBody() throws IOException {
        handlerErrorResponseBody("Valid message");
    }

    void handlerErrorResponseBody(String message) {
        ClientHttpResponse response = createClientHttpResponse(HttpStatus.BAD_REQUEST, message);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> errorHandler.handleError(URL, HttpMethod.GET, response));
    }

}