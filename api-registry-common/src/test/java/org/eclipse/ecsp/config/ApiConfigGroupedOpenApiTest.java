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

import org.eclipse.ecsp.customizers.CustomGatewayFilterCustomizer;
import org.eclipse.ecsp.security.CachingTagger;
import org.eclipse.ecsp.security.ScopeTagger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for ApiConfig groupedOpenApi method using mocks (test doubles).
 * This demonstrates the use of Mockito mocks for dependency injection testing.
 */
@ExtendWith(MockitoExtension.class)
class ApiConfigGroupedOpenApiTest {

    @Mock
    private ScopeTagger scopeTagger;

    @Mock
    private CachingTagger cachingTagger;

    @Mock
    private CustomGatewayFilterCustomizer customGatewayFilterCustomizer;

    @InjectMocks
    private ApiConfig apiConfig;

    private static final String TEST_APP_NAME = "test-service";
    private static final String[] TEST_PATHS_INCLUDE = new String[]{"/*", "/api/**"};
    private static final String[] TEST_PATHS_EXCLUDE = new String[]{"/admin/**"};

    @BeforeEach
    void setUp() {
        // Set up test data using ReflectionTestUtils (stub technique)
        ReflectionTestUtils.setField(apiConfig, "pathsInclude", TEST_PATHS_INCLUDE);
        ReflectionTestUtils.setField(apiConfig, "pathsExclude", TEST_PATHS_EXCLUDE);
        ReflectionTestUtils.setField(apiConfig, "applicationName", TEST_APP_NAME);
        ReflectionTestUtils.setField(apiConfig, "applicationVersion", "1.0.0");
    }

    @Test
    void testGroupedOpenApi_WithValidDependencies() {
        // When
        GroupedOpenApi result = apiConfig.groupedOpenApi(scopeTagger, cachingTagger, customGatewayFilterCustomizer);

        // Then
        assertNotNull(result);
        assertEquals(TEST_APP_NAME, result.getGroup());
        assertNotNull(result.getPathsToMatch());
        assertNotNull(result.getPathsToExclude());
    }

    @Test
    void testGroupedOpenApi_WithEmptyPathsInclude() {
        // Given
        ReflectionTestUtils.setField(apiConfig, "pathsInclude", new String[]{});

        // When
        GroupedOpenApi result = apiConfig.groupedOpenApi(scopeTagger, cachingTagger, customGatewayFilterCustomizer);

        // Then
        assertNotNull(result);
        assertEquals(TEST_APP_NAME, result.getGroup());
    }

    @Test
    void testGroupedOpenApi_WithEmptyPathsExclude() {
        // Given
        ReflectionTestUtils.setField(apiConfig, "pathsExclude", new String[]{});

        // When
        GroupedOpenApi result = apiConfig.groupedOpenApi(scopeTagger, cachingTagger, customGatewayFilterCustomizer);

        // Then
        assertNotNull(result);
        assertEquals(TEST_APP_NAME, result.getGroup());
    }

    @Test
    void testGroupedOpenApi_CreatesGroupedOpenApiWithCustomizers() {
        // When
        GroupedOpenApi result = apiConfig.groupedOpenApi(scopeTagger, cachingTagger, customGatewayFilterCustomizer);

        // Then
        assertNotNull(result);
        assertEquals(TEST_APP_NAME, result.getGroup());
        // Note: Customizers are added via builder pattern, but GroupedOpenApi doesn't expose
        // a way to verify they were added. The fact that build() succeeds confirms they are valid.
    }

    @Test
    void testGroupedOpenApi_WithDifferentApplicationName() {
        // Given
        String customAppName = "custom-service";
        ReflectionTestUtils.setField(apiConfig, "applicationName", customAppName);

        // When
        GroupedOpenApi result = apiConfig.groupedOpenApi(scopeTagger, cachingTagger, customGatewayFilterCustomizer);

        // Then
        assertNotNull(result);
        assertEquals(customAppName, result.getGroup());
    }

    @Test
    void testGroupedOpenApi_WithMultiplePaths() {
        // Given
        String[] multiplePaths = new String[]{"/api/v1/**", "/api/v2/**", "/public/**"};
        ReflectionTestUtils.setField(apiConfig, "pathsInclude", multiplePaths);

        // When
        GroupedOpenApi result = apiConfig.groupedOpenApi(scopeTagger, cachingTagger, customGatewayFilterCustomizer);

        // Then
        assertNotNull(result);
        assertNotNull(result.getPathsToMatch());
    }
}

