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

package org.eclipse.ecsp.gateway.service;

import org.eclipse.ecsp.gateway.utils.InputValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ClientIdValidator.
 * Tests security pattern detection for SQL injection, XSS, and path traversal.
 */
class ClientIdValidatorTest {

    private static final String VALID_CLIENT_ID = "valid_client_id";

    // Valid client ID tests
    @Test
    void testIsValidStandardClientId() {
        assertTrue(InputValidator.isValid("test_client_123"));
    }

    @Test
    void testIsValidWithDots() {
        assertTrue(InputValidator.isValid("client.app.service"));
    }

    @Test
    void testIsValidWithHyphens() {
        assertTrue(InputValidator.isValid("mobile-app-client"));
    }

    @Test
    void testIsValidWithUnderscores() {
        assertTrue(InputValidator.isValid("automation_qa_client"));
    }

    @Test
    void testIsValidAlphanumeric() {
        assertTrue(InputValidator.isValid("Client123ABC"));
    }

    // Invalid - Null/Empty tests
    @Test
    void testIsValidNull() {
        assertFalse(InputValidator.isValid(null));
    }

    @Test
    void testIsValidEmpty() {
        assertFalse(InputValidator.isValid(""));
    }

    @Test
    void testIsValidBlank() {
        assertFalse(InputValidator.isValid("   "));
    }

    // Invalid - Length tests
    @Test
    void testIsValidTooShort() {
        assertFalse(InputValidator.isValid("ab")); // 2 characters
    }

    @Test
    void testIsValidMinimumLength() {
        assertTrue(InputValidator.isValid("abc")); // 3 characters (minimum)
    }

    @Test
    void testIsValidMaximumLength() {
        String maxLength = "a".repeat(128);
        assertTrue(InputValidator.isValid(maxLength));
    }

    @Test
    void testIsValidTooLong() {
        String tooLong = "a".repeat(129);
        assertFalse(InputValidator.isValid(tooLong));
    }

    // SQL Injection detection tests
    @Test
    void testDetectsSqlInjectionSelectStatement() {
        assertTrue(InputValidator.detectsSqlInjection("client' OR '1'='1'"));
        assertTrue(InputValidator.detectsSqlInjection("test'; SELECT * FROM users--"));
    }

    @Test
    void testDetectsSqlInjectionUnionAttack() {
        assertTrue(InputValidator.detectsSqlInjection("client' UNION SELECT password FROM users--"));
    }

    @Test
    void testDetectsSqlInjectionCommentMarkers() {
        assertTrue(InputValidator.detectsSqlInjection("client'--"));
        assertTrue(InputValidator.detectsSqlInjection("client'/*"));
    }

    @Test
    void testDetectsSqlInjectionBooleanLogic() {
        assertTrue(InputValidator.detectsSqlInjection("' OR true--"));
        assertTrue(InputValidator.detectsSqlInjection("' AND false--"));
    }

    @Test
    void testDetectsSqlInjectionInsertStatement() {
        assertTrue(InputValidator.detectsSqlInjection("'; INSERT INTO clients VALUES('hacker')--"));
    }

    @Test
    void testDetectsSqlInjectionDropStatement() {
        assertTrue(InputValidator.detectsSqlInjection("'; DROP TABLE clients--"));
    }

    @Test
    void testDetectsSqlInjectionNoPattern() {
        assertFalse(InputValidator.detectsSqlInjection(VALID_CLIENT_ID));
    }

    // XSS detection tests
    @Test
    void testDetectsXssScriptTag() {
        assertTrue(InputValidator.detectsXss("<script>alert('xss')</script>"));
        assertTrue(InputValidator.detectsXss("client<script>malicious</script>"));
    }

    @Test
    void testDetectsXssIframeTag() {
        assertTrue(InputValidator.detectsXss("<iframe src='evil.com'></iframe>"));
    }

    @Test
    void testDetectsXssEventHandlers() {
        assertTrue(InputValidator.detectsXss("<img onerror='alert(1)'>"));
        assertTrue(InputValidator.detectsXss("<div onclick='malicious()'>"));
    }

    @Test
    void testDetectsXssJavascriptProtocol() {
        assertTrue(InputValidator.detectsXss("javascript:alert(1)"));
    }

    @Test
    void testDetectsXssNoPattern() {
        assertFalse(InputValidator.detectsXss(VALID_CLIENT_ID));
    }

    // Path traversal detection tests
    @Test
    void testDetectsPathTraversalDotDotSlash() {
        assertTrue(InputValidator.detectsPathTraversal("../../../etc/passwd"));
    }

    @Test
    void testDetectsPathTraversalDotDotBackslash() {
        assertTrue(InputValidator.detectsPathTraversal("..\\..\\windows\\system32"));
    }

    @Test
    void testDetectsPathTraversalUrlEncoded() {
        assertTrue(InputValidator.detectsPathTraversal("%2e%2e%2f"));
        assertTrue(InputValidator.detectsPathTraversal("%2e%2e\\"));
    }

    @Test
    void testDetectsPathTraversalDoubleSlash() {
        assertTrue(InputValidator.detectsPathTraversal("..//file"));
    }

    @Test
    void testDetectsPathTraversalNoPattern() {
        assertFalse(InputValidator.detectsPathTraversal(VALID_CLIENT_ID));
    }

    // Integration tests combining validation checks
    @Test
    void testIsValidSqlInjectionRejected() {
        assertFalse(InputValidator.isValid("client' OR '1'='1'"));
    }

    @Test
    void testIsValidXssRejected() {
        assertFalse(InputValidator.isValid("<script>alert('xss')</script>"));
    }

    @Test
    void testIsValidPathTraversalRejected() {
        assertFalse(InputValidator.isValid("../../../etc/passwd"));
    }

    @Test
    void testIsValidMultipleLegitimateClients() {
        String[] validClients = {
            "mobile_app",
            "web-portal",
            "automation.qa.client",
            "uidam-portal",
            "sdp_mobile_app",
            "external_client_123",
            "Client.With.Dots",
            "client-with-hyphens",
            "client_with_underscores"
        };

        for (String clientId : validClients) {
            assertTrue(InputValidator.isValid(clientId), "Should be valid: " + clientId);
        }
    }
}
