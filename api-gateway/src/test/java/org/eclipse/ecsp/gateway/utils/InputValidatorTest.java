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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test purpose    - Verify InputValidator security pattern detection.
 * Test data       - Various malicious and valid input strings.
 * Test expected   - Correct detection of SQL injection, XSS, and path traversal.
 * Test type       - Positive and Negative.
 */
class InputValidatorTest {

    /**
     * Test purpose          - Verify isValid with clean input.
     * Test data             - Valid alphanumeric client ID.
     * Test expected result  - Returns true.
     * Test type             - Positive.
     */
    @Test
    void isValidCleanInputReturnsTrue() {
        // GIVEN: Valid client ID
        String validClientId = "client123ABC";

        // WHEN: Validation is performed
        boolean result = InputValidator.isValid(validClientId);

        // THEN: Should be valid
        assertTrue(result);
    }

    /**
     * Test purpose          - Verify isValid rejects null input.
     * Test data             - Null value.
     * Test expected result  - Returns false.
     * Test type             - Negative.
     */
    @Test
    void isValidNullInputReturnsFalse() {
        // GIVEN: Null input
        String nullInput = null;

        // WHEN: Validation is performed
        boolean result = InputValidator.isValid(nullInput);

        // THEN: Should be invalid
        assertFalse(result);
    }

    /**
     * Test purpose          - Verify isValid rejects empty string.
     * Test data             - Empty string.
     * Test expected result  - Returns false.
     * Test type             - Negative.
     */
    @Test
    void isValidEmptyStringReturnsFalse() {
        // GIVEN: Empty string
        String emptyInput = "";

        // WHEN: Validation is performed
        boolean result = InputValidator.isValid(emptyInput);

        // THEN: Should be invalid
        assertFalse(result);
    }

    /**
     * Test purpose          - Verify isValid rejects blank string.
     * Test data             - Whitespace only string.
     * Test expected result  - Returns false.
     * Test type             - Negative.
     */
    @Test
    void isValidBlankStringReturnsFalse() {
        // GIVEN: Whitespace only
        String blankInput = "   ";

        // WHEN: Validation is performed
        boolean result = InputValidator.isValid(blankInput);

        // THEN: Should be invalid
        assertFalse(result);
    }

    /**
     * Test purpose          - Verify SQL injection detection with UNION SELECT.
     * Test data             - SQL UNION attack pattern.
     * Test expected result  - Returns false (attack detected).
     * Test type             - Negative.
     */
    @Test
    void isValidSqlInjectionUnionSelectReturnsFalse() {
        // GIVEN: SQL injection with UNION SELECT
        String sqlInjection = "' UNION SELECT * FROM users--";

        // WHEN: Validation is performed
        boolean result = InputValidator.isValid(sqlInjection);

        // THEN: Should detect attack
        assertFalse(result);
    }

    /**
     * Test purpose          - Verify SQL injection detection with OR condition.
     * Test data             - SQL OR true pattern.
     * Test expected result  - Returns false (attack detected).
     * Test type             - Negative.
     */
    @Test
    void isValidSqlInjectionOrTrueReturnsFalse() {
        // GIVEN: SQL injection with OR condition
        String sqlInjection = "' or 1=1--";

        // WHEN: Validation is performed
        boolean result = InputValidator.isValid(sqlInjection);

        // THEN: Should detect attack
        assertFalse(result);
    }

    /**
     * Test purpose          - Verify SQL injection detection with comment.
     * Test data             - SQL comment pattern.
     * Test expected result  - Returns false (attack detected).
     * Test type             - Negative.
     */
    @Test
    void isValidSqlInjectionWithCommentReturnsFalse() {
        // GIVEN: SQL injection with comment
        String sqlInjection = "admin'--";

        // WHEN: Validation is performed
        boolean result = InputValidator.isValid(sqlInjection);

        // THEN: Should detect attack
        assertFalse(result);
    }

    /**
     * Test purpose          - Verify XSS detection with script tag.
     * Test data             - XSS with script tag.
     * Test expected result  - Returns false (attack detected).
     * Test type             - Negative.
     */
    @Test
    void isValidXssWithScriptTagReturnsFalse() {
        // GIVEN: XSS attack with script tag
        String xssAttack = "<script>alert('XSS')</script>";

        // WHEN: Validation is performed
        boolean result = InputValidator.isValid(xssAttack);

        // THEN: Should detect attack
        assertFalse(result);
    }

    /**
     * Test purpose          - Verify XSS detection with event handler.
     * Test data             - XSS with onerror event.
     * Test expected result  - Returns false (attack detected).
     * Test type             - Negative.
     */
    @Test
    void isValidXssWithOnerrorReturnsFalse() {
        // GIVEN: XSS attack with onerror
        String xssAttack = "<img src=x onerror=alert('XSS')>";

        // WHEN: Validation is performed
        boolean result = InputValidator.isValid(xssAttack);

        // THEN: Should detect attack
        assertFalse(result);
    }

    /**
     * Test purpose          - Verify XSS detection with javascript protocol.
     * Test data             - XSS with javascript: protocol.
     * Test expected result  - Returns false (attack detected).
     * Test type             - Negative.
     */
    @Test
    void isValidXssWithJavascriptProtocolReturnsFalse() {
        // GIVEN: XSS attack with javascript protocol
        String xssAttack = "javascript:alert('XSS')";

        // WHEN: Validation is performed
        boolean result = InputValidator.isValid(xssAttack);

        // THEN: Should detect attack
        assertFalse(result);
    }

    /**
     * Test purpose          - Verify path traversal detection.
     * Test data             - Path traversal with ../ .
     * Test expected result  - Returns false (attack detected).
     * Test type             - Negative.
     */
    @Test
    void isValidPathTraversalDotDotSlashReturnsFalse() {
        // GIVEN: Path traversal attack
        String pathTraversal = "../../etc/passwd";

        // WHEN: Validation is performed
        boolean result = InputValidator.isValid(pathTraversal);

        // THEN: Should detect attack
        assertFalse(result);
    }

    /**
     * Test purpose          - Verify path traversal detection with backslash.
     * Test data             - Path traversal with backslash.
     * Test expected result  - Returns false (attack detected).
     * Test type             - Negative.
     */
    @Test
    void isValidPathTraversalBackslashReturnsFalse() {
        // GIVEN: Path traversal with backslash
        String pathTraversal = "..\\..\\windows\\system32";

        // WHEN: Validation is performed
        boolean result = InputValidator.isValid(pathTraversal);

        // THEN: Should detect attack
        assertFalse(result);
    }

    /**
     * Test purpose          - Verify detectsSqlInjection method directly.
     * Test data             - SQL keyword INSERT.
     * Test expected result  - Returns true.
     * Test type             - Positive.
     */
    @Test
    void detectsSqlInjectionInsertKeywordReturnsTrue() {
        // GIVEN: Input with SQL INSERT keyword
        String input = "INSERT INTO users";

        // WHEN: Detection is performed
        boolean result = InputValidator.detectsSqlInjection(input);

        // THEN: Should detect SQL injection
        assertTrue(result);
    }

    /**
     * Test purpose          - Verify detectsSqlInjection with clean input.
     * Test data             - Clean string.
     * Test expected result  - Returns false.
     * Test type             - Negative.
     */
    @Test
    void detectsSqlInjectionCleanInputReturnsFalse() {
        // GIVEN: Clean input
        String input = "normalClientId123";

        // WHEN: Detection is performed
        boolean result = InputValidator.detectsSqlInjection(input);

        // THEN: Should not detect SQL injection
        assertFalse(result);
    }

    /**
     * Test purpose          - Verify detectsXss method directly.
     * Test data             - iframe tag.
     * Test expected result  - Returns true.
     * Test type             - Positive.
     */
    @Test
    void detectsXssIframeTagReturnsTrue() {
        // GIVEN: Input with iframe tag
        String input = "<iframe src='evil.com'></iframe>";

        // WHEN: Detection is performed
        boolean result = InputValidator.detectsXss(input);

        // THEN: Should detect XSS
        assertTrue(result);
    }

    /**
     * Test purpose          - Verify detectsPathTraversal method directly.
     * Test data             - URL-encoded traversal.
     * Test expected result  - Returns true.
     * Test type             - Positive.
     */
    @Test
    void detectsPathTraversalUrlEncodedTraversalReturnsTrue() {
        // GIVEN: URL-encoded path traversal
        String input = "%2e%2e%2f%2e%2e%2f";

        // WHEN: Detection is performed
        boolean result = InputValidator.detectsPathTraversal(input);

        // THEN: Should detect path traversal
        assertTrue(result);
    }

    /**
     * Test purpose          - Verify detection methods handle null input.
     * Test data             - Null value.
     * Test expected result  - Returns false (no detection on null).
     * Test type             - Negative.
     */
    @Test
    void detectionMethodsNullInputReturnsFalse() {
        // GIVEN: Null input

        // WHEN: Detection methods are called
        boolean sqlResult = InputValidator.detectsSqlInjection(null);
        boolean xssResult = InputValidator.detectsXss(null);
        boolean pathResult = InputValidator.detectsPathTraversal(null);

        // THEN: Should not detect attacks on null
        assertFalse(sqlResult);
        assertFalse(xssResult);
        assertFalse(pathResult);
    }

    /**
     * Test purpose          - Verify isValid accepts valid special characters.
     * Test data             - Client ID with hyphens and underscores.
     * Test expected result  - Returns true.
     * Test type             - Positive.
     */
    @Test
    void isValidValidSpecialCharactersReturnsTrue() {
        // GIVEN: Valid client ID with allowed special chars
        String validInput = "client-id_123";

        // WHEN: Validation is performed
        boolean result = InputValidator.isValid(validInput);

        // THEN: Should be valid
        assertTrue(result);
    }
}
