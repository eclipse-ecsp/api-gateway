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

import java.security.PublicKey;
import java.util.Optional;

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
     * @param value public key
     */
    void put(String key, PublicKey value);

    /**
     * Gets a public key from the cache.
     *
     * @param key cache key
     * @return optional public key
     */
    Optional<PublicKey> get(String key);

    /**
     * Removes a public key from the cache.
     *
     * @param key cache key
     */
    void remove(String key);

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
}

