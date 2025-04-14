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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Test class for RestTemplateLogInterceptor.
 */
@ExtendWith(SpringExtension.class)
class RestTemplateLogInterceptorTest {

    private RestTemplateLogInterceptor restTemplateLogInterceptor;

    private static final int STATUS_CODE = 500;

    @BeforeEach
    public void before() {
        restTemplateLogInterceptor = new RestTemplateLogInterceptor();
    }

    @Test
    void intercept() throws Exception {
        Request request = new Request();
        byte[] body = "Test body".getBytes(StandardCharsets.UTF_8);
        ClientHttpResponse response = restTemplateLogInterceptor.intercept(request, body, new RequestExecution());
        Assertions.assertNotNull(response);
    }

    static class Request implements HttpRequest {

        HttpHeaders headers = new HttpHeaders();

        @Override
        public HttpMethod getMethod() {
            return HttpMethod.GET;
        }

        @Override
        public URI getURI() {
            try {
                return new URI("http://www.google.com");
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Map<String, Object> getAttributes() {
            return Map.of();
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }

    static class Response implements ClientHttpResponse {

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return null;
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return 0;
        }

        @Override
        public String getStatusText() throws IOException {
            return null;
        }

        @Override
        public void close() {

        }

        @Override
        public InputStream getBody() throws IOException {
            org.eclipse.ecsp.register.model.Response response =
                    new org.eclipse.ecsp.register.model.Response(STATUS_CODE, "IO");
            response.setArgs(new HashMap<>());
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(response);
            return new ByteArrayInputStream(json.getBytes());
        }

        @Override
        public HttpHeaders getHeaders() {
            return null;
        }
    }

    static class RequestExecution implements ClientHttpRequestExecution {

        @Override
        public ClientHttpResponse execute(HttpRequest request, byte[] body) throws
                IOException {
            return new Response();
        }

    }
}
