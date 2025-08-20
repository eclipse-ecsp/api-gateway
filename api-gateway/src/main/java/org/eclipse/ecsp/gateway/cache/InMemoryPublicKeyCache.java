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

package org.eclipse.ecsp.gateway.cache;

import com.google.common.base.Strings;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.security.PublicKey;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of PublicKeyCache using ConcurrentHashMap.
 *
 * @author Abhishek Kumar
 */
@Component
@ConditionalOnProperty(name = "jwt.cache.type", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryPublicKeyCache implements PublicKeyCache {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(InMemoryPublicKeyCache.class);
    private final ConcurrentHashMap<String, PublicKey> keyCache;

    /**
     * Constructor initializes the cache.
     */
    public InMemoryPublicKeyCache() {
        this.keyCache = new ConcurrentHashMap<>();
    }

    /**
     * Puts a public key into the cache.
     *
     * @param key cache key
     * @param value public key
     */
    @Override
    public void put(String key, PublicKey value) {
        if (Strings.isNullOrEmpty(key) || value == null) {
            LOGGER.warn("Cannot put null or empty key/value into cache. Key: {}, Value: {}", key, value);
            return;
        }
        keyCache.put(key, value);
    }

    /**
     * Gets a public key from the cache.
     *
     * @param key cache key
     * @return optional public key
     */
    @Override
    public Optional<PublicKey> get(String key) {
        return Optional.ofNullable(keyCache.get(key));
    }

    /**
     * Removes a public key from the cache.
     *
     * @param key cache key
     */
    @Override
    public void remove(String key) {
        if (Strings.isNullOrEmpty(key)) {
            LOGGER.warn("Cannot remove null or empty key from cache. Key: {}", key);
            return;
        }
        keyCache.remove(key);
    }

    /**
     * Clears the cache.
     */
    @Override
    public void clear() {
        keyCache.clear();
    }

    /**
     * Returns the size of the cache.
     *
     * @return cache size
     */
    @Override
    public int size() {
        return keyCache.size();
    }
}
