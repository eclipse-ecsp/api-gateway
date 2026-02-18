package org.eclipse.ecsp.gateway.service;

import org.eclipse.ecsp.gateway.utils.InputValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClientIdValidator.
 * Tests security pattern detection for SQL injection, XSS, and path traversal.
 */
class ClientIdValidatorTest {

    // Valid client ID tests
    @Test
    void testIsValid_StandardClientId() {
        assertTrue(InputValidator.isValid("test_client_123"));
    }

    @Test
    void testIsValid_WithDots() {
        assertTrue(InputValidator.isValid("client.app.service"));
    }

    @Test
    void testIsValid_WithHyphens() {
        assertTrue(InputValidator.isValid("mobile-app-client"));
    }

    @Test
    void testIsValid_WithUnderscores() {
        assertTrue(InputValidator.isValid("automation_qa_client"));
    }

    @Test
    void testIsValid_Alphanumeric() {
        assertTrue(InputValidator.isValid("Client123ABC"));
    }

    // Invalid - Null/Empty tests
    @Test
    void testIsValid_Null() {
        assertFalse(InputValidator.isValid(null));
    }

    @Test
    void testIsValid_Empty() {
        assertFalse(InputValidator.isValid(""));
    }

    @Test
    void testIsValid_Blank() {
        assertFalse(InputValidator.isValid("   "));
    }

    // Invalid - Length tests
    @Test
    void testIsValid_TooShort() {
        assertFalse(InputValidator.isValid("ab")); // 2 characters
    }

    @Test
    void testIsValid_MinimumLength() {
        assertTrue(InputValidator.isValid("abc")); // 3 characters (minimum)
    }

    @Test
    void testIsValid_MaximumLength() {
        String maxLength = "a".repeat(128);
        assertTrue(InputValidator.isValid(maxLength));
    }

    @Test
    void testIsValid_TooLong() {
        String tooLong = "a".repeat(129);
        assertFalse(InputValidator.isValid(tooLong));
    }

    // SQL Injection detection tests
    @Test
    void testDetectsSqlInjection_SelectStatement() {
        assertTrue(InputValidator.detectsSqlInjection("client' OR '1'='1'"));
        assertTrue(InputValidator.detectsSqlInjection("test'; SELECT * FROM users--"));
    }

    @Test
    void testDetectsSqlInjection_UnionAttack() {
        assertTrue(InputValidator.detectsSqlInjection("client' UNION SELECT password FROM users--"));
    }

    @Test
    void testDetectsSqlInjection_CommentMarkers() {
        assertTrue(InputValidator.detectsSqlInjection("client'--"));
        assertTrue(InputValidator.detectsSqlInjection("client'/*"));
    }

    @Test
    void testDetectsSqlInjection_BooleanLogic() {
        assertTrue(InputValidator.detectsSqlInjection("' OR true--"));
        assertTrue(InputValidator.detectsSqlInjection("' AND false--"));
    }

    @Test
    void testDetectsSqlInjection_InsertStatement() {
        assertTrue(InputValidator.detectsSqlInjection("'; INSERT INTO clients VALUES('hacker')--"));
    }

    @Test
    void testDetectsSqlInjection_DropStatement() {
        assertTrue(InputValidator.detectsSqlInjection("'; DROP TABLE clients--"));
    }

    @Test
    void testDetectsSqlInjection_NoPattern() {
        assertFalse(InputValidator.detectsSqlInjection("valid_client_id"));
    }

    // XSS detection tests
    @Test
    void testDetectsXss_ScriptTag() {
        assertTrue(InputValidator.detectsXss("<script>alert('xss')</script>"));
        assertTrue(InputValidator.detectsXss("client<script>malicious</script>"));
    }

    @Test
    void testDetectsXss_IframeTag() {
        assertTrue(InputValidator.detectsXss("<iframe src='evil.com'></iframe>"));
    }

    @Test
    void testDetectsXss_EventHandlers() {
        assertTrue(InputValidator.detectsXss("<img onerror='alert(1)'>"));
        assertTrue(InputValidator.detectsXss("<div onclick='malicious()'>"));
    }

    @Test
    void testDetectsXss_JavascriptProtocol() {
        assertTrue(InputValidator.detectsXss("javascript:alert(1)"));
    }

    @Test
    void testDetectsXss_NoPattern() {
        assertFalse(InputValidator.detectsXss("valid_client_id"));
    }

    // Path traversal detection tests
    @Test
    void testDetectsPathTraversal_DotDotSlash() {
        assertTrue(InputValidator.detectsPathTraversal("../../../etc/passwd"));
    }

    @Test
    void testDetectsPathTraversal_DotDotBackslash() {
        assertTrue(InputValidator.detectsPathTraversal("..\\..\\windows\\system32"));
    }

    @Test
    void testDetectsPathTraversal_UrlEncoded() {
        assertTrue(InputValidator.detectsPathTraversal("%2e%2e%2f"));
        assertTrue(InputValidator.detectsPathTraversal("%2e%2e\\"));
    }

    @Test
    void testDetectsPathTraversal_DoubleSlash() {
        assertTrue(InputValidator.detectsPathTraversal("..//file"));
    }

    @Test
    void testDetectsPathTraversal_NoPattern() {
        assertFalse(InputValidator.detectsPathTraversal("valid_client_id"));
    }

    // Integration tests combining validation checks
    @Test
    void testIsValid_SqlInjectionRejected() {
        assertFalse(InputValidator.isValid("client' OR '1'='1'"));
    }

    @Test
    void testIsValid_XssRejected() {
        assertFalse(InputValidator.isValid("<script>alert('xss')</script>"));
    }

    @Test
    void testIsValid_PathTraversalRejected() {
        assertFalse(InputValidator.isValid("../../../etc/passwd"));
    }

    @Test
    void testIsValid_MultipleLegitimateClients() {
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
