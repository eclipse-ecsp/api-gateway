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
    void testNormalizeServerUrlWithDuplicateDomainPattern() {
        // Test URL that already has duplication in configuration
        // Note: This should not happen in real configuration, but if it does,
        // we just add the scheme - the duplication will remain
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "api-gateway.example.comapi-gateway.example.com");
        
        // With scheme added, OpenAPI Explorer will treat it as absolute URL
        // and won't concatenate it with current page URL
        assertEquals("https://api-gateway.example.comapi-gateway.example.com", result);
    }

    @Test
    void testNormalizeServerUrlWithoutScheme() {
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "api-gateway.example.com");
        
        assertEquals("https://api-gateway.example.com", result);
    }

    @Test
    void testNormalizeServerUrlWithHttpScheme() {
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "http://api-gateway.example.com");
        
        assertEquals("http://api-gateway.example.com", result);
    }

    @Test
    void testNormalizeServerUrlWithHttpsScheme() {
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "https://api-gateway.example.com");
        
        assertEquals("https://api-gateway.example.com", result);
    }

    @Test
    void testNormalizeServerUrlProtocolRelative() {
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "//api-gateway.example.com");
        
        assertEquals("https://api-gateway.example.com", result);
    }

    @Test
    void testNormalizeServerUrlEmptyString() {
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl", "");
        
        assertEquals("", result);
    }

    @Test
    void testNormalizeServerUrlNull() {
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl", (String) null);
        
        assertNull(result);
    }

    @Test
    void testOpenApiWithDuplicateDomainUrls() {
        // Set up server URLs with duplicate domain pattern
        // Note: This should not happen in real configuration, but if it does,
        // we just add the scheme - the duplication will remain
        ReflectionTestUtils.setField(apiConfig, "serverUrls", 
            "api-gateway.example.comapi-gateway.example.com,api-gateway.test.com");
        
        OpenAPI openApi = apiConfig.openApi();
        
        assertNotNull(openApi);
        List<Server> servers = openApi.getServers();
        assertNotNull(servers);
        assertEquals(TWO_CONSTANT, servers.size());
        
        // First server: scheme added, but duplication remains (this is a config issue)
        assertEquals("https://api-gateway.example.comapi-gateway.example.com", servers.get(0).getUrl());
        
        // Second server should have https added
        assertEquals("https://api-gateway.test.com", servers.get(1).getUrl());
    }

    @Test
    void testOpenApiWithMixedUrlFormats() {
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
    void testOpenApiWithEmptyServerUrls() {
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
    void testOpenApiWithWhitespaceUrls() {
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

    @Test
    void testNormalizeServerUrlWithDifferentDnsFormat() {
        // Test URL with different DNS format (api.gateway instead of api-gateway)
        // This simulates the scenario where DNS is configured as api.gateway.example.com
        // and gets concatenated with api-gateway.example.com from OpenAPI spec
        // Note: If URL already has scheme and duplication, we don't fix duplication
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "https://api.gateway.example.comapi-gateway.example.com");
        
        // URL already has scheme, so it's returned as-is
        assertEquals("https://api.gateway.example.comapi-gateway.example.com", result);
    }

    @Test
    void testNormalizeServerUrlWithConcatenatedDomainsNoScheme() {
        // Test URL without scheme that has concatenated domains with different formats
        // Note: This should not happen in real configuration, but if it does,
        // we just add the scheme - the duplication will remain
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "api.gateway.example.comapi-gateway.example.com");
        
        // Scheme is added, but duplication remains
        assertEquals("https://api.gateway.example.comapi-gateway.example.com", result);
    }

    @Test
    void testNormalizeServerUrlWithHttpSchemeAndConcatenatedDomains() {
        // Test URL with http scheme that has concatenated domains
        // Note: If URL already has scheme and duplication, we don't fix duplication
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "http://api.gateway.example.comapi-gateway.example.com");
        
        // URL already has scheme, so it's returned as-is
        assertEquals("http://api.gateway.example.comapi-gateway.example.com", result);
    }

    @Test
    void testOpenApiWithDifferentDnsFormatConcatenation() {
        // Test the scenario from the PR review: different DNS format
        // Note: If URL already has scheme and duplication, we don't fix duplication
        ReflectionTestUtils.setField(apiConfig, "serverUrls", 
            "https://api.gateway.example.comapi-gateway.example.com");
        
        OpenAPI openApi = apiConfig.openApi();
        
        assertNotNull(openApi);
        List<Server> servers = openApi.getServers();
        assertNotNull(servers);
        assertEquals(1, servers.size());
        
        // URL already has scheme, so it's returned as-is (duplication remains)
        assertEquals("https://api.gateway.example.comapi-gateway.example.com", servers.get(0).getUrl());
    }

    @Test
    void testOpenApiWithMixedConcatenationScenarios() {
        // Test multiple concatenation scenarios
        // Note: These should not happen in real configuration, but if they do,
        // we just add the scheme where needed - the duplication will remain
        ReflectionTestUtils.setField(apiConfig, "serverUrls", 
            "api.gateway.example.comapi-gateway.example.com,https://gateway.test.comanother.domain.com,normal-domain.com");
        
        OpenAPI openApi = apiConfig.openApi();
        
        assertNotNull(openApi);
        List<Server> servers = openApi.getServers();
        assertNotNull(servers);
        assertEquals(THREE_CONSTANT, servers.size());
        
        // First server: concatenated domains without scheme, scheme added but duplication remains
        assertEquals("https://api.gateway.example.comapi-gateway.example.com", servers.get(0).getUrl());
        
        // Second server: concatenated domains with scheme, returned as-is
        assertEquals("https://gateway.test.comanother.domain.com", servers.get(1).getUrl());
        
        // Third server: normal domain, should add https
        final int thirdServerIndex = 2;
        assertEquals("https://normal-domain.com", servers.get(thirdServerIndex).getUrl());
    }

    @Test
    void testNormalizeServerUrlWithCustomDnsName() {
        // Test URL with custom DNS name (not api-gateway.)
        // This demonstrates that the solution works for any DNS name
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "my-custom-gateway.corp.net");
        
        assertEquals("https://my-custom-gateway.corp.net", result);
    }

    @Test
    void testNormalizeServerUrlWithApiGatewayDnsName() {
        // Test URL with api.gateway format (dots instead of dashes)
        // This demonstrates that the solution works for api.gateway format
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "api.gateway.example.com");
        
        assertEquals("https://api.gateway.example.com", result);
    }

    @Test
    void testOpenApiWithCustomDnsNames() {
        // Test the scenario from PR review: custom DNS names without api-gateway.
        // This demonstrates that the solution works universally
        ReflectionTestUtils.setField(apiConfig, "serverUrls", 
            "my-custom-gateway.corp.net,api.gateway.example.com,gateway.company.com");
        
        OpenAPI openApi = apiConfig.openApi();
        
        assertNotNull(openApi);
        List<Server> servers = openApi.getServers();
        assertNotNull(servers);
        assertEquals(THREE_CONSTANT, servers.size());
        
        // All servers should have https scheme added
        assertEquals("https://my-custom-gateway.corp.net", servers.get(0).getUrl());
        assertEquals("https://api.gateway.example.com", servers.get(1).getUrl());
        final int thirdServerIndex = 2;
        assertEquals("https://gateway.company.com", servers.get(thirdServerIndex).getUrl());
    }

    @Test
    void testNormalizeServerUrlWithIpAddress() {
        // Test URL with IP address instead of domain name
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "192.168.1.100");
        
        assertEquals("https://192.168.1.100", result);
    }

    @Test
    void testNormalizeServerUrlWithPortNumber() {
        // Test URL with port number
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "gateway.example.com:8080");
        
        assertEquals("https://gateway.example.com:8080", result);
    }

    @Test
    void testNormalizeServerUrlWithPath() {
        // Test URL with path
        String result = ReflectionTestUtils.invokeMethod(apiConfig, "normalizeServerUrl",
            "gateway.example.com/api/v1");
        
        assertEquals("https://gateway.example.com/api/v1", result);
    }

}