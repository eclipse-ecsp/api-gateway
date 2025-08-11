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

