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

package org.eclipse.ecsp.registry.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for ErrorResponseDto.
 *
 * <p>Tests basic DTO functionality following Engineering Fundamentals Guidelines.
 */
class ErrorResponseDtoTest {

    /**
     * Test purpose          - Verify default constructor creates instance.
     * Test data             - None.
     * Test expected result  - Instance created with null error field.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void constructorDefaultConstructorCreateInstanceWithNullError() {
        // GIVEN: No input

        // WHEN: Create instance using default constructor
        ErrorResponseDto dto = new ErrorResponseDto();

        // THEN: Instance should be created with null error
        assertNotNull(dto);
        assertNull(dto.getError());
    }

    /**
     * Test purpose          - Verify setError and getError work correctly.
     * Test data             - Error message string.
     * Test expected result  - Error message is set and retrieved correctly.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void setErrorValidErrorMessageSetAndGetCorrectly() {
        // GIVEN: DTO instance and error message
        ErrorResponseDto dto = new ErrorResponseDto();
        String errorMessage = "Test error message";

        // WHEN: Set error message
        dto.setError(errorMessage);

        // THEN: Error message should be retrieved correctly
        assertEquals(errorMessage, dto.getError());
    }

    /**
     * Test purpose          - Verify setError with null value.
     * Test data             - null error message.
     * Test expected result  - Error field set to null.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void setErrorNullValueSetToNull() {
        // GIVEN: DTO instance with initial error
        ErrorResponseDto dto = new ErrorResponseDto();
        dto.setError("Initial error");

        // WHEN: Set error to null
        dto.setError(null);

        // THEN: Error should be null
        assertNull(dto.getError());
    }

    /**
     * Test purpose          - Verify setError with empty string.
     * Test data             - Empty string.
     * Test expected result  - Error field set to empty string.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void setErrorEmptyStringSetToEmptyString() {
        // GIVEN: DTO instance
        ErrorResponseDto dto = new ErrorResponseDto();

        // WHEN: Set error to empty string
        dto.setError("");

        // THEN: Error should be empty string
        assertEquals("", dto.getError());
    }

    /**
     * Test purpose          - Verify setError with long error message.
     * Test data             - Long error message.
     * Test expected result  - Full message stored and retrieved.
     * Test type             - Positive.
     *
     * @throws Exception if test fails
     **/
    @Test
    void setErrorLongErrorMessageStoredAndRetrievedCorrectly() {
        // GIVEN: DTO instance and long error message
        ErrorResponseDto dto = new ErrorResponseDto();
        String longError = "This is a very long error message that contains multiple details about what went wrong "
                + "in the system and should still be stored and retrieved correctly without any truncation or data loss.";

        // WHEN: Set long error message
        dto.setError(longError);

        // THEN: Full message should be retrieved
        assertEquals(longError, dto.getError());
    }
}
