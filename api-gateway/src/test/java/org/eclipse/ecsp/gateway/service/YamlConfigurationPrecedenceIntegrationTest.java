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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.gateway.clients.ApiRegistryClient;
import org.eclipse.ecsp.gateway.config.ClientAccessControlProperties;
import org.eclipse.ecsp.gateway.model.AccessRule;
import org.eclipse.ecsp.gateway.model.ClientAccessConfig;
import org.eclipse.ecsp.gateway.utils.AccessControlConfigMerger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for YAML configuration precedence.
 *
 * <p>Tests FR-039 to FR-043: YAML overrides take precedence over database.
 * Uses test profile with inline YAML configuration.
 *
 * <p>Validates YAML overrides database configuration for emergency access changes.
 */
@SpringBootTest
@DirtiesContext
@TestPropertySource(properties = {
    //  Enable client access control feature
    "api.gateway.client-access-control.enabled=true",
    
    // YAML overrides configuration
    "api.gateway.client-access-control.overrides[0].clientId=emergency-client",
    "api.gateway.client-access-control.overrides[0].tenant=ops-team",
    "api.gateway.client-access-control.overrides[0].active=true",
    "api.gateway.client-access-control.overrides[0].allow[0]=*:*",
    
    "api.gateway.client-access-control.overrides[1].clientId=restricted-client",
    "api.gateway.client-access-control.overrides[1].tenant=dev-team",
    "api.gateway.client-access-control.overrides[1].active=false",
    "api.gateway.client-access-control.overrides[1].description=Temporary access restriction",
    "api.gateway.client-access-control.overrides[1].allow[0]=user-service:profile",
    "api.gateway.client-access-control.overrides[1].allow[1]=!payment-service:*",
    
    "api.gateway.client-access-control.overrides[2].clientId=audit-client",
    "api.gateway.client-access-control.overrides[2].tenant=security-team",
    "api.gateway.client-access-control.overrides[2].active=true",
    "api.gateway.client-access-control.overrides[2].allow[0]=audit-service:*",
    "api.gateway.client-access-control.overrides[2].allow[1]=log-service:*",
    
    // Enable dynamic routes but disable registry integration
    "api.registry.enabled=false",
    "api.dynamic.routes.enabled=true",
    
    // Disable features not relevant for this test
    "api.gateway.jwt.key-sources=",  // Empty JWT key sources to prevent PublicKeyServiceImpl initialization errors
    "spring.data.redis.host=localhost",  // Required for ReactiveRedisTemplate mock
    "spring.data.redis.port=6379"
})
@DisplayName("YAML Configuration Precedence Integration Tests")
class YamlConfigurationPrecedenceIntegrationTest {

    @TestConfiguration
    static class YamlTestConfig {
        @Bean
        public ApiRegistryClient yamlTestApiRegistryClient() {
            ApiRegistryClient mock = mock(ApiRegistryClient.class);
            // Stub to return empty flux (no dynamic routes from registry)
            when(mock.getRoutes()).thenReturn(Flux.empty());
            return mock;
        }
    }

    private static final int EXPECTED_OVERRIDE_COUNT = 3;
    private static final int EMERGENCY_CLIENT_INDEX = 0;
    private static final int RESTRICTED_CLIENT_INDEX = 1;
    private static final int AUDIT_CLIENT_INDEX = 2;
    private static final int FOUR_CONFIGS = 4;
    private static final int SIX_CONFIGS = 6;

    private static WireMockServer wireMockServer;

    @Autowired
    private ClientAccessControlProperties yamlProperties;

    @Autowired
    private AccessControlConfigMerger merger;

    @Autowired
    private AccessRuleMatcherService ruleMatcherService;

    // Mock beans required by Spring context (not HTTP-related)
    @MockitoBean(name = "routesRefreshRetryTemplate")
    private RetryTemplate retryTemplate;

    @MockitoBean
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @MockitoBean
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    // Mock PublicKeyService to avoid JWT key initialization issues
    @MockitoBean
    private PublicKeyService publicKeyService;

    /**
     * Configure dynamic properties to point ApiRegistryClient to WireMock server.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("api.registry.base-url", () -> "http://localhost:" + wireMockServer.port());
    }

    @BeforeAll
    static void setUpWireMock() {
        CollectorRegistry.defaultRegistry.clear();
        // Initialize and start WireMock server before Spring context
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        // Configure WireMock client to use the correct port
        WireMock.configureFor("localhost", wireMockServer.port());

        // Configure WireMock to return empty list for client access control endpoint
        // This allows YAML overrides to work without HTTP data
        wireMockServer.stubFor(get(urlEqualTo("/v1/config/client-access-control"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));
    }

    @AfterAll
    static void tearDownWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @Test
    @DisplayName("AS-5: YAML properties should be loaded correctly")
    void testYamlPropertiesLoadedCorrectly() {
        // Assert
        assertThat(yamlProperties.getOverrides()).hasSize(EXPECTED_OVERRIDE_COUNT);

        // Verify emergency-client
        ClientAccessControlProperties.YamlOverride emergencyClient = 
                yamlProperties.getOverrides().get(EMERGENCY_CLIENT_INDEX);
        assertThat(emergencyClient.getClientId()).isEqualTo("emergency-client");
        assertThat(emergencyClient.getTenant()).isEqualTo("ops-team");
        assertThat(emergencyClient.isActive()).isTrue();
        assertThat(emergencyClient.getAllow()).containsExactly("*:*");

        // Verify restricted-client
        ClientAccessControlProperties.YamlOverride restrictedClient = 
                yamlProperties.getOverrides().get(RESTRICTED_CLIENT_INDEX);
        assertThat(restrictedClient.getClientId()).isEqualTo("restricted-client");
        assertThat(restrictedClient.getTenant()).isEqualTo("dev-team");
        assertThat(restrictedClient.isActive()).isFalse();
        assertThat(restrictedClient.getDescription()).isEqualTo("Temporary access restriction");
        assertThat(restrictedClient.getAllow())
                .containsExactly("user-service:profile", "!payment-service:*");

        // Verify audit-client
        ClientAccessControlProperties.YamlOverride auditClient = 
                yamlProperties.getOverrides().get(AUDIT_CLIENT_INDEX);
        assertThat(auditClient.getClientId()).isEqualTo("audit-client");
        assertThat(auditClient.getTenant()).isEqualTo("security-team");
        assertThat(auditClient.isActive()).isTrue();
        assertThat(auditClient.getAllow()).containsExactly("audit-service:*", "log-service:*");
    }

    @Test
    @DisplayName("YAML override should replace database config for same clientId")
    void testYamlOverrideReplacesDatabase() {
        // Arrange - simulate database config for emergency-client
        ClientAccessConfig dbConfig = ClientAccessConfig.builder()
                .clientId("emergency-client")
                .tenant("default-tenant")
                .active(false) // Inactive in database
                .rules(Arrays.asList(
                        AccessRule.builder().service("user-service").route("login").deny(false).build()
                ))
                .source("DATABASE")
                .build();

        List<ClientAccessConfig> databaseConfigs = Arrays.asList(dbConfig);

        // Act
        List<ClientAccessConfig> merged = merger.merge(databaseConfigs);

        // Assert
        assertThat(merged).hasSize(EXPECTED_OVERRIDE_COUNT); // 1 override + 2 YAML-only clients

        // Find emergency-client in merged result
        ClientAccessConfig mergedConfig = merged.stream()
                .filter(c -> c.getClientId().equals("emergency-client"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("emergency-client not found in merged result"));

        // Verify YAML values took precedence
        assertThat(mergedConfig.getTenant()).isEqualTo("ops-team"); // From YAML, not "default-tenant"
        assertThat(mergedConfig.isActive()).isTrue(); // From YAML, not false
        assertThat(mergedConfig.getSource()).isEqualTo("YAML_OVERRIDE");

        // Verify rules parsed correctly
        assertThat(mergedConfig.getRules()).isNotEmpty();
        assertThat(mergedConfig.getRules()).anyMatch(r -> 
                r.getService().equals("*") && r.getRoute().equals("*") && !r.isDeny());
    }

    @Test
    @DisplayName("YAML-only clients should be added to merged result")
    void testYamlOnlyClientsAddedToMerged() {
        // Arrange - database config without any YAML override matches
        ClientAccessConfig dbConfig = ClientAccessConfig.builder()
                .clientId("regular-client")
                .tenant("default-tenant")
                .active(true)
                .rules(Arrays.asList(
                        AccessRule.builder().service("user-service").route("*").deny(false).build()
                ))
                .source("DATABASE")
                .build();

        List<ClientAccessConfig> databaseConfigs = Arrays.asList(dbConfig);

        // Act
        List<ClientAccessConfig> merged = merger.merge(databaseConfigs);

        // Assert
        assertThat(merged)
                .hasSize(FOUR_CONFIGS) // 1 database + 3 YAML-only
                .anyMatch(c -> c.getClientId().equals("regular-client") && "DATABASE".equals(c.getSource()))
                .anyMatch(c -> c.getClientId().equals("emergency-client") && "YAML_OVERRIDE".equals(c.getSource()))
                .anyMatch(c -> c.getClientId().equals("restricted-client") && "YAML_OVERRIDE".equals(c.getSource()))
                .anyMatch(c -> c.getClientId().equals("audit-client") && "YAML_OVERRIDE".equals(c.getSource()));
    }

    @Test
    @DisplayName("Multiple YAML overrides with complex rules should work")
    void testMultipleOverridesComplexRules() {
        // Arrange - database configs for restricted-client and audit-client
        ClientAccessConfig restrictedDbConfig = ClientAccessConfig.builder()
                .clientId("restricted-client")
                .tenant("old-tenant")
                .active(true) // Active in database
                .rules(Arrays.asList(
                        AccessRule.builder().service("*").route("*").deny(false).build()
                ))
                .source("DATABASE")
                .build();

        ClientAccessConfig auditDbConfig = ClientAccessConfig.builder()
                .clientId("audit-client")
                .tenant("old-audit-tenant")
                .active(false)
                .rules(Arrays.asList(
                        AccessRule.builder().service("log-service").route("write").deny(false).build()
                ))
                .source("DATABASE")
                .build();

        List<ClientAccessConfig> databaseConfigs = Arrays.asList(restrictedDbConfig, auditDbConfig);

        // Act
        List<ClientAccessConfig> merged = merger.merge(databaseConfigs);

        // Assert
        assertThat(merged).hasSize(EXPECTED_OVERRIDE_COUNT); // 2 overrides + 1 YAML-only (emergency-client)

        // Verify restricted-client: YAML override applied (now inactive)
        ClientAccessConfig restrictedMerged = merged.stream()
                .filter(c -> c.getClientId().equals("restricted-client"))
                .findFirst()
                .orElseThrow();
        assertThat(restrictedMerged.getTenant()).isEqualTo("dev-team"); // YAML value
        assertThat(restrictedMerged.isActive()).isFalse(); // YAML value (was true in DB)
        assertThat(restrictedMerged.getSource()).isEqualTo("YAML_OVERRIDE");

        // Verify audit-client: YAML override applied (now active with different rules)
        ClientAccessConfig auditMerged = merged.stream()
                .filter(c -> c.getClientId().equals("audit-client"))
                .findFirst()
                .orElseThrow();
        assertThat(auditMerged.getTenant()).isEqualTo("security-team"); // YAML value
        assertThat(auditMerged.isActive()).isTrue(); // YAML value (was false in DB)
        assertThat(auditMerged.getSource()).isEqualTo("YAML_OVERRIDE");
        // audit-service:*, log-service:*
        assertThat(auditMerged.getRules())
                .hasSizeGreaterThanOrEqualTo(RESTRICTED_CLIENT_INDEX);
    }

    @Test
    @DisplayName("Database configs without YAML overrides should be preserved")
    void testDatabaseOnlyPreserved() {
        // Arrange - database configs without YAML matches
        List<ClientAccessConfig> databaseConfigs = Arrays.asList(
                createDatabaseConfig("client1", "tenant1"),
                createDatabaseConfig("client2", "tenant2"),
                createDatabaseConfig("client3", "tenant3")
        );

        // Act
        List<ClientAccessConfig> merged = merger.merge(databaseConfigs);

        // Assert
        assertThat(merged)
                .hasSize(SIX_CONFIGS) // 3 database + 3 YAML-only
                .anyMatch(c -> c.getClientId().equals("client1") && "DATABASE".equals(c.getSource()))
                .anyMatch(c -> c.getClientId().equals("client2") && "DATABASE".equals(c.getSource()))
                .anyMatch(c -> c.getClientId().equals("client3") && "DATABASE".equals(c.getSource()));

        assertThat(merged.stream().filter(c -> "DATABASE".equals(c.getSource())))
                .hasSize(EXPECTED_OVERRIDE_COUNT);

        assertThat(merged.stream().filter(c -> "YAML_OVERRIDE".equals(c.getSource())))
                .hasSize(EXPECTED_OVERRIDE_COUNT);
    }

    @Test
    @DisplayName("getYamlOverrideCount() should return correct count")
    void testGetYamlOverrideCount() {
        // Act
        int count = merger.getYamlOverrideCount();

        // Assert
        assertThat(count).isEqualTo(EXPECTED_OVERRIDE_COUNT);
    }

    @Test
    @DisplayName("Deny rules in YAML should be parsed correctly")
    void testYamlDenyRulesParsedCorrectly() {
        // Arrange - Get restricted-client YAML config
        ClientAccessControlProperties.YamlOverride restrictedOverride = yamlProperties.getOverrides().stream()
                .filter(o -> o.getClientId().equals("restricted-client"))
                .findFirst()
                .orElseThrow();

        // Act - Parse rules using AccessRuleMatcherService
        List<AccessRule> rules = ruleMatcherService.parseRules(restrictedOverride.getAllow());

        // Assert
        assertThat(rules)
                .hasSizeGreaterThanOrEqualTo(RESTRICTED_CLIENT_INDEX)
                // Verify allow rule
                .anyMatch(r ->
                        r.getService().equals("user-service")
                        && r.getRoute().equals("profile")
                        && !r.isDeny())
                // Verify deny rule (! prefix)
                .anyMatch(r ->
                        r.getService().equals("payment-service")
                        && r.getRoute().equals("*")
                        && r.isDeny());
    }

    // Helper method
    private ClientAccessConfig createDatabaseConfig(String clientId, String tenant) {
        return ClientAccessConfig.builder()
                .clientId(clientId)
                .tenant(tenant)
                .active(true)
                .rules(Arrays.asList(
                        AccessRule.builder().service("*").route("*").deny(false).build()
                ))
                .source("DATABASE")
                .build();
    }
}
