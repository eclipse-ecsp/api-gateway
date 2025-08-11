package org.eclipse.ecsp.gateway.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.security.PublicKey;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of PublicKeyCache using ConcurrentHashMap.
 * Note: redisTemplate is used to interact with Redis for handling distributed JWKS updates.
 *
 * @author Abhishek Kumar
 */
@Component
@ConditionalOnProperty(name = "jwt.cache.type", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryPublicKeyCache implements PublicKeyCache {
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
