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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.model.PublicKeyAuthType;
import org.eclipse.ecsp.gateway.model.PublicKeyCredentials;
import org.eclipse.ecsp.gateway.model.PublicKeySource;
import org.eclipse.ecsp.gateway.model.PublicKeyType;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of PublicKeyLoader for JWKS (JSON Web Key Set) format keys.
 *
 * @author Abhishek Kumar
 */
@Component
public class JwksPublicKeyLoader implements PublicKeyLoader {
    private static final String AUTHORIZATION = "Authorization";
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(JwksPublicKeyLoader.class);
    private static final int THIRTY_SECONDS = 30;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient;

    /**
     * Constructor to initialize the JwksPublicKeyLoader with a WebClient.
     *
     * @param webClientBuilder the WebClient builder to create the WebClient instance
     */
    public JwksPublicKeyLoader(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Loads a public key from JWKS format source.
     *
     * @param config the public key source configuration
     * @return the loaded PublicKey
     */
    @Override
    public Map<String, PublicKey> loadKeys(PublicKeySource config) {
        if (config == null || config.getUrl() == null) {
            throw new IllegalArgumentException("PublicKeySource configuration or URL cannot be null");
        }

        Map<String, PublicKey> publicKeys = new HashMap<>();
        try {
            LOGGER.info("Fetching JWKS from URL: {}", config.getUrl());
            String jwksResponse = fetchJwksWithAuthentication(config);
            JWKSet jwkSet = JWKSet.parse(jwksResponse);
            List<JWK> jwkList = jwkSet.getKeys();

            if (!jwkList.isEmpty()) {
                for (JWK jwk : jwkList) {
                    processJwk(jwk, publicKeys);
                }
            }
            LOGGER.info("Successfully fetched {} key(s) {} from JWKS URL: {}", 
                                publicKeys.size(), publicKeys.keySet().toArray(), config.getUrl());
        } catch (Exception e) {
            LOGGER.error("Failed to load JWKS from URL: {}", config.getUrl(), e);
            return publicKeys; // Return empty map on failure
        }
        return publicKeys;
    }

    private static void processJwk(JWK jwk, Map<String, PublicKey> publicKeys) {
        try {
            PublicKey publicKey;
            if (jwk instanceof RSAKey rsaKey) {
                publicKey = rsaKey.toPublicKey();
            } else if (jwk instanceof ECKey ecKey) {
                publicKey = ecKey.toPublicKey();
            }  else {
                LOGGER.warn("Unsupported JWK type: {}. Only RSA and EC keys are supported.", jwk.getKeyType());
                return; // Skip processing this key
            }

            // Check if the public key is valid before adding it
            if (publicKey != null) {
                publicKeys.put(jwk.getKeyID(), publicKey);
                LOGGER.info("Processed public key with ID: {}", jwk.getKeyID());
            } else {
                LOGGER.warn("JWK with ID {} does not contain a valid public key", jwk.getKeyID());
            }
        } catch (Exception keyException) {
            LOGGER.error("Failed to process JWK with ID {}: {}",
                    jwk.getKeyID(),
                    keyException.getMessage(),
                    keyException);
            // Continue processing other keys even if one fails
        }
    }

    /**
     * Fetches JWKS response using WebClient with appropriate authentication.
     *
     * @param config the public key source configuration
     *
     * @return the JWKS response as string
     */
    private String fetchJwksWithAuthentication(PublicKeySource config) {
        WebClient.RequestHeadersSpec<?> requestSpec = webClient.get()
                .uri(config.getUrl())
                .accept(MediaType.APPLICATION_JSON);

        if (config.getAuthType() != null && config.getAuthType() != PublicKeyAuthType.NONE) {
            PublicKeyCredentials credentials = config.getCredentials();

            switch (config.getAuthType()) {
                case BASIC:
                    requestSpec = addBasicAuthentication(requestSpec, credentials);
                    break;
                case CLIENT_CREDENTIALS:
                    String accessToken = generateAccessToken(credentials);
                    requestSpec = requestSpec.header(AUTHORIZATION, "Bearer " + accessToken);
                    break;
                default:
                    // No authentication needed
                    break;
            }
        }

        LOGGER.debug("fetchJwksWithAuthentication - Fetching JWKS from URL: {}", config.getUrl());
        return requestSpec.retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(THIRTY_SECONDS))
                .block();
    }

    /**
     * Adds Basic Authentication header to the request.
     *
     * @param requestSpec the WebClient request spec
     * @param credentials the credentials containing username and password
     * @return the updated request spec
     */
    private WebClient.RequestHeadersSpec<?> addBasicAuthentication(
            WebClient.RequestHeadersSpec<?> requestSpec,
            PublicKeyCredentials credentials) {
        if (credentials == null || credentials.getUsername() == null
                || credentials.getPassword() == null
                || credentials.getUsername().trim().isEmpty()
                || credentials.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Username and password are required for Basic authentication");
        }

        String auth = credentials.getUsername() + ":" + credentials.getPassword();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        LOGGER.debug("Added Basic authentication header for user: {}", credentials.getUsername());
        return requestSpec.header(AUTHORIZATION, "Basic " + encodedAuth);
    }

    /**
     * Generates an access token using OAuth2 client credentials flow.
     *
     * @param credentials the credentials containing client ID, secret, scopes, and token endpoint
     * @return the access token
     */
    private String generateAccessToken(PublicKeyCredentials credentials) {
        if (credentials == null || StringUtils.isBlank(credentials.getClientId())
                || StringUtils.isBlank(credentials.getClientSecret())) {
            LOGGER.error("Client ID and client secret are required for client credentials authentication");
            throw new IllegalArgumentException(
                    "Client ID and client secret are required for client credentials authentication");
        }

        if (StringUtils.isBlank(credentials.getTokenEndpoint())) {
            LOGGER.error("Token endpoint is required for client credentials flow");
            throw new IllegalArgumentException("Token endpoint is required for client credentials flow");
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", credentials.getClientId());
        formData.add("client_secret", credentials.getClientSecret());

        if (credentials.getScopes() != null && !credentials.getScopes().isEmpty()) {
            formData.add("scope", String.join(" ", credentials.getScopes()));
        }

        try {
            String authValue = credentials.getClientId() + ":" + credentials.getClientSecret();
            String encodedAuth = Base64.getEncoder().encodeToString(authValue.getBytes(StandardCharsets.UTF_8));
            String responseBody = webClient.post()
                    .uri(credentials.getTokenEndpoint())
                    .header(AUTHORIZATION, "Basic " + encodedAuth)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(THIRTY_SECONDS))
                    .block();

            JsonNode response = objectMapper.readTree(responseBody);
            String accessToken = response.get("access_token").asText();

            LOGGER.info("Successfully generated access token for client: {}", credentials.getClientId());
            return accessToken;
        } catch (Exception e) {
            LOGGER.error("Failed to obtain access token from {}: {}", credentials.getTokenEndpoint(), e);
            throw new IllegalStateException("Failed to obtain access token: " + e.getMessage(), e);
        }
    }

    @Override
    public PublicKeyType getType() {
        return PublicKeyType.JWKS;
    }
}
