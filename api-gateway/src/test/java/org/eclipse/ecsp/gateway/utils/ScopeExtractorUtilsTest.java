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

package org.eclipse.ecsp.gateway.utils;

import org.eclipse.ecsp.gateway.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScopeExtractorUtilsTest {

    private JwtProperties jwtProperties;
    private Map<String, Object> claims;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        claims = new HashMap<>();
    }

    // Default Scope Claim Tests

    @Test
    void shouldExtractWhenDefaultClaimIsUsed() {
        claims.put("scope", "Scope1 Scope2");
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(Set.of("Scope1", "Scope2"), scopes);
    }

    @Test
    void shouldExtractWhenConfiguredClaimIsNull() {
        jwtProperties.setScopeClaims(null);
        claims.put("scope", "Scope1 Scope2");
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(Set.of("Scope1", "Scope2"), scopes);
    }

    @Test
    void shouldExtractWhenConfiguredClaimIsBlank() {
        jwtProperties.setScopeClaims(Arrays.asList("  "));
        claims.put("scope", "Scope1 Scope2");
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(Set.of("Scope1", "Scope2"), scopes);
    }

    // Configured Scope Claim Tests

    @Test
    void shouldExtractWhenCustomClaimIsConfigured() {
        jwtProperties.setScopeClaims(Arrays.asList("scp"));
        claims.put("scp", Arrays.asList("Scope1", "Scope2"));
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(Set.of("Scope1", "Scope2"), scopes);
    }

    @Test
    void shouldExtractWhenCustomNamespacedClaimIsConfigured() {
        jwtProperties.setScopeClaims(Arrays.asList("custom:scp"));
        claims.put("custom:scp", "[\"Scope1\",\"Scope2\"]");
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(Set.of("Scope1", "Scope2"), scopes);
    }

    // Whitespace Scope Parsing Tests

    @Test
    void shouldExtractSingleScope() {
        claims.put("scope", "Scope1");
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(Set.of("Scope1"), scopes);
    }

    @Test
    void shouldExtractMultipleScopes() {
        claims.put("scope", "Scope1 Scope2 Scope3");
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(3, scopes.size());
        assertTrue(scopes.containsAll(Arrays.asList("Scope1", "Scope2", "Scope3")));
    }

    @Test
    void shouldExtractWithMultipleSpaces() {
        claims.put("scope", "Scope1    Scope2     Scope3");
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(3, scopes.size());
    }

    @Test
    void shouldExtractWithLeadingAndTrailingWhitespace() {
        claims.put("scope", "   Scope1 Scope2   ");
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(Set.of("Scope1", "Scope2"), scopes);
    }

    // Collection Scope Parsing Tests

    @Test
    void shouldExtractFromArrayClaim() {
        claims.put("scp", Arrays.asList("Scope1", "Scope2"));
        jwtProperties.setScopeClaims(Arrays.asList("scp"));
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(2, scopes.size());
    }

    @Test
    void shouldExtractFromSetClaim() {
        claims.put("scp", new HashSet<>(Arrays.asList("Scope1", "Scope2")));
        jwtProperties.setScopeClaims(Arrays.asList("scp"));
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(2, scopes.size());
    }

    @Test
    void shouldExtractCollectionContainingNulls() {
        claims.put("scp", Arrays.asList("Scope1", null, "Scope2"));
        jwtProperties.setScopeClaims(Arrays.asList("scp"));
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(Set.of("Scope1", "Scope2"), scopes);
    }

    // JSON String Array Parsing Tests

    @Test
    void shouldExtractValidJsonArrayString() {
        claims.put("custom:scp", "[\"Scope1\",\"Scope2\"]");
        jwtProperties.setScopeClaims(Arrays.asList("custom:scp"));
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(Set.of("Scope1", "Scope2"), scopes);
    }

    @Test
    void shouldExtractJsonArrayStringWithWhitespace() {
        claims.put("custom:scp", "[\n     \"Scope1\",\n     \"Scope2\"\n   ]");
        jwtProperties.setScopeClaims(Arrays.asList("custom:scp"));
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(Set.of("Scope1", "Scope2"), scopes);
    }

    @Test
    void shouldExtractEmptyJsonArray() {
        claims.put("custom:scp", "[]");
        jwtProperties.setScopeClaims(Arrays.asList("custom:scp"));
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertNotNull(scopes);
        assertTrue(scopes.isEmpty());
    }

    @Test
    void shouldNotFailOnMalformedJsonArray() {
        claims.put("custom:scp", "[Scope1");
        jwtProperties.setScopeClaims(Arrays.asList("custom:scp"));
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertNotNull(scopes);
        assertTrue(scopes.isEmpty());
    }

    // Duplicate Handling Tests

    @Test
    void shouldHandleWhitespaceFormatDuplicates() {
        claims.put("scope", "Scope1 Scope1 Scope2");
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(Set.of("Scope1", "Scope2"), scopes);
    }

    @Test
    void shouldHandleArrayDuplicates() {
        claims.put("scp", Arrays.asList("Scope1", "Scope1", "Scope2"));
        jwtProperties.setScopeClaims(Arrays.asList("scp"));
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(Set.of("Scope1", "Scope2"), scopes);
    }

    // Empty Value Tests

    @Test
    void shouldHandleEmptyString() {
        claims.put("scope", "");
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertNotNull(scopes);
        assertTrue(scopes.isEmpty());
    }

    @Test
    void shouldHandleWhitespaceOnlyString() {
        claims.put("scope", "     ");
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertNotNull(scopes);
        assertTrue(scopes.isEmpty());
    }

    @Test
    void shouldHandleNullValue() {
        claims.put("scope", null);
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertNotNull(scopes);
        assertTrue(scopes.isEmpty());
    }

    // Missing Claim Tests

    @Test
    void shouldHandleClaimMissing() {
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertNotNull(scopes);
        assertTrue(scopes.isEmpty());
    }

    @Test
    void shouldHandleConfiguredClaimMissing() {
        jwtProperties.setScopeClaims(Arrays.asList("scp"));
        claims.put("scope", "Scope1");
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertNotNull(scopes);
        assertTrue(scopes.isEmpty());
    }

    // Unsupported Type Tests

    @Test
    void shouldHandleObjectClaim() {
        claims.put("scope", Map.of("name", "Scope1"));
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertNotNull(scopes);
        assertTrue(scopes.isEmpty());
    }

    @Test
    void shouldHandleNumericClaim() {
        claims.put("scope", 123);
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertNotNull(scopes);
        assertTrue(scopes.isEmpty());
    }

    // Normalization Tests

    @Test
    void shouldTrimValues() {
        claims.put("scp", Arrays.asList(" Scope1 ", " Scope2 "));
        jwtProperties.setScopeClaims(Arrays.asList("scp"));
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(Set.of("Scope1", "Scope2"), scopes);
    }

    @Test
    void shouldIgnoreEmptyEntries() {
        claims.put("scp", Arrays.asList("Scope1", "", " ", "Scope2"));
        jwtProperties.setScopeClaims(Arrays.asList("scp"));
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(Set.of("Scope1", "Scope2"), scopes);
    }

    @Test
    void shouldPreserveCase() {
        claims.put("scp", Arrays.asList("Scope1", "SCOPE2", "scope3"));
        jwtProperties.setScopeClaims(Arrays.asList("scp"));
        Set<String> scopes = ScopeExtractorUtils.extractScopes(claims, jwtProperties);
        assertEquals(Set.of("Scope1", "SCOPE2", "scope3"), scopes);
    }
}
