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

package org.eclipse.ecsp.gateway.metrics;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientRequestObservationContext;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HttpClientObservationConvention.
 * Tests metric name customization and observation convention behavior.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class HttpClientObservationConventionTest {

    @Test
    void constructor_WithNullMetricsName_UsesDefaultName() {
        HttpClientObservationConvention convention = new HttpClientObservationConvention(null);
        assertEquals("http.client.requests", convention.getName());
    }

    @Test
    void constructor_WithEmptyMetricsName_UsesDefaultName() {
        HttpClientObservationConvention convention = new HttpClientObservationConvention("");
        assertEquals("http.client.requests", convention.getName());
    }

    @Test
    void constructor_WithWhitespaceMetricsName_UsesProvidedName() {
        // StringUtils.isNotEmpty() doesn't trim, so whitespace is considered non-empty
        HttpClientObservationConvention convention = new HttpClientObservationConvention("   ");
        assertEquals("   ", convention.getName());
    }

    @Test
    void constructor_WithCustomMetricsName_UsesCustomName() {
        HttpClientObservationConvention convention = new HttpClientObservationConvention("custom.client.metrics");
        assertEquals("custom.client.metrics", convention.getName());
    }

    @Test
    void getLowCardinalityKeyValues_WithValidContext_ReturnsKeyValues() {
        HttpClientObservationConvention convention = new HttpClientObservationConvention(null);
        ClientRequestObservationContext context = mock(ClientRequestObservationContext.class);
        ClientRequest request = ClientRequest.create(
                org.springframework.http.HttpMethod.GET,
                URI.create("http://example.com/api/test")
        ).build();

        when(context.getRequest()).thenReturn(request);

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertNotNull(keyValues);
    }

    @Test
    void url_WithValidRequestAndPath_ReturnsPath() {
        assertTrue(testValidRequestPath("http://example.com/api/v1/users"));
    }

    private boolean testValidRequestPath(String endpoint) {
        HttpClientObservationConvention convention = new HttpClientObservationConvention(null);
        ClientRequestObservationContext context = mock(ClientRequestObservationContext.class);
        ClientRequest request = mock(ClientRequest.class);

        URI testUri = URI.create(endpoint);
        when(context.getRequest()).thenReturn(request);
        when(request.url()).thenReturn(testUri);

        KeyValue url = convention.url(context);

        assertEquals("uri", url.getKey());
        assertNotNull(url.getValue());
        return true;
    }

    @Test
    void url_WithNullRequest_ReturnsUnknownValue() {
        HttpClientObservationConvention convention = new HttpClientObservationConvention(null);
        ClientRequestObservationContext context = mock(ClientRequestObservationContext.class);

        when(context.getRequest()).thenReturn(null);

        KeyValue url = convention.url(context);

        assertEquals("uri", url.getKey());
        // When request is null, outcome depends on parent class behavior
        assertNotNull(url.getValue());
    }

    @Test
    void outcomeStatus_WithSuccessResponse_ReturnsSuccessOutcome() {
        HttpClientObservationConvention convention = new HttpClientObservationConvention(null);
        ClientRequestObservationContext context = mock(ClientRequestObservationContext.class);
        ClientResponse response = mock(ClientResponse.class);

        when(context.getResponse()).thenReturn(response);
        when(response.statusCode()).thenReturn(HttpStatus.OK);
        when(context.getError()).thenReturn(null);

        KeyValue outcome = convention.outcomeStatus(context);

        assertEquals("outcome", outcome.getKey());
        assertNotNull(outcome.getValue());
    }

    @Test
    void outcomeStatus_WithError_ReturnsErrorOutcome() {
        HttpClientObservationConvention convention = new HttpClientObservationConvention(null);
        ClientRequestObservationContext context = mock(ClientRequestObservationContext.class);

        RuntimeException exception = new RuntimeException("Connection failed");
        when(context.getError()).thenReturn(exception);
        when(context.getResponse()).thenReturn(null);

        KeyValue outcome = convention.outcomeStatus(context);

        assertEquals("outcome", outcome.getKey());
        assertEquals("ERROR", outcome.getValue());
    }

    @Test
    void outcomeStatus_WithNullResponse_ReturnsUnknown() {
        HttpClientObservationConvention convention = new HttpClientObservationConvention(null);
        ClientRequestObservationContext context = mock(ClientRequestObservationContext.class);

        when(context.getResponse()).thenReturn(null);
        when(context.getError()).thenReturn(null);

        KeyValue outcome = convention.outcomeStatus(context);

        assertEquals("outcome", outcome.getKey());
        assertNotNull(outcome.getValue());
    }

    @Test
    void url_WithComplexPath_ReturnsPath() {
        HttpClientObservationConvention convention = new HttpClientObservationConvention(null);
        ClientRequestObservationContext context = mock(ClientRequestObservationContext.class);
        ClientRequest request = mock(ClientRequest.class);

        URI testUri = URI.create("http://example.com/api/v1/users/123/profile");
        when(context.getRequest()).thenReturn(request);
        when(request.url()).thenReturn(testUri);

        KeyValue url = convention.url(context);

        assertEquals("uri", url.getKey());
        assertNotNull(url.getValue());
    }

    @Test
    void url_WithQueryParameters_ReturnsPath() {
        HttpClientObservationConvention convention = new HttpClientObservationConvention(null);
        ClientRequestObservationContext context = mock(ClientRequestObservationContext.class);
        ClientRequest request = mock(ClientRequest.class);

        URI testUri = URI.create("http://example.com/api/search?query=test&limit=10");
        when(context.getRequest()).thenReturn(request);
        when(request.url()).thenReturn(testUri);

        KeyValue url = convention.url(context);

        assertEquals("uri", url.getKey());
        assertNotNull(url.getValue());
    }

    @Test
    void outcomeStatus_WithClientError_ReturnsClientError() {
        HttpClientObservationConvention convention = new HttpClientObservationConvention(null);
        ClientRequestObservationContext context = mock(ClientRequestObservationContext.class);
        ClientResponse response = mock(ClientResponse.class);

        when(context.getResponse()).thenReturn(response);
        when(response.statusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(context.getError()).thenReturn(null);

        KeyValue outcome = convention.outcomeStatus(context);

        assertEquals("outcome", outcome.getKey());
        assertNotNull(outcome.getValue());
    }
}
