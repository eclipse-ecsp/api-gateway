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
import org.eclipse.ecsp.gateway.config.JwtProperties;
import org.eclipse.ecsp.gateway.events.PublicKeyRefreshEvent;
import org.eclipse.ecsp.gateway.metrics.PublicKeyMetrics;
import org.eclipse.ecsp.gateway.model.PublicKeyInfo;
import org.eclipse.ecsp.gateway.model.PublicKeySource;
import org.eclipse.ecsp.gateway.model.PublicKeyType;
import org.eclipse.ecsp.gateway.plugins.keyloaders.PublicKeyLoader;
import org.eclipse.ecsp.gateway.plugins.keysources.PublicKeySourceProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
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
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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
    private static final int TWO = 2;
    private static final long TEST_INTERVAL_MILLIS = 300000L; // 5 minutes in milliseconds
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
    private JwtProperties jwtProperties;

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
        jwtProperties = new JwtProperties();
        // Initialize service with mocks
        publicKeyService = new PublicKeyServiceImpl(
                List.of(sourceProvider),
                List.of(keyLoader),
                publicKeyCache,
                eventPublisher,
                jwtProperties);
    }

    /**
     * Test successful initialization of the service.
     */
    @Test
    void initializeWhenValidConfigurationThenSuccessfullyInitializes() {
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
        verify(publicKeyCache).put(anyString(), any(PublicKeyInfo.class));
    }

    /**
     * Test finding a public key by ID.
     */
    @Test
    void findPublicKeyWhenKeyExistsThenReturnsKey() {
        // Given
        String keyId = "test-key";
        PublicKeyInfo testPublicKeyInfo = new PublicKeyInfo();
        testPublicKeyInfo.setKid(keyId);
        testPublicKeyInfo.setPublicKey(testPublicKey);
        testPublicKeyInfo.setSourceId("test");

        when(publicKeyCache.get(keyId)).thenReturn(Optional.of(testPublicKeyInfo));

        // When
        Optional<PublicKeyInfo> result = publicKeyService.findPublicKey(keyId, null);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testPublicKey, result.get().getPublicKey());
        verify(publicKeyCache).get(keyId);
    }

    /**
     * Test finding a public key with provider prefix.
     */
    @Test
    void findPublicKeyWhenKeyExistsWithProviderThenReturnsKey() {
        // Given
        String keyId = "test-key";
        String provider = "test-provider";
        String prefixedKey = provider + "_" + keyId;

        PublicKeyInfo testPublicKeyInfo = new PublicKeyInfo();
        testPublicKeyInfo.setKid(prefixedKey);
        testPublicKeyInfo.setPublicKey(testPublicKey);
        when(publicKeyCache.get(keyId)).thenReturn(Optional.empty());
        when(publicKeyCache.get(prefixedKey)).thenReturn(Optional.of(testPublicKeyInfo));

        // When
        Optional<PublicKeyInfo> result = publicKeyService.findPublicKey(keyId, provider);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testPublicKey, result.get().getPublicKey());
        verify(publicKeyCache).get(keyId);
        verify(publicKeyCache).get(prefixedKey);
    }

    /**
     * Test finding a non-existent public key.
     */
    @Test
    void findPublicKeyWhenKeyDoesNotExistThenReturnsEmpty() {
        // Given
        String keyId = "non-existent-key";
        when(publicKeyCache.get(anyString())).thenReturn(Optional.empty());

        // When
        Optional<PublicKeyInfo> result = publicKeyService.findPublicKey(keyId, null);

        // Then
        assertFalse(result.isPresent());
        verify(publicKeyCache).get(keyId);
    }

    /**
     * Test refreshing public keys.
     */
    @Test
    void refreshPublicKeysWhenCalledThenRefreshesAllKeys() {
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
    void refreshPublicKeysWhenNoKeySourcesThenHandlesGracefully() {
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
    void findPublicKeyWhenKeyIdIsNullThenReturnsEmpty() {
        // When
        Optional<PublicKeyInfo> result = publicKeyService.findPublicKey(null, "provider");

        // Then
        assertFalse(result.isPresent());
        verify(publicKeyCache, never()).get(anyString());
    }

    /**
     * Test handling null provider with existing key.
     */
    @Test
    void findPublicKeyWhenProviderIsNullThenSearchesWithoutPrefix() {
        // Given
        String keyId = "test-key";
        PublicKeyInfo testPublicKeyInfo = new PublicKeyInfo();
        testPublicKeyInfo.setKid(keyId);
        testPublicKeyInfo.setPublicKey(testPublicKey);
        testPublicKeyInfo.setSourceId("test");
        when(publicKeyCache.get(keyId)).thenReturn(Optional.of(testPublicKeyInfo));

        // When
        Optional<PublicKeyInfo> result = publicKeyService.findPublicKey(keyId, null);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testPublicKey, result.get().getPublicKey());
        verify(publicKeyCache).get(keyId);
        verify(publicKeyCache, never()).get(contains("_"));
    }

    /**
     * Test cache key generation with provider prefix.
     * Verifies that cache keys are generated correctly when provider prefix is
     * enabled.
     */
    @Test
    void generateCacheKeyWhenUseProviderPrefixedKeyEnabledThenGeneratesPrefixedKey() {
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
        PublicKeyInfo publicKeyInfo = new PublicKeyInfo();
        publicKeyInfo.setKid("test-provider_test-key-123");
        publicKeyInfo.setSourceId("test-provider");
        publicKeyInfo.setPublicKey(testPublicKey);

        verify(publicKeyCache).put(eq("test-provider_test-key-123"), any(PublicKeyInfo.class));
    }

    /**
     * Test cache key generation without provider prefix.
     * Verifies that cache keys use original key ID when provider prefix is
     * disabled.
     */
    @Test
    void generateCacheKeyWhenUseProviderPrefixedKeyDisabledThenUsesOriginalKeyId() {
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
        verify(publicKeyCache).put(eq("test-key-456"), any(PublicKeyInfo.class));
    }

    /**
     * Test handling of null and empty key IDs in cache key generation.
     * Verifies proper handling of edge cases in key generation.
     */
    @Test
    void loadPublicKeysWhenNullOrEmptyKeyIdThenSkipsKey() {
        // Given
        PublicKeySource source = new PublicKeySource();
        source.setId("test-provider");
        source.setUseProviderPrefixedKey(true);

        Map<String, PublicKey> mockKeys = new HashMap<>();
        mockKeys.put(null, testPublicKey); // null key ID
        mockKeys.put("", testPublicKey); // empty key ID
        mockKeys.put("valid-key", testPublicKey); // valid key

        when(keyLoader.getType()).thenReturn(PublicKeyType.JWKS);
        when(sourceProvider.keySources()).thenReturn(List.of(source));
        when(keyLoader.loadKeys(any(PublicKeySource.class))).thenReturn(mockKeys);

        // When
        publicKeyService.refreshPublicKeys();

        // Then
        // Only the valid key should be cached
        PublicKeyInfo publicKeyInfo = new PublicKeyInfo();
        publicKeyInfo.setKid("valid-key");
        publicKeyInfo.setSourceId("test-provider");
        publicKeyInfo.setPublicKey(testPublicKey);

        verify(publicKeyCache).put(eq("test-provider_valid-key"), any(PublicKeyInfo.class));
        verify(publicKeyCache, never()).put(eq(null), any(PublicKeyInfo.class));
        verify(publicKeyCache, never()).put(eq(""), any(PublicKeyInfo.class));
        verify(publicKeyCache, never()).put(eq("test-provider_"), any(PublicKeyInfo.class));
    }

    /**
     * Test initialization with metrics integration.
     * Verifies that service initialization works correctly with metrics.
     */
    @Test
    void initializeWhenCalledThenInitializesWithMetrics() {
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
        verify(publicKeyCache).put(anyString(), any(PublicKeyInfo.class));
        verify(publicKeyCache, atLeast(1)).size();
    }

    /**
     * Test removePublicKeysBySourceId with existing keys.
     * Verifies that keys belonging to specific source are removed correctly.
     */
    @Test
    void removePublicKeysBySourceIdWhenKeysExistForSourceThenRemovesCorrectKeys() {
        // Given
        final String sourceId1 = "source1";
        final String sourceId2 = "source2";

        PublicKeyInfo keyInfo1 = new PublicKeyInfo();
        keyInfo1.setKid("key1");
        keyInfo1.setSourceId(sourceId1);
        keyInfo1.setPublicKey(testPublicKey);
        keyInfo1.setType(PublicKeyType.JWKS);

        PublicKeyInfo keyInfo2 = new PublicKeyInfo();
        keyInfo2.setKid("key2");
        keyInfo2.setSourceId(sourceId1);
        keyInfo2.setPublicKey(testPublicKey);
        keyInfo2.setType(PublicKeyType.JWKS);

        PublicKeyInfo keyInfo3 = new PublicKeyInfo();
        keyInfo3.setKid("key3");
        keyInfo3.setSourceId(sourceId2);
        keyInfo3.setPublicKey(testPublicKey);
        keyInfo3.setType(PublicKeyType.JWKS);

        // Create entry set mock
        Set<Map.Entry<String, PublicKeyInfo>> entrySet = Set.of(
                Map.entry("key1", keyInfo1),
                Map.entry("key2", keyInfo2),
                Map.entry("key3", keyInfo3)
        );

        when(publicKeyCache.entrySet()).thenReturn(entrySet);

        // When - use reflection to call private method
        try {
            java.lang.reflect.Method method = PublicKeyServiceImpl.class
                    .getDeclaredMethod("removePublicKeysBySourceId", String.class);
            method.setAccessible(true);
            int result = (int) method.invoke(publicKeyService, sourceId1);

            // Then
            assertEquals(TWO, result);
            // Verify that entrySet() was called to get entries for filtering
            verify(publicKeyCache).entrySet();
            // Verify that the predicate-based remove method was called  
            verify(publicKeyCache).remove(
                org.mockito.ArgumentMatchers.<Predicate<Map.Entry<String, PublicKeyInfo>>>any());
        } catch (Exception e) {
            throw new RuntimeException("Failed to test removePublicKeysBySourceId", e);
        }
    }

    /**
     * Test removePublicKeysBySourceId with no matching keys.
     * Verifies that method returns 0 when no keys match the source ID.
     */
    @Test
    void removePublicKeysBySourceIdWhenNoKeysMatchSourceThenReturnsZero() {
        // Given
        String sourceId = "non-existent-source";
        String existingSourceId = "existing-source";

        PublicKeyInfo keyInfo = new PublicKeyInfo();
        keyInfo.setKid("key1");
        keyInfo.setSourceId(existingSourceId);
        keyInfo.setPublicKey(testPublicKey);

        Set<Map.Entry<String, PublicKeyInfo>> entrySet = Set.of(
                Map.entry("key1", keyInfo)
        );

        when(publicKeyCache.entrySet()).thenReturn(entrySet);

        // When - use reflection to call private method
        try {
            java.lang.reflect.Method method = PublicKeyServiceImpl.class
                    .getDeclaredMethod("removePublicKeysBySourceId", String.class);
            method.setAccessible(true);
            int result = (int) method.invoke(publicKeyService, sourceId);

            // Then
            assertEquals(0, result);
            verify(publicKeyCache, never()).remove(anyString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to test removePublicKeysBySourceId", e);
        }
    }

    /**
     * Test removePublicKeysBySourceId with null source ID.
     * Verifies that method handles null source ID gracefully.
     */
    @Test
    void removePublicKeysBySourceIdWhenSourceIdIsNullThenReturnsZero() {
        // When - use reflection to call private method
        try {
            java.lang.reflect.Method method = PublicKeyServiceImpl.class
                    .getDeclaredMethod("removePublicKeysBySourceId", String.class);
            method.setAccessible(true);
            int result = (int) method.invoke(publicKeyService, (String) null);

            // Then
            assertEquals(0, result);
            verify(publicKeyCache, never()).entrySet();
            verify(publicKeyCache, never()).remove(anyString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to test removePublicKeysBySourceId", e);
        }
    }

    /**
     * Test removePublicKeysBySourceId with empty source ID.
     * Verifies that method handles empty source ID gracefully.
     */
    @Test
    void removePublicKeysBySourceIdWhenSourceIdIsEmptyThenReturnsZero() {
        // When - use reflection to call private method
        try {
            java.lang.reflect.Method method = PublicKeyServiceImpl.class
                    .getDeclaredMethod("removePublicKeysBySourceId", String.class);
            method.setAccessible(true);
            int result = (int) method.invoke(publicKeyService, "");

            // Then
            assertEquals(0, result);
            verify(publicKeyCache, never()).entrySet();
            verify(publicKeyCache, never()).remove(anyString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to test removePublicKeysBySourceId", e);
        }
    }

    /**
     * Test removePublicKeysBySourceId with whitespace-only source ID.
     * Verifies that method handles whitespace-only source ID gracefully.
     */
    @Test
    void removePublicKeysBySourceIdWhenSourceIdIsWhitespaceThenReturnsZero() {
        // When - use reflection to call private method
        try {
            java.lang.reflect.Method method = PublicKeyServiceImpl.class
                    .getDeclaredMethod("removePublicKeysBySourceId", String.class);
            method.setAccessible(true);
            int result = (int) method.invoke(publicKeyService, "   ");

            // Then
            assertEquals(0, result);
            verify(publicKeyCache, never()).entrySet();
            verify(publicKeyCache, never()).remove(anyString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to test removePublicKeysBySourceId", e);
        }
    }

    /**
     * Test removePublicKeysBySourceId with empty cache.
     * Verifies that method handles empty cache gracefully.
     */
    @Test
    void removePublicKeysBySourceIdWhenCacheIsEmptyThenReturnsZero() {
        // Given
        String sourceId = "test-source";
        when(publicKeyCache.entrySet()).thenReturn(Collections.emptySet());

        // When - use reflection to call private method
        try {
            java.lang.reflect.Method method = PublicKeyServiceImpl.class
                    .getDeclaredMethod("removePublicKeysBySourceId", String.class);
            method.setAccessible(true);
            int result = (int) method.invoke(publicKeyService, sourceId);

            // Then
            assertEquals(0, result);
            verify(publicKeyCache).entrySet();
            verify(publicKeyCache, never()).remove(anyString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to test removePublicKeysBySourceId", e);
        }
    }

    /**
     * Test scheduleJwksRefresh method scheduling.
     * Verifies that JWKS refresh is scheduled correctly with proper interval.
     */
    @Test
    void scheduleJwksRefreshWhenCalledThenSchedulesRefreshTask() {
        // Given
        PublicKeySource source = new PublicKeySource();
        source.setId("test-jwks-source");
        source.setType(PublicKeyType.JWKS);
        source.setRefreshInterval(Duration.ofMinutes(MINUTES));

        // Create a mock ScheduledExecutorService using reflection
        try {
            // Get the threadPoolExecutor field
            java.lang.reflect.Field executorField = PublicKeyServiceImpl.class
                    .getDeclaredField("threadPoolExecutor");
            executorField.setAccessible(true);

            // Create a mock executor
            ScheduledExecutorService mockExecutor = mock(ScheduledExecutorService.class);

            // Replace the real executor with mock
            executorField.set(publicKeyService, mockExecutor);

            // Get the scheduleJwksRefresh method
            java.lang.reflect.Method method = PublicKeyServiceImpl.class
                    .getDeclaredMethod("scheduleJwksRefresh", PublicKeySource.class, PublicKeyLoader.class);
            method.setAccessible(true);

            // When
            method.invoke(publicKeyService, source, keyLoader);

            // Then
            verify(mockExecutor).scheduleAtFixedRate(
                    any(Runnable.class),
                    eq(TEST_INTERVAL_MILLIS),
                    eq(TEST_INTERVAL_MILLIS),
                    eq(TimeUnit.MILLISECONDS)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to test scheduleJwksRefresh", e);
        }
    }

    /**
     * Test scheduleJwksRefresh task execution.
     * Verifies that the scheduled task executes refresh logic correctly.
     */
    @Test
    void scheduleJwksRefreshWhenTaskExecutesThenRefreshesKeys() {
        // Given
        PublicKeySource source = new PublicKeySource();
        source.setId("test-jwks-source");
        source.setType(PublicKeyType.JWKS);
        source.setRefreshInterval(Duration.ofMinutes(MINUTES));

        Map<String, PublicKey> mockKeys = new HashMap<>();
        mockKeys.put("refreshed-key", testPublicKey);
        when(keyLoader.loadKeys(any(PublicKeySource.class))).thenReturn(mockKeys);

        // Mock cache entrySet for removePublicKeysBySourceId
        PublicKeyInfo existingKeyInfo = new PublicKeyInfo();
        existingKeyInfo.setKid("old-key");
        existingKeyInfo.setSourceId("test-jwks-source");
        existingKeyInfo.setPublicKey(testPublicKey);
        existingKeyInfo.setType(PublicKeyType.JWKS);

        Set<Map.Entry<String, PublicKeyInfo>> entrySet = Set.of(
                Map.entry("old-key", existingKeyInfo)
        );
        when(publicKeyCache.entrySet()).thenReturn(entrySet);

        try {
            // Get the threadPoolExecutor field and replace with mock
            java.lang.reflect.Field executorField = PublicKeyServiceImpl.class
                    .getDeclaredField("threadPoolExecutor");
            executorField.setAccessible(true);

            ScheduledExecutorService mockExecutor = mock(ScheduledExecutorService.class);

            // Capture the runnable and execute it immediately
            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run(); // Execute the task immediately
                return mock(ScheduledFuture.class);
            }).when(mockExecutor).scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));

            executorField.set(publicKeyService, mockExecutor);

            // Get the scheduleJwksRefresh method
            java.lang.reflect.Method method = PublicKeyServiceImpl.class
                    .getDeclaredMethod("scheduleJwksRefresh", PublicKeySource.class, PublicKeyLoader.class);
            method.setAccessible(true);

            // When
            method.invoke(publicKeyService, source, keyLoader);

            // Then
            // Verify that entrySet() was called to get entries for filtering
            verify(publicKeyCache).entrySet();
            // Verify that the predicate-based remove method was called
            verify(publicKeyCache).remove(
                org.mockito.ArgumentMatchers.<Predicate<Map.Entry<String, PublicKeyInfo>>>any());
            verify(keyLoader).loadKeys(source); // loadPublicKeys was called
            // Note: Event publishing is tested indirectly through successful key refresh
        } catch (Exception e) {
            throw new RuntimeException("Failed to test scheduleJwksRefresh task execution", e);
        }
    }

    /**
     * Test scheduleJwksRefresh task execution with exception.
     * Verifies that exceptions during refresh are handled properly.
     */
    @Test
    void scheduleJwksRefreshWhenTaskThrowsExceptionThenHandlesGracefully() {
        // Given
        PublicKeySource source = new PublicKeySource();
        source.setId("test-jwks-source");
        source.setType(PublicKeyType.JWKS);
        source.setRefreshInterval(Duration.ofMinutes(MINUTES));

        when(keyLoader.loadKeys(any(PublicKeySource.class)))
                .thenThrow(new RuntimeException("JWKS fetch failed"));

        try {
            // Get the threadPoolExecutor field and replace with mock
            java.lang.reflect.Field executorField = PublicKeyServiceImpl.class
                    .getDeclaredField("threadPoolExecutor");
            executorField.setAccessible(true);

            ScheduledExecutorService mockExecutor = mock(ScheduledExecutorService.class);

            // Capture the runnable and execute it immediately
            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run(); // Execute the task immediately
                return mock(ScheduledFuture.class);
            }).when(mockExecutor).scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));

            executorField.set(publicKeyService, mockExecutor);

            // Get the scheduleJwksRefresh method
            java.lang.reflect.Method method = PublicKeyServiceImpl.class
                    .getDeclaredMethod("scheduleJwksRefresh", PublicKeySource.class, PublicKeyLoader.class);
            method.setAccessible(true);

            // When
            method.invoke(publicKeyService, source, keyLoader);

            // Then - should not throw exception and should handle error gracefully
            verify(keyLoader).loadKeys(source);
            // Note: Exception handling is verified by successful test completion without throwing
        } catch (Exception e) {
            throw new RuntimeException("Failed to test scheduleJwksRefresh exception handling", e);
        }
    }

    /**
     * Test scheduleJwksRefresh with multiple sources.
     * Verifies that multiple JWKS sources can be scheduled independently.
     */
    @Test
    void scheduleJwksRefreshWhenMultipleSourcesThenSchedulesIndependently() {
        // Given
        PublicKeySource source1 = new PublicKeySource();
        source1.setId("jwks-source-1");
        source1.setType(PublicKeyType.JWKS);
        source1.setRefreshInterval(Duration.ofMinutes(MINUTES));

        PublicKeySource source2 = new PublicKeySource();
        source2.setId("jwks-source-2");
        source2.setType(PublicKeyType.JWKS);
        source2.setRefreshInterval(Duration.ofMinutes(MINUTES * TWO)); // Different interval

        try {
            // Get the threadPoolExecutor field and replace with mock
            java.lang.reflect.Field executorField = PublicKeyServiceImpl.class
                    .getDeclaredField("threadPoolExecutor");
            executorField.setAccessible(true);

            ScheduledExecutorService mockExecutor = mock(ScheduledExecutorService.class);

            executorField.set(publicKeyService, mockExecutor);

            // Get the scheduleJwksRefresh method
            java.lang.reflect.Method method = PublicKeyServiceImpl.class
                    .getDeclaredMethod("scheduleJwksRefresh", PublicKeySource.class, PublicKeyLoader.class);
            method.setAccessible(true);

            // When
            method.invoke(publicKeyService, source1, keyLoader);
            method.invoke(publicKeyService, source2, keyLoader);

            // Then
            verify(mockExecutor).scheduleAtFixedRate(
                    any(Runnable.class),
                    eq(TEST_INTERVAL_MILLIS),
                    eq(TEST_INTERVAL_MILLIS),
                    eq(TimeUnit.MILLISECONDS)
            );
            verify(mockExecutor).scheduleAtFixedRate(
                    any(Runnable.class),
                    eq(TEST_INTERVAL_MILLIS * TWO),
                    eq(TEST_INTERVAL_MILLIS * TWO),
                    eq(TimeUnit.MILLISECONDS)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to test scheduleJwksRefresh with multiple sources", e);
        }
    }

    /**
     * Test removePublicKeysBySourceId with mixed source IDs.
     * Verifies that only keys from the specified source are removed when multiple sources exist.
     */
    @Test
    void removePublicKeysBySourceIdWhenMixedSourcesThenRemovesOnlyTargetSource() {
        // Given
        final String targetSourceId = "target-source";
        final String otherSourceId1 = "other-source-1";
        final String otherSourceId2 = "other-source-2";

        PublicKeyInfo targetKey1 = new PublicKeyInfo();
        targetKey1.setKid("target-key-1");
        targetKey1.setSourceId(targetSourceId);
        targetKey1.setPublicKey(testPublicKey);
        targetKey1.setType(PublicKeyType.JWKS);

        PublicKeyInfo targetKey2 = new PublicKeyInfo();
        targetKey2.setKid("target-key-2");
        targetKey2.setSourceId(targetSourceId);
        targetKey2.setPublicKey(testPublicKey);
        targetKey2.setType(PublicKeyType.JWKS);

        PublicKeyInfo otherKey1 = new PublicKeyInfo();
        otherKey1.setKid("other-key-1");
        otherKey1.setSourceId(otherSourceId1);
        otherKey1.setPublicKey(testPublicKey);
        otherKey1.setType(PublicKeyType.JWKS);

        PublicKeyInfo otherKey2 = new PublicKeyInfo();
        otherKey2.setKid("other-key-2");
        otherKey2.setSourceId(otherSourceId2);
        otherKey2.setPublicKey(testPublicKey);
        otherKey2.setType(PublicKeyType.JWKS);

        Set<Map.Entry<String, PublicKeyInfo>> entrySet = Set.of(
                Map.entry("target-key-1", targetKey1),
                Map.entry("target-key-2", targetKey2),
                Map.entry("other-key-1", otherKey1),
                Map.entry("other-key-2", otherKey2)
        );

        when(publicKeyCache.entrySet()).thenReturn(entrySet);

        // When - use reflection to call private method
        try {
            java.lang.reflect.Method method = PublicKeyServiceImpl.class
                    .getDeclaredMethod("removePublicKeysBySourceId", String.class);
            method.setAccessible(true);
            int result = (int) method.invoke(publicKeyService, targetSourceId);

            // Then
            assertEquals(TWO, result);
            // Verify that entrySet() was called to get entries for filtering
            verify(publicKeyCache).entrySet();
            // Verify that the predicate-based remove method was called
            verify(publicKeyCache).remove(
                org.mockito.ArgumentMatchers.<Predicate<Map.Entry<String, PublicKeyInfo>>>any());
        } catch (Exception e) {
            throw new RuntimeException("Failed to test removePublicKeysBySourceId with mixed sources", e);
        }
    }

    /**
     * Test the default implementation of refreshPublicKeys in PublicKeyService.
     */
    @Test
    void defaultRefreshPublicKeysInInterfaceReturnsFalse() {
        PublicKeyService defaultService = new PublicKeyService() {
            @Override
            public Optional<PublicKeyInfo> findPublicKey(String keyId, String provider) {
                return Optional.empty();
            }

            @Override
            public void refreshPublicKeys() {
            }
        };
        assertFalse(defaultService.refreshPublicKeys("test-issuer"));
    }

    /**
     * Test refreshPublicKeys(issuer) when JWKS is disabled in properties.
     */
    @Test
    void refreshPublicKeysWhenJwksDisabledThenReturnsFalse() {
        jwtProperties.getJwks().setEnabled(false);
        boolean result = publicKeyService.refreshPublicKeys("test-issuer");
        assertFalse(result);
        verify(eventPublisher, never()).publishEvent(any());
    }

    /**
     * Test refreshPublicKeys(issuer) when successful.
     */
    @Test
    void refreshPublicKeysWhenSuccessfulThenLoadsKeysAndPublishesSuccessEvent() {
        testKeySource.setIssuer("https://auth.example.com");
        when(sourceProvider.keySources()).thenReturn(List.of(testKeySource));
        when(keyLoader.loadKeys(testKeySource)).thenReturn(Map.of("new-kid", testPublicKey));
        when(publicKeyCache.entrySet()).thenReturn(Collections.emptySet());

        boolean result = publicKeyService.refreshPublicKeys("https://auth.example.com");

        assertTrue(result);
        ArgumentCaptor<PublicKeyRefreshEvent> captor = ArgumentCaptor.forClass(PublicKeyRefreshEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        PublicKeyRefreshEvent event = captor.getValue();
        assertEquals(PublicKeyRefreshEvent.RefreshType.PUBLIC_KEY, event.getRefreshType());
        assertEquals("test-provider", event.getSourceId());
        assertEquals(PublicKeyRefreshEvent.Trigger.UNKNOWN_KID, event.getTrigger());
        assertEquals("success", event.getOutcome());
    }

    /**
     * Test refreshPublicKeys(null) matches any JWKS source regardless of issuer.
     */
    @Test
    void refreshPublicKeysWhenIssuerIsNullThenMatchesAnyJwksSource() {
        testKeySource.setIssuer("https://auth.example.com");
        when(sourceProvider.keySources()).thenReturn(List.of(testKeySource));
        when(keyLoader.loadKeys(testKeySource)).thenReturn(Map.of("new-kid", testPublicKey));
        when(publicKeyCache.entrySet()).thenReturn(Collections.emptySet());

        boolean result = publicKeyService.refreshPublicKeys(null);

        assertTrue(result);
        ArgumentCaptor<PublicKeyRefreshEvent> captor = ArgumentCaptor.forClass(PublicKeyRefreshEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("success", captor.getValue().getOutcome());
    }

    /**
     * Test refreshPublicKeys(issuer) cooldown suppression on rapid subsequent calls.
     */
    @Test
    void refreshPublicKeysWhenCalledWithinCooldownThenSuppressed() {
        testKeySource.setIssuer("https://auth.example.com");
        when(sourceProvider.keySources()).thenReturn(List.of(testKeySource));
        when(keyLoader.loadKeys(testKeySource)).thenReturn(Map.of("new-kid", testPublicKey));
        when(publicKeyCache.entrySet()).thenReturn(Collections.emptySet());

        // First call succeeds
        boolean first = publicKeyService.refreshPublicKeys("https://auth.example.com");
        assertTrue(first);

        // Immediate second call is suppressed by cooldown
        boolean second = publicKeyService.refreshPublicKeys("https://auth.example.com");
        assertFalse(second);

        ArgumentCaptor<PublicKeyRefreshEvent> captor = ArgumentCaptor.forClass(PublicKeyRefreshEvent.class);
        verify(eventPublisher, atLeast(TWO)).publishEvent(captor.capture());
        List<PublicKeyRefreshEvent> events = captor.getAllValues();
        assertEquals("success", events.get(0).getOutcome());
        assertEquals("suppressed", events.get(1).getOutcome());
    }

    /**
     * Test refreshPublicKeys(issuer) when key loader returns empty map.
     */
    @Test
    void refreshPublicKeysWhenKeyLoaderReturnsEmptyThenPublishesFailureEvent() {
        testKeySource.setIssuer("https://auth.example.com");
        when(sourceProvider.keySources()).thenReturn(List.of(testKeySource));
        when(keyLoader.loadKeys(testKeySource)).thenReturn(Collections.emptyMap());

        boolean result = publicKeyService.refreshPublicKeys("https://auth.example.com");

        assertFalse(result);
        ArgumentCaptor<PublicKeyRefreshEvent> captor = ArgumentCaptor.forClass(PublicKeyRefreshEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("failure", captor.getValue().getOutcome());
    }

    /**
     * Test refreshPublicKeys(issuer) when key loader throws exception.
     */
    @Test
    void refreshPublicKeysWhenKeyLoaderThrowsExceptionThenHandlesGracefullyAndPublishesFailureEvent() {
        testKeySource.setIssuer("https://auth.example.com");
        when(sourceProvider.keySources()).thenReturn(List.of(testKeySource));
        when(keyLoader.loadKeys(testKeySource)).thenThrow(new RuntimeException("Connection timeout"));

        boolean result = publicKeyService.refreshPublicKeys("https://auth.example.com");

        assertFalse(result);
        ArgumentCaptor<PublicKeyRefreshEvent> captor = ArgumentCaptor.forClass(PublicKeyRefreshEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("failure", captor.getValue().getOutcome());
    }

    /**
     * Test refreshPublicKeys(issuer) when no key loader is found for the source type.
     */
    @Test
    void refreshPublicKeysWhenKeyLoaderNotFoundThenPublishesFailureEvent() {
        testKeySource.setIssuer("https://auth.example.com");
        PublicKeyServiceImpl serviceWithoutLoaders = new PublicKeyServiceImpl(
                List.of(sourceProvider),
                Collections.emptyList(),
                publicKeyCache,
                eventPublisher,
                jwtProperties);

        when(sourceProvider.keySources()).thenReturn(List.of(testKeySource));

        boolean result = serviceWithoutLoaders.refreshPublicKeys("https://auth.example.com");

        assertFalse(result);
        ArgumentCaptor<PublicKeyRefreshEvent> captor = ArgumentCaptor.forClass(PublicKeyRefreshEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("failure", captor.getValue().getOutcome());
    }

    /**
     * Test refreshPublicKeys(issuer) when no matching sources are found.
     */
    @Test
    void refreshPublicKeysWhenNoMatchingSourcesThenReturnsFalse() {
        testKeySource.setIssuer("https://other-issuer.com");
        when(sourceProvider.keySources()).thenReturn(List.of(testKeySource));

        boolean result = publicKeyService.refreshPublicKeys("https://auth.example.com");

        assertFalse(result);
        verify(eventPublisher, never()).publishEvent(any());
    }

    /**
     * Test refreshPublicKeys(issuer) when source is not JWKS or provider returns null.
     */
    @Test
    void refreshPublicKeysWhenSourceProviderReturnsNullOrNonJwksThenReturnsFalse() {
        PublicKeySource pemSource = new PublicKeySource();
        pemSource.setType(PublicKeyType.PEM);
        pemSource.setIssuer("https://auth.example.com");

        when(sourceProvider.keySources()).thenReturn(List.of(pemSource));
        assertFalse(publicKeyService.refreshPublicKeys("https://auth.example.com"));

        when(sourceProvider.keySources()).thenReturn(null);
        assertFalse(publicKeyService.refreshPublicKeys("https://auth.example.com"));
    }
}
