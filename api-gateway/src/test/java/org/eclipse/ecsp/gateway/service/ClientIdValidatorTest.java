package org.eclipse.ecsp.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClientIdValidator.
 * Tests security pattern detection for SQL injection, XSS, and path traversal.
 */
class ClientIdValidatorTest {

    private ClientIdValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ClientIdValidator();
    }

    // Valid client ID tests
    @Test
    void testIsValid_StandardClientId() {
        assertTrue(validator.isValid("test_client_123"));
    }

    @Test
    void testIsValid_WithDots() {
        assertTrue(validator.isValid("client.app.service"));
    }

    @Test
    void testIsValid_WithHyphens() {
        assertTrue(validator.isValid("mobile-app-client"));
    }

    @Test
    void testIsValid_WithUnderscores() {
        assertTrue(validator.isValid("automation_qa_client"));
    }

    @Test
    void testIsValid_Alphanumeric() {
        assertTrue(validator.isValid("Client123ABC"));
    }

    // Invalid - Null/Empty tests
    @Test
    void testIsValid_Null() {
        assertFalse(validator.isValid(null));
    }

    @Test
    void testIsValid_Empty() {
        assertFalse(validator.isValid(""));
    }

    @Test
    void testIsValid_Blank() {
        assertFalse(validator.isValid("   "));
    }

    // Invalid - Length tests
    @Test
    void testIsValid_TooShort() {
        assertFalse(validator.isValid("ab")); // 2 characters
    }

    @Test
    void testIsValid_MinimumLength() {
        assertTrue(validator.isValid("abc")); // 3 characters (minimum)
    }

    @Test
    void testIsValid_MaximumLength() {
        String maxLength = "a".repeat(128);
        assertTrue(validator.isValid(maxLength));
    }

    @Test
    void testIsValid_TooLong() {
        String tooLong = "a".repeat(129);
        assertFalse(validator.isValid(tooLong));
    }

    // SQL Injection detection tests
    @Test
    void testDetectsSqlInjection_SelectStatement() {
        assertTrue(validator.detectsSqlInjection("client' OR '1'='1'"));
        assertTrue(validator.detectsSqlInjection("test'; SELECT * FROM users--"));
    }

    @Test
    void testDetectsSqlInjection_UnionAttack() {
        assertTrue(validator.detectsSqlInjection("client' UNION SELECT password FROM users--"));
    }

    @Test
    void testDetectsSqlInjection_CommentMarkers() {
        assertTrue(validator.detectsSqlInjection("client'--"));
        assertTrue(validator.detectsSqlInjection("client'/*"));
    }

    @Test
    void testDetectsSqlInjection_BooleanLogic() {
        assertTrue(validator.detectsSqlInjection("' OR true--"));
        assertTrue(validator.detectsSqlInjection("' AND false--"));
    }

    @Test
    void testDetectsSqlInjection_InsertStatement() {
        assertTrue(validator.detectsSqlInjection("'; INSERT INTO clients VALUES('hacker')--"));
    }

    @Test
    void testDetectsSqlInjection_DropStatement() {
        assertTrue(validator.detectsSqlInjection("'; DROP TABLE clients--"));
    }

    @Test
    void testDetectsSqlInjection_NoPattern() {
        assertFalse(validator.detectsSqlInjection("valid_client_id"));
    }

    // XSS detection tests
    @Test
    void testDetectsXss_ScriptTag() {
        assertTrue(validator.detectsXss("<script>alert('xss')</script>"));
        assertTrue(validator.detectsXss("client<script>malicious</script>"));
    }

    @Test
    void testDetectsXss_IframeTag() {
        assertTrue(validator.detectsXss("<iframe src='evil.com'></iframe>"));
    }

    @Test
    void testDetectsXss_EventHandlers() {
        assertTrue(validator.detectsXss("<img onerror='alert(1)'>"));
        assertTrue(validator.detectsXss("<div onclick='malicious()'>"));
    }

    @Test
    void testDetectsXss_JavascriptProtocol() {
        assertTrue(validator.detectsXss("javascript:alert(1)"));
    }

    @Test
    void testDetectsXss_NoPattern() {
        assertFalse(validator.detectsXss("valid_client_id"));
    }

    // Path traversal detection tests
    @Test
    void testDetectsPathTraversal_DotDotSlash() {
        assertTrue(validator.detectsPathTraversal("../../../etc/passwd"));
    }

    @Test
    void testDetectsPathTraversal_DotDotBackslash() {
        assertTrue(validator.detectsPathTraversal("..\\..\\windows\\system32"));
    }

    @Test
    void testDetectsPathTraversal_UrlEncoded() {
        assertTrue(validator.detectsPathTraversal("%2e%2e%2f"));
        assertTrue(validator.detectsPathTraversal("%2e%2e\\"));
    }

    @Test
    void testDetectsPathTraversal_DoubleSlash() {
        assertTrue(validator.detectsPathTraversal("..//file"));
    }

    @Test
    void testDetectsPathTraversal_NoPattern() {
        assertFalse(validator.detectsPathTraversal("valid_client_id"));
    }

    // Integration tests combining validation checks
    @Test
    void testIsValid_SqlInjectionRejected() {
        assertFalse(validator.isValid("client' OR '1'='1'"));
    }

    @Test
    void testIsValid_XssRejected() {
        assertFalse(validator.isValid("<script>alert('xss')</script>"));
    }

    @Test
    void testIsValid_PathTraversalRejected() {
        assertFalse(validator.isValid("../../../etc/passwd"));
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
            assertTrue(validator.isValid(clientId), "Should be valid: " + clientId);
        }
    }
}
