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

package org.eclipse.ecsp.gateway.utils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GatewayUtils class.
 */
@ExtendWith(MockitoExtension.class)
class GatewayUtilsTest {

    @Mock
    private ServerHttpRequest request;

    @Mock
    private HttpHeaders headers;

    @Mock
    private InetSocketAddress remoteAddress;

    @Mock
    private InetAddress inetAddress;

    /**
     * Test getOutcomeFromHttpStatus with null status code.
     */
    @Test
    void testGetOutcomeFromHttpStatusWithNull() {
        String result = GatewayUtils.getOutcomeFromHttpStatus(null);
        assertEquals(GatewayConstants.UNKNOWN, result);
    }

    /**
     * Test getOutcomeFromHttpStatus with 2xx success status codes.
     */
    @Test
    void testGetOutcomeFromHttpStatusWith2xxSuccess() {
        assertEquals("SUCCESS", GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.OK));
        assertEquals("SUCCESS", GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.CREATED));
        assertEquals("SUCCESS", GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.ACCEPTED));
        assertEquals("SUCCESS", GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.NO_CONTENT));
    }

    /**
     * Test getOutcomeFromHttpStatus with 4xx client error status codes.
     */
    @Test
    void testGetOutcomeFromHttpStatusWith4xxClientError() {
        assertEquals("CLIENT_ERROR", GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.BAD_REQUEST));
        assertEquals("CLIENT_ERROR", GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.UNAUTHORIZED));
        assertEquals("CLIENT_ERROR", GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.FORBIDDEN));
        assertEquals("CLIENT_ERROR", GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.NOT_FOUND));
        assertEquals("CLIENT_ERROR", GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.CONFLICT));
    }

    /**
     * Test getOutcomeFromHttpStatus with 5xx server error status codes.
     */
    @Test
    void testGetOutcomeFromHttpStatusWith5xxServerError() {
        assertEquals("SERVER_ERROR", GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertEquals("SERVER_ERROR", GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.BAD_GATEWAY));
        assertEquals("SERVER_ERROR", GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertEquals("SERVER_ERROR", GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.GATEWAY_TIMEOUT));
    }

    /**
     * Test getOutcomeFromHttpStatus with 3xx redirection status codes.
     */
    @Test
    void testGetOutcomeFromHttpStatusWith3xxRedirection() {
        assertEquals(GatewayConstants.UNKNOWN, GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.MOVED_PERMANENTLY));
        assertEquals(GatewayConstants.UNKNOWN, GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.FOUND));
        assertEquals(GatewayConstants.UNKNOWN, GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.NOT_MODIFIED));
    }

    /**
     * Test getOutcomeFromHttpStatus with 1xx informational status codes.
     */
    @Test
    void testGetOutcomeFromHttpStatusWith1xxInformational() {
        assertEquals(GatewayConstants.UNKNOWN, GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.CONTINUE));
        assertEquals(GatewayConstants.UNKNOWN, 
                GatewayUtils.getOutcomeFromHttpStatus(HttpStatus.SWITCHING_PROTOCOLS));
    }

    /**
     * Test getClientIpAddress with X-Forwarded-For header containing single IP.
     */
    @Test
    void testGetClientIpAddressWithForwardedForSingleIp() {
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst("X-Forwarded-For")).thenReturn("192.168.1.100");

        String result = GatewayUtils.getClientIpAddress(request);
        assertEquals("192.168.1.100", result);
    }

    /**
     * Test getClientIpAddress with X-Forwarded-For header containing multiple IPs.
     */
    @Test
    void testGetClientIpAddressWithForwardedForMultipleIps() {
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.1, 172.16.0.1");

        String result = GatewayUtils.getClientIpAddress(request);
        assertEquals("192.168.1.100", result);
    }

    /**
     * Test getClientIpAddress with X-Forwarded-For header containing IP with spaces.
     */
    @Test
    void testGetClientIpAddressWithForwardedForIpWithSpaces() {
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst("X-Forwarded-For")).thenReturn("  192.168.1.100  , 10.0.0.1");

        String result = GatewayUtils.getClientIpAddress(request);
        assertEquals("192.168.1.100", result);
    }

    /**
     * Test getClientIpAddress without X-Forwarded-For header but with remote address.
     */
    @Test
    void testGetClientIpAddressWithRemoteAddress() {
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddress()).thenReturn(remoteAddress);
        when(remoteAddress.getAddress()).thenReturn(inetAddress);
        when(inetAddress.getHostAddress()).thenReturn("10.0.0.100");

        String result = GatewayUtils.getClientIpAddress(request);
        assertEquals("10.0.0.100", result);
    }

    /**
     * Test getClientIpAddress with empty X-Forwarded-For header and remote address.
     */
    @Test
    void testGetClientIpAddressWithEmptyForwardedForAndRemoteAddress() {
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst("X-Forwarded-For")).thenReturn("");
        when(request.getRemoteAddress()).thenReturn(remoteAddress);
        when(remoteAddress.getAddress()).thenReturn(inetAddress);
        when(inetAddress.getHostAddress()).thenReturn("10.0.0.100");

        String result = GatewayUtils.getClientIpAddress(request);
        assertEquals("10.0.0.100", result);
    }

    /**
     * Test getClientIpAddress with null remote address.
     */
    @Test
    void testGetClientIpAddressWithNullRemoteAddress() {
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddress()).thenReturn(null);

        String result = GatewayUtils.getClientIpAddress(request);
        assertEquals("unknown", result);
    }

    /**
     * Test getClientIpAddress with remote address having null InetAddress.
     */
    @Test
    void testGetClientIpAddressWithNullInetAddress() {
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddress()).thenReturn(remoteAddress);
        when(remoteAddress.getAddress()).thenReturn(null);

        String result = GatewayUtils.getClientIpAddress(request);
        assertEquals("unknown", result);
    }

    /**
     * Test getClientIpAddress when exception occurs during IP extraction.
     */
    @Test
    void testGetClientIpAddressWithException() {
        when(request.getHeaders()).thenThrow(new RuntimeException("Test exception"));

        String result = GatewayUtils.getClientIpAddress(request);
        assertEquals("unknown", result);
    }

    /**
     * Test getTokenValidationFailureReason with ExpiredJwtException.
     */
    @Test
    void testGetTokenValidationFailureReasonWithExpiredJwtException() {
        ExpiredJwtException ex = new ExpiredJwtException(null, null, "Token expired");
        String result = GatewayUtils.getTokenValidationFailureReason(ex);
        assertEquals("Token expired", result);
    }

    /**
     * Test getTokenValidationFailureReason with MalformedJwtException.
     */
    @Test
    void testGetTokenValidationFailureReasonWithMalformedJwtException() {
        MalformedJwtException ex = new MalformedJwtException("Malformed token");
        String result = GatewayUtils.getTokenValidationFailureReason(ex);
        assertEquals("Malformed token", result);
    }

    /**
     * Test getTokenValidationFailureReason with UnsupportedJwtException.
     */
    @Test
    void testGetTokenValidationFailureReasonWithUnsupportedJwtException() {
        UnsupportedJwtException ex = new UnsupportedJwtException("Unsupported token");
        String result = GatewayUtils.getTokenValidationFailureReason(ex);
        assertEquals("Unsupported token format", result);
    }

    /**
     * Test getTokenValidationFailureReason with SecurityException.
     */
    @Test
    void testGetTokenValidationFailureReasonWithSecurityException() {
        SecurityException ex = new SecurityException("Security error");
        String result = GatewayUtils.getTokenValidationFailureReason(ex);
        assertEquals("Token signature verification failed", result);
    }

    /**
     * Test getTokenValidationFailureReason with IllegalArgumentException.
     */
    @Test
    void testGetTokenValidationFailureReasonWithIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");
        String result = GatewayUtils.getTokenValidationFailureReason(ex);
        assertEquals("Invalid token argument", result);
    }

    /**
     * Test getTokenValidationFailureReason with RuntimeException.
     */
    @Test
    void testGetTokenValidationFailureReasonWithRuntimeException() {
        RuntimeException ex = new RuntimeException("Unknown error");
        String result = GatewayUtils.getTokenValidationFailureReason(ex);
        assertEquals("Unknown token validation error", result);
    }

    /**
     * Test getTokenValidationFailureReason with null exception.
     */
    @Test
    void testGetTokenValidationFailureReasonWithNullException() {
        String result = GatewayUtils.getTokenValidationFailureReason(null);
        assertEquals("Unknown token validation error", result);
    }

    /**
     * Test getTokenValidationFailureReason with generic Exception.
     */
    @Test
    void testGetTokenValidationFailureReasonWithGenericException() {
        Exception ex = new Exception("Generic exception");
        String result = GatewayUtils.getTokenValidationFailureReason(ex);
        assertEquals("Unknown token validation error", result);
    }
}
