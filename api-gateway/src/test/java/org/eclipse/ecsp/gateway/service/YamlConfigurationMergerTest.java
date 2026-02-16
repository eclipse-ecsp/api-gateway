package org.eclipse.ecsp.gateway.service;

import org.eclipse.ecsp.gateway.config.ClientAccessControlYamlProperties;
import org.eclipse.ecsp.gateway.model.AccessRule;
import org.eclipse.ecsp.gateway.model.ClientAccessConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Unit tests for YamlConfigurationMerger.
 *
 * <p>
 * Tests YAML precedence logic per FR-039 to FR-043:
 * - YAML overrides replace database configurations
 * - YAML-only clients without database entries
 * - Database configurations without YAML overrides preserved
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("YamlConfigurationMerger Tests")
class YamlConfigurationMergerTest {

    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int TWO_CLIENTS = 2;
    private static final int THREE_CLIENTS = 3;
    private static final int FOUR_CLIENTS = 4;

    @Mock
    private ClientAccessControlYamlProperties yamlProperties;

    @Mock
    private AccessRuleMatcherService ruleMatcherService;

    @InjectMocks
    private YamlConfigurationMerger merger;

    private List<ClientAccessConfig> databaseConfigs;
    private List<ClientAccessControlYamlProperties.YamlOverride> yamlOverrides;

    @BeforeEach
    void setUp() {
        databaseConfigs = new ArrayList<>();
        yamlOverrides = new ArrayList<>();
    }

    @Test
    @DisplayName("merge() with no YAML overrides should return database configs unchanged")
    void testMerge_NoYamlOverrides_ReturnsDatabaseConfigs() {
        // Arrange
        ClientAccessConfig dbConfig1 = createDatabaseConfig("client1", "tenant1");
        ClientAccessConfig dbConfig2 = createDatabaseConfig("client2", "tenant2");
        databaseConfigs.add(dbConfig1);
        databaseConfigs.add(dbConfig2);

        when(yamlProperties.getOverrides()).thenReturn(Collections.emptyList());

        // Act
        List<ClientAccessConfig> result = merger.merge(databaseConfigs);

        // Assert
        assertThat(result).hasSize(THREE_CLIENTS);
        assertThat(result).containsExactlyInAnyOrder(dbConfig1, dbConfig2);
    }

    @Test
    @DisplayName("merge() with YAML override should replace database config")
    void testMerge_YamlOverride_ReplacesDatabase() {
        // Arrange
        // Database config
        ClientAccessConfig dbConfig = createDatabaseConfig("client1", "tenant1");
        dbConfig.setActive(true);
        databaseConfigs.add(dbConfig);

        // YAML override (inactive)
        ClientAccessControlYamlProperties.YamlOverride yamlOverride = 
                createYamlOverride("client1", "tenant-yaml", false);
        yamlOverrides.add(yamlOverride);

        when(yamlProperties.getOverrides()).thenReturn(yamlOverrides);
        when(ruleMatcherService.parseRules(anyList())).thenReturn(createAllowRules());

        // Act
        List<ClientAccessConfig> result = merger.merge(databaseConfigs);

        // Assert
        assertThat(result).hasSize(1);
        ClientAccessConfig merged = result.get(0);
        assertThat(merged.getClientId()).isEqualTo("client1");
        assertThat(merged.getTenant()).isEqualTo("tenant-yaml"); // YAML value
        assertThat(merged.isActive()).isFalse(); // YAML value
        assertThat(merged.getSource()).isEqualTo("YAML_OVERRIDE");
    }

    @Test
    @DisplayName("merge() with YAML-only client should add to result")
    void testMerge_YamlOnlyClient_AddedToResult() {
        // Arrange
        // Database config for client1
        ClientAccessConfig dbConfig = createDatabaseConfig("client1", "tenant1");
        databaseConfigs.add(dbConfig);

        // YAML override for client2 (not in database)
        ClientAccessControlYamlProperties.YamlOverride yamlOverride = 
                createYamlOverride("client2", "tenant2", true);
        yamlOverrides.add(yamlOverride);

        when(yamlProperties.getOverrides()).thenReturn(yamlOverrides);
        when(ruleMatcherService.parseRules(anyList())).thenReturn(createAllowRules());

        // Act
        List<ClientAccessConfig> result = merger.merge(databaseConfigs);

        // Assert
        assertThat(result).hasSize(TWO_CLIENTS);
        assertThat(result).extracting(ClientAccessConfig::getClientId)
                .containsExactlyInAnyOrder("client1", "client2");

        // Verify database config preserved
        ClientAccessConfig dbResult = result.stream()
                .filter(c -> c.getClientId().equals("client1"))
                .findFirst().orElseThrow();
        assertThat(dbResult.getSource()).isEqualTo("DATABASE");

        // Verify YAML-only config added
        ClientAccessConfig yamlResult = result.stream()
                .filter(c -> c.getClientId().equals("client2"))
                .findFirst().orElseThrow();
        assertThat(yamlResult.getSource()).isEqualTo("YAML_OVERRIDE");
    }

    @Test
    @DisplayName("merge() with mixed scenario should apply correct precedence")
    void testMerge_MixedScenario_AppliesCorrectPrecedence() {
        // Arrange
        // Database configs for client1, client2, client3
        databaseConfigs.add(createDatabaseConfig("client1", "tenant-db1"));
        databaseConfigs.add(createDatabaseConfig("client2", "tenant-db2"));
        databaseConfigs.add(createDatabaseConfig("client3", "tenant-db3"));

        // YAML overrides for client1 (override) and client4 (new)
        yamlOverrides.add(createYamlOverride("client1", "tenant-yaml1", false));
        yamlOverrides.add(createYamlOverride("client4", "tenant-yaml4", true));

        when(yamlProperties.getOverrides()).thenReturn(yamlOverrides);
        when(ruleMatcherService.parseRules(anyList())).thenReturn(createAllowRules());

        // Act
        List<ClientAccessConfig> result = merger.merge(databaseConfigs);

        // Assert
        assertThat(result).hasSize(FOUR_CLIENTS);
        assertThat(result).extracting(ClientAccessConfig::getClientId)
                .containsExactlyInAnyOrder("client1", "client2", "client3", "client4");

        // Verify client1 uses YAML override
        ClientAccessConfig client1 = result.stream()
                .filter(c -> c.getClientId().equals("client1"))
                .findFirst().orElseThrow();
        assertThat(client1.getSource()).isEqualTo("YAML_OVERRIDE");
        assertThat(client1.getTenant()).isEqualTo("tenant-yaml1");

        // Verify client2 and client3 use database configs
        ClientAccessConfig client2 = result.stream()
                .filter(c -> c.getClientId().equals("client2"))
                .findFirst().orElseThrow();
        assertThat(client2.getSource()).isEqualTo("DATABASE");

        ClientAccessConfig client3 = result.stream()
                .filter(c -> c.getClientId().equals("client3"))
                .findFirst().orElseThrow();
        assertThat(client3.getSource()).isEqualTo("DATABASE");

        // Verify client4 is YAML-only
        ClientAccessConfig client4 = result.stream()
                .filter(c -> c.getClientId().equals("client4"))
                .findFirst().orElseThrow();
        assertThat(client4.getSource()).isEqualTo("YAML_OVERRIDE");
        assertThat(client4.getTenant()).isEqualTo("tenant-yaml4");
    }

    @Test
    @DisplayName("merge() should skip YAML override with missing clientId")
    void testMerge_MissingClientId_Skipped() {
        // Arrange
        databaseConfigs.add(createDatabaseConfig("client1", "tenant1"));

        // YAML override with null clientId
        ClientAccessControlYamlProperties.YamlOverride invalidOverride =
                new ClientAccessControlYamlProperties.YamlOverride();
        invalidOverride.setClientId(null);
        invalidOverride.setTenant("tenant-invalid");
        yamlOverrides.add(invalidOverride);

        when(yamlProperties.getOverrides()).thenReturn(yamlOverrides);

        // Act
        List<ClientAccessConfig> result = merger.merge(databaseConfigs);

        // Assert
        assertThat(result).hasSize(TWO_CLIENTS);
        assertThat(result.get(0).getClientId()).isEqualTo("client1");
        assertThat(result.get(0).getSource()).isEqualTo("DATABASE");
    }

    @Test
    @DisplayName("merge() should skip YAML override with blank clientId")
    void testMerge_BlankClientId_Skipped() {
        // Arrange
        databaseConfigs.add(createDatabaseConfig("client1", "tenant1"));

        // YAML override with blank clientId
        ClientAccessControlYamlProperties.YamlOverride invalidOverride =
                new ClientAccessControlYamlProperties.YamlOverride();
        invalidOverride.setClientId("   ");
        invalidOverride.setTenant("tenant-invalid");
        yamlOverrides.add(invalidOverride);

        when(yamlProperties.getOverrides()).thenReturn(yamlOverrides);

        // Act
        List<ClientAccessConfig> result = merger.merge(databaseConfigs);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClientId()).isEqualTo("client1");
        assertThat(result.get(0).getSource()).isEqualTo("DATABASE");
    }

    @Test
    @DisplayName("merge() with empty database configs should add YAML-only clients")
    void testMerge_EmptyDatabase_AddsYamlClients() {
        // Arrange
        yamlOverrides.add(createYamlOverride("client1", "tenant1", true));
        yamlOverrides.add(createYamlOverride("client2", "tenant2", false));

        when(yamlProperties.getOverrides()).thenReturn(yamlOverrides);
        when(ruleMatcherService.parseRules(anyList())).thenReturn(createAllowRules());

        // Act
        List<ClientAccessConfig> result = merger.merge(Collections.emptyList());

        // Assert
        assertThat(result).hasSize(TWO_CLIENTS);
        assertThat(result).extracting(ClientAccessConfig::getClientId)
                .containsExactlyInAnyOrder("client1", "client2");
        assertThat(result).allMatch(c -> "YAML_OVERRIDE".equals(c.getSource()));
    }

    @Test
    @DisplayName("getYamlOverrideCount() should return correct count")
    void testGetYamlOverrideCount() {
        // Arrange
        yamlOverrides.add(createYamlOverride("client1", "tenant1", true));
        yamlOverrides.add(createYamlOverride("client2", "tenant2", true));
        yamlOverrides.add(createYamlOverride("client3", "tenant3", false));

        when(yamlProperties.getOverrides()).thenReturn(yamlOverrides);

        // Act
        int count = merger.getYamlOverrideCount();

        // Assert
        assertThat(count).isEqualTo(THREE_CLIENTS);
    }

    @Test
    @DisplayName("getYamlOverrideCount() with no overrides should return 0")
    void testGetYamlOverrideCount_NoOverrides() {
        // Arrange
        when(yamlProperties.getOverrides()).thenReturn(Collections.emptyList());

        // Act
        int count = merger.getYamlOverrideCount();

        // Assert
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("merge() should parse YAML rules using AccessRuleMatcherService")
    void testMerge_ParsesRulesCorrectly() {
        // Arrange
        ClientAccessControlYamlProperties.YamlOverride yamlOverride =
                new ClientAccessControlYamlProperties.YamlOverride();
        yamlOverride.setClientId("client1");
        yamlOverride.setTenant("tenant1");
        yamlOverride.setActive(true);
        yamlOverride.setAllow(Arrays.asList("user-service:*", "!payment-service:refund"));
        yamlOverrides.add(yamlOverride);

        when(yamlProperties.getOverrides()).thenReturn(yamlOverrides);

        // Mock parsed rules
        List<AccessRule> parsedRules = Arrays.asList(
                AccessRule.builder().service("user-service").route("*").deny(false).build(),
                AccessRule.builder().service("payment-service").route("refund").deny(true).build()
        );
        when(ruleMatcherService.parseRules(yamlOverride.getAllow())).thenReturn(parsedRules);

        // Act
        List<ClientAccessConfig> result = merger.merge(Collections.emptyList());

        // Assert
        assertThat(result).hasSize(1);
        ClientAccessConfig config = result.get(0);
        assertThat(config.getRules()).hasSize(TWO_CLIENTS);
        assertThat(config.getRules().get(0).getService()).isEqualTo("user-service");
        assertThat(config.getRules().get(0).isDeny()).isFalse();
        assertThat(config.getRules().get(1).getService()).isEqualTo("payment-service");
        assertThat(config.getRules().get(1).isDeny()).isTrue();
    }

    // Helper methods

    private ClientAccessConfig createDatabaseConfig(String clientId, String tenant) {
        return ClientAccessConfig.builder()
                .clientId(clientId)
                .tenant(tenant)
                .active(true)
                .rules(createAllowRules())
                .lastUpdated(Instant.now())
                .source("DATABASE")
                .build();
    }

    private ClientAccessControlYamlProperties.YamlOverride createYamlOverride(
            String clientId, String tenant, boolean active) {
        ClientAccessControlYamlProperties.YamlOverride override = new ClientAccessControlYamlProperties.YamlOverride();
        override.setClientId(clientId);
        override.setTenant(tenant);
        override.setActive(active);
        override.setAllow(Arrays.asList("*:*"));
        return override;
    }

    private List<AccessRule> createAllowRules() {
        return Arrays.asList(
                AccessRule.builder().service("*").route("*").deny(false).build()
        );
    }
}
