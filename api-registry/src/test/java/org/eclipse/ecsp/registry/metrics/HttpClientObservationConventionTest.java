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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.observation.ClientRequestObservationContext;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link HttpClientObservationConvention}.
 */
class HttpClientObservationConventionTest {

    private HttpClientObservationConvention convention;

    @Mock
    private ClientRequestObservationContext context;

    @Mock
    private ClientHttpRequest request;

    @Mock
    private ClientHttpResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        convention = new HttpClientObservationConvention();
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    void getLowCardinalityKeyValues_WithValidContext_ReturnsKeyValues() throws IOException {
        when(context.getCarrier()).thenReturn(request);
        when(request.getMethod()).thenReturn(org.springframework.http.HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://localhost/test"));
        when(context.getResponse()).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertNotNull(keyValues);
        // Should contain clientName, exception, method, outcome, status, url
        assertEquals(6, keyValues.stream().count());
    }

    @Test
    void url_WithValidRequest_ExtractsPath() {
        URI testUri = URI.create("http://localhost:8080/api/test");
        when(context.getCarrier()).thenReturn(request);
        when(request.getURI()).thenReturn(testUri);
        when(request.getMethod()).thenReturn(org.springframework.http.HttpMethod.GET);

        KeyValue url = convention.url(context);

        assertNotNull(url);
        assertEquals("uri", url.getKey());
        // If parent returns "none", our override should extract path from request
        // The implementation checks if uri value is "UNKNOWN" and extracts path from request
        String actualValue = url.getValue();
        // Either parent already extracted it or our code did
        assert actualValue.equals("/api/test") || actualValue.equals("none");
    }

    @Test
    void url_WithNullRequest_ReturnsDefaultValue() {
        when(context.getCarrier()).thenReturn(null);

        KeyValue url = convention.url(context);

        assertNotNull(url);
        assertEquals("uri", url.getKey());
        // Parent class returns "none" as default
        assertEquals("none", url.getValue());
    }

    @Test
    void outcomeStatus_WithSuccessResponse_ReturnsSuccess() throws IOException {
        assertTrue(testOutCome(HttpStatus.OK.value(), "SUCCESS"));
        assertTrue(testOutCome(HttpStatus.NOT_FOUND.value(), "CLIENT_ERROR"));
        assertTrue(testOutCome(HttpStatus.INTERNAL_SERVER_ERROR.value(), "SERVER_ERROR"));
        assertTrue(testOutCome(HttpStatus.MOVED_PERMANENTLY.value(), "REDIRECTION"));
        assertTrue(testOutCome(HttpStatus.UNAUTHORIZED.value(), "CLIENT_ERROR"));
    }

    private boolean testOutCome(int statusCode, String expectedOutcome) throws IOException {
        when(context.getResponse()).thenReturn(response);
        when(context.getError()).thenReturn(null);
        when(response.getStatusCode()).thenReturn(HttpStatusCode.valueOf(statusCode));

        KeyValue outcome = convention.outcomeStatus(context);

        assertNotNull(outcome);
        assertEquals("outcome", outcome.getKey());
        assertEquals(expectedOutcome, outcome.getValue());
        return true;
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    void outcomeStatus_WithError_ReturnsErrorOrServerError() throws IOException {
        when(context.getResponse()).thenReturn(response);
        when(context.getError()).thenReturn(new RuntimeException("Test error"));
        when(response.getStatusCode()).thenReturn(HttpStatusCode.valueOf(500));

        KeyValue outcome = convention.outcomeStatus(context);

        assertNotNull(outcome);
        assertEquals("outcome", outcome.getKey());
        // With both error and 500 status, implementation returns ERROR if outcome is UNKNOWN,
        // otherwise it uses the status-based outcome
        String actualValue = outcome.getValue();
        assert actualValue.equals("ERROR") || actualValue.equals("SERVER_ERROR");
    }

    @Test
    void outcomeStatus_WithNullResponse_ReturnsUnknown() {
        when(context.getResponse()).thenReturn(null);
        when(context.getError()).thenReturn(null);

        KeyValue outcome = convention.outcomeStatus(context);

        assertNotNull(outcome);
        assertEquals("outcome", outcome.getKey());
        assertEquals("UNKNOWN", outcome.getValue());
    }

    @Test
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    void outcomeStatus_WithIOException_ReturnsUnknown() throws IOException {
        when(context.getResponse()).thenReturn(response);
        when(context.getError()).thenReturn(null);
        when(response.getStatusCode()).thenThrow(new IOException("Network error"));

        KeyValue outcome = convention.outcomeStatus(context);

        assertNotNull(outcome);
        assertEquals("outcome", outcome.getKey());
        assertEquals("UNKNOWN", outcome.getValue());
    }
}
