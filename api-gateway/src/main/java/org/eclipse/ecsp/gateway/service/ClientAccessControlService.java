package org.eclipse.ecsp.gateway.service;

import jakarta.annotation.PostConstruct;
import org.eclipse.ecsp.gateway.clients.ApiRegistryClient;
import org.eclipse.ecsp.gateway.metrics.ClientAccessControlMetrics;
import org.eclipse.ecsp.gateway.model.AccessRule;
import org.eclipse.ecsp.gateway.model.ClientAccessConfig;
import org.eclipse.ecsp.gateway.model.ClientAccessControlConfigDto;
import org.eclipse.ecsp.gateway.utils.AccessControlConfigMerger;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.event.EventListener;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache service for client access configurations in the gateway.
 *
 * <p>Uses ConcurrentHashMap for O(1) client config lookup during request validation.
 * Loaded from Api-Registry REST endpoint at startup and refreshed via Redis events or polling.
 *
 * <p>Cache key: clientId (String)
 * Cache value: ClientAccessConfig (pre-parsed rules)
 */
public class ClientAccessControlService {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ClientAccessControlService.class);
    private final ConcurrentHashMap<String, ClientAccessConfig> cache = new ConcurrentHashMap<>();
    private final AccessRuleMatcherService ruleMatcherService;
    private final ApiRegistryClient apiRegistryClient;
    private final AccessControlConfigMerger yamlConfigurationMerger;
    private final ClientAccessControlMetrics metrics;

    /**
     * Constructor for ClientAccessControlCacheService.
     *
     * @param ruleMatcherService the rule matcher service
     * @param apiRegistryClient the API registry client
     * @param yamlConfigurationMerger the YAML configuration merger
     * @param metrics the client access control metrics
     */
    public ClientAccessControlService(AccessRuleMatcherService ruleMatcherService, 
            ApiRegistryClient apiRegistryClient, 
            AccessControlConfigMerger yamlConfigurationMerger, 
            ClientAccessControlMetrics metrics) {
        this.ruleMatcherService = ruleMatcherService;
        this.apiRegistryClient = apiRegistryClient;
        this.yamlConfigurationMerger = yamlConfigurationMerger;
        this.metrics = metrics;
    }
    
    /**
     * Initialize cache at startup.
     * Loads all active client configurations from Api-Registry.
     */
    @PostConstruct
    public void init() {
        LOGGER.info("Initializing client access control cache from Api-Registry");
        
        // Register cache size gauge
        metrics.registerCacheSizeGauge(this::getCacheSize);
        
        try {
            Integer count = loadAllConfigurations();
            LOGGER.info("Cache initialized successfully: {} clients loaded", count);
        } catch (Exception e) {
            LOGGER.error("Cache initialization error", e);
        }
    }

    /**
     * Load all active client configurations from Api-Registry.
     * Applies YAML overrides before caching.
     *
     * @return Count of loaded configurations
     */
    public Integer loadAllConfigurations() {
        Instant refreshStart = Instant.now();
        LOGGER.info("Loading client access control configurations from Api-Registry, starting at {}", refreshStart);
        List<ClientAccessControlConfigDto> clientIds = apiRegistryClient.getClientAccessControlConfigs();
        LOGGER.debug("Raw Client IDs fetched: {}", clientIds);
        List<ClientAccessConfig> registryConfigs  = List.of();
        if (clientIds == null || clientIds.isEmpty()) {
            LOGGER.warn("No client access control configurations found in Api-Registry");
        } else {
            LOGGER.info("Fetched {} client IDs from Api-Registry", clientIds.size());
            registryConfigs = clientIds.stream().map(this::parseConfig).filter(Objects::nonNull).toList();
        }
        
        List<ClientAccessConfig> mergedConfigs = yamlConfigurationMerger.merge(registryConfigs);
        
        // Clear and repopulate cache
        cache.clear();
        
        for (ClientAccessConfig config : mergedConfigs) {
            cache.put(config.getClientId(), config);
        }
                    
        // Record refresh duration
        Duration refreshDuration = Duration.between(refreshStart, Instant.now());
        metrics.recordConfigRefreshDuration(refreshDuration);
        
        LOGGER.info("Loaded and merged {} client configurations (YAML overrides applied) in {} ms", 
                            mergedConfigs.size(), refreshDuration.toMillis());
        return mergedConfigs.size();
    }

    /**
     * Refresh specific client configurations.
     *
     * @return Count of refreshed configurations
     */
    @EventListener(RefreshRoutesEvent.class)
    public Integer refresh() {       
        LOGGER.info("Refreshing client access control cache from Api-Registry");
        Integer count = loadAllConfigurations();
        LOGGER.info("Cache refresh completed: {} clients refreshed", count);
        return count;
    }

    /**
     * Get client configuration from cache.
     *
     * @param clientId Client identifier
     * @return ClientAccessConfig if found, null otherwise
     */
    public ClientAccessConfig getConfig(String clientId) {
        ClientAccessConfig config = cache.get(clientId);
        
        if (config == null) {
            LOGGER.debug("Cache miss for clientId: {}", clientId);
            metrics.recordCacheMiss(clientId);
            return null;
        }

        LOGGER.debug("Cache hit for clientId: {}", clientId);
        metrics.recordCacheHit(clientId);
        return config;
    }

    /**
     * Get current cache size.
     *
     * @return Number of cached client configurations
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Clear all cached configurations.
     */
    public void clearCache() {
        cache.clear();
        LOGGER.info("Cache cleared");
    }

    /**
     * Parse DTO to ClientAccessConfig (without caching).
     *
     * @param dto Client access configuration DTO
     * @return Parsed ClientAccessConfig
     */
    private ClientAccessConfig parseConfig(ClientAccessControlConfigDto dto) {
        try {
            // Parse rules from string array to AccessRule objects
            List<AccessRule> rules = ruleMatcherService.parseRules(dto.getAllow());

            return ClientAccessConfig.builder()
                    .clientId(dto.getClientId())
                    .tenant(dto.getTenant())
                    .active(dto.isActive())
                    .rules(rules)
                    .lastUpdated(Instant.now())
                    .source("REGISTRY")
                    .build();
        } catch (Exception e) {
            LOGGER.error("Failed to parse configuration for clientId: {}", dto.getClientId(), e);
            return null;
        }
    }
}
