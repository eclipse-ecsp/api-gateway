package org.eclipse.ecsp.gateway.utils;

import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;

import java.util.regex.Pattern;

/**
 * Service for validating client IDs against security patterns.
 *
 * <p>Prevents injection attacks by detecting malicious patterns in client IDs:
 * - SQL injection patterns
 * - XSS (Cross-Site Scripting) patterns
 * - Path traversal patterns
 *
 * @see <a href="https://owasp.org/www-community/attacks/">OWASP Attack Patterns</a>
 */
public class InputValidator {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(InputValidator.class);
    private static final int MAX_LOG_LENGTH = 50;

    private InputValidator() {
        // Private constructor to prevent instantiation
    }

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
     * @param input Client identifier to validate
     * @return true if valid (no malicious patterns detected), false otherwise
     */
    public static boolean isValid(String input) {
        if (input == null || input.isBlank()) {
            LOGGER.warn("Input is null or empty");
            return false;
        }

        // Security pattern validation - log as security events
        if (detectsSqlInjection(input)) {
            LOGGER.error("[SECURITY] SQL injection attack detected in client ID: {} - Request blocked",
                    sanitizeForLogging(input));
            return false;
        }

        if (detectsXss(input)) {
            LOGGER.error("[SECURITY] XSS attack detected in client ID: {} - Request blocked",
                    sanitizeForLogging(input));
            return false;
        }

        if (detectsPathTraversal(input)) {
            LOGGER.error("[SECURITY] Path traversal attack detected in client ID: {} - Request blocked",
                    sanitizeForLogging(input));
            return false;
        }

        return true;
    }

    /**
     * Detect SQL injection patterns in client ID.
     *
     * <p>Checks for:
     * - SQL keywords (SELECT, UNION, INSERT, etc.)
     * - SQL comment markers (--, &#47;*, *&#47;)
     * - Boolean logic patterns (OR true, AND false)
     *
     * @param input Client identifier
     * @return true if SQL injection pattern detected
     */
    public static boolean detectsSqlInjection(String input) {
        if (input == null) {
            return false;
        }
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Detect XSS (Cross-Site Scripting) patterns in client ID.
     *
     * <p>Checks for:
     * - Script tags
     * - iframe tags
     * - Event handlers (onclick, onerror, etc.)
     * - javascript: protocol
     *
     * @param clientId Client identifier
     * @return true if XSS pattern detected
     */
    public static boolean detectsXss(String clientId) {
        if (clientId == null) {
            return false;
        }
        return XSS_PATTERN.matcher(clientId).find();
    }

    /**
     * Detect path traversal patterns in client ID.
     *
     * <p>Checks for:
     * - Directory traversal sequences (../, ..\
     * - URL-encoded traversal (%2e%2e)
     * - Double-encoded variations
     *
     * @param input string to check for path traversal patterns
     * @return true if path traversal pattern detected
     */
    public static boolean detectsPathTraversal(String input) {
        if (input == null) {
            return false;
        }
        return PATH_TRAVERSAL_PATTERN.matcher(input).find();
    }

    /**
     * Sanitize input string for safe logging (prevent log injection).
     *
     * @param input input string to sanitize
     * @return Sanitized string safe for logging
     */
    private static String sanitizeForLogging(String input) {
        if (input == null) {
            return "null";
        }
        // Truncate long IDs and remove newlines/tabs to prevent log injection
        String sanitized = input.replaceAll("[\\r\\n\\t]", " ");
        if (sanitized.length() > MAX_LOG_LENGTH) {
            return sanitized.substring(0, MAX_LOG_LENGTH) + "...";
        }
        return sanitized;
    }
}
