package org.eclipse.ecsp.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtClaimExtractor.
 */
class JwtClaimExtractorTest {

    private JwtClaimExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new JwtClaimExtractor();
    }

    @Test
    void testExtractClientId_FirstClaimFound() {
        // Arrange
        String jwt = createUnsignedJwt(Claims.class, claims -> {
            claims.put("clientId", "test_client_123");
            claims.put("azp", "backup_client");
        });

        // Act
        String clientId = extractor.extractClientId(jwt, List.of("clientId", "azp", "client_id"));

        // Assert
        assertEquals("test_client_123", clientId);
    }

    @Test
    void testExtractClientId_FallbackToSecondClaim() {
        // Arrange
        String jwt = createUnsignedJwt(Claims.class, claims -> {
            claims.put("azp", "azure_client");
        });

        // Act
        String clientId = extractor.extractClientId(jwt, List.of("clientId", "azp", "client_id"));

        // Assert
        assertEquals("azure_client", clientId);
    }

    @Test
    void testExtractClientId_CaseInsensitiveMatching() {
        // Arrange
        String jwt = createUnsignedJwt(Claims.class, claims -> {
            claims.put("ClientID", "case_insensitive_client");
        });

        // Act
        String clientId = extractor.extractClientId(jwt, List.of("clientId", "azp"));

        // Assert
        assertEquals("case_insensitive_client", clientId);
    }

    @Test
    void testExtractClientId_NoMatchingClaims() {
        // Arrange
        String jwt = createUnsignedJwt(Claims.class, claims -> {
            claims.put("sub", "user123");
            claims.put("iss", "https://issuer.com");
        });

        // Act
        String clientId = extractor.extractClientId(jwt, List.of("clientId", "azp", "client_id"));

        // Assert
        assertNull(clientId);
    }

    @Test
    void testExtractClientId_NullJwt() {
        // Act
        String clientId = extractor.extractClientId(null, List.of("clientId"));

        // Assert
        assertNull(clientId);
    }

    @Test
    void testExtractClientId_EmptyJwt() {
        // Act
        String clientId = extractor.extractClientId("", List.of("clientId"));

        // Assert
        assertNull(clientId);
    }

    @Test
    void testExtractClientId_EmptyClaimNames() {
        // Arrange
        String jwt = createUnsignedJwt(Claims.class, claims -> {
            claims.put("clientId", "test");
        });

        // Act
        String clientId = extractor.extractClientId(jwt, List.of());

        // Assert
        assertNull(clientId);
    }

    @Test
    void testExtractClientId_TrimWhitespace() {
        // Arrange
        String jwt = createUnsignedJwt(Claims.class, claims -> {
            claims.put("clientId", "  whitespace_client  ");
        });

        // Act
        String clientId = extractor.extractClientId(jwt, List.of("clientId"));

        // Assert
        assertEquals("whitespace_client", clientId);
    }

    @Test
    void testExtractClientId_MultipleIdpFormats() {
        // Test Azure AD format (azp)
        String azureJwt = createUnsignedJwt(Claims.class, claims -> {
            claims.put("azp", "azure_client_id");
        });
        assertEquals("azure_client_id", extractor.extractClientId(azureJwt, List.of("clientId", "azp")));

        // Test OAuth2 standard (client_id)
        String oauth2Jwt = createUnsignedJwt(Claims.class, claims -> {
            claims.put("client_id", "oauth2_client");
        });
        assertEquals("oauth2_client", extractor.extractClientId(oauth2Jwt, List.of("clientId", "client_id")));

        // Test alternative format (cid)
        String altJwt = createUnsignedJwt(Claims.class, claims -> {
            claims.put("cid", "alternative_client");
        });
        assertEquals("alternative_client", extractor.extractClientId(altJwt, List.of("clientId", "cid")));
    }

    /**
     * Helper method to create unsigned JWT for testing.
     */
    private String createUnsignedJwt(Class<Claims> claimsClass, java.util.function.Consumer<java.util.Map<String, Object>> claimsBuilder) {
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
