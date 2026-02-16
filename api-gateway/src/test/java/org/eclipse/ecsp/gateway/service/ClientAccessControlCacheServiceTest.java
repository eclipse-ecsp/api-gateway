package org.eclipse.ecsp.gateway.service;

import org.eclipse.ecsp.gateway.metrics.ClientAccessControlMetrics;
import org.eclipse.ecsp.gateway.model.AccessRule;
import org.eclipse.ecsp.gateway.model.ClientAccessConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ClientAccessControlCacheService.
 *
 * <p>Tests cache operations, API interactions, and YAML override integration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientAccessControlCacheService Tests")
class ClientAccessControlCacheServiceTest {

    private static final int TWO_CLIENTS = 2;
    private static final int THREE_CLIENTS = 3;

    @Mock(lenient = true)
    private AccessRuleMatcherService ruleMatcherService;

    @Mock(lenient = true)
    private WebClient.Builder webClientBuilder;

    @Mock(lenient = true)
    private YamlConfigurationMerger yamlConfigurationMerger;

    @Mock(lenient = true)
    private ClientAccessControlMetrics metrics;

    @Mock(lenient = true)
    private WebClient webClient;

    @Mock(lenient = true)
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock(lenient = true)
    private WebClient.ResponseSpec responseSpec;

    private ClientAccessControlCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new ClientAccessControlCacheService(
                ruleMatcherService,
                webClientBuilder,
                yamlConfigurationMerger,
                metrics
        );
        
        // Clear cache before each test
        cacheService.clearCache();
        
        // Setup WebClient mock chain (lenient to avoid UnnecessaryStubbingException)
        lenient().when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        lenient().when(webClientBuilder.build()).thenReturn(webClient);
        lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("loadAllConfigurations() should fetch configs from API and populate cache")
    void testLoadAllConfigurations_Success() {
        // Given: Mock API response with 2 clients
        List<Object> mockDtos = Arrays.asList(
                createDto("client-1", "tenant-1", true, List.of("user-service:*")),
                createDto("client-2", "tenant-2", true, List.of("payment-service:*"))
        );
        
        when(responseSpec.bodyToFlux(any(Class.class))).thenReturn(Flux.fromIterable(mockDtos));
        
        // Mock rule parsing
        when(ruleMatcherService.parseRules(List.of("user-service:*")))
                .thenReturn(List.of(AccessRule.builder().service("user-service").route("*").deny(false).build()));
        when(ruleMatcherService.parseRules(List.of("payment-service:*")))
                .thenReturn(List.of(AccessRule.builder().service("payment-service").route("*").deny(false).build()));
        
        // Mock YAML merger (no overrides, return as-is)
        when(yamlConfigurationMerger.merge(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Mono<Integer> result = cacheService.loadAllConfigurations();

        // Then
        StepVerifier.create(result)
                .expectNext(TWO_CLIENTS)
                .verifyComplete();
        
        assertThat(cacheService.getCacheSize()).isEqualTo(TWO_CLIENTS);
        assertThat(cacheService.getConfig("client-1")).isNotNull();
        assertThat(cacheService.getConfig("client-2")).isNotNull();
    }

    @Test
    @DisplayName("loadAllConfigurations() with YAML overrides should apply precedence")
    void testLoadAllConfigurations_WithYamlOverrides() {
        // Given: Mock API response
        List<Object> mockDtos = Arrays.asList(
                createDto("client-1", "tenant-1", true, List.of("user-service:*"))
        );
        
        when(responseSpec.bodyToFlux(any(Class.class))).thenReturn(Flux.fromIterable(mockDtos));
        when(ruleMatcherService.parseRules(any())).thenReturn(List.of(
                AccessRule.builder().service("*").route("*").deny(false).build()
        ));
        
        // Mock YAML merger to return 2 configs (1 from DB + 1 YAML-only)
        ClientAccessConfig dbConfig = ClientAccessConfig.builder()
                .clientId("client-1")
                .tenant("tenant-1")
                .active(true)
                .rules(List.of())
                .source("DATABASE")
                .build();
        
        ClientAccessConfig yamlOverride = ClientAccessConfig.builder()
                .clientId("emergency-client")
                .tenant("ops-team")
                .active(true)
                .rules(List.of())
                .source("YAML_OVERRIDE")
                .build();
        
        when(yamlConfigurationMerger.merge(any()))
                .thenReturn(Arrays.asList(yamlOverride, dbConfig)); // YAML first

        // When
        Mono<Integer> result = cacheService.loadAllConfigurations();

        // Then
        StepVerifier.create(result)
                .expectNext(TWO_CLIENTS) // Both configs loaded
                .verifyComplete();
        
        assertThat(cacheService.getCacheSize()).isEqualTo(TWO_CLIENTS);
        assertThat(cacheService.getConfig("emergency-client").getSource()).isEqualTo("YAML_OVERRIDE");
    }

    @Test
    @DisplayName("loadAllConfigurations() should handle API errors gracefully")
    void testLoadAllConfigurations_ApiError() {
        // Given: API call fails
        when(responseSpec.bodyToFlux(any(Class.class)))
                .thenReturn(Flux.error(new RuntimeException("API unavailable")));

        // When
        Mono<Integer> result = cacheService.loadAllConfigurations();

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
        
        assertThat(cacheService.getCacheSize()).isEqualTo(0);
    }

    @Test
    @DisplayName("loadAllConfigurations() should clear existing cache before loading")
    void testLoadAllConfigurations_ClearsCacheBefore() {
        // Given: Pre-populate cache
        seedCache("old-client", "old-tenant");
        assertThat(cacheService.getCacheSize()).isEqualTo(1);
        
        // Given: New API response
        List<Object> mockDtos = Arrays.asList(
                createDto("new-client", "new-tenant", true, List.of("*:*"))
        );
        
        when(responseSpec.bodyToFlux(any(Class.class))).thenReturn(Flux.fromIterable(mockDtos));
        when(ruleMatcherService.parseRules(any())).thenReturn(List.of());
        when(yamlConfigurationMerger.merge(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Mono<Integer> result = cacheService.loadAllConfigurations();

        // Then
        StepVerifier.create(result)
                .expectNext(1)
                .verifyComplete();
        
        assertThat(cacheService.getCacheSize()).isEqualTo(1);
        assertThat(cacheService.getConfig("old-client")).isNull();
        assertThat(cacheService.getConfig("new-client")).isNotNull();
    }

    @Test
    @DisplayName("refresh() should update specific client configs in cache")
    void testRefresh_Success() {
        // Given: Pre-populate cache with old data
        seedCache("client-1", "old-tenant");
        
        // Mock API response for client-1
        Object mockDto = createDto("client-1", "new-tenant", true, List.of("*:*"));
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(mockDto));
        when(ruleMatcherService.parseRules(any())).thenReturn(List.of(
                AccessRule.builder().service("*").route("*").deny(false).build()
        ));

        // When
        Mono<Integer> result = cacheService.refresh(List.of("client-1"));

        // Then
        StepVerifier.create(result)
                .expectNext(1)
                .verifyComplete();
        
        ClientAccessConfig updated = cacheService.getConfig("client-1");
        assertThat(updated).isNotNull();
        assertThat(updated.getTenant()).isEqualTo("new-tenant");
    }

    @Test
    @DisplayName("refresh() with multiple clients should update all")
    void testRefresh_MultipleClients() {
        // Given: Mock API responses for 3 clients
        when(responseSpec.bodyToMono(any(Class.class)))
                .thenReturn(Mono.just(createDto("client-1", "tenant-1", true, List.of("*:*"))))
                .thenReturn(Mono.just(createDto("client-2", "tenant-2", true, List.of("*:*"))))
                .thenReturn(Mono.just(createDto("client-3", "tenant-3", true, List.of("*:*"))));
        
        when(ruleMatcherService.parseRules(any())).thenReturn(List.of());

        // When
        Mono<Integer> result = cacheService.refresh(List.of("client-1", "client-2", "client-3"));

        // Then
        StepVerifier.create(result)
                .expectNext(THREE_CLIENTS)
                .verifyComplete();
        
        assertThat(cacheService.getCacheSize()).isEqualTo(THREE_CLIENTS);
    }

    @Test
    @DisplayName("refresh() with empty list should return 0")
    void testRefresh_EmptyList() {
        // When
        Mono<Integer> result = cacheService.refresh(List.of());

        // Then
        StepVerifier.create(result)
                .expectNext(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("refresh() with null list should return 0")
    void testRefresh_NullList() {
        // When
        Mono<Integer> result = cacheService.refresh(null);

        // Then
        StepVerifier.create(result)
                .expectNext(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("refresh() should handle API errors and continue")
    void testRefresh_ApiError() {
        // Given: API call fails for one client but succeeds for another
        when(responseSpec.bodyToMono(any(Class.class)))
                .thenReturn(Mono.error(new RuntimeException("Not found")))
                .thenReturn(Mono.just(createDto("client-2", "tenant-2", true, List.of("*:*"))));
        
        when(ruleMatcherService.parseRules(any())).thenReturn(List.of());

        // When
        Mono<Integer> result = cacheService.refresh(List.of("client-1", "client-2"));

        // Then: Should continue and load client-2
        StepVerifier.create(result)
                .expectNext(1) // Only 1 succeeded
                .verifyComplete();
    }

    @Test
    @DisplayName("getConfig() with cache hit should return config")
    void testGetConfig_CacheHit() {
        // Given: Config in cache
        seedCache("test-client", "test-tenant");

        // When
        ClientAccessConfig result = cacheService.getConfig("test-client");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getClientId()).isEqualTo("test-client");
        assertThat(result.getTenant()).isEqualTo("test-tenant");
    }

    @Test
    @DisplayName("getConfig() with cache miss should return null")
    void testGetConfig_CacheMiss() {
        // When
        ClientAccessConfig result = cacheService.getConfig("nonexistent-client");

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getConfig() should be case-sensitive for clientId")
    void testGetConfig_CaseSensitive() {
        // Given
        seedCache("TestClient", "test-tenant");

        // When
        ClientAccessConfig result1 = cacheService.getConfig("TestClient");
        ClientAccessConfig result2 = cacheService.getConfig("testclient");

        // Then
        assertThat(result1).isNotNull();
        assertThat(result2).isNull(); // Different case
    }

    @Test
    @DisplayName("getCacheSize() should return correct count")
    void testGetCacheSize() {
        // Given: Empty cache
        assertThat(cacheService.getCacheSize()).isEqualTo(0);

        // When: Add configs
        seedCache("client-1", "tenant-1");
        seedCache("client-2", "tenant-2");
        seedCache("client-3", "tenant-3");

        // Then
        assertThat(cacheService.getCacheSize()).isEqualTo(THREE_CLIENTS);
    }

    @Test
    @DisplayName("clearCache() should remove all configs")
    void testClearCache() {
        // Given: Populate cache
        seedCache("client-1", "tenant-1");
        seedCache("client-2", "tenant-2");
        assertThat(cacheService.getCacheSize()).isEqualTo(TWO_CLIENTS);

        // When
        cacheService.clearCache();

        // Then
        assertThat(cacheService.getCacheSize()).isEqualTo(0);
        assertThat(cacheService.getConfig("client-1")).isNull();
        assertThat(cacheService.getConfig("client-2")).isNull();
    }

    /**
     * Helper: Create mock DTO.
     */
    private Object createDto(String clientId, String tenant, boolean active, List<String> allow) {
        // Create a simple Map to simulate DTO structure
        java.util.Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("clientId", clientId);
        dto.put("tenant", tenant);
        dto.put("active", active);
        dto.put("allow", allow);
        return dto;
    }

    /**
     * Helper: Seed cache with test data using reflection.
     */
    @SuppressWarnings("unchecked")
    private void seedCache(String clientId, String tenant) {
        ClientAccessConfig config = ClientAccessConfig.builder()
                .clientId(clientId)
                .tenant(tenant)
                .active(true)
                .rules(List.of())
                .source("TEST")
                .build();
        
        // Use reflection to access private cache field
        ConcurrentHashMap<String, ClientAccessConfig> cache = 
                (ConcurrentHashMap<String, ClientAccessConfig>) ReflectionTestUtils.getField(cacheService, "cache");
        
        if (cache != null) {
            cache.put(clientId, config);
        }
    }
}
