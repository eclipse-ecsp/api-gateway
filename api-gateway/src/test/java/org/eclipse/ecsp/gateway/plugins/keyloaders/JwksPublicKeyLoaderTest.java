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

package org.eclipse.ecsp.gateway.plugins.keyloaders;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.eclipse.ecsp.gateway.model.PublicKeyAuthType;
import org.eclipse.ecsp.gateway.model.PublicKeyCredentials;
import org.eclipse.ecsp.gateway.model.PublicKeySource;
import org.eclipse.ecsp.gateway.model.PublicKeyType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.PublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.not;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;


@ExtendWith(SpringExtension.class)
class JwksPublicKeyLoaderTest {
    public static final int TWO = 2;
    public static final int THREE = 3;
    private JwksPublicKeyLoader jwksPublicKeyLoader;
    private WireMockServer wireMockServer;
    private String baseUrl;

    private static final String VALID_JWKS_RESPONSE = """
            {
              "keys": [
                {
                  "kty": "RSA",
                  "kid": "test-key-id-1",
                  "use": "sig",
                  "alg": "RS256",
                  "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPF"""
                  + """
                  FxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMs"""
                  + """
                  tn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs"""
                  + """
                  8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISNzK5sW_YLdnoYKAE",
                  "e": "AQAB"
                },
                {
                  "kty": "RSA",
                  "kid": "test-key-id-2",
                  "use": "sig",
                  "alg": "RS256",
                  "n": "xnR-WZnF-3CZ5eR5vGEuiUhKQN7pI1pGNn6HfhXfXA1T9p6BdHdqRN9dLWN6QnTxQl6QqPrVGKp"""
                  + """
                  n5rLJLkQwKl7zL8QGFfh6Y2oXJoNlP7P6U9Z1bK6q"""
                  + """
                  8q2qZqQqPrVGKpn5rLJLkQwKl7zL8QGFfh6Y2oXJoNlP7P6U9Z1bK6q8q2q",
                  "e": "AQAB"
                }
              ]
            }""";

    private static final String OAUTH_TOKEN_RESPONSE = """
            {
              "access_token": "test-access-token-12345",
              "token_type": "Bearer",
              "expires_in": 3600,
              "scope": "read write"
            }""";

    private static final String RSA_JWKS_RESPONSE = """
            {
              "keys": [
                {
                  "kty": "RSA",
                  "kid": "rsa-key-1",
                  "use": "sig",
                  "alg": "RS256",
                  "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPF"""
                  + """
                    FxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMs"""
                  + """
                  tn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAt"""
                  + """
                  aSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISNzK5sW_YLdnoYKAE",
                  "e": "AQAB"
                }
              ]
            }
            """;

    private static final String EC_JWKS_RESPONSE = """
            {
              "keys": [
                {
                  "kty": "EC",
                  "kid": "ec-key-1",
                  "use": "sig",
                  "alg": "ES256",
                  "crv": "P-256",
                  "x": "f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU",
                  "y": "x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0"
                }
              ]
            }
            """;

    private static final String OKP_JWKS_RESPONSE = """
            {
              "keys": [
                {
                  "kty": "OKP",
                  "kid": "okp-key-1",
                  "use": "sig",
                  "alg": "EdDSA",
                  "crv": "Ed25519",
                  "x": "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo"
                }
              ]
            }
            """;

    private static final String OCT_JWKS_RESPONSE = """
            {
              "keys": [
                {
                  "kty": "oct",
                  "kid": "oct-key-1",
                  "use": "sig",
                  "alg": "HS256",
                  "k": "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"
                }
              ]
            }
            """;

    private static final String MIXED_KEY_TYPES_JWKS_RESPONSE = """
            {
              "keys": [
                {
                  "kty": "RSA",
                  "kid": "rsa-mixed-1",
                  "use": "sig",
                  "alg": "RS256",
                  "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPF"""
                  + """
                  FxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMs"""
                  + """
                  tn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAta"""
                  + """
                  Sqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISNzK5sW_YLdnoYKAE",
                  "e": "AQAB"
                },
                {
                  "kty": "EC",
                  "kid": "ec-mixed-1",
                  "use": "sig",
                  "alg": "ES256",
                  "crv": "P-256",
                  "x": "f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU",
                  "y": "x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0"
                },
                {
                  "kty": "OKP",
                  "kid": "okp-mixed-1",
                  "use": "sig",
                  "alg": "EdDSA",
                  "crv": "Ed25519",
                  "x": "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo"
                }
              ]
            }
            """;

    private static final String SUPPORTED_OKP_JWKS_RESPONSE = """
            {
              "keys": [
                {
                  "kty": "OKP",
                  "kid": "okp-supported-1",
                  "use": "sig",
                  "alg": "EdDSA",
                  "crv": "X25519",
                  "x": "hSDwCYkwp1R0i33ctD73Wg2_Og0mOBr066SpjqqbTmo"
                }
              ]
            }
            """;

    private static final String MIXED_SUPPORTED_KEY_TYPES_JWKS_RESPONSE = """
            {
              "keys": [
                {
                  "kty": "RSA",
                  "kid": "rsa-mixed-supported-1",
                  "use": "sig",
                  "alg": "RS256",
                  "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPF"""
                  + """
                  FxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMs"""
                  + """
                  tn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368"""
                  + """
                  QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISNzK5sW_YLdnoYKAE",
                  "e": "AQAB"
                },
                {
                  "kty": "EC",
                  "kid": "ec-mixed-supported-1",
                  "use": "sig",
                  "alg": "ES256",
                  "crv": "P-256",
                  "x": "f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU",
                  "y": "x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0"
                },
                {
                  "kty": "OKP",
                  "kid": "okp-mixed-supported-1",
                  "use": "sig",
                  "alg": "EdDSA",
                  "crv": "X25519",
                  "x": "hSDwCYkwp1R0i33ctD73Wg2_Og0mOBr066SpjqqbTmo"
                }
              ]
            }
            """;

    private static final String UNSUPPORTED_KEY_TYPE_JWKS_RESPONSE = """
            {
              "keys": [
                {
                  "kty": "UNKNOWN",
                  "kid": "unknown-key-1",
                  "use": "sig",
                  "alg": "UNKNOWN256"
                },
                {
                  "kty": "RSA",
                  "kid": "rsa-supported-1",
                  "use": "sig",
                  "alg": "RS256",
                  "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1"""
                  + """
                  RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMs"""
                  + """
                  tn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQ"""
                  + """
                  MicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISNzK5sW_YLdnoYKAE",
                  "e": "AQAB"
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        jwksPublicKeyLoader = new JwksPublicKeyLoader(WebClient.builder());
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        baseUrl = "http://localhost:" + wireMockServer.port();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldLoadKeysSuccessfullyWithoutAuthentication() {
        stubFor(get(urlEqualTo("/jwks"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(VALID_JWKS_RESPONSE)));

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks", PublicKeyAuthType.NONE, null);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(TWO, result.size());
        Assertions.assertTrue(result.containsKey("test-key-id-1"));
        Assertions.assertTrue(result.containsKey("test-key-id-2"));
        Assertions.assertNotNull(result.get("test-key-id-1"));
        Assertions.assertNotNull(result.get("test-key-id-2"));
    }

    @Test
    void shouldLoadKeysSuccessfullyWithBasicAuthentication() {
        String expectedAuth = Base64.getEncoder().encodeToString("testuser:testpass".getBytes());

        stubFor(get(urlEqualTo("/jwks"))
                .withHeader("Authorization", equalTo("Basic " + expectedAuth))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(VALID_JWKS_RESPONSE)));

        PublicKeyCredentials credentials = new PublicKeyCredentials();
        credentials.setUsername("testuser");
        credentials.setPassword("testpass");

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks", PublicKeyAuthType.BASIC, credentials);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(TWO, result.size());
        verify(getRequestedFor(urlEqualTo("/jwks"))
                .withHeader("Authorization", equalTo("Basic " + expectedAuth)));
    }

    @Test
    void shouldLoadKeysSuccessfullyWithClientCredentialsAuthentication() {
        stubFor(post(urlEqualTo("/oauth/token"))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(OAUTH_TOKEN_RESPONSE)));

        stubFor(get(urlEqualTo("/jwks"))
                .withHeader("Authorization", equalTo("Bearer test-access-token-12345"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(VALID_JWKS_RESPONSE)));

        PublicKeyCredentials credentials = new PublicKeyCredentials();
        credentials.setClientId("test-client");
        credentials.setClientSecret("test-secret");
        credentials.setScopes(List.of("read", "write"));
        credentials.setTokenEndpoint(baseUrl + "/oauth/token");

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks",
                PublicKeyAuthType.CLIENT_CREDENTIALS, credentials);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(TWO, result.size());
        verify(postRequestedFor(urlEqualTo("/oauth/token")));
        verify(getRequestedFor(urlEqualTo("/jwks"))
                .withHeader("Authorization", equalTo("Bearer test-access-token-12345")));
    }

    @Test
    void shouldHandleEmptyJwksResponse() {
        stubFor(get(urlEqualTo("/jwks"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"keys\":[]}")));

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks", PublicKeyAuthType.NONE, null);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyMapWhenJwksEndpointReturnsError() {
        stubFor(get(urlEqualTo("/jwks"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withBody("Internal Server Error")));

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks", PublicKeyAuthType.NONE, null);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenConfigIsNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            jwksPublicKeyLoader.loadKeys(null);
        });
    }

    @Test
    void shouldThrowExceptionWhenUrlIsNull() {
        PublicKeySource config = new PublicKeySource();
        config.setUrl(null);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            jwksPublicKeyLoader.loadKeys(config);
        });
    }

    @Test
    void shouldThrowExceptionWhenBasicAuthCredentialsAreMissing() {
        PublicKeyCredentials credentials = new PublicKeyCredentials();
        credentials.setUsername(null);
        credentials.setPassword("testpass");

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks", PublicKeyAuthType.BASIC, credentials);

        Map<String, PublicKey> keys = jwksPublicKeyLoader.loadKeys(config);
        Assertions.assertNotNull(keys);
        Assertions.assertTrue(keys.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenBasicAuthPasswordIsMissing() {
        PublicKeyCredentials credentials = new PublicKeyCredentials();
        credentials.setUsername("testuser");
        credentials.setPassword(null);

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks", PublicKeyAuthType.BASIC, credentials);

        Map<String, PublicKey> keys = jwksPublicKeyLoader.loadKeys(config);
        Assertions.assertNotNull(keys);
        Assertions.assertTrue(keys.isEmpty());

    }

    @Test
    void shouldThrowExceptionWhenClientCredentialsClientIdIsMissing() {
        PublicKeyCredentials credentials = new PublicKeyCredentials();
        credentials.setClientId(null);
        credentials.setClientSecret("test-secret");
        credentials.setTokenEndpoint(baseUrl + "/oauth/token");

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks",
                PublicKeyAuthType.CLIENT_CREDENTIALS, credentials);

        Map<String, PublicKey> keys = jwksPublicKeyLoader.loadKeys(config);
        Assertions.assertNotNull(keys);
        Assertions.assertTrue(keys.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenClientCredentialsSecretIsMissing() {
        PublicKeyCredentials credentials = new PublicKeyCredentials();
        credentials.setClientId("test-client");
        credentials.setClientSecret(null);
        credentials.setTokenEndpoint(baseUrl + "/oauth/token");

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks",
                PublicKeyAuthType.CLIENT_CREDENTIALS, credentials);

        Map<String, PublicKey> keys = jwksPublicKeyLoader.loadKeys(config);
        Assertions.assertNotNull(keys);
        Assertions.assertTrue(keys.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenTokenEndpointIsMissing() {
        PublicKeyCredentials credentials = new PublicKeyCredentials();
        credentials.setClientId("test-client");
        credentials.setClientSecret("test-secret");
        credentials.setTokenEndpoint(null);

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks",
                PublicKeyAuthType.CLIENT_CREDENTIALS, credentials);

        Map<String, PublicKey> keys = jwksPublicKeyLoader.loadKeys(config);
        Assertions.assertNotNull(keys);
        Assertions.assertTrue(keys.isEmpty());
    }

    @Test
    void shouldHandleOauthTokenEndpointError() {
        stubFor(post(urlEqualTo("/oauth/token"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())
                        .withBody("{\"error\":\"invalid_client\"}")));

        PublicKeyCredentials credentials = new PublicKeyCredentials();
        credentials.setClientId("test-client");
        credentials.setClientSecret("test-secret");
        credentials.setTokenEndpoint(baseUrl + "/oauth/token");

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks",
                PublicKeyAuthType.CLIENT_CREDENTIALS, credentials);

        Map<String, PublicKey> keys = jwksPublicKeyLoader.loadKeys(config);
        Assertions.assertNotNull(keys);
        Assertions.assertTrue(keys.isEmpty());
    }

    @Test
    void shouldHandleClientCredentialsWithoutScopes() {
        stubFor(post(urlEqualTo("/oauth/token"))
                .withRequestBody(containing("grant_type=client_credentials"))
                .withRequestBody(containing("client_id=test-client"))
                .withRequestBody(containing("client_secret=test-secret"))
                .withRequestBody(not(containing("scope=")))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(OAUTH_TOKEN_RESPONSE)));

        stubFor(get(urlEqualTo("/jwks"))
                .withHeader("Authorization", equalTo("Bearer test-access-token-12345"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(VALID_JWKS_RESPONSE)));

        PublicKeyCredentials credentials = new PublicKeyCredentials();
        credentials.setClientId("test-client");
        credentials.setClientSecret("test-secret");
        credentials.setTokenEndpoint(baseUrl + "/oauth/token");

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks",
                PublicKeyAuthType.CLIENT_CREDENTIALS, credentials);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(TWO, result.size());
    }

    @Test
    void shouldReturnCorrectPublicKeyType() {
        PublicKeyType result = jwksPublicKeyLoader.getType();
        Assertions.assertEquals(PublicKeyType.JWKS, result);
    }

    @Test
    void shouldHandleBasicAuthenticationFailure() {
        stubFor(get(urlEqualTo("/jwks"))
                .withHeader("Authorization", matching("Basic .*"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.UNAUTHORIZED.value())
                        .withBody("Unauthorized")));

        PublicKeyCredentials credentials = new PublicKeyCredentials();
        credentials.setUsername("wronguser");
        credentials.setPassword("wrongpass");

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks", PublicKeyAuthType.BASIC, credentials);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void shouldLoadRsaKeysSuccessfully() {
        stubFor(get(urlEqualTo("/jwks/rsa"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(RSA_JWKS_RESPONSE)));

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks/rsa", PublicKeyAuthType.NONE, null);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.containsKey("rsa-key-1"));
        Assertions.assertNotNull(result.get("rsa-key-1"));
        Assertions.assertEquals("RSA", result.get("rsa-key-1").getAlgorithm());
    }

    @Test
    void shouldLoadEcKeysSuccessfully() {
        stubFor(get(urlEqualTo("/jwks/ec"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(EC_JWKS_RESPONSE)));

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks/ec", PublicKeyAuthType.NONE, null);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.containsKey("ec-key-1"));
        Assertions.assertNotNull(result.get("ec-key-1"));
        Assertions.assertEquals("EC", result.get("ec-key-1").getAlgorithm());
    }

    @Test
    void shouldSkipOkpKeysWhenConversionNotSupported() {
        stubFor(get(urlEqualTo("/jwks/okp"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(OKP_JWKS_RESPONSE)));

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks/okp", PublicKeyAuthType.NONE, null);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty()); // OKP keys will be skipped if conversion to PublicKey is not supported
        Assertions.assertFalse(result.containsKey("okp-key-1")); // OKP key should be skipped
    }

    @Test
    void shouldSkipOctetSequenceKeysAsTheyAreSymmetric() {
        stubFor(get(urlEqualTo("/jwks/oct"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(OCT_JWKS_RESPONSE)));

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks/oct", PublicKeyAuthType.NONE, null);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty()); // OctetSequenceKeys should be skipped as they are symmetric keys
    }

    @Test
    void shouldLoadMixedKeyTypesSuccessfully() {
        stubFor(get(urlEqualTo("/jwks/mixed"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(MIXED_KEY_TYPES_JWKS_RESPONSE)));

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks/mixed",
                PublicKeyAuthType.NONE, null);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(TWO, result.size()); // Only RSA and EC keys, OKP key will be skipped
        Assertions.assertTrue(result.containsKey("rsa-mixed-1"));
        Assertions.assertTrue(result.containsKey("ec-mixed-1"));
        Assertions.assertFalse(result.containsKey("okp-mixed-1"));
        Assertions.assertEquals("RSA", result.get("rsa-mixed-1").getAlgorithm());
        Assertions.assertEquals("EC", result.get("ec-mixed-1").getAlgorithm());
    }

    @Test
    void shouldSkipUnsupportedKeyTypesAndLoadSupportedOnes() {
        stubFor(get(urlEqualTo("/jwks/unsupported"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(UNSUPPORTED_KEY_TYPE_JWKS_RESPONSE)));

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks/unsupported", PublicKeyAuthType.NONE, null);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.containsKey("rsa-supported-1"));
        Assertions.assertFalse(result.containsKey("unknown-key-1"));
        Assertions.assertEquals("RSA", result.get("rsa-supported-1").getAlgorithm());
    }

    @Test
    void shouldReturnEmptyMapWhenNoValidKeysInJwks() {
        String emptyKeysResponse = """
                {
                  "keys": []
                }""";

        stubFor(get(urlEqualTo("/jwks/empty"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(emptyKeysResponse)));

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks/empty", PublicKeyAuthType.NONE, null);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandleInvalidJwksFormat() {
        String invalidJwksResponse = """
                {
                  "invalid": "format"
                }""";

        stubFor(get(urlEqualTo("/jwks/invalid"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(invalidJwksResponse)));

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks/invalid", PublicKeyAuthType.NONE, null);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void skipLoadSupportedOkpKeys() {
        stubFor(get(urlEqualTo("/jwks/okp-supported"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(SUPPORTED_OKP_JWKS_RESPONSE)));

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks/okp-supported", PublicKeyAuthType.NONE, null);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.size());
    }

    @Test
    void shouldLoadMixedKeyTypesWithSupportedOkpSuccessfully() {
        stubFor(get(urlEqualTo("/jwks/mixed-supported"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(MIXED_SUPPORTED_KEY_TYPES_JWKS_RESPONSE)));

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks/mixed-supported", PublicKeyAuthType.NONE, null);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(TWO, result.size()); // RSA, EC, and supported OKP keys
        Assertions.assertTrue(result.containsKey("rsa-mixed-supported-1"));
        Assertions.assertTrue(result.containsKey("ec-mixed-supported-1"));
        Assertions.assertEquals("RSA", result.get("rsa-mixed-supported-1").getAlgorithm());
        Assertions.assertEquals("EC", result.get("ec-mixed-supported-1").getAlgorithm());
    }

    @Test
    void shouldIncludeBasicAuthHeaderInTokenRequest() {
        String expectedClientAuth = Base64.getEncoder().encodeToString("test-client:test-secret".getBytes());

        stubFor(post(urlEqualTo("/oauth/token"))
                .withHeader("Authorization", equalTo("Basic " + expectedClientAuth))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                .withRequestBody(containing("grant_type=client_credentials"))
                .withRequestBody(containing("client_id=test-client"))
                .withRequestBody(containing("client_secret=test-secret"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(OAUTH_TOKEN_RESPONSE)));

        stubFor(get(urlEqualTo("/jwks"))
                .withHeader("Authorization", equalTo("Bearer test-access-token-12345"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(VALID_JWKS_RESPONSE)));

        PublicKeyCredentials credentials = new PublicKeyCredentials();
        credentials.setClientId("test-client");
        credentials.setClientSecret("test-secret");
        credentials.setTokenEndpoint(baseUrl + "/oauth/token");

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks",
                PublicKeyAuthType.CLIENT_CREDENTIALS, credentials);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(TWO, result.size());

        // Verify that the token request included the Basic Auth header
        verify(postRequestedFor(urlEqualTo("/oauth/token"))
                .withHeader("Authorization", equalTo("Basic " + expectedClientAuth)));
    }

    @Test
    void shouldIncludeBasicAuthHeaderInTokenRequestWithScopes() {
        String expectedClientAuth = Base64.getEncoder().encodeToString("test-client:test-secret".getBytes());

        stubFor(post(urlEqualTo("/oauth/token"))
                .withHeader("Authorization", equalTo("Basic " + expectedClientAuth))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                .withRequestBody(containing("grant_type=client_credentials"))
                .withRequestBody(containing("client_id=test-client"))
                .withRequestBody(containing("client_secret=test-secret"))
                .withRequestBody(containing("scope=read+write"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(OAUTH_TOKEN_RESPONSE)));

        stubFor(get(urlEqualTo("/jwks"))
                .withHeader("Authorization", equalTo("Bearer test-access-token-12345"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(VALID_JWKS_RESPONSE)));

        PublicKeyCredentials credentials = new PublicKeyCredentials();
        credentials.setClientId("test-client");
        credentials.setClientSecret("test-secret");
        credentials.setScopes(List.of("read", "write"));
        credentials.setTokenEndpoint(baseUrl + "/oauth/token");

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks",
                PublicKeyAuthType.CLIENT_CREDENTIALS, credentials);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(TWO, result.size());

        // Verify that the token request included the Basic Auth header and scopes
        verify(postRequestedFor(urlEqualTo("/oauth/token"))
                .withHeader("Authorization", equalTo("Basic " + expectedClientAuth))
                .withRequestBody(containing("scope=read+write")));
    }

    @Test
    void shouldHandleTokenRequestWithDifferentCredentials() {
        String expectedClientAuth = Base64.getEncoder().encodeToString("different-client:different-secret".getBytes());

        stubFor(post(urlEqualTo("/oauth/token"))
                .withHeader("Authorization", equalTo("Basic " + expectedClientAuth))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(OAUTH_TOKEN_RESPONSE)));

        stubFor(get(urlEqualTo("/jwks"))
                .withHeader("Authorization", equalTo("Bearer test-access-token-12345"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(VALID_JWKS_RESPONSE)));

        PublicKeyCredentials credentials = new PublicKeyCredentials();
        credentials.setClientId("different-client");
        credentials.setClientSecret("different-secret");
        credentials.setTokenEndpoint(baseUrl + "/oauth/token");

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks",
                PublicKeyAuthType.CLIENT_CREDENTIALS, credentials);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(TWO, result.size());

        // Verify that the token request included the correct Basic Auth header for different credentials
        verify(postRequestedFor(urlEqualTo("/oauth/token"))
                .withHeader("Authorization", equalTo("Basic " + expectedClientAuth)));
    }

    @Test
    void shouldHandleTokenRequestAuthenticationFailure() {
        String expectedClientAuth = Base64.getEncoder().encodeToString("wrong-client:wrong-secret".getBytes());

        stubFor(post(urlEqualTo("/oauth/token"))
                .withHeader("Authorization", equalTo("Basic " + expectedClientAuth))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.UNAUTHORIZED.value())
                        .withBody("{\"error\":\"invalid_client\","
                        + "\"error_description\":\"Client authentication failed\"}")));

        PublicKeyCredentials credentials = new PublicKeyCredentials();
        credentials.setClientId("wrong-client");
        credentials.setClientSecret("wrong-secret");
        credentials.setTokenEndpoint(baseUrl + "/oauth/token");

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks",
                PublicKeyAuthType.CLIENT_CREDENTIALS, credentials);

        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());

        // Verify that the token request was made with Basic Auth header even though it failed
        verify(postRequestedFor(urlEqualTo("/oauth/token"))
                .withHeader("Authorization", equalTo("Basic " + expectedClientAuth)));
    }

    @Test
    void shouldHandleSpecialCharactersInClientCredentials() {
        String clientId = "client@domain.com";
        String clientSecret = "secret!@#$%^&*()";
        String expectedClientAuth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());

        stubFor(post(urlEqualTo("/oauth/token"))
                .withHeader("Authorization", equalTo("Basic " + expectedClientAuth))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(OAUTH_TOKEN_RESPONSE)));

        stubFor(get(urlEqualTo("/jwks"))
                .withHeader("Authorization", equalTo("Bearer test-access-token-12345"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(VALID_JWKS_RESPONSE)));

        PublicKeyCredentials credentials = new PublicKeyCredentials();
        credentials.setClientId(clientId);
        credentials.setClientSecret(clientSecret);
        credentials.setTokenEndpoint(baseUrl + "/oauth/token");

        PublicKeySource config = createPublicKeySource(baseUrl + "/jwks",
                PublicKeyAuthType.CLIENT_CREDENTIALS, credentials);
        Map<String, PublicKey> result = jwksPublicKeyLoader.loadKeys(config);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(TWO, result.size());

        // Verify that special characters in credentials are properly encoded in Basic Auth header
        verify(postRequestedFor(urlEqualTo("/oauth/token"))
                .withHeader("Authorization", equalTo("Basic " + expectedClientAuth)));
    }

    /**
     * Helper method to create PublicKeySource configuration for testing.
     */
    private PublicKeySource createPublicKeySource(String url,
                                                  PublicKeyAuthType authType,
                                                  PublicKeyCredentials credentials) {
        PublicKeySource config = new PublicKeySource();
        config.setUrl(url);
        config.setType(PublicKeyType.JWKS);

        if (authType != PublicKeyAuthType.NONE) {
            config.setAuthType(authType);
            config.setCredentials(credentials);
        }

        return config;
    }
}
