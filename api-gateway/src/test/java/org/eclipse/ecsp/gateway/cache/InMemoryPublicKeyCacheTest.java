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
 *        cache.put("prefix_key1", testPublicKeyInfo1);
        cache.put("other_key2", testPublicKeyInfo2);
        cache.put("prefix_key3", testPublicKeyInfo3);
        assertEquals(THREE, cache.size());HOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.gateway.cache;

import org.eclipse.ecsp.gateway.model.PublicKeyInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private static final int THREE = 3;
    private static final int FIVE = 5;
    private static final int ONE_HUNDRED = 100;
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
        PublicKeyInfo testPublicKeyInfo1 = new PublicKeyInfo();
        testPublicKeyInfo1.setKid(keyId);
        testPublicKeyInfo1.setPublicKey(this.testPublicKey1);
        // When
        cache.put(keyId, testPublicKeyInfo1);
        Optional<PublicKeyInfo> result = cache.get(keyId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testPublicKeyInfo1, result.get());
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
        Optional<PublicKeyInfo> result = cache.get(nonExistentKey);

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
        PublicKeyInfo testPublicKeyInfo1 = new PublicKeyInfo();
        testPublicKeyInfo1.setKid(keyId1);
        testPublicKeyInfo1.setPublicKey(this.testPublicKey1);

        PublicKeyInfo testPublicKeyInfo2 = new PublicKeyInfo();
        testPublicKeyInfo2.setKid(keyId2);
        testPublicKeyInfo2.setPublicKey(this.testPublicKey2);
        // When
        cache.put(keyId1, testPublicKeyInfo1);
        cache.put(keyId2, testPublicKeyInfo2);

        // Then
        Optional<PublicKeyInfo> result1 = cache.get(keyId1);
        Optional<PublicKeyInfo> result2 = cache.get(keyId2);

        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());
        assertEquals(testPublicKeyInfo1, result1.get());
        assertEquals(testPublicKeyInfo2, result2.get());
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
        PublicKeyInfo testPublicKeyInfo1 = new PublicKeyInfo();
        testPublicKeyInfo1.setKid(keyId);
        testPublicKeyInfo1.setPublicKey(this.testPublicKey1);
        cache.put(keyId, testPublicKeyInfo1);

        // When
        PublicKeyInfo testPublicKeyInfo2 = new PublicKeyInfo();
        testPublicKeyInfo2.setKid(keyId);
        testPublicKeyInfo2.setPublicKey(this.testPublicKey2);
        cache.put(keyId, testPublicKeyInfo2);
        Optional<PublicKeyInfo> result = cache.get(keyId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testPublicKeyInfo2, result.get());
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
        PublicKeyInfo testPublicKeyInfo1 = new PublicKeyInfo();
        testPublicKeyInfo1.setKid(keyId);
        testPublicKeyInfo1.setPublicKey(this.testPublicKey1);
        cache.put(keyId, testPublicKeyInfo1);

        // When
        cache.remove(keyId);

        Optional<PublicKeyInfo> result = cache.get(keyId);

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
        PublicKeyInfo testPublicKeyInfo1 = new PublicKeyInfo();
        testPublicKeyInfo1.setKid("key1");
        testPublicKeyInfo1.setPublicKey(this.testPublicKey1);

        PublicKeyInfo testPublicKeyInfo2 = new PublicKeyInfo();
        testPublicKeyInfo2.setKid("key2");
        testPublicKeyInfo2.setPublicKey(this.testPublicKey2);
        cache.put("key1", testPublicKeyInfo1);
        cache.put("key2", testPublicKeyInfo2);
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
        PublicKeyInfo testPublicKeyInfo1 = new PublicKeyInfo();
        testPublicKeyInfo1.setKid("key1");
        testPublicKeyInfo1.setPublicKey(this.testPublicKey1);
        cache.put("key1", testPublicKeyInfo1);
        assertEquals(1, cache.size());

        PublicKeyInfo testPublicKeyInfo2 = new PublicKeyInfo();
        testPublicKeyInfo2.setKid("key2");
        testPublicKeyInfo2.setPublicKey(this.testPublicKey2);
        cache.put("key2", testPublicKeyInfo2);
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
            PublicKeyInfo testPublicKeyInfo1 = new PublicKeyInfo();
            testPublicKeyInfo1.setKid("test-key");
            testPublicKeyInfo1.setPublicKey(this.testPublicKey1);
            cache.put(null, testPublicKeyInfo1);
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


        Optional<PublicKeyInfo> result = cache.get(keyId);
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
                        
                        PublicKeyInfo testPublicKeyInfo1 = new PublicKeyInfo();
                        testPublicKeyInfo1.setKid(key);
                        testPublicKeyInfo1.setPublicKey(this.testPublicKey1);
                        cache.put(key, testPublicKeyInfo1);
                        Optional<PublicKeyInfo> retrieved = cache.get(key);
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

    /**
     * Test removing entries with predicate - successful removal.
     * Verifies that entries matching the predicate are removed correctly.
     */
    @Test
    void remove_whenPredicateMatchesEntries_thenRemovesMatchingEntries() {
        // Given
        PublicKeyInfo testPublicKeyInfo1 = new PublicKeyInfo();
        testPublicKeyInfo1.setKid("key1");
        testPublicKeyInfo1.setSourceId("source1");
        testPublicKeyInfo1.setPublicKey(this.testPublicKey1);

        PublicKeyInfo testPublicKeyInfo2 = new PublicKeyInfo();
        testPublicKeyInfo2.setKid("key2");
        testPublicKeyInfo2.setSourceId("source2");
        testPublicKeyInfo2.setPublicKey(this.testPublicKey2);

        PublicKeyInfo testPublicKeyInfo3 = new PublicKeyInfo();
        testPublicKeyInfo3.setKid("key3");
        testPublicKeyInfo3.setSourceId("source1");
        testPublicKeyInfo3.setPublicKey(this.testPublicKey1);

        cache.put("key1", testPublicKeyInfo1);
        cache.put("key2", testPublicKeyInfo2);
        cache.put("key3", testPublicKeyInfo3);
        assertEquals(THREE, cache.size());

        // When - remove entries from source1
        Predicate<Map.Entry<String, PublicKeyInfo>> predicate =
                entry -> "source1".equals(entry.getValue().getSourceId());
        boolean result = cache.remove(predicate);

        // Then
        assertTrue(result);
        assertEquals(1, cache.size());
        assertFalse(cache.get("key1").isPresent());
        assertTrue(cache.get("key2").isPresent());
        assertFalse(cache.get("key3").isPresent());
    }

    /**
     * Test removing entries with predicate - no matching entries.
     * Verifies that false is returned when no entries match the predicate.
     */
    @Test
    void remove_whenPredicateMatchesNoEntries_thenReturnsFalse() {
        // Given
        PublicKeyInfo testPublicKeyInfo1 = new PublicKeyInfo();
        testPublicKeyInfo1.setKid("key1");
        testPublicKeyInfo1.setSourceId("source1");
        testPublicKeyInfo1.setPublicKey(this.testPublicKey1);

        cache.put("key1", testPublicKeyInfo1);
        assertEquals(1, cache.size());

        // When - try to remove entries from non-existent source
        Predicate<Map.Entry<String, PublicKeyInfo>> predicate =
                entry -> "non-existent-source".equals(entry.getValue().getSourceId());
        boolean result = cache.remove(predicate);

        // Then
        assertFalse(result);
        assertEquals(1, cache.size());
        assertTrue(cache.get("key1").isPresent());
    }

    /**
     * Test removing entries with predicate - empty cache.
     * Verifies that false is returned when cache is empty.
     */
    @Test
    void remove_whenCacheIsEmpty_thenReturnsFalse() {
        // Given
        assertEquals(0, cache.size());

        // When
        Predicate<Map.Entry<String, PublicKeyInfo>> predicate =
                entry -> true; // Match all entries
        boolean result = cache.remove(predicate);

        // Then
        assertFalse(result);
        assertEquals(0, cache.size());
    }

    /**
     * Test removing entries with predicate - remove all entries.
     * Verifies that all entries can be removed using a predicate.
     */
    @Test
    void remove_whenPredicateMatchesAllEntries_thenRemovesAllEntries() {
        // Given
        PublicKeyInfo testPublicKeyInfo1 = new PublicKeyInfo();
        testPublicKeyInfo1.setKid("key1");
        testPublicKeyInfo1.setPublicKey(this.testPublicKey1);

        PublicKeyInfo testPublicKeyInfo2 = new PublicKeyInfo();
        testPublicKeyInfo2.setKid("key2");
        testPublicKeyInfo2.setPublicKey(this.testPublicKey2);

        cache.put("key1", testPublicKeyInfo1);
        cache.put("key2", testPublicKeyInfo2);
        assertEquals(EXPECTED, cache.size());

        // When - remove all entries
        Predicate<Map.Entry<String, PublicKeyInfo>> predicate = entry -> true;
        boolean result = cache.remove(predicate);

        // Then
        assertTrue(result);
        assertEquals(0, cache.size());
        assertFalse(cache.get("key1").isPresent());
        assertFalse(cache.get("key2").isPresent());
    }

    /**
     * Test removing entries with predicate - null predicate handling.
     * Verifies proper handling of null predicate.
     */
    @Test
    void remove_whenPredicateIsNull_thenHandlesGracefully() {
        // Given
        PublicKeyInfo testPublicKeyInfo1 = new PublicKeyInfo();
        testPublicKeyInfo1.setKid("key1");
        testPublicKeyInfo1.setPublicKey(this.testPublicKey1);
        cache.put("key1", testPublicKeyInfo1);

        // When & Then
        try {
            boolean result = cache.remove((Predicate<Map.Entry<String, PublicKeyInfo>>) null);
            // If implementation allows null, it should return false
            assertFalse(result);
            assertEquals(1, cache.size());
        } catch (NullPointerException e) {
            // If implementation throws NPE for null predicate, that's also acceptable
            assertEquals(1, cache.size());
        }
    }

    /**
     * Test removing entries with predicate - complex predicate condition.
     * Verifies that complex predicate conditions work correctly.
     */
    @Test
    void remove_whenComplexPredicate_thenRemovesCorrectEntries() {
        // Given
        PublicKeyInfo testPublicKeyInfo1 = new PublicKeyInfo();
        testPublicKeyInfo1.setKid("prefix_key1");
        testPublicKeyInfo1.setSourceId("source1");
        testPublicKeyInfo1.setPublicKey(this.testPublicKey1);

        PublicKeyInfo testPublicKeyInfo2 = new PublicKeyInfo();
        testPublicKeyInfo2.setKid("other_key2");
        testPublicKeyInfo2.setSourceId("source1");
        testPublicKeyInfo2.setPublicKey(this.testPublicKey2);

        PublicKeyInfo testPublicKeyInfo3 = new PublicKeyInfo();
        testPublicKeyInfo3.setKid("prefix_key3");
        testPublicKeyInfo3.setSourceId("source2");
        testPublicKeyInfo3.setPublicKey(this.testPublicKey1);

        cache.put("prefix_key1", testPublicKeyInfo1);
        cache.put("other_key2", testPublicKeyInfo2);
        cache.put("prefix_key3", testPublicKeyInfo3);
        assertEquals(THREE, cache.size());

        // When - remove entries with keys starting with "prefix_" AND from "source1"
        Predicate<Map.Entry<String, PublicKeyInfo>> predicate = entry ->
                entry.getKey().startsWith("prefix_") && "source1".equals(entry.getValue().getSourceId());
        boolean result = cache.remove(predicate);

        // Then
        assertTrue(result);
        assertEquals(EXPECTED, cache.size());
        assertFalse(cache.get("prefix_key1").isPresent());
        assertTrue(cache.get("other_key2").isPresent());
        assertTrue(cache.get("prefix_key3").isPresent());
    }

    /**
     * Test entrySet method - basic functionality.
     * Verifies that entrySet returns correct entries.
     */
    @Test
    void entrySet_whenCacheHasEntries_thenReturnsCorrectEntrySet() {
        // Given
        PublicKeyInfo testPublicKeyInfo1 = new PublicKeyInfo();
        testPublicKeyInfo1.setKid("key1");
        testPublicKeyInfo1.setPublicKey(this.testPublicKey1);

        PublicKeyInfo testPublicKeyInfo2 = new PublicKeyInfo();
        testPublicKeyInfo2.setKid("key2");
        testPublicKeyInfo2.setPublicKey(this.testPublicKey2);

        cache.put("key1", testPublicKeyInfo1);
        cache.put("key2", testPublicKeyInfo2);

        // When
        Set<Map.Entry<String, PublicKeyInfo>> entrySet = cache.entrySet();

        // Then
        assertNotNull(entrySet);
        assertEquals(EXPECTED, entrySet.size());
        
        boolean key1Found = false;
        boolean key2Found = false;
        
        for (Map.Entry<String, PublicKeyInfo> entry : entrySet) {
            if ("key1".equals(entry.getKey())) {
                assertEquals(testPublicKeyInfo1, entry.getValue());
                key1Found = true;
            } else if ("key2".equals(entry.getKey())) {
                assertEquals(testPublicKeyInfo2, entry.getValue());
                key2Found = true;
            }
        }
        
        assertTrue(key1Found);
        assertTrue(key2Found);
    }

    /**
     * Test entrySet method - empty cache.
     * Verifies that entrySet returns empty set when cache is empty.
     */
    @Test
    void entrySet_whenCacheIsEmpty_thenReturnsEmptyEntrySet() {
        // Given
        assertEquals(0, cache.size());

        // When
        Set<Map.Entry<String, PublicKeyInfo>> entrySet = cache.entrySet();

        // Then
        assertNotNull(entrySet);
        assertTrue(entrySet.isEmpty());
        assertEquals(0, entrySet.size());
    }

    /**
     * Test entrySet method - modifications after retrieval.
     * Verifies that entrySet reflects changes in the underlying cache.
     */
    @Test
    void entrySet_whenCacheModifiedAfterRetrieval_thenReflectsChanges() {
        // Given
        PublicKeyInfo testPublicKeyInfo1 = new PublicKeyInfo();
        testPublicKeyInfo1.setKid("key1");
        testPublicKeyInfo1.setPublicKey(this.testPublicKey1);
        cache.put("key1", testPublicKeyInfo1);

        Set<Map.Entry<String, PublicKeyInfo>> entrySet = cache.entrySet();
        assertEquals(1, entrySet.size());

        // When - add another entry
        PublicKeyInfo testPublicKeyInfo2 = new PublicKeyInfo();
        testPublicKeyInfo2.setKid("key2");
        testPublicKeyInfo2.setPublicKey(this.testPublicKey2);
        cache.put("key2", testPublicKeyInfo2);

        // Then - entrySet should reflect the change (live view)
        assertEquals(EXPECTED, entrySet.size());
        assertEquals(EXPECTED, cache.size());
    }

    /**
     * Test entrySet method - iteration safety.
     * Verifies that entrySet can be safely iterated over.
     */
    @Test
    void entrySet_whenIterating_thenIteratesSafely() {
        // Given
        for (int i = 1; i <= FIVE; i++) {
            PublicKeyInfo testPublicKeyInfo = new PublicKeyInfo();
            testPublicKeyInfo.setKid("key" + i);
            testPublicKeyInfo.setPublicKey(this.testPublicKey1);
            cache.put("key" + i, testPublicKeyInfo);
        }
        assertEquals(FIVE, cache.size());

        // When & Then
        Set<Map.Entry<String, PublicKeyInfo>> entrySet = cache.entrySet();
        int count = 0;
        try {
            for (Map.Entry<String, PublicKeyInfo> entry : entrySet) {
                assertNotNull(entry.getKey());
                assertNotNull(entry.getValue());
                assertTrue(entry.getKey().startsWith("key"));
                count++;
            }
        } catch (Exception e) {
            fail("Should not throw exception during iteration: " + e.getMessage());
        }
        
        assertEquals(FIVE, count);
    }

    /**
     * Test entrySet method - concurrent modification handling.
     * Verifies that entrySet handles concurrent modifications appropriately.
     */
    @Test
    void entrySet_whenConcurrentModification_thenHandlesGracefully() {
        // Given
        PublicKeyInfo testPublicKeyInfo1 = new PublicKeyInfo();
        testPublicKeyInfo1.setKid("key1");
        testPublicKeyInfo1.setPublicKey(this.testPublicKey1);

        PublicKeyInfo testPublicKeyInfo2 = new PublicKeyInfo();
        testPublicKeyInfo2.setKid("key2");
        testPublicKeyInfo2.setPublicKey(this.testPublicKey2);

        cache.put("key1", testPublicKeyInfo1);
        cache.put("key2", testPublicKeyInfo2);

        // When & Then
        Set<Map.Entry<String, PublicKeyInfo>> entrySet = cache.entrySet();
        try {
            for (Map.Entry<String, PublicKeyInfo> entry : entrySet) {
                // Modify cache during iteration - ConcurrentHashMap should handle this
                if ("key1".equals(entry.getKey())) {
                    PublicKeyInfo testPublicKeyInfo3 = new PublicKeyInfo();
                    testPublicKeyInfo3.setKid("key3");
                    testPublicKeyInfo3.setPublicKey(this.testPublicKey1);
                    cache.put("key3", testPublicKeyInfo3);
                }
            }
        } catch (Exception e) {
            fail("Should handle concurrent modification gracefully: " + e.getMessage());
        }
    }

    /**
     * Test thread safety of predicate-based remove method.
     * Verifies that concurrent predicate-based removal operations are thread-safe.
     */
    @Test
    void remove_whenConcurrentPredicateRemoval_thenThreadSafe() throws InterruptedException {
        // Given
        int numberOfKeys = ONE_HUNDRED;
        for (int i = 0; i < numberOfKeys; i++) {
            PublicKeyInfo testPublicKeyInfo = new PublicKeyInfo();
            testPublicKeyInfo.setKid("key" + i);
            testPublicKeyInfo.setSourceId(i % EXPECTED == 0 ? "source1" : "source2");
            testPublicKeyInfo.setPublicKey(this.testPublicKey1);
            cache.put("key" + i, testPublicKeyInfo);
        }
        assertEquals(numberOfKeys, cache.size());

        ExecutorService executor = Executors.newFixedThreadPool(EXPECTED);
        CountDownLatch latch = new CountDownLatch(EXPECTED);
        AtomicInteger removedCount = new AtomicInteger(0);

        // When - concurrent removal with different predicates
        executor.submit(() -> {
            try {
                boolean removed = cache.remove(entry -> "source1".equals(entry.getValue().getSourceId()));
                if (removed) {
                    removedCount.incrementAndGet();
                }
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                boolean removed = cache.remove(entry -> "source2".equals(entry.getValue().getSourceId()));
                if (removed) {
                    removedCount.incrementAndGet();
                }
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executor.shutdown();

        // Then
        assertEquals(0, cache.size());
        assertEquals(EXPECTED, removedCount.get());
    }
}
