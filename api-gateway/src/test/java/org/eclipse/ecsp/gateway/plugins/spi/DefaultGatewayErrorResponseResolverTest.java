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
import org.eclipse.ecsp.gateway.plugins.spi.DefaultGatewayErrorResponseResolver.ErrorResponseFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;

class DefaultGatewayErrorResponseResolverTest {

    @Test
    void testDefaultConstructorBuildsObjectFormat() {
        DefaultGatewayErrorResponseResolver builder = new DefaultGatewayErrorResponseResolver();
        ApiGatewayException exception = new ApiGatewayException(HttpStatus.UNAUTHORIZED,
                "api.gateway.error.token.invalid", "Token verification failed");
        ServerWebExchange exchange = mock(ServerWebExchange.class);

        ResponseEntity<Object> responseEntity = builder.buildResponse(exception, exchange);
        Object response = responseEntity.getBody();

        Assertions.assertTrue(response instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) response;
        Assertions.assertEquals("api.gateway.error.token.invalid", map.get("code"));
        Assertions.assertEquals("Token verification failed", map.get("message"));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
    }

    @Test
    void testListFormatConstructorBuildsListFormat() {
        DefaultGatewayErrorResponseResolver builder =
                new DefaultGatewayErrorResponseResolver(ErrorResponseFormat.LIST);
        ApiGatewayException exception = new ApiGatewayException(HttpStatus.UNAUTHORIZED,
                "api.gateway.error.token.invalid", "Token verification failed");
        ServerWebExchange exchange = mock(ServerWebExchange.class);

        ResponseEntity<Object> responseEntity = builder.buildResponse(exception, exchange);
        Object response = responseEntity.getBody();

        Assertions.assertTrue(response instanceof List);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> list = (List<Map<String, String>>) response;
        Assertions.assertEquals(1, list.size());
        Map<String, String> element = list.get(0);
        Assertions.assertEquals("api.gateway.error.token.invalid", element.get("detailedErrorCode"));
        Assertions.assertEquals("Token verification failed", element.get("message"));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
    }
}
