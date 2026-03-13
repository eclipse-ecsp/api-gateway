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

package org.eclipse.ecsp.gateway.config;

import org.eclipse.ecsp.gateway.model.PublicKeyAuthType;
import org.eclipse.ecsp.gateway.model.PublicKeySource;
import org.eclipse.ecsp.gateway.model.PublicKeyType;
import org.eclipse.ecsp.gateway.model.TokenHeaderValidationConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for JwtProperties configuration binding and validation.
 * Tests property mapping, configuration validation, and Spring Boot configuration binding.
 */
class JwtPropertiesTest {

    public static final int TWO = 2;
    public static final int FIVE = 5;
    public static final int TEN = 10;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        // Given
        jwtProperties = new JwtProperties();
    }

    /**
     * Test basic property binding.
     * Verifies that JWT properties are correctly bound from configuration.
     */
    @Test
    void bindWhenValidConfigurationThenBindsPropertiesCorrectly() {
        // Given
        Map<String, Object> properties = new HashMap<>();
        properties.put("jwt.token-claim-to-header-mapping.sub", "X-User-Id");
        properties.put("jwt.token-claim-to-header-mapping.scope", "X-User-Scope");
        properties.put("jwt.key-sources[0].id", "test-provider");
        properties.put("jwt.key-sources[0].type", "JWKS");
        properties.put("jwt.key-sources[0].location", "https://test.com/.well-known/jwks.json");
        properties.put("jwt.key-sources[0].refresh-interval", "PT5M");
        properties.put("jwt.key-sources[0].use-provider-prefixed-key", "true");

        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);

        // When
        jwtProperties = binder.bind("jwt", JwtProperties.class).get();

        // Then
        assertNotNull(jwtProperties.getTokenClaimToHeaderMapping());
        assertEquals(TWO, jwtProperties.getTokenClaimToHeaderMapping().size());
        assertEquals("X-User-Id", jwtProperties.getTokenClaimToHeaderMapping().get("sub"));
        assertEquals("X-User-Scope", jwtProperties.getTokenClaimToHeaderMapping().get("scope"));

        assertNotNull(jwtProperties.getKeySources());
        assertEquals(1, jwtProperties.getKeySources().size());

        PublicKeySource keySource = jwtProperties.getKeySources().get(0);
        assertEquals("test-provider", keySource.getId());
        assertEquals(PublicKeyType.JWKS, keySource.getType());
        assertEquals("https://test.com/.well-known/jwks.json", keySource.getLocation());
        assertEquals(Duration.ofMinutes(FIVE), keySource.getRefreshInterval());
        assertTrue(keySource.isUseProviderPrefixedKey());
    }

    /**
     * Test token header validation configuration binding.
     * Verifies that token header validation configs are correctly bound.
     */
    @Test
    void bindWhenTokenHeaderValidationConfigThenBindsCorrectly() {
        // Given
        Map<String, Object> properties = new HashMap<>();
        properties.put("jwt.token-header-validation-config.authorization.required", "true");
        properties.put("jwt.token-header-validation-config.authorization.regex",
                "Bearer\\s[\\d|a-f]{8}-([\\d|a-f]{4}-){3}[\\d|a-f]{12}");
        properties.put("jwt.token-header-validation-config.x-api-key.required", "false");
        properties.put("jwt.token-header-validation-config.x-api-key.regex", "");

        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);

        // When
        jwtProperties = binder.bind("jwt", JwtProperties.class).get();

        // Then
        assertNotNull(jwtProperties.getTokenHeaderValidationConfig());
        assertEquals(TWO, jwtProperties.getTokenHeaderValidationConfig().size());

        TokenHeaderValidationConfig authConfig = jwtProperties.getTokenHeaderValidationConfig().get("authorization");
        assertNotNull(authConfig);
        assertTrue(authConfig.isRequired());
        assertEquals("Bearer\\s[\\d|a-f]{8}-([\\d|a-f]{4}-){3}[\\d|a-f]{12}", authConfig.getRegex());

        TokenHeaderValidationConfig apiKeyConfig = jwtProperties.getTokenHeaderValidationConfig().get("x-api-key");
        assertNotNull(apiKeyConfig);
        assertFalse(apiKeyConfig.isRequired());
        assertEquals("", apiKeyConfig.getRegex());
    }

    /**
     * Test multiple key sources configuration.
     * Verifies that multiple public key sources can be configured.
     */
    @Test
    void bindWhenMultipleKeySourcesThenBindsAllSources() {
        // Given
        Map<String, Object> properties = new HashMap<>();
        properties.put("jwt.key-sources[0].id", "jwks-provider");
        properties.put("jwt.key-sources[0].type", "JWKS");
        properties.put("jwt.key-sources[0].location", "https://jwks.example.com/.well-known/jwks.json");
        properties.put("jwt.key-sources[0].refresh-interval", "PT5M");

        properties.put("jwt.key-sources[1].id", "pem-provider");
        properties.put("jwt.key-sources[1].type", "PEM");
        properties.put("jwt.key-sources[1].location", "file:///opt/certs/public.pem");
        properties.put("jwt.key-sources[1].refresh-interval", "PT10M");
        properties.put("jwt.key-sources[1].auth-type", "BASIC");

        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);

        // When
        jwtProperties = binder.bind("jwt", JwtProperties.class).get();

        // Then
        assertNotNull(jwtProperties.getKeySources());
        assertEquals(TWO, jwtProperties.getKeySources().size());

        PublicKeySource jwksSource = jwtProperties.getKeySources().get(0);
        assertEquals("jwks-provider", jwksSource.getId());
        assertEquals(PublicKeyType.JWKS, jwksSource.getType());
        assertEquals("https://jwks.example.com/.well-known/jwks.json", jwksSource.getLocation());
        assertEquals(Duration.ofMinutes(FIVE), jwksSource.getRefreshInterval());

        PublicKeySource pemSource = jwtProperties.getKeySources().get(1);
        assertEquals("pem-provider", pemSource.getId());
        assertEquals(PublicKeyType.PEM, pemSource.getType());
        assertEquals("file:///opt/certs/public.pem", pemSource.getLocation());
        assertEquals(Duration.ofMinutes(TEN), pemSource.getRefreshInterval());
        assertEquals(PublicKeyAuthType.BASIC, pemSource.getAuthType());
    }

    /**
     * Test empty configuration handling.
     * Verifies that empty configurations are handled gracefully.
     */
    @Test
    void bindWhenEmptyConfigurationThenHandlesGracefully() {
        // Given
        Map<String, Object> properties = new HashMap<>();
        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);

        // When
        JwtProperties boundProperties = binder.bind("jwt", JwtProperties.class).orElse(new JwtProperties());

        // Then
        assertNull(boundProperties.getTokenClaimToHeaderMapping());
        assertNull(boundProperties.getKeySources());
        assertNull(boundProperties.getTokenHeaderValidationConfig());
    }

    /**
     * Test setter and getter methods.
     * Verifies that all properties can be set and retrieved correctly.
     */
    @Test
    void settersAndGettersWhenCalledThenWorkCorrectly() {
        // Given
        Map<String, TokenHeaderValidationConfig> headerConfig = new HashMap<>();
        headerConfig.put("authorization", new TokenHeaderValidationConfig(true, "Bearer "));

        PublicKeySource keySource = new PublicKeySource();
        keySource.setId("test-source");
        keySource.setType(PublicKeyType.JWKS);
        List<PublicKeySource> keySources = List.of(keySource);

        // When
        Map<String, String> tokenMapping = Map.of("sub", "X-User-Id", "scope", "X-Scope");
        jwtProperties.setTokenClaimToHeaderMapping(tokenMapping);
        jwtProperties.setTokenHeaderValidationConfig(headerConfig);
        jwtProperties.setKeySources(keySources);

        // Then
        assertEquals(tokenMapping, jwtProperties.getTokenClaimToHeaderMapping());
        assertEquals(headerConfig, jwtProperties.getTokenHeaderValidationConfig());
        assertEquals(keySources, jwtProperties.getKeySources());
    }

    /**
     * Test configuration with credentials.
     * Verifies that key sources with authentication credentials are handled correctly.
     */
    @Test
    void bindWhenKeySourceHasCredentialsThenBindsCredentialsCorrectly() {
        // Given
        Map<String, Object> properties = new HashMap<>();
        properties.put("jwt.key-sources[0].id", "secured-provider");
        properties.put("jwt.key-sources[0].type", "JWKS");
        properties.put("jwt.key-sources[0].location", "https://secured.example.com/jwks.json");
        properties.put("jwt.key-sources[0].auth-type", "BASIC");
        properties.put("jwt.key-sources[0].credentials.username", "test-user");
        properties.put("jwt.key-sources[0].credentials.password", "test-pass");

        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);

        // When
        jwtProperties = binder.bind("jwt", JwtProperties.class).get();

        // Then
        assertNotNull(jwtProperties.getKeySources());
        assertEquals(1, jwtProperties.getKeySources().size());

        PublicKeySource keySource = jwtProperties.getKeySources().get(0);
        assertEquals("secured-provider", keySource.getId());
        assertEquals(PublicKeyAuthType.BASIC, keySource.getAuthType());
        assertNotNull(keySource.getCredentials());
        assertEquals("test-user", keySource.getCredentials().getUsername());
        assertEquals("test-pass", keySource.getCredentials().getPassword());
    }

    /**
     * Test null safety of property access.
     * Verifies that null properties don't cause issues.
     */
    @Test
    void propertiesWhenNullThenHandledSafely() {
        // Given
        JwtProperties nullProperties = new JwtProperties();

        // When & Then
        assertNull(nullProperties.getTokenClaimToHeaderMapping());
        assertNull(nullProperties.getKeySources());
        assertNull(nullProperties.getTokenHeaderValidationConfig());

        // Setting null values should work
        try {
            nullProperties.setTokenClaimToHeaderMapping(null);
            nullProperties.setKeySources(null);
            nullProperties.setTokenHeaderValidationConfig(null);
        } catch (Exception e) {
            Assertions.fail("should not throw error", e);
        }
    }
}
