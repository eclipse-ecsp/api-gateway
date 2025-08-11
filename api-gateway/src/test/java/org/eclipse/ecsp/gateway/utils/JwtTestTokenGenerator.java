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

import io.jsonwebtoken.Jwts;
import lombok.NoArgsConstructor;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for generating JWT tokens for testing purposes.
 * Provides methods to create valid JWT tokens with various claims and configurations.
 */
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class JwtTestTokenGenerator {

    private static final KeyPair TEST_KEY_PAIR = generateTestKeyPair();
    private static final String DEFAULT_ISSUER = "test-issuer";
    private static final String DEFAULT_SUBJECT = "admin";
    private static final String DEFAULT_AUDIENCE = "test-audience";
    private static final String DEFAULT_KID = "test-key-id";
    private static final String DEFAULT_USER = "8762871629812981298";
    public static final int KEYSIZE = 2048;
    public static final int TWO = 2;
    public static final int ONE = 1;

    /**
     * Generates a test RSA key pair for JWT signing.
     *
     * @return KeyPair for testing
     */
    private static KeyPair generateTestKeyPair() {
        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
            keyGenerator.initialize(KEYSIZE);
            return keyGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate test key pair", e);
        }
    }

    /**
     * Gets the test public key for token verification.
     *
     * @return PublicKey for verification
     */
    public static PublicKey getTestPublicKey() {
        return TEST_KEY_PAIR.getPublic();
    }

    /**
     * Gets the test private key for token signing.
     *
     * @return PrivateKey for signing
     */
    public static PrivateKey getTestPrivateKey() {
        return TEST_KEY_PAIR.getPrivate();
    }

    /**
     * Creates a default valid JWT token for testing.
     *
     * @return Bearer token string
     */
    public static String createDefaultToken() {
        return createToken(DEFAULT_SUBJECT, DEFAULT_ISSUER, DEFAULT_AUDIENCE, "SelfManage", DEFAULT_USER,  DEFAULT_KID);
    }

    /**
     * Creates a JWT token with KID header for testing.
     *
     * @return Bearer token string with KID
     */
    public static String createTokenWithKid() {
        return createToken(DEFAULT_SUBJECT, DEFAULT_ISSUER, DEFAULT_AUDIENCE, "SelfManage", DEFAULT_USER, DEFAULT_KID);
    }

    /**
     * Creates a JWT token without KID header for testing.
     *
     * @return Bearer token string without KID
     */
    public static String createTokenWithoutKid() {
        return createToken(DEFAULT_SUBJECT, DEFAULT_ISSUER, DEFAULT_AUDIENCE, "SelfManage", null, null);
    }

    /**
     * Creates an expired JWT token for testing token expiration scenarios.
     *
     * @return Expired bearer token string
     */
    public static String createExpiredToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("scope", "SelfManage");
        claims.put("aud", DEFAULT_AUDIENCE);
        claims.put("user_id", DEFAULT_USER);

        Map<String, Object> headers = new HashMap<>();
        headers.put("kid", DEFAULT_KID);

        return "Bearer " + Jwts.builder()
                .header().add(headers).and()
                .claims().add(claims)
                .subject(DEFAULT_SUBJECT)
                .issuer(DEFAULT_ISSUER)
                .issuedAt(Date.from(Instant.now().minus(TWO, ChronoUnit.HOURS)))
                .expiration(Date.from(Instant.now().minus(ONE, ChronoUnit.HOURS))) // Expired 1 hour ago
                .and()
                .signWith(TEST_KEY_PAIR.getPrivate())
                .compact();
    }

    /**
     * Creates a JWT token with invalid signature for testing verification failures.
     *
     * @return Bearer token string with invalid signature
     */
    public static String createTokenWithInvalidSignature() {
        // Create a different key pair for invalid signature

        Map<String, Object> claims = new HashMap<>();
        claims.put("scope", "SelfManage");
        claims.put("aud", DEFAULT_AUDIENCE);

        Map<String, Object> headers = new HashMap<>();
        headers.put("kid", DEFAULT_KID);
        KeyPair invalidKeyPair = generateTestKeyPair();
        return "Bearer " + Jwts.builder()
                .header().add(headers)
                .and().claims().add(claims)
                .subject(DEFAULT_SUBJECT)
                .issuer(DEFAULT_ISSUER)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(ONE, ChronoUnit.HOURS)))
                .and()
                .signWith(invalidKeyPair.getPrivate()) // Different key
                .compact();
    }

    /**
     * Creates a custom JWT token with specified parameters.
     *
     * @param subject   Subject claim
     * @param issuer    Issuer claim
     * @param audience  Audience claim
     * @param scope     Scope claim
     * @param keyId     Key ID for header (can be null)
     * @return Bearer token string
     */
    public static String createToken(String subject,
                                     String issuer,
                                     String audience,
                                     String scope,
                                     String userId,
                                     String keyId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("scope", scope);
        if (audience != null) {
            claims.put("aud", audience);
        }
        if (userId != null) {
            claims.put("user_id", userId);
        }

        Map<String, Object> headers = new HashMap<>();
        if (keyId != null) {
            headers.put("kid", keyId);
        }

        return "Bearer " + Jwts.builder()
                .header().add(headers)
                .and()
                .claims().add(claims)
                .subject(subject)
                .issuer(issuer)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(ONE, ChronoUnit.HOURS)))
                .and()
                .signWith(TEST_KEY_PAIR.getPrivate())
                .compact();
    }

    /**
     * Creates a JWT token with multiple scopes.
     *
     * @param scopes Array of scopes
     * @return Bearer token string
     */
    public static String createTokenWithMultipleScopes(String... scopes) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("scope", Arrays.asList(scopes));
        claims.put("aud", DEFAULT_AUDIENCE);
        claims.put("user_id", DEFAULT_USER);

        Map<String, Object> headers = new HashMap<>();
        headers.put("kid", DEFAULT_KID);

        return "Bearer " + Jwts.builder()
                .header().add(headers).and()
                .claims().add(claims)
                .subject(DEFAULT_SUBJECT)
                .issuer(DEFAULT_ISSUER)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(ONE, ChronoUnit.HOURS)))
                .and()
                .signWith(TEST_KEY_PAIR.getPrivate())
                .compact();
    }

    /**
     * Creates a JWT token with custom claims.
     *
     * @param customClaims Map of custom claims to add
     * @return Bearer token string
     */
    public static String createTokenWithCustomClaims(Map<String, Object> customClaims) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("scope", "SelfManage");
        claims.put("aud", DEFAULT_AUDIENCE);
        claims.putAll(customClaims);

        return "Bearer " + Jwts.builder()
                .header()
                .keyId(DEFAULT_KID)
                .and()
                .claims()
                .add(claims)
                .subject(DEFAULT_SUBJECT)
                .issuer(DEFAULT_ISSUER)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(ONE, ChronoUnit.HOURS)))
                .and().signWith(TEST_KEY_PAIR.getPrivate())
                .compact();
    }

    /**
     * Creates an invalid JWT token (malformed).
     *
     * @return Invalid bearer token string
     */
    public static String createInvalidToken() {
        return "Bearer invalid.jwt.token";
    }

    /**
     * Creates a JWT token with missing required claims.
     *
     * @return Bearer token string without required claims
     */
    public static String createTokenWithMissingClaims() {
        return "Bearer " + Jwts.builder()
                .header()
                .keyId(DEFAULT_KID)
                .and()
                .claims()
                .subject(DEFAULT_SUBJECT)
                .issuer(DEFAULT_ISSUER)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(ONE, ChronoUnit.HOURS)))
                .and()
                .signWith(TEST_KEY_PAIR.getPrivate())
                .compact();
    }
}
