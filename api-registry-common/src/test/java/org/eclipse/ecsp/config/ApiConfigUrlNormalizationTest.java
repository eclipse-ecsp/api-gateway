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

package org.eclipse.ecsp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * Test class for ApiConfig URL normalization functionality.
 */
class ApiConfigUrlNormalizationTest {

    private ApiConfig apiConfig;

    private static final int TWO_CONSTANT = 2;
    private static final int THREE_CONSTANT = 3;

    @BeforeEach
    void setUp() {
        apiConfig = new ApiConfig();
        ReflectionTestUtils.setField(apiConfig, "pathsInclude", new String[]{"/*"});
        ReflectionTestUtils.setField(apiConfig, "pathsExclude", new String[]{});
        ReflectionTestUtils.setField(apiConfig, "applicationName", "test-service");
        ReflectionTestUtils.setField(apiConfig, "applicationVersion", "1.0.0");
    }

    @Test
    void testNormalizeServerUrl_WithDuplicateDomainPattern() {
        // Test the private method using reflection
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "api-gateway.example.comapi-gateway.example.com");
        
        assertEquals("https://api-gateway.example.com", result);
    }

    @Test
    void testNormalizeServerUrl_WithoutScheme() {
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "api-gateway.example.com");
        
        assertEquals("https://api-gateway.example.com", result);
    }

    @Test
    void testNormalizeServerUrl_WithHttpScheme() {
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "http://api-gateway.example.com");
        
        assertEquals("http://api-gateway.example.com", result);
    }

    @Test
    void testNormalizeServerUrl_WithHttpsScheme() {
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "https://api-gateway.example.com");
        
        assertEquals("https://api-gateway.example.com", result);
    }

    @Test
    void testNormalizeServerUrl_ProtocolRelative() {
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "//api-gateway.example.com");
        
        assertEquals("https://api-gateway.example.com", result);
    }

    @Test
    void testNormalizeServerUrl_EmptyString() {
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl", "");
        
        assertEquals("", result);
    }

    @Test
    void testNormalizeServerUrl_Null() {
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl", (String) null);
        
        assertNull(result);
    }

    @Test
    void testOpenApi_WithDuplicateDomainUrls() {
        // Set up server URLs with duplicate domain pattern
        ReflectionTestUtils.setField(apiConfig, "serverUrls", 
            "api-gateway.example.comapi-gateway.example.com,api-gateway.test.com");
        
        OpenAPI openApi = apiConfig.openApi();
        
        assertNotNull(openApi);
        List<Server> servers = openApi.getServers();
        assertNotNull(servers);
        assertEquals(TWO_CONSTANT, servers.size());
        
        // First server should be normalized (duplicate removed, https added)
        assertEquals("https://api-gateway.example.com", servers.get(0).getUrl());
        
        // Second server should have https added
        assertEquals("https://api-gateway.test.com", servers.get(1).getUrl());
    }

    @Test
    void testOpenApi_WithMixedUrlFormats() {
        // Set up server URLs with mixed formats
        ReflectionTestUtils.setField(apiConfig, "serverUrls", 
            "api-gateway.example.com,https://api-gateway.secure.com,//api-gateway.protocol.com");
        
        OpenAPI openApi = apiConfig.openApi();
        
        assertNotNull(openApi);
        List<Server> servers = openApi.getServers();
        assertNotNull(servers);
        assertEquals(THREE_CONSTANT, servers.size());
        
        // First server should have https added
        assertEquals("https://api-gateway.example.com", servers.get(0).getUrl());
        
        // Second server should remain unchanged
        assertEquals("https://api-gateway.secure.com", servers.get(1).getUrl());
        
        // Third server should have https added to protocol-relative URL
        assertEquals("https://api-gateway.protocol.com", servers.get(TWO_CONSTANT).getUrl());
    }

    @Test
    void testOpenApi_WithEmptyServerUrls() {
        // Set up empty server URLs
        ReflectionTestUtils.setField(apiConfig, "serverUrls", "");
        
        OpenAPI openApi = apiConfig.openApi();
        
        assertNotNull(openApi);
        List<Server> servers = openApi.getServers();
        assertNotNull(servers);
        assertEquals(1, servers.size());
        assertEquals("", servers.get(0).getUrl());
    }

    @Test
    void testOpenApi_WithWhitespaceUrls() {
        // Set up server URLs with whitespace
        ReflectionTestUtils.setField(
                apiConfig, "serverUrls", "  api-gateway.example.com  ,  https://api-gateway.secure.com  ");
        
        OpenAPI openApi = apiConfig.openApi();
        
        assertNotNull(openApi);
        List<Server> servers = openApi.getServers();
        assertNotNull(servers);
        assertEquals(TWO_CONSTANT, servers.size());
        
        // URLs should be trimmed and normalized
        assertEquals("https://api-gateway.example.com", servers.get(0).getUrl());
        assertEquals("https://api-gateway.secure.com", servers.get(1).getUrl());
    }

}