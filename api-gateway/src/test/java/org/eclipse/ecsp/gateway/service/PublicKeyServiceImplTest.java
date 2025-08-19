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

import io.micrometer.core.instrument.Timer;
import org.eclipse.ecsp.gateway.cache.PublicKeyCache;
import org.eclipse.ecsp.gateway.events.PublicKeyRefreshEvent;
import org.eclipse.ecsp.gateway.metrics.PublicKeyMetrics;
import org.eclipse.ecsp.gateway.model.PublicKeySource;
import org.eclipse.ecsp.gateway.model.PublicKeyType;
import org.eclipse.ecsp.gateway.plugins.keyloaders.PublicKeyLoader;
import org.eclipse.ecsp.gateway.plugins.keysources.PublicKeySourceProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PublicKeyServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class PublicKeyServiceImplTest {

    public static final int MINUTES = 5;
    public static final int KEYSIZE = 2048;
    @Mock
    private PublicKeySourceProvider sourceProvider;

    @Mock
    private PublicKeyLoader keyLoader;

    @Mock
    private PublicKeyCache publicKeyCache;

    @Mock
    private PublicKeyMetrics cacheMetrics;

    @Mock
    private Timer.Sample timerSample;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PublicKeyServiceImpl publicKeyService;
    private PublicKey testPublicKey;
    private PublicKeySource testKeySource;

    @BeforeEach
    void setUp() throws Exception {
        // Given
        // Create a test public key
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(KEYSIZE);
        KeyPair keyPair = keyGen.generateKeyPair();
        testPublicKey = keyPair.getPublic();

        // Setup test key source
        testKeySource = new PublicKeySource();
        testKeySource.setId("test-provider");
        testKeySource.setType(PublicKeyType.JWKS);
        testKeySource.setLocation("https://test.com/.well-known/jwks.json");
        testKeySource.setRefreshInterval(Duration.ofMinutes(MINUTES));
        testKeySource.setUseProviderPrefixedKey(true);

        when(keyLoader.getType()).thenReturn(PublicKeyType.JWKS);
        // Initialize service with mocks
        publicKeyService = new PublicKeyServiceImpl(
            List.of(sourceProvider),
            List.of(keyLoader),
            publicKeyCache,
            eventPublisher
        );
    }

    /**
     * Test successful initialization of the service.
     */
    @Test
    void initialize_whenValidConfiguration_thenSuccessfullyInitializes() {
        // Given
        Map<String, PublicKey> mockKeys = new HashMap<>();
        mockKeys.put("key1", testPublicKey);
        when(keyLoader.getType()).thenReturn(PublicKeyType.JWKS);
        when(sourceProvider.keySources()).thenReturn(List.of(testKeySource));
        when(keyLoader.loadKeys(any(PublicKeySource.class))).thenReturn(mockKeys);
        when(publicKeyCache.size()).thenReturn(1);

        // When
        publicKeyService.initialize();

        // Then
        verify(sourceProvider).keySources();
        verify(keyLoader).loadKeys(any(PublicKeySource.class));
        verify(publicKeyCache).clear();
        verify(publicKeyCache).put(anyString(), any(PublicKey.class));
    }

    /**
     * Test finding a public key by ID.
     */
    @Test
    void findPublicKey_whenKeyExists_thenReturnsKey() {
        // Given
        String keyId = "test-key";
        when(publicKeyCache.get(keyId)).thenReturn(Optional.of(testPublicKey));

        // When
        Optional<PublicKey> result = publicKeyService.findPublicKey(keyId, null);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testPublicKey, result.get());
        verify(publicKeyCache).get(keyId);
    }

    /**
     * Test finding a public key with provider prefix.
     */
    @Test
    void findPublicKey_whenKeyExistsWithProvider_thenReturnsKey() {
        // Given
        String keyId = "test-key";
        String provider = "test-provider";
        String prefixedKey = provider + "_" + keyId;
        
        when(publicKeyCache.get(keyId)).thenReturn(Optional.empty());
        when(publicKeyCache.get(prefixedKey)).thenReturn(Optional.of(testPublicKey));

        // When
        Optional<PublicKey> result = publicKeyService.findPublicKey(keyId, provider);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testPublicKey, result.get());
        verify(publicKeyCache).get(keyId);
        verify(publicKeyCache).get(prefixedKey);
    }

    /**
     * Test finding a non-existent public key.
     */
    @Test
    void findPublicKey_whenKeyDoesNotExist_thenReturnsEmpty() {
        // Given
        String keyId = "non-existent-key";
        when(publicKeyCache.get(anyString())).thenReturn(Optional.empty());

        // When
        Optional<PublicKey> result = publicKeyService.findPublicKey(keyId, null);

        // Then
        assertFalse(result.isPresent());
        verify(publicKeyCache).get(keyId);
    }

    /**
     * Test refreshing public keys.
     */
    @Test
    void refreshPublicKeys_whenCalled_thenRefreshesAllKeys() {
        // Given
        Map<String, PublicKey> mockKeys = new HashMap<>();
        mockKeys.put("key1", testPublicKey);
        doReturn(PublicKeyType.JWKS).when(keyLoader).getType();
        when(sourceProvider.keySources()).thenReturn(List.of(testKeySource));
        when(keyLoader.loadKeys(any(PublicKeySource.class))).thenReturn(mockKeys);

        // When
        publicKeyService.refreshPublicKeys();

        // Then
        verify(publicKeyCache).clear();
        verify(sourceProvider).keySources();
        verify(keyLoader).loadKeys(any(PublicKeySource.class));
    }

    /**
     * Test handling empty key sources.
     */
    @Test
    void refreshPublicKeys_whenNoKeySources_thenHandlesGracefully() {
        // Given
        when(sourceProvider.keySources()).thenReturn(Collections.emptyList());

        // When
        publicKeyService.refreshPublicKeys();

        // Then
        verify(publicKeyCache).clear();
        verify(sourceProvider).keySources();
        verify(keyLoader, never()).loadKeys(any(PublicKeySource.class));
    }

    /**
     * Test handling null key ID.
     */
    @Test
    void findPublicKey_whenKeyIdIsNull_thenReturnsEmpty() {
        // When
        Optional<PublicKey> result = publicKeyService.findPublicKey(null, "provider");

        // Then
        assertFalse(result.isPresent());
        verify(publicKeyCache, never()).get(anyString());
    }

    /**
     * Test handling null provider with existing key.
     */
    @Test
    void findPublicKey_whenProviderIsNull_thenSearchesWithoutPrefix() {
        // Given
        String keyId = "test-key";
        when(publicKeyCache.get(keyId)).thenReturn(Optional.of(testPublicKey));

        // When
        Optional<PublicKey> result = publicKeyService.findPublicKey(keyId, null);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testPublicKey, result.get());
        verify(publicKeyCache).get(keyId);
        verify(publicKeyCache, never()).get(contains("_"));
    }

    /**
     * Test cache key generation with provider prefix.
     * Verifies that cache keys are generated correctly when provider prefix is enabled.
     */
    @Test
    void generateCacheKey_whenUseProviderPrefixedKeyEnabled_thenGeneratesPrefixedKey() {
        // Given
        PublicKeySource source = new PublicKeySource();
        source.setId("test-provider");
        source.setUseProviderPrefixedKey(true);
        String keyId = "test-key-123";

        Map<String, PublicKey> mockKeys = new HashMap<>();
        mockKeys.put(keyId, testPublicKey);

        when(keyLoader.getType()).thenReturn(PublicKeyType.JWKS);
        when(sourceProvider.keySources()).thenReturn(List.of(source));
        when(keyLoader.loadKeys(any(PublicKeySource.class))).thenReturn(mockKeys);

        // When
        publicKeyService.refreshPublicKeys();

        // Then
        verify(publicKeyCache).put("test-provider_test-key-123", testPublicKey);
    }

    /**
     * Test cache key generation without provider prefix.
     * Verifies that cache keys use original key ID when provider prefix is disabled.
     */
    @Test
    void generateCacheKey_whenUseProviderPrefixedKeyDisabled_thenUsesOriginalKeyId() {
        // Given
        PublicKeySource source = new PublicKeySource();
        source.setId("test-provider");
        source.setUseProviderPrefixedKey(false);
        String keyId = "test-key-456";

        Map<String, PublicKey> mockKeys = new HashMap<>();
        mockKeys.put(keyId, testPublicKey);

        when(keyLoader.getType()).thenReturn(PublicKeyType.JWKS);
        when(sourceProvider.keySources()).thenReturn(List.of(source));
        when(keyLoader.loadKeys(any(PublicKeySource.class))).thenReturn(mockKeys);

        // When
        publicKeyService.refreshPublicKeys();

        // Then
        verify(publicKeyCache).put("test-key-456", testPublicKey);
    }

    /**
     * Test handling of null and empty key IDs in cache key generation.
     * Verifies proper handling of edge cases in key generation.
     */
    @Test
    void loadPublicKeys_whenNullOrEmptyKeyId_thenSkipsKey() {
        // Given
        PublicKeySource source = new PublicKeySource();
        source.setId("test-provider");
        source.setUseProviderPrefixedKey(true);

        Map<String, PublicKey> mockKeys = new HashMap<>();
        mockKeys.put(null, testPublicKey);  // null key ID
        mockKeys.put("", testPublicKey);    // empty key ID
        mockKeys.put("valid-key", testPublicKey); // valid key

        when(keyLoader.getType()).thenReturn(PublicKeyType.JWKS);
        when(sourceProvider.keySources()).thenReturn(List.of(source));
        when(keyLoader.loadKeys(any(PublicKeySource.class))).thenReturn(mockKeys);

        // When
        publicKeyService.refreshPublicKeys();

        // Then
        // Only the valid key should be cached
        verify(publicKeyCache).put("test-provider_valid-key", testPublicKey);
        verify(publicKeyCache, never()).put(eq(null), any(PublicKey.class));
        verify(publicKeyCache, never()).put(eq(""), any(PublicKey.class));
        verify(publicKeyCache, never()).put(eq("test-provider_"), any(PublicKey.class));
    }

    /**
     * Test initialization with metrics integration.
     * Verifies that service initialization works correctly with metrics.
     */
    @Test
    void initialize_whenCalled_thenInitializesWithMetrics() {
        // Given
        Map<String, PublicKey> mockKeys = new HashMap<>();
        mockKeys.put("key1", testPublicKey);
        when(keyLoader.getType()).thenReturn(PublicKeyType.JWKS);
        when(sourceProvider.keySources()).thenReturn(List.of(testKeySource));
        when(keyLoader.loadKeys(any(PublicKeySource.class))).thenReturn(mockKeys);
        when(publicKeyCache.size()).thenReturn(1);

        // When
        publicKeyService.initialize();

        // Then
        verify(sourceProvider).keySources();
        verify(keyLoader).loadKeys(any(PublicKeySource.class));
        verify(publicKeyCache).clear();
        verify(publicKeyCache).put(anyString(), any(PublicKey.class));
        verify(publicKeyCache, atLeast(1)).size();
    }
}
