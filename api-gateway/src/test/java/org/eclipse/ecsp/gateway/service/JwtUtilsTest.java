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

package org.eclipse.ecsp.gateway.service;

import io.jsonwebtoken.Jwts;
import org.eclipse.ecsp.gateway.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for JwtUtils.
 */
class JwtUtilsTest {

    private static final String CLIENT_ID2 = "client_id";
    private static final String CLIENT_ID = "clientId";

    @Test
    void testExtractClientIdFirstClaimFound() {
        // Arrange
        String jwt = createUnsignedJwt(claims -> {
            claims.put(CLIENT_ID, "test_client_123");
            claims.put("azp", "backup_client");
        });

        // Act
        String clientId = JwtUtils.extractClientId(jwt, List.of(CLIENT_ID, "azp", CLIENT_ID2));

        // Assert
        assertEquals("test_client_123", clientId);
    }

    @Test
    void testExtractClientIdFallbackToSecondClaim() {
        // Arrange
        String jwt = createUnsignedJwt(claims -> 
            claims.put("azp", "azure_client")
        );

        // Act
        String clientId = JwtUtils.extractClientId(jwt, List.of(CLIENT_ID, "azp", CLIENT_ID2));

        // Assert
        assertEquals("azure_client", clientId);
    }

    @Test
    void testExtractClientIdCaseInsensitiveMatching() {
        // Arrange
        String jwt = createUnsignedJwt(claims -> 
            claims.put("ClientID", "case_insensitive_client")
        );

        // Act
        String clientId = JwtUtils.extractClientId(jwt, List.of(CLIENT_ID, "azp"));

        // Assert
        assertEquals("case_insensitive_client", clientId);
    }

    @Test
    void testExtractClientIdNoMatchingClaims() {
        // Arrange
        String jwt = createUnsignedJwt(claims -> {
            claims.put("sub", "user123");
            claims.put("iss", "https://issuer.com");
        });

        // Act
        String clientId = JwtUtils.extractClientId(jwt, List.of(CLIENT_ID, "azp", CLIENT_ID2));

        // Assert
        assertNull(clientId);
    }

    @Test
    void testExtractClientIdNullJwt() {
        // Act
        String clientId = JwtUtils.extractClientId(null, List.of(CLIENT_ID));

        // Assert
        assertNull(clientId);
    }

    @Test
    void testExtractClientIdEmptyJwt() {
        // Act
        String clientId = JwtUtils.extractClientId("", List.of(CLIENT_ID));

        // Assert
        assertNull(clientId);
    }

    @Test
    void testExtractClientIdEmptyClaimNames() {
        // Arrange
        String jwt = createUnsignedJwt(claims ->
            claims.put(CLIENT_ID, "test")
        );

        // Act
        String clientId = JwtUtils.extractClientId(jwt, List.of());

        // Assert
        assertNull(clientId);
    }

    @Test
    void testExtractClientIdTrimWhitespace() {
        // Arrange
        String jwt = createUnsignedJwt(claims -> 
            claims.put(CLIENT_ID, "  whitespace_client  ")
        );

        // Act
        String clientId = JwtUtils.extractClientId(jwt, List.of(CLIENT_ID));

        // Assert
        assertEquals("whitespace_client", clientId);
    }

    @Test
    void testExtractClientIdMultipleIdpFormats() {
        // Test Azure AD format (azp)
        String azureJwt = createUnsignedJwt(claims -> 
            claims.put("azp", "azure_client_id")
        );
        assertEquals("azure_client_id", JwtUtils.extractClientId(azureJwt, List.of(CLIENT_ID, "azp")));

        // Test OAuth2 standard (client_id)
        String oauth2Jwt = createUnsignedJwt(claims -> 
            claims.put(CLIENT_ID2, "oauth2_client")
        );
        assertEquals("oauth2_client", JwtUtils.extractClientId(oauth2Jwt, List.of(CLIENT_ID, CLIENT_ID2)));

        // Test alternative format (cid)
        String altJwt = createUnsignedJwt(claims -> 
            claims.put("cid", "alternative_client")
        );
        assertEquals("alternative_client", JwtUtils.extractClientId(altJwt, List.of(CLIENT_ID, "cid")));
    }

    @Test
    void testExtractClientIdFromArrayClaim() {
        // Arrange - some IdPs encode client_id as a single-element JSON array: ["test-portal"]
        String jwt = createUnsignedJwt(claims ->
            claims.put(CLIENT_ID, List.of("test-portal"))
        );

        // Act
        String clientId = JwtUtils.extractClientId(jwt, List.of(CLIENT_ID));

        // Assert - should return "test-portal", not "[test-portal]"
        assertEquals("test-portal", clientId);
    }

    /**
     * Helper method to create unsigned JWT for testing.
     */
    @SuppressWarnings("java:S5659")
    private String createUnsignedJwt(
            java.util.function.Consumer<java.util.Map<String, Object>> claimsBuilder) {
        // Build claims using mutable map
        java.util.Map<String, Object> claimsMap = new java.util.HashMap<>();
        
        // Apply the consumer to populate the map
        claimsBuilder.accept(claimsMap);
        
        // Build the JWT with the populated claims
        return Jwts.builder()
                .claims(claimsMap)
                .compact();
    }
}
