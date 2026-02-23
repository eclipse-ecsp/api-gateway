package org.eclipse.ecsp.gateway.service;

import org.eclipse.ecsp.gateway.clients.ApiRegistryClient;
import org.eclipse.ecsp.gateway.metrics.ClientAccessControlMetrics;
import org.eclipse.ecsp.gateway.model.AccessRule;
import org.eclipse.ecsp.gateway.model.ClientAccessConfig;
import org.eclipse.ecsp.gateway.model.ClientAccessControlConfigDto;
import org.eclipse.ecsp.gateway.utils.AccessControlConfigMerger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private AccessRuleMatcherService ruleMatcherService;

    @Mock
    private ApiRegistryClient apiRegistryClient;

    @Mock
    private AccessControlConfigMerger yamlConfigurationMerger;

    @Mock
    private ClientAccessControlMetrics metrics;

    private ClientAccessControlService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new ClientAccessControlService(
                ruleMatcherService,
                apiRegistryClient,
                yamlConfigurationMerger,
                metrics
        );
        
        // Clear cache before each test
        cacheService.clearCache();
    }

    @Test
    @DisplayName("loadAllConfigurations() should fetch configs from API and populate cache")
    void testLoadAllConfigurationsSuccess() {
        // Given: Mock API response with 2 clients
        List<ClientAccessControlConfigDto> mockDtos = Arrays.asList(
                createDto("client-1", "tenant-1", true, List.of("user-service:*")),
                createDto("client-2", "tenant-2", true, List.of("payment-service:*"))
        );
        
        when(apiRegistryClient.getClientAccessControlConfigs()).thenReturn(mockDtos);
        
        // Mock rule parsing
        when(ruleMatcherService.parseRules(List.of("user-service:*")))
                .thenReturn(List.of(AccessRule.builder().service("user-service").route("*").deny(false).build()));
        when(ruleMatcherService.parseRules(List.of("payment-service:*")))
                .thenReturn(List.of(AccessRule.builder().service("payment-service").route("*").deny(false).build()));
        
        // Mock YAML merger (no overrides, return as-is)
        when(yamlConfigurationMerger.merge(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Integer result = cacheService.loadAllConfigurations();

        // Then
        assertThat(result).isEqualTo(TWO_CLIENTS);
        assertThat(cacheService.getCacheSize()).isEqualTo(TWO_CLIENTS);
        assertThat(cacheService.getConfig("client-1")).isNotNull();
        assertThat(cacheService.getConfig("client-2")).isNotNull();
    }

    @Test
    @DisplayName("loadAllConfigurations() with YAML overrides should apply precedence")
    void testLoadAllConfigurationsWithYamlOverrides() {
        // Given: Mock API response
        List<ClientAccessControlConfigDto> mockDtos = Arrays.asList(
                createDto("client-1", "tenant-1", true, List.of("user-service:*"))
        );
        
        when(apiRegistryClient.getClientAccessControlConfigs()).thenReturn(mockDtos);
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
        Integer result = cacheService.loadAllConfigurations();

        // Then
        assertThat(result).isEqualTo(TWO_CLIENTS); // Both configs loaded
        assertThat(cacheService.getCacheSize()).isEqualTo(TWO_CLIENTS);
        assertThat(cacheService.getConfig("emergency-client").getSource()).isEqualTo("YAML_OVERRIDE");
    }

    @Test
    @DisplayName("loadAllConfigurations() should clear existing cache before loading")
    void testLoadAllConfigurationsClearsCacheBefore() {
        // Given: Pre-populate cache
        seedCache("old-client", "old-tenant");
        assertThat(cacheService.getCacheSize()).isEqualTo(1);
        
        // Given: New API response
        List<ClientAccessControlConfigDto> mockDtos = Arrays.asList(
                createDto("new-client", "new-tenant", true, List.of("*:*"))
        );
        
        when(apiRegistryClient.getClientAccessControlConfigs()).thenReturn(mockDtos);
        when(ruleMatcherService.parseRules(any())).thenReturn(List.of());
        when(yamlConfigurationMerger.merge(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Integer result = cacheService.loadAllConfigurations();

        // Then
        assertThat(result).isEqualTo(1);
        assertThat(cacheService.getCacheSize()).isEqualTo(1);
        assertThat(cacheService.getConfig("old-client")).isNull();
        assertThat(cacheService.getConfig("new-client")).isNotNull();
    }

    @Test
    @DisplayName("refresh() should reload all configurations")
    void testRefreshSuccess() {
        // Given: Pre-populate cache with old data
        seedCache("client-1", "old-tenant");
        
        // Mock API response
        List<ClientAccessControlConfigDto> mockDtos = Arrays.asList(
                createDto("client-1", "new-tenant", true, List.of("*:*"))
        );
        when(apiRegistryClient.getClientAccessControlConfigs()).thenReturn(mockDtos);
        when(ruleMatcherService.parseRules(any())).thenReturn(List.of(
                AccessRule.builder().service("*").route("*").deny(false).build()
        ));
        when(yamlConfigurationMerger.merge(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Integer result = cacheService.refresh();

        // Then
        assertThat(result).isEqualTo(1);
        ClientAccessConfig updated = cacheService.getConfig("client-1");
        assertThat(updated).isNotNull();
        assertThat(updated.getTenant()).isEqualTo("new-tenant");
    }

    @Test
    @DisplayName("getConfig() with cache hit should return config")
    void testGetConfigCacheHit() {
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
    void testGetConfigCacheMiss() {
        // When
        ClientAccessConfig result = cacheService.getConfig("nonexistent-client");

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getConfig() should be case-sensitive for clientId")
    void testGetConfigCaseSensitive() {
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
        assertThat(cacheService.getCacheSize()).isZero();

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
        assertThat(cacheService.getCacheSize()).isZero();
        assertThat(cacheService.getConfig("client-1")).isNull();
        assertThat(cacheService.getConfig("client-2")).isNull();
    }

    /**
     * Helper: Create mock DTO.
     */
    private ClientAccessControlConfigDto createDto(String clientId, String tenant, boolean active, List<String> allow) {
        return ClientAccessControlConfigDto.builder()
                .clientId(clientId)
                .tenant(tenant)
                .active(active)
                .allow(allow)
                .build();
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
