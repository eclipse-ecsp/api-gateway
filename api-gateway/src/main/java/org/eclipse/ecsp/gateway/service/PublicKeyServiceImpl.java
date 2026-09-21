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

import jakarta.annotation.PostConstruct;
import org.eclipse.ecsp.gateway.cache.PublicKeyCache;
import org.eclipse.ecsp.gateway.config.JwtProperties;
import org.eclipse.ecsp.gateway.events.PublicKeyRefreshEvent;
import org.eclipse.ecsp.gateway.model.PublicKeyInfo;
import org.eclipse.ecsp.gateway.model.PublicKeySource;
import org.eclipse.ecsp.gateway.model.PublicKeyType;
import org.eclipse.ecsp.gateway.plugins.keyloaders.PublicKeyLoader;
import org.eclipse.ecsp.gateway.plugins.keysources.PublicKeySourceProvider;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Implementation of PublicKeyService that manages multiple key sources and loaders.
 *
 * @author Abhishek Kumar
 */
@Service
public class PublicKeyServiceImpl implements PublicKeyService {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PublicKeyServiceImpl.class);
    private final List<PublicKeySourceProvider> sourceProviders;
    private final Map<PublicKeyType, PublicKeyLoader> keyLoaders;
    private final PublicKeyCache publicKeyCache;
    private final ScheduledExecutorService threadPoolExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ApplicationEventPublisher eventPublisher;
    private final JwtProperties jwtProperties;
    private final Map<String, AtomicLong> lastForcedRefreshAt = new ConcurrentHashMap<>();

    /**
     * Constructor with explicit JWT refresh configuration.
     *
     * @param sourceProviders list of public key source providers
     * @param keyLoaders list of public key loaders
     * @param publicKeyCache cache for public keys
     * @param eventPublisher event publisher for refresh events
     * @param jwtProperties JWT refresh configuration
     */
    public PublicKeyServiceImpl(List<PublicKeySourceProvider> sourceProviders,
                                List<PublicKeyLoader> keyLoaders,
                                PublicKeyCache publicKeyCache,
                                ApplicationEventPublisher eventPublisher,
                                JwtProperties jwtProperties) {
        this.sourceProviders = sourceProviders;
        this.keyLoaders = keyLoaders.stream()
                .collect(Collectors.toMap(PublicKeyLoader::getType, loader -> loader));
        this.publicKeyCache = publicKeyCache;
        this.eventPublisher = eventPublisher;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Initializes the service by loading public keys from all configured sources.
     * This method is called after the bean is constructed and dependencies are injected.
     */
    @PostConstruct
    public void initialize() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Initializing PublicKeyServiceImpl with {} source providers [{}] and {} key loaders [{}]",
                    sourceProviders.size(),
                    String.join(",", sourceProviders
                            .stream()
                            .map(provider -> provider.getClass().getSimpleName()).toList()),
                    keyLoaders.size(),
                    String.join(",", keyLoaders.values().stream()
                            .map(loader -> loader.getClass().getSimpleName()).toList()));
        }
        this.refreshPublicKeys();
        LOGGER.info("PublicKeyServiceImpl initialized with {} public keys loaded", publicKeyCache.size());
    }

    @Override
    public Optional<PublicKeyInfo> findPublicKey(String keyId, String provider) {
        LOGGER.info("Finding public key for keyId: {} and provider: {}", keyId, provider);
        if (keyId == null || keyId.isEmpty()) {
            LOGGER.warn("Key ID is null or empty, cannot find public key");
            return Optional.empty();
        }
        Optional<PublicKeyInfo> publicKey = publicKeyCache.get(keyId);

        // If not found and issuer is provided, try with provider-prefixed key
        if (publicKey.isEmpty() && provider != null && !provider.isEmpty()) {
            String prefixedKey = provider + "_" + keyId;
            publicKey = publicKeyCache.get(prefixedKey);
            LOGGER.info("Attempted lookup with prefixed key: {}", prefixedKey);
        }

        publicKey.ifPresentOrElse(
            pk -> LOGGER.info("Public key found in cache for keyId: {}, sourceId: {}", pk.getKid(), pk.getSourceId()),
            () -> LOGGER.info("Public key not found in cache for keyId: {}", keyId)
        );
        return publicKey;
    }

    @Override
    public void refreshPublicKeys() {
        LOGGER.info("Refreshing public keys from all available sources, current cache size: {}", publicKeyCache.size());
        
        try {
            publicKeyCache.clear();
            for (PublicKeySourceProvider sourceProvider : sourceProviders) {
                List<PublicKeySource> sources = sourceProvider.keySources();
                if (sources == null || sources.isEmpty()) {
                    LOGGER.warn("No public key sources available from provider: {}",
                            sourceProvider.getClass().getSimpleName());
                    continue;
                }

                LOGGER.info("Found {} public key sources from provider: {}", sources.size(),
                        sourceProvider.getClass().getSimpleName());
                for (PublicKeySource source : sources) {
                    PublicKeyLoader loader = keyLoaders.get(source.getType());
                    if (loader == null) {
                        LOGGER.warn("No loader found for public key type: {}", source.getType());
                        continue;
                    }

                    LOGGER.info("Loading public key from source: {} with type: {}",
                            source.getId(), source.getType());
                    loadPublicKeys(source, loader);

                    if (loader.getType() == PublicKeyType.JWKS) {
                        scheduleJwksRefresh(source, loader);
                    }
                }
            }
            
            LOGGER.info("Public keys refreshed, current cache size: {}", publicKeyCache.size());
            
        } catch (Exception e) {
            LOGGER.error("Error during public key refresh", e);
            throw e;
        }
    }

    @Override
    public boolean refreshPublicKeys(String issuer) {
        if (!jwtProperties.getJwksRefreshOnUnknownKid().isEnabled()) {
            return false;
        }

        boolean refreshed = false;
        for (PublicKeySource source : findSourcesByIssuer(issuer)) {
            if (!tryAcquireRefreshSlot(source.getId(), jwtProperties.getJwksRefreshOnUnknownKid().getCooldownMs())) {
                LOGGER.warn("JWKS refresh suppressed by cooldown for source: {}", source.getId());
                publishUnknownKidEvent(source.getId(), "suppressed");
                continue;
            }
            PublicKeyLoader loader = keyLoaders.get(source.getType());
            if (loader != null && loadAndSwapPublicKeys(source, loader)) {
                refreshed = true;
                publishUnknownKidEvent(source.getId(), "success");
            } else {
                publishUnknownKidEvent(source.getId(), "failure");
            }
        }
        return refreshed;
    }

    private void publishUnknownKidEvent(String sourceId, String outcome) {
        eventPublisher.publishEvent(new PublicKeyRefreshEvent(PublicKeyRefreshEvent.RefreshType.PUBLIC_KEY,
                sourceId, PublicKeyRefreshEvent.Trigger.UNKNOWN_KID, outcome));
    }

    private List<PublicKeySource> findSourcesByIssuer(String issuer) {
        return sourceProviders.stream()
            .flatMap(provider -> Optional.ofNullable(provider.keySources()).orElse(List.of()).stream())
            .filter(source -> source.getType() == PublicKeyType.JWKS
                && (issuer == null || issuer.equals(source.getIssuer())))
                .toList();
    }

    private boolean tryAcquireRefreshSlot(String sourceId, long cooldownMs) {
        long now = System.currentTimeMillis();
        AtomicLong lastRefresh = lastForcedRefreshAt.computeIfAbsent(sourceId, key -> new AtomicLong(0L));
        long previous = lastRefresh.get();
        return now - previous >= cooldownMs && lastRefresh.compareAndSet(previous, now);
    }

    private boolean loadAndSwapPublicKeys(PublicKeySource source, PublicKeyLoader loader) {
        try {
            Map<String, PublicKey> loadedKeys = loader.loadKeys(source);
            if (CollectionUtils.isEmpty(loadedKeys)) {
                LOGGER.warn("No public keys loaded from source: {}, retaining cached keys", source.getId());
                return false;
            }
            removePublicKeysBySourceId(source.getId());
            loadPublicKeys(source, loadedKeys);
            return true;
        } catch (Exception e) {
            LOGGER.warn("Error refreshing public keys from source: {}, retaining cached keys", source.getId(), e);
            return false;
        }
    }

    private void loadPublicKeys(PublicKeySource source, PublicKeyLoader loader) {
        loadPublicKeys(source, loader.loadKeys(source));
    }

    private void loadPublicKeys(PublicKeySource source, Map<String, PublicKey> loadedKeys) {
        if (!CollectionUtils.isEmpty(loadedKeys)) {
            LOGGER.info("Public key fetched successfully from source: {}, type {}", source.getId(), source.getType());
            for (Entry<String, PublicKey> entry : loadedKeys.entrySet()) {
                String keyId = entry.getKey();
                PublicKey publicKey = entry.getValue();
                if (keyId != null && !keyId.trim().isEmpty() && publicKey != null) {
                    PublicKeyInfo publicKeyInfo = new PublicKeyInfo();
                    publicKeyInfo.setKid(keyId);
                    publicKeyInfo.setPublicKey(publicKey);
                    publicKeyInfo.setType(source.getType());
                    publicKeyInfo.setIssuer(source.getIssuer());
                    publicKeyInfo.setSourceId(source.getId());
                    publicKeyInfo.setSkipAuthz(source.isSkipAuthz());
                    publicKeyInfo.setSkipClaimValidation(source.isSkipClaimValidation());
                    String cacheKey = generateCacheKey(source, keyId);
                    publicKeyCache.put(cacheKey, publicKeyInfo);
                    LOGGER.info("Public key with KID: {} for source: {}, type: {}, added to cache with key: {}", 
                                    keyId, source.getId(), source.getType(), cacheKey);
                } else {
                    LOGGER.warn("Public key from source {} has null KID or publicKey, skipping", source.getId());
                }
            }
        }
    }

    private String generateCacheKey(PublicKeySource source, String keyId) {
        if (source.isUseProviderPrefixedKey()) {
            LOGGER.info("Using provider-prefixed key for source: {}, keyId: {}", source.getId(), keyId);
            return source.getId() + "_" + keyId;
        }
        return keyId;
    }

    private void scheduleJwksRefresh(PublicKeySource source, PublicKeyLoader loader) {
        threadPoolExecutor.scheduleAtFixedRate(() -> {
            LOGGER.info("Refreshing JWKS public key from source: {}", source.getId());
            
            try {
                Map<String, PublicKey> loadedKeys = loader.loadKeys(source);
                if (!CollectionUtils.isEmpty(loadedKeys)) {
                    removePublicKeysBySourceId(source.getId());
                    this.loadPublicKeys(source, loadedKeys);
                }
                LOGGER.info("JWKS public key refresh completed for source: {}", source.getId());
                
                // Publish individual source refresh event
                eventPublisher.publishEvent(new PublicKeyRefreshEvent(PublicKeyRefreshEvent.RefreshType.PUBLIC_KEY,
                    source.getId(), PublicKeyRefreshEvent.Trigger.SCHEDULED));
                
            } catch (Exception e) {
                LOGGER.error("Error during JWKS refresh for source: " + source.getId(), e);
                eventPublisher.publishEvent(new PublicKeyRefreshEvent(PublicKeyRefreshEvent.RefreshType.PUBLIC_KEY,
                    source.getId(), PublicKeyRefreshEvent.Trigger.SCHEDULED));
            }
        }, source.getRefreshInterval().toMillis(), source.getRefreshInterval().toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Removes all public keys from cache that belong to the specified sourceId.
     * Uses the cache's removeIf operation for optimal performance.
     *
     * @param sourceId the source ID to remove keys for
     * @return number of keys removed
     */
    private int removePublicKeysBySourceId(String sourceId) {
        if (sourceId == null || sourceId.trim().isEmpty()) {
            LOGGER.warn("SourceId is null or empty, cannot remove public keys");
            return 0;
        }

        // Count removed entries before removal for logging
        long removedCount = publicKeyCache.entrySet()
            .stream()
            .filter(entry -> sourceId.equals(entry.getValue().getSourceId()))
            .count();

        // Use removeIf for efficient single-pass removal
        boolean removed = publicKeyCache.remove(entry -> 
            sourceId.equals(entry.getValue().getSourceId()));

        int count = (int) removedCount;
        if (removed && count > 0) {
            LOGGER.info("Removed {} public key(s) for sourceId: {}", count, sourceId);
        }
        return count;
    }
}

