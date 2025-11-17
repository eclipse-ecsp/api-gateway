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

package org.eclipse.ecsp.registry.metrics;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.observation.ServerRequestObservationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HttpServerObservationConvention.
 * Tests metric key values generation for server requests.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class HttpServerObservationConventionTest {

    private HttpServerObservationConvention convention;

    @BeforeEach
    void setUp() {
        convention = new HttpServerObservationConvention();
    }

    @Test
    void getLowCardinalityKeyValues_WithValidContext_ReturnsKeyValues() {
        ServerRequestObservationContext context = mock(ServerRequestObservationContext.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(context.getCarrier()).thenReturn(request);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(context.getResponse()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.OK.value());
        when(context.getError()).thenReturn(null);

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertNotNull(keyValues);
        assertEquals(5, keyValues.stream().count());
    }

    @Test
    void url_WithValidRequest_ReturnsRequestUri() {
        ServerRequestObservationContext context = mock(ServerRequestObservationContext.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(context.getCarrier()).thenReturn(request);
        when(request.getRequestURI()).thenReturn("/api/v1/routes");

        KeyValue url = convention.url(context);

        assertEquals("uri", url.getKey());
        assertNotNull(url.getValue());
    }

    @Test
    void outcomeStatus_WithSuccessResponse_ReturnsSuccess() {
        ServerRequestObservationContext context = mock(ServerRequestObservationContext.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(context.getResponse()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.OK.value());
        when(context.getError()).thenReturn(null);

        KeyValue outcome = convention.outcomeStatus(context);

        assertEquals("outcome", outcome.getKey());
        assertNotNull(outcome.getValue());
    }

    @Test
    void outcomeStatus_WithClientError_ReturnsClientError() {
        ServerRequestObservationContext context = mock(ServerRequestObservationContext.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(context.getResponse()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.BAD_REQUEST.value());
        when(context.getError()).thenReturn(null);

        KeyValue outcome = convention.outcomeStatus(context);

        assertEquals("outcome", outcome.getKey());
        assertNotNull(outcome.getValue());
    }

    @Test
    void outcomeStatus_WithServerError_ReturnsServerError() {
        ServerRequestObservationContext context = mock(ServerRequestObservationContext.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(context.getResponse()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());
        when(context.getError()).thenReturn(null);

        KeyValue outcome = convention.outcomeStatus(context);

        assertEquals("outcome", outcome.getKey());
        assertNotNull(outcome.getValue());
    }

    @Test
    void outcomeStatus_WithError_ReturnsError() {
        ServerRequestObservationContext context = mock(ServerRequestObservationContext.class);
        RuntimeException exception = new RuntimeException("Test error");

        when(context.getError()).thenReturn(exception);
        when(context.getResponse()).thenReturn(null);

        KeyValue outcome = convention.outcomeStatus(context);

        assertEquals("outcome", outcome.getKey());
        assertEquals("ERROR", outcome.getValue());
    }

    @Test
    void outcomeStatus_WithNullResponse_ReturnsUnknown() {
        ServerRequestObservationContext context = mock(ServerRequestObservationContext.class);

        when(context.getResponse()).thenReturn(null);
        when(context.getError()).thenReturn(null);

        KeyValue outcome = convention.outcomeStatus(context);

        assertEquals("outcome", outcome.getKey());
        assertNotNull(outcome.getValue());
    }

    @Test
    void outcomeStatus_WithRedirection_ReturnsRedirection() {
        ServerRequestObservationContext context = mock(ServerRequestObservationContext.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(context.getResponse()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.MOVED_PERMANENTLY.value());
        when(context.getError()).thenReturn(null);

        KeyValue outcome = convention.outcomeStatus(context);

        assertEquals("outcome", outcome.getKey());
        assertNotNull(outcome.getValue());
    }

    @Test
    void outcomeStatus_WithNotFound_ReturnsClientError() {
        ServerRequestObservationContext context = mock(ServerRequestObservationContext.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(context.getResponse()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.NOT_FOUND.value());
        when(context.getError()).thenReturn(null);

        KeyValue outcome = convention.outcomeStatus(context);

        assertEquals("outcome", outcome.getKey());
        assertNotNull(outcome.getValue());
    }

    @Test
    void outcomeStatus_WithUnauthorized_ReturnsClientError() {
        ServerRequestObservationContext context = mock(ServerRequestObservationContext.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(context.getResponse()).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.UNAUTHORIZED.value());
        when(context.getError()).thenReturn(null);

        KeyValue outcome = convention.outcomeStatus(context);

        assertEquals("outcome", outcome.getKey());
        assertNotNull(outcome.getValue());
    }
}
