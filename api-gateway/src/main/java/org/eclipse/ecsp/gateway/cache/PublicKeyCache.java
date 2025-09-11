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

import org.eclipse.ecsp.gateway.model.PublicKeyInfo;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Caching interface for public keys used in JWT validation.
 *
 * @author Abhishek Kumar
 */
public interface PublicKeyCache {
    /**
     * Puts a public key into the cache.
     *
     * @param key cache key
     * @param value public key info
     */
    void put(String key, PublicKeyInfo value);

    /**
     * Gets a public key from the cache.
     *
     * @param key cache key
     * @return optional public key
     */
    Optional<PublicKeyInfo> get(String key);

    /**
     * Removes a public key from the cache.
     *
     * @param key cache key
     */
    void remove(String key);

    /**
     * Removes all entries matching the given predicate.
     *
     * @param predicate the condition to match for removal
     *
     * @return true if any entries were removed
     */
    boolean remove(Predicate<Map.Entry<String, PublicKeyInfo>> predicate);

    /**
     * Clears the cache.
     */
    void clear();

    /**
     * Returns the size of the cache.
     *
     * @return cache size
     */
    int size();
    
    /**
     * Returns entry set for iteration.
     *
     * @return set of cache entries
     */
    Set<Map.Entry<String, PublicKeyInfo>> entrySet();
}

