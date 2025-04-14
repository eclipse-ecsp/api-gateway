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

package org.eclipse.ecsp.gateway.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.ecsp.gateway.exceptions.ApiGatewayException;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.gateway.utils.ObjectMapperUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.schema.validator.v3.SchemaValidator;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;
import java.util.Map;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class RequestBodyFilterTest {

    RequestBodyValidator.Config requestBodyValidatorConfig;
    private RequestBodyFilter requestBodyFilter;
    private ServerWebExchange request;
    private GatewayFilterChain gatewayFilterChain;

    @BeforeEach
    void setUp() {
        requestBodyValidatorConfig = new RequestBodyValidator.Config();
        requestBodyFilter = new RequestBodyFilter(requestBodyValidatorConfig);
        ReflectionTestUtils.setField(requestBodyFilter, "requestBodyValidation", true);
        gatewayFilterChain = Mockito.mock(GatewayFilterChain.class);
    }

    @Test
    void testFilterWithValidRequestBody() throws ResolutionException, JsonProcessingException {
        String body = """
                 {
                   "name": "Bob"
                 }
                """;
        request = MockServerWebExchange.builder(
                MockServerHttpRequest.method(HttpMethod.POST, "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("validHeader", "headerValue")
                        .body(body)
        ).build();
        request.mutate();
        Map<String, Object> attributes = request.getAttributes();
        attributes.put(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR, body);
        Route route = Mockito.mock(Route.class);
        JsonNode schema = ObjectMapperUtil.getObjectMapper().readTree("""
                {
                  "$schema": "http://json-schema.org/draft-04/schema#",
                  "type": "object",
                  "properties": {
                     "name": {
                        "type": "string"
                     }
                  },
                  "required": [
                     "name"
                  ]
                }""");
        SchemaValidator schemaValidator = new SchemaValidator("schema", schema);
        when(route.getMetadata()).thenReturn(Map.of(GatewayConstants.SCHEMA_VALIDATOR, schemaValidator));
        attributes.put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        StepVerifier.create(requestBodyFilter.filter(request, gatewayFilterChain))
                .expectComplete();
    }

    @Test
    void testFilterWithInvalidRequestBody() throws JsonProcessingException, ResolutionException {
        String body = """
                 {
                   "key": "value"
                 }
                """;
        request = MockServerWebExchange.builder(
                MockServerHttpRequest.method(HttpMethod.POST, "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("validHeader", "headerValue")
                        .body(body)
        ).build();
        request.mutate();
        Map<String, Object> attributes = request.getAttributes();
        attributes.put(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR, body);
        Route route = Mockito.mock(Route.class);
        JsonNode schema = ObjectMapperUtil.getObjectMapper().readTree("""
                {
                  "$schema": "http://json-schema.org/draft-04/schema#",
                  "type": "object",
                  "properties": {
                     "name": {
                        "type": "string"
                     }
                  },
                  "required": [
                     "name"
                  ]
                }""");
        SchemaValidator schemaValidator = new SchemaValidator("schema", schema);
        when(route.getMetadata()).thenReturn(Map.of(GatewayConstants.SCHEMA_VALIDATOR, schemaValidator));
        attributes.put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        ApiGatewayException invalidRequestEx = Assertions.assertThrows(ApiGatewayException.class,
                () -> requestBodyFilter.filter(request, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, invalidRequestEx.getStatusCode());
        Assertions.assertEquals("Validation failed : Field 'name' is required.", invalidRequestEx.getMessage());
    }

    @Test
    void testFilterWithNoSchemaValidator() {
        String body = """
                 {
                   "key": "value"
                 }
                """;
        request = MockServerWebExchange.builder(
                MockServerHttpRequest.method(HttpMethod.POST, "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("validHeader", "headerValue")
                        .body(body)
        ).build();
        request.mutate();
        Map<String, Object> attributes = request.getAttributes();
        attributes.put(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR, body);
        Route route = Mockito.mock(Route.class);
        when(route.getMetadata()).thenReturn(null);
        attributes.put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        StepVerifier.create(requestBodyFilter.filter(request, gatewayFilterChain))
                .expectComplete();
        Mockito.verify(gatewayFilterChain, atLeastOnce()).filter(Mockito.any());
    }

    @Test
    void testRouteNull() {
        request = MockServerWebExchange.builder(
                MockServerHttpRequest.method(HttpMethod.POST, "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("validHeader", "headerValue")
                        .body("body")).build();
        ApiGatewayException invalidJsonEx = Assertions.assertThrows(ApiGatewayException.class,
                () -> requestBodyFilter.filter(request, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.NOT_FOUND, invalidJsonEx.getStatusCode());
        Assertions.assertEquals("Request not found", invalidJsonEx.getMessage());
    }

    @Test
    void testFilterWithInvalidJsonBody() throws JsonProcessingException, ResolutionException {
        String body = """
                 {
                   "key": "value"
                """;
        request = MockServerWebExchange.builder(
                MockServerHttpRequest.method(HttpMethod.POST, "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("validHeader", "headerValue")
                        .body(body)
        ).build();
        request.mutate();
        Map<String, Object> attributes = request.getAttributes();
        attributes.put(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR, body);
        Route route = Mockito.mock(Route.class);
        JsonNode schema = ObjectMapperUtil.getObjectMapper().readTree("""
                {
                  "$schema": "http://json-schema.org/draft-04/schema#",
                  "type": "object",
                  "properties": {
                     "name": {
                        "type": "string"
                     }
                  },
                  "required": [
                     "name"
                  ]
                }""");
        SchemaValidator schemaValidator = new SchemaValidator("schema", schema);
        when(route.getMetadata()).thenReturn(Map.of(GatewayConstants.SCHEMA_VALIDATOR, schemaValidator));
        attributes.put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        ApiGatewayException invalidJsonEx = Assertions.assertThrows(ApiGatewayException.class,
                () -> requestBodyFilter.filter(request, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, invalidJsonEx.getStatusCode());
        Assertions.assertEquals("Invalid request payload", invalidJsonEx.getMessage());
    }

    @Test
    void testFilterWithEmptyJsonBody() throws JsonProcessingException, ResolutionException {
        String body = "";
        request = MockServerWebExchange.builder(
                MockServerHttpRequest.method(HttpMethod.POST, "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("validHeader", "headerValue")
                        .body(body)
        ).build();
        request.mutate();
        Map<String, Object> attributes = request.getAttributes();
        attributes.put(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR, body);
        Route route = Mockito.mock(Route.class);
        JsonNode schema = ObjectMapperUtil.getObjectMapper().readTree("""
                {
                  "$schema": "http://json-schema.org/draft-04/schema#",
                  "type": "object",
                  "properties": {
                     "name": {
                        "type": "string"
                     }
                  },
                  "required": [
                     "name"
                  ]
                }""");
        SchemaValidator schemaValidator = new SchemaValidator("schema", schema);
        when(route.getMetadata()).thenReturn(Map.of(GatewayConstants.SCHEMA_VALIDATOR, schemaValidator));
        attributes.put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        ApiGatewayException invalidRequestEx = Assertions.assertThrows(ApiGatewayException.class,
                () -> requestBodyFilter.filter(request, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, invalidRequestEx.getStatusCode());
        Assertions.assertEquals("Invalid request payload", invalidRequestEx.getMessage());
    }

    @Test
    void testFilterWithValidationDisabled() throws ResolutionException, JsonProcessingException {
        ReflectionTestUtils.setField(requestBodyFilter, "requestBodyValidation", false);
        RequestBodyValidator requestBodyValidator = new RequestBodyValidator();
        requestBodyValidator.apply(requestBodyValidatorConfig);
        String body = "";
        request = MockServerWebExchange.builder(
                MockServerHttpRequest.method(HttpMethod.POST, "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("validHeader", "headerValue")
                        .body(body)
        ).build();
        request.mutate();
        Map<String, Object> attributes = request.getAttributes();
        attributes.put(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR, body);
        Route route = Mockito.mock(Route.class);
        JsonNode schema = ObjectMapperUtil.getObjectMapper().readTree("""
                {
                  "$schema": "http://json-schema.org/draft-04/schema#",
                  "type": "object",
                  "properties": {
                     "name": {
                        "type": "string"
                     }
                  },
                  "required": [
                     "name"
                  ]
                }""");
        SchemaValidator schemaValidator = new SchemaValidator("schema", schema);
        when(route.getMetadata()).thenReturn(Map.of(GatewayConstants.SCHEMA_VALIDATOR, schemaValidator));
        attributes.put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

        ApiGatewayException invalidRequestEx = Assertions.assertThrows(ApiGatewayException.class,
                () -> requestBodyFilter.filter(request, gatewayFilterChain));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, invalidRequestEx.getStatusCode());
        Assertions.assertEquals("Invalid request payload", invalidRequestEx.getMessage());
    }

}
