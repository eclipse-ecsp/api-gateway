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

package org.eclipse.ecsp.gateway.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for InMemoryPublicKeyCache.
 * Tests cache operations, thread safety, and configuration-based behavior.
 */
@ExtendWith(MockitoExtension.class)
class InMemoryPublicKeyCacheTest {

    private static final int KEYSIZE = 2048;
    public static final int EXPECTED = 2;
    public static final int NUMBER_OF_THREADS = 10;
    public static final int OPERATIONS_PER_THREAD = 100;
    private InMemoryPublicKeyCache cache;
    private PublicKey testPublicKey1;
    private PublicKey testPublicKey2;

    @BeforeEach
    void setUp() throws Exception {
        // Given
        cache = new InMemoryPublicKeyCache();

        // Create test public keys
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(KEYSIZE);
        KeyPair keyPair1 = keyGen.generateKeyPair();
        KeyPair keyPair2 = keyGen.generateKeyPair();
        testPublicKey1 = keyPair1.getPublic();
        testPublicKey2 = keyPair2.getPublic();
    }

    /**
     * Test successful put and get operations.
     * Verifies that keys can be stored and retrieved correctly.
     */
    @Test
    void put_whenValidKeyAndValue_thenStoresSuccessfully() {
        // Given
        String keyId = "test-key-1";

        // When
        cache.put(keyId, testPublicKey1);
        Optional<PublicKey> result = cache.get(keyId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testPublicKey1, result.get());
        assertEquals(1, cache.size());
    }

    /**
     * Test get operation for non-existent key.
     * Verifies that empty Optional is returned for missing keys.
     */
    @Test
    void get_whenKeyDoesNotExist_thenReturnsEmpty() {
        // Given
        String nonExistentKey = "non-existent-key";

        // When
        Optional<PublicKey> result = cache.get(nonExistentKey);

        // Then
        assertFalse(result.isPresent());
        assertEquals(0, cache.size());
    }

    /**
     * Test putting multiple keys.
     * Verifies that multiple keys can be stored independently.
     */
    @Test
    void put_whenMultipleKeys_thenStoresAllCorrectly() {
        // Given
        String keyId1 = "test-key-1";
        String keyId2 = "test-key-2";

        // When
        cache.put(keyId1, testPublicKey1);
        cache.put(keyId2, testPublicKey2);

        // Then
        Optional<PublicKey> result1 = cache.get(keyId1);
        Optional<PublicKey> result2 = cache.get(keyId2);

        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());
        assertEquals(testPublicKey1, result1.get());
        assertEquals(testPublicKey2, result2.get());
        assertEquals(EXPECTED, cache.size());
    }

    /**
     * Test overwriting existing key.
     * Verifies that existing keys can be updated with new values.
     */
    @Test
    void put_whenKeyAlreadyExists_thenOverwritesValue() {
        // Given
        String keyId = "test-key";
        cache.put(keyId, testPublicKey1);

        // When
        cache.put(keyId, testPublicKey2);
        Optional<PublicKey> result = cache.get(keyId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testPublicKey2, result.get());
        assertEquals(1, cache.size());
    }

    /**
     * Test removing a key from cache.
     * Verifies that keys can be removed successfully.
     */
    @Test
    void remove_whenKeyExists_thenRemovesSuccessfully() {
        // Given
        String keyId = "test-key";
        cache.put(keyId, testPublicKey1);

        // When
        cache.remove(keyId);
        Optional<PublicKey> result = cache.get(keyId);

        // Then
        assertFalse(result.isPresent());
        assertEquals(0, cache.size());
    }

    /**
     * Test removing non-existent key.
     * Verifies that removing non-existent keys doesn't cause errors.
     */
    @Test
    void remove_whenKeyDoesNotExist_thenHandlesGracefully() {
        // Given
        String nonExistentKey = "non-existent-key";

        // When & Then
        try {
            cache.remove(nonExistentKey);
        } catch (Exception e) {
            fail("Should not throw exception when removing non-existent key: " + e.getMessage());
        }
        assertEquals(0, cache.size());
    }

    /**
     * Test clearing the entire cache.
     * Verifies that all keys are removed when cache is cleared.
     */
    @Test
    void clear_whenCacheHasKeys_thenRemovesAllKeys() {
        // Given
        cache.put("key1", testPublicKey1);
        cache.put("key2", testPublicKey2);
        assertEquals(EXPECTED, cache.size());

        // When
        cache.clear();

        // Then
        assertEquals(0, cache.size());
        assertFalse(cache.get("key1").isPresent());
        assertFalse(cache.get("key2").isPresent());
    }

    /**
     * Test cache size reporting.
     * Verifies that size is correctly reported after various operations.
     */
    @Test
    void size_whenVariousOperations_thenReportsCorrectSize() {
        // Given
        assertEquals(0, cache.size());

        // When adding keys
        cache.put("key1", testPublicKey1);
        assertEquals(1, cache.size());

        cache.put("key2", testPublicKey2);
        assertEquals(EXPECTED, cache.size());

        // When removing key
        cache.remove("key1");
        assertEquals(1, cache.size());

        // When clearing
        cache.clear();
        assertEquals(0, cache.size());
    }

    /**
     * Test handling null key.
     * Verifies proper handling of null keys.
     */
    @Test
    void put_whenNullKey_thenHandlesCorrectly() {
        // When & Then
        try {
            cache.put(null, testPublicKey1);
        } catch (NullPointerException e) {
            fail("Should not throw exception for null key: " + e.getMessage());
        }
    }

    /**
     * Test handling null value.
     * Verifies proper handling of null values.
     */
    @Test
    void put_whenNullValue_thenHandlesCorrectly() {
        // Given
        String keyId = "test-key";

        // When & Then
        try {
            cache.put(keyId, null);
        } catch (NullPointerException e) {
            fail("Should not throw exception for null value: " + e.getMessage());
        }

        Optional<PublicKey> result = cache.get(keyId);
        assertFalse(result.isPresent());
    }

    /**
     * Test thread safety of cache operations.
     * Verifies that concurrent access doesn't cause data corruption.
     */
    @Test
    void concurrentAccess_whenMultipleThreads_thenThreadSafe() throws InterruptedException {
        // Given
        int numberOfThreads = NUMBER_OF_THREADS;
        int operationsPerThread = OPERATIONS_PER_THREAD;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successfulOperations = new AtomicInteger(0);

        // When
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "thread-" + threadId + "-key-" + j;
                        cache.put(key, testPublicKey1);

                        Optional<PublicKey> retrieved = cache.get(key);
                        if (retrieved.isPresent()) {
                            successfulOperations.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then
        assertEquals(numberOfThreads * operationsPerThread, successfulOperations.get());
        assertEquals(numberOfThreads * operationsPerThread, cache.size());
    }
}
