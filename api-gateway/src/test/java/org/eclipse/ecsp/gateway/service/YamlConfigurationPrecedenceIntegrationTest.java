package org.eclipse.ecsp.gateway.service;

import org.eclipse.ecsp.gateway.config.ClientAccessControlYamlProperties;
import org.eclipse.ecsp.gateway.model.AccessRule;
import org.eclipse.ecsp.gateway.model.ClientAccessConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for YAML configuration precedence.
 *
 * <p>Tests FR-039 to FR-043: YAML overrides take precedence over database.
 * Uses test profile with inline YAML configuration.
 *
 * <p>Validates AS-5: YAML overrides database configuration for emergency access changes.
 */
@SpringBootTest
@ActiveProfiles("yaml-precedence-test")
@TestPropertySource(properties = {
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
    "api.gateway.client-access-control.overrides[2].allow[1]=log-service:*"
})
@DisplayName("YAML Configuration Precedence Integration Tests")
class YamlConfigurationPrecedenceIntegrationTest {

    private static final int EXPECTED_OVERRIDE_COUNT = 3;
    private static final int EMERGENCY_CLIENT_INDEX = 0;
    private static final int RESTRICTED_CLIENT_INDEX = 1;
    private static final int AUDIT_CLIENT_INDEX = 2;
    private static final int FOUR_CONFIGS = 4;
    private static final int SIX_CONFIGS = 6;

    @Autowired
    private ClientAccessControlYamlProperties yamlProperties;

    @Autowired
    private YamlConfigurationMerger merger;

    @Autowired
    private AccessRuleMatcherService ruleMatcherService;

    // Mock beans not needed for YAML precedence tests but required by Spring context
    @MockBean
    private WebClient.Builder webClientBuilder;

    @MockBean(name = "routesRefreshRetryTemplate")
    private RetryTemplate retryTemplate;

    @MockBean
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Test
    @DisplayName("AS-5: YAML properties should be loaded correctly")
    void testYamlProperties_LoadedCorrectly() {
        // Assert
        assertThat(yamlProperties.getOverrides()).hasSize(EXPECTED_OVERRIDE_COUNT);

        // Verify emergency-client
        ClientAccessControlYamlProperties.YamlOverride emergencyClient = 
                yamlProperties.getOverrides().get(EMERGENCY_CLIENT_INDEX);
        assertThat(emergencyClient.getClientId()).isEqualTo("emergency-client");
        assertThat(emergencyClient.getTenant()).isEqualTo("ops-team");
        assertThat(emergencyClient.isActive()).isTrue();
        assertThat(emergencyClient.getAllow()).containsExactly("*:*");

        // Verify restricted-client
        ClientAccessControlYamlProperties.YamlOverride restrictedClient = 
                yamlProperties.getOverrides().get(RESTRICTED_CLIENT_INDEX);
        assertThat(restrictedClient.getClientId()).isEqualTo("restricted-client");
        assertThat(restrictedClient.getTenant()).isEqualTo("dev-team");
        assertThat(restrictedClient.isActive()).isFalse();
        assertThat(restrictedClient.getDescription()).isEqualTo("Temporary access restriction");
        assertThat(restrictedClient.getAllow())
                .containsExactly("user-service:profile", "!payment-service:*");

        // Verify audit-client
        ClientAccessControlYamlProperties.YamlOverride auditClient = 
                yamlProperties.getOverrides().get(AUDIT_CLIENT_INDEX);
        assertThat(auditClient.getClientId()).isEqualTo("audit-client");
        assertThat(auditClient.getTenant()).isEqualTo("security-team");
        assertThat(auditClient.isActive()).isTrue();
        assertThat(auditClient.getAllow()).containsExactly("audit-service:*", "log-service:*");
    }

    @Test
    @DisplayName("AS-5: YAML override should replace database config for same clientId")
    void testYamlOverride_ReplacesDatabase() {
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
    @DisplayName("AS-5: YAML-only clients should be added to merged result")
    void testYamlOnlyClients_AddedToMerged() {
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
        assertThat(merged).hasSize(FOUR_CONFIGS); // 1 database + 3 YAML-only

        // Verify database config preserved
        assertThat(merged).anyMatch(c -> 
                c.getClientId().equals("regular-client") && "DATABASE".equals(c.getSource()));

        // Verify all YAML clients added
        assertThat(merged).anyMatch(c -> 
                c.getClientId().equals("emergency-client") && "YAML_OVERRIDE".equals(c.getSource()));
        assertThat(merged).anyMatch(c -> 
                c.getClientId().equals("restricted-client") && "YAML_OVERRIDE".equals(c.getSource()));
        assertThat(merged).anyMatch(c -> 
                c.getClientId().equals("audit-client") && "YAML_OVERRIDE".equals(c.getSource()));
    }

    @Test
    @DisplayName("AS-5: Multiple YAML overrides with complex rules should work")
    void testMultipleOverrides_ComplexRules() {
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
    @DisplayName("AS-5: Database configs without YAML overrides should be preserved")
    void testDatabaseOnly_Preserved() {
        // Arrange - database configs without YAML matches
        List<ClientAccessConfig> databaseConfigs = Arrays.asList(
                createDatabaseConfig("client1", "tenant1"),
                createDatabaseConfig("client2", "tenant2"),
                createDatabaseConfig("client3", "tenant3")
        );

        // Act
        List<ClientAccessConfig> merged = merger.merge(databaseConfigs);

        // Assert
        assertThat(merged).hasSize(SIX_CONFIGS); // 3 database + 3 YAML-only

        // Verify database configs preserved with DATABASE source
        assertThat(merged.stream().filter(c -> "DATABASE".equals(c.getSource()))).hasSize(EXPECTED_OVERRIDE_COUNT);
        assertThat(merged).anyMatch(c -> c.getClientId().equals("client1") && "DATABASE".equals(c.getSource()));
        assertThat(merged).anyMatch(c -> c.getClientId().equals("client2") && "DATABASE".equals(c.getSource()));
        assertThat(merged).anyMatch(c -> c.getClientId().equals("client3") && "DATABASE".equals(c.getSource()));

        // Verify YAML configs added
        assertThat(merged.stream().filter(c -> "YAML_OVERRIDE".equals(c.getSource()))).hasSize(EXPECTED_OVERRIDE_COUNT);
    }

    @Test
    @DisplayName("AS-5: getYamlOverrideCount() should return correct count")
    void testGetYamlOverrideCount() {
        // Act
        int count = merger.getYamlOverrideCount();

        // Assert
        assertThat(count).isEqualTo(EXPECTED_OVERRIDE_COUNT);
    }

    @Test
    @DisplayName("AS-5: Deny rules in YAML should be parsed correctly")
    void testYamlDenyRules_ParsedCorrectly() {
        // Arrange - Get restricted-client YAML config
        ClientAccessControlYamlProperties.YamlOverride restrictedOverride = yamlProperties.getOverrides().stream()
                .filter(o -> o.getClientId().equals("restricted-client"))
                .findFirst()
                .orElseThrow();

        // Act - Parse rules using AccessRuleMatcherService
        List<AccessRule> rules = ruleMatcherService.parseRules(restrictedOverride.getAllow());

        // Assert
        assertThat(rules).hasSizeGreaterThanOrEqualTo(RESTRICTED_CLIENT_INDEX);

        // Verify allow rule
        assertThat(rules).anyMatch(r ->
                r.getService().equals("user-service")
                && r.getRoute().equals("profile")
                && !r.isDeny());

        // Verify deny rule (! prefix)
        assertThat(rules).anyMatch(r ->
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
