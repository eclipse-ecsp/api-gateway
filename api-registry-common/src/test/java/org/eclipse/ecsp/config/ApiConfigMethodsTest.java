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

package org.eclipse.ecsp.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for ApiConfig getter methods.
 * Uses ReflectionTestUtils as stubs to set up test data.
 */
class ApiConfigMethodsTest {

    private ApiConfig apiConfig;

    @BeforeEach
    void setUp() {
        apiConfig = new ApiConfig();
    }

    @Test
    void testPathsInclude() {
        // Given
        String[] expectedPaths = new String[]{"/*", "/api/**"};
        ReflectionTestUtils.setField(apiConfig, "pathsInclude", expectedPaths);

        // When
        String[] result = apiConfig.pathsInclude();

        // Then
        assertNotNull(result);
        assertArrayEquals(expectedPaths, result);
    }

    @Test
    void testPathsExclude() {
        // Given
        String[] expectedPaths = new String[]{"/admin/**"};
        ReflectionTestUtils.setField(apiConfig, "pathsExclude", expectedPaths);

        // When
        String[] result = apiConfig.pathsExclude();

        // Then
        assertNotNull(result);
        assertArrayEquals(expectedPaths, result);
    }

    @Test
    void testPathsExcludeEmptyArray() {
        // Given
        String[] expectedPaths = new String[]{};
        ReflectionTestUtils.setField(apiConfig, "pathsExclude", expectedPaths);

        // When
        String[] result = apiConfig.pathsExclude();

        // Then
        assertNotNull(result);
        assertArrayEquals(expectedPaths, result);
    }

    @Test
    void testApplicationName() {
        // Given
        String expectedName = "test-service";
        ReflectionTestUtils.setField(apiConfig, "applicationName", expectedName);

        // When
        String result = apiConfig.applicationName();

        // Then
        assertNotNull(result);
        assertEquals(expectedName, result);
    }

    @Test
    void testApplicationVersion() {
        // Given
        String expectedVersion = "1.0.0";
        ReflectionTestUtils.setField(apiConfig, "applicationVersion", expectedVersion);

        // When
        String result = apiConfig.applicationVersion();

        // Then
        assertNotNull(result);
        assertEquals(expectedVersion, result);
    }

    @Test
    void testApplicationNameWithSpecialCharacters() {
        // Given
        String expectedName = "test-service-v2.1";
        ReflectionTestUtils.setField(apiConfig, "applicationName", expectedName);

        // When
        String result = apiConfig.applicationName();

        // Then
        assertEquals(expectedName, result);
    }

    @Test
    void testApplicationVersionWithSnapshot() {
        // Given
        String expectedVersion = "1.0.0-SNAPSHOT";
        ReflectionTestUtils.setField(apiConfig, "applicationVersion", expectedVersion);

        // When
        String result = apiConfig.applicationVersion();

        // Then
        assertEquals(expectedVersion, result);
    }
}

