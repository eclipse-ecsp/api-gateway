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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTokenParserTest {

    private final DefaultTokenParser tokenParser = new DefaultTokenParser();

    private ServerWebExchange mockExchange(HttpHeaders headers) {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        Builder builder = mock(Builder.class);
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(request.getId()).thenReturn("test-request-id");
        when(request.mutate()).thenReturn(builder);
        RequestPath path = mock(RequestPath.class);
        when(path.value()).thenReturn("/test-path");
        when(request.getPath()).thenReturn(path);
        return exchange;
    }

    @Test
    void testParseExtractsTokenFromAuthorizationHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer sample.jwt.token");
        ServerWebExchange exchange = mockExchange(headers);

        String token = tokenParser.parse(exchange);

        Assertions.assertEquals("sample.jwt.token", token);
    }

    @Test
    void testParseExtractsTokenFromWebSocketProtocolHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Upgrade", "websocket");
        headers.put("Sec-WebSocket-Protocol", List.of("chat, header.payload.signature"));
        ServerWebExchange exchange = mockExchange(headers);

        String token = tokenParser.parse(exchange);

        Assertions.assertEquals("header.payload.signature", token);
    }

    @Test
    void testParseThrowsWhenWebSocketProtocolHasNoJwt() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Upgrade", "websocket");
        headers.put("Sec-WebSocket-Protocol", List.of("chat, plain-protocol"));
        ServerWebExchange exchange = mockExchange(headers);

        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class,
                () -> tokenParser.parse(exchange));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void testParseThrowsWhenWebSocketUpgradeHasNoProtocolHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Upgrade", "websocket");
        ServerWebExchange exchange = mockExchange(headers);

        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class,
                () -> tokenParser.parse(exchange));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void testParseThrowsWhenHeaderMissingAndNotWebSocket() {
        HttpHeaders headers = new HttpHeaders();
        ServerWebExchange exchange = mockExchange(headers);

        ApiGatewayException exception = Assertions.assertThrows(ApiGatewayException.class,
                () -> tokenParser.parse(exchange));
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
}
