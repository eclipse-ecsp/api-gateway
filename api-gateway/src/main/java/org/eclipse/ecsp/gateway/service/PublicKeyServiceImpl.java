package org.eclipse.ecsp.gateway.service;

import jakarta.annotation.PostConstruct;
import org.eclipse.ecsp.gateway.cache.PublicKeyCache;
import org.eclipse.ecsp.gateway.events.PublicKeyRefreshEvent;
import org.eclipse.ecsp.gateway.model.PublicKeyInfo;
import org.eclipse.ecsp.gateway.model.PublicKeySource;
import org.eclipse.ecsp.gateway.model.PublicKeyType;
import org.eclipse.ecsp.gateway.plugins.keyloaders.PublicKeyLoader;
import org.eclipse.ecsp.gateway.plugins.keysources.PublicKeySourceProvider;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation of PublicKeyService that manages multiple key sources and loaders.
 *
 * @author Abhishek Kumar
 */
@Component
public class PublicKeyServiceImpl implements PublicKeyService {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PublicKeyServiceImpl.class);
    private final List<PublicKeySourceProvider> sourceProviders;
    private final Map<PublicKeyType, PublicKeyLoader> keyLoaders;
    private final PublicKeyCache publicKeyCache;
    private final ScheduledExecutorService threadPoolExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructor with dependencies for key sources, loaders, cache, and metrics.
     *
     * @param sourceProviders list of public key source providers
     * @param keyLoaders map of public key loaders by type
     * @param publicKeyCache cache for public keys
     */
    public PublicKeyServiceImpl(List<PublicKeySourceProvider> sourceProviders,
                                List<PublicKeyLoader> keyLoaders,
                                PublicKeyCache publicKeyCache,
                                ApplicationEventPublisher eventPublisher) {
        this.sourceProviders = sourceProviders;
        this.keyLoaders = keyLoaders.stream()
                .collect(Collectors.toMap(PublicKeyLoader::getType, loader -> loader));
        this.publicKeyCache = publicKeyCache;
        this.eventPublisher = eventPublisher;
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

    private void loadPublicKeys(PublicKeySource source, PublicKeyLoader loader) {
        Map<String, PublicKey> loadedKeys = loader.loadKeys(source);
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
                removePublicKeysBySourceId(source.getId());
                LOGGER.info("Cleared existing keys from cache for source: {}", source.getId());

                this.loadPublicKeys(source, loader);
                LOGGER.info("JWKS public key refresh completed for source: {}", source.getId());
                
                // Publish individual source refresh event
                eventPublisher.publishEvent(new PublicKeyRefreshEvent(PublicKeyRefreshEvent.RefreshType.PUBLIC_KEY, 
                    source.getId()));
                
            } catch (Exception e) {
                LOGGER.error("Error during JWKS refresh for source: " + source.getId(), e);
                eventPublisher.publishEvent(new PublicKeyRefreshEvent(PublicKeyRefreshEvent.RefreshType.PUBLIC_KEY,
                    source.getId()));
            }
        }, source.getRefreshInterval().toMillis(), source.getRefreshInterval().toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Removes all public keys from cache that belong to the specified sourceId.
    *
    * @param sourceId the source ID to remove keys for
    * @return number of keys removed
    */
    private int removePublicKeysBySourceId(String sourceId) {
        if (sourceId == null || sourceId.trim().isEmpty()) {
            LOGGER.warn("SourceId is null or empty, cannot remove public keys");
            return 0;
        }
    
        // Get keys to remove using stream
        List<String> keysToRemove = publicKeyCache.entrySet()
            .stream()
            .filter(entry -> sourceId.equals(entry.getValue().getSourceId()))
            .map(Map.Entry::getKey)
            .toList();
        
        // Remove the keys
        keysToRemove.forEach(publicKeyCache::remove);

        LOGGER.info("Removed {} public key(s) {} for sourceId: {}", 
                    keysToRemove.size(), keysToRemove.toArray(), sourceId);
        return keysToRemove.size();
    }
}

