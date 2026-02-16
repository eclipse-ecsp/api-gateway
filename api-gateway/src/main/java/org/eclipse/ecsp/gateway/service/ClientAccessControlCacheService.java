package org.eclipse.ecsp.gateway.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.gateway.metrics.ClientAccessControlMetrics;
import org.eclipse.ecsp.gateway.model.AccessRule;
import org.eclipse.ecsp.gateway.model.ClientAccessConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache service for client access configurations in the gateway.
 *
 * <p>
 * Uses ConcurrentHashMap for O(1) client config lookup during request validation.
 * Loaded from Api-Registry REST endpoint at startup and refreshed via Redis events or polling.
 *
 * <p>
 * Cache key: clientId (String)
 * Cache value: ClientAccessConfig (pre-parsed rules)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientAccessControlCacheService {

    private final ConcurrentHashMap<String, ClientAccessConfig> cache = new ConcurrentHashMap<>();
    private final AccessRuleMatcherService ruleMatcherService;
    private final WebClient.Builder webClientBuilder;
    private final YamlConfigurationMerger yamlConfigurationMerger;
    private final ClientAccessControlMetrics metrics;

    /**
     * Api-Registry base URL.
     * Currently hardcoded, should be moved to configuration properties in future release.
     */
    private static final String API_REGISTRY_BASE_URL = "http://api-registry:8080";
    private static final String GET_ALL_ENDPOINT = "/api/registry/client-access-control?includeInactive=false";

    /**
     * Initialize cache at startup.
     * Loads all active client configurations from Api-Registry.
     */
    @PostConstruct
    public void init() {
        log.info("Initializing client access control cache from Api-Registry");
        
        // Register cache size gauge
        metrics.registerCacheSizeGauge(this::getCacheSize);
        
        try {
            loadAllConfigurations().subscribe(
                    count -> log.info("Cache initialized successfully: {} clients loaded", count),
                    error -> log.error("Cache initialization failed - will retry via fallback polling", error)
            );
        } catch (Exception e) {
            log.error("Cache initialization error", e);
        }
    }

    /**
     * Load all active client configurations from Api-Registry.
     * Applies YAML overrides before caching.
     *
     * @return Mono emitting count of loaded configurations
     */
    public Mono<Integer> loadAllConfigurations() {
        WebClient webClient = webClientBuilder.baseUrl(API_REGISTRY_BASE_URL).build();
        Instant refreshStart = Instant.now();

        return webClient.get()
                .uri(GET_ALL_ENDPOINT)
                .retrieve()
                .bodyToFlux(ClientAccessConfigDto.class)
                .flatMap(this::parseConfig)
                .collectList()
                .map(databaseConfigs -> {
                    // Apply YAML overrides
                    List<ClientAccessConfig> mergedConfigs = yamlConfigurationMerger.merge(databaseConfigs);
                    
                    // Clear and repopulate cache
                    cache.clear();
                    for (ClientAccessConfig config : mergedConfigs) {
                        cache.put(config.getClientId(), config);
                    }
                    
                    // Record refresh duration
                    Duration refreshDuration = Duration.between(refreshStart, Instant.now());
                    metrics.recordConfigRefreshDuration(refreshDuration);
                    
                    log.debug("Loaded and merged {} client configurations (YAML overrides applied)", 
                            mergedConfigs.size());
                    return mergedConfigs.size();
                })
                .doOnError(error -> log.error("Failed to load configurations from Api-Registry", error));
    }

    /**
     * Refresh specific client configurations.
     *
     * @param clientIds List of client IDs to refresh
     * @return Mono emitting count of refreshed configurations
     */
    public Mono<Integer> refresh(List<String> clientIds) {
        if (clientIds == null || clientIds.isEmpty()) {
            return Mono.just(0);
        }

        log.info("Refreshing cache for {} clients", clientIds.size());

        // Fetch each client configuration individually
        return Flux.fromIterable(clientIds)
                .flatMap(this::fetchClientConfig)
                .flatMap(this::parseAndCacheConfig)
                .count()
                .map(Long::intValue)
                .doOnSuccess(count -> log.info("Refreshed {} client configurations", count))
                .doOnError(error -> log.error("Failed to refresh configurations", error));
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
            log.debug("Cache miss for clientId: {}", clientId);
            metrics.recordCacheMiss(clientId);
            return null;
        }

        log.debug("Cache hit for clientId: {}", clientId);
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
        log.info("Cache cleared");
    }

    /**
     * Parse DTO to ClientAccessConfig (without caching).
     *
     * @param dto Client access configuration DTO
     * @return Mono emitting the parsed config
     */
    private Mono<ClientAccessConfig> parseConfig(ClientAccessConfigDto dto) {
        try {
            // Parse rules from string array to AccessRule objects
            List<AccessRule> rules = ruleMatcherService.parseRules(dto.getAllow());

            ClientAccessConfig config = ClientAccessConfig.builder()
                    .clientId(dto.getClientId())
                    .tenant(dto.getTenant())
                    .active(dto.isActive())
                    .rules(rules)
                    .lastUpdated(Instant.now())
                    .source("DATABASE")
                    .build();

            return Mono.just(config);
        } catch (Exception e) {
            log.error("Failed to parse configuration for clientId: {}", dto.getClientId(), e);
            return Mono.empty();
        }
    }

    /**
     * Parse DTO and add to cache.
     *
     * @param dto Client access configuration DTO
     * @return Mono emitting the parsed config
     */
    private Mono<ClientAccessConfig> parseAndCacheConfig(ClientAccessConfigDto dto) {
        return parseConfig(dto)
                .doOnNext(config -> {
                    cache.put(config.getClientId(), config);
                    log.debug("Cached configuration for clientId: {}", config.getClientId());
                });
    }

    /**
     * Fetch single client configuration from Api-Registry.
     *
     * @param clientId Client identifier
     * @return Mono emitting the DTO
     */
    private Mono<ClientAccessConfigDto> fetchClientConfig(String clientId) {
        WebClient webClient = webClientBuilder.baseUrl(API_REGISTRY_BASE_URL).build();
        String endpoint = "/api/registry/client-access-control/client/" + clientId;

        return webClient.get()
                .uri(endpoint)
                .retrieve()
                .bodyToMono(ClientAccessConfigDto.class)
                .doOnError(error -> log.error("Failed to fetch config for clientId: {}", clientId, error))
                .onErrorResume(error -> Mono.empty()); // Continue on error
    }

    /**
     * DTO for API response from Api-Registry.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class ClientAccessConfigDto {
        private Long id;
        private String clientId;
        private String tenant;
        private String description;
        private boolean active;
        private boolean deleted;
        private List<String> allow;
    }
}
