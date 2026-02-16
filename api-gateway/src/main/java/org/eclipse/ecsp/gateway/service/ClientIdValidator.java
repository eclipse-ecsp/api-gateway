package org.eclipse.ecsp.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Service for validating client IDs against security patterns.
 *
 * <p>
 * Prevents injection attacks by detecting malicious patterns in client IDs:
 * - SQL injection patterns
 * - XSS (Cross-Site Scripting) patterns
 * - Path traversal patterns
 *
 * @see <a href="https://owasp.org/www-community/attacks/">OWASP Attack Patterns</a>
 */
@Service
@Slf4j
public class ClientIdValidator {

    private static final int MIN_CLIENT_ID_LENGTH = 3;
    private static final int MAX_CLIENT_ID_LENGTH = 128;
    private static final int MAX_LOG_LENGTH = 50;

    // SQL Injection patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "('.*(--))"
            + "|(';)"
            + "|('.*(/\\*|\\*))"
            + "|(\\b(union|select|insert|update|delete|drop|create|alter|exec|execute|xp_|sp_)\\b)"
            + "|('\\s*(or|and)\\s+(true|'[^']*'\\s*=\\s*'[^']*'|\\d+\\s*=\\s*\\d+))"
            + "|(\\b(or|and)\\b\\s+(true|false|'[^']*'\\s*=\\s*'[^']*'|\\d+\\s*=\\s*\\d+))",
            Pattern.CASE_INSENSITIVE
    );

    // XSS patterns
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(<script[^>]*>.*</script>)"
            + "|(<iframe[^>]*>.*</iframe>)"
            + "|(<img[^>]*onerror[^>]*>)"
            + "|(javascript:)"
            + "|(<\\s*[^>]*\\s+on\\w+\\s*=)",
            Pattern.CASE_INSENSITIVE
    );

    // Path traversal patterns
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
            "(\\.\\./)"
            + "|(\\.\\.\\\\)"
            + "|(%2e%2e%2f)"
            + "|(%2e%2e\\\\)"
            + "|(\\.\\.//)"
            + "|(\\.\\.\\\\\\\\)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Validate client ID against all security patterns.
     *
     * @param clientId Client identifier to validate
     * @return true if valid (no malicious patterns detected), false otherwise
     */
    public boolean isValid(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            log.warn("Client ID is null or empty");
            return false;
        }

        // Length validation (3-128 characters per data model)
        if (clientId.length() < MIN_CLIENT_ID_LENGTH || clientId.length() > MAX_CLIENT_ID_LENGTH) {
            log.warn("Client ID length invalid: {} characters (expected {}-{})",
                    clientId.length(), MIN_CLIENT_ID_LENGTH, MAX_CLIENT_ID_LENGTH);
            return false;
        }

        // Security pattern validation - log as security events
        if (detectsSqlInjection(clientId)) {
            log.error("[SECURITY] SQL injection attack detected in client ID: {} - Request blocked",
                    sanitizeForLogging(clientId));
            return false;
        }

        if (detectsXss(clientId)) {
            log.error("[SECURITY] XSS attack detected in client ID: {} - Request blocked",
                    sanitizeForLogging(clientId));
            return false;
        }

        if (detectsPathTraversal(clientId)) {
            log.error("[SECURITY] Path traversal attack detected in client ID: {} - Request blocked",
                    sanitizeForLogging(clientId));
            return false;
        }

        return true;
    }

    /**
     * Detect SQL injection patterns in client ID.
     *
     * <p>
     * Checks for:
     * - SQL keywords (SELECT, UNION, INSERT, etc.)
     * - SQL comment markers (--, &#47;*, *&#47;)
     * - Boolean logic patterns (OR true, AND false)
     *
     * @param clientId Client identifier
     * @return true if SQL injection pattern detected
     */
    public boolean detectsSqlInjection(String clientId) {
        if (clientId == null) {
            return false;
        }
        return SQL_INJECTION_PATTERN.matcher(clientId).find();
    }

    /**
     * Detect XSS (Cross-Site Scripting) patterns in client ID.
     *
     * <p>
     * Checks for:
     * - Script tags
     * - iframe tags
     * - Event handlers (onclick, onerror, etc.)
     * - javascript: protocol
     *
     * @param clientId Client identifier
     * @return true if XSS pattern detected
     */
    public boolean detectsXss(String clientId) {
        if (clientId == null) {
            return false;
        }
        return XSS_PATTERN.matcher(clientId).find();
    }

    /**
     * Detect path traversal patterns in client ID.
     *
     * <p>
     * Checks for:
     * - Directory traversal sequences (../, ..\
     * - URL-encoded traversal (%2e%2e)
     * - Double-encoded variations
     *
     * @param clientId Client identifier
     * @return true if path traversal pattern detected
     */
    public boolean detectsPathTraversal(String clientId) {
        if (clientId == null) {
            return false;
        }
        return PATH_TRAVERSAL_PATTERN.matcher(clientId).find();
    }

    /**
     * Sanitize client ID for safe logging (prevent log injection).
     *
     * @param clientId Client identifier
     * @return Sanitized string safe for logging
     */
    private String sanitizeForLogging(String clientId) {
        if (clientId == null) {
            return "null";
        }
        // Truncate long IDs and remove newlines/tabs to prevent log injection
        String sanitized = clientId.replaceAll("[\\r\\n\\t]", " ");
        if (sanitized.length() > MAX_LOG_LENGTH) {
            return sanitized.substring(0, MAX_LOG_LENGTH) + "...";
        }
        return sanitized;
    }
}
