package org.eclipse.ecsp.gateway.utils;

import org.eclipse.ecsp.gateway.config.ClientAccessControlProperties;
import org.eclipse.ecsp.gateway.metrics.ClientAccessControlMetrics;
import org.eclipse.ecsp.gateway.model.AccessRule;
import org.eclipse.ecsp.gateway.model.ClientAccessConfig;
import org.eclipse.ecsp.gateway.service.AccessRuleMatcherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test purpose    - Verify AccessControlConfigMerger YAML override merging logic.
 * Test data       - Various database and YAML configurations.
 * Test expected   - Correct precedence and merging behavior.
 * Test type       - Positive and Negative.
 */
@ExtendWith(MockitoExtension.class)
class AccessControlConfigMergerTest {

    @Mock
    private ClientAccessControlProperties yamlProperties;

    @Mock
    private AccessRuleMatcherService ruleMatcherService;

    @Mock
    private ClientAccessControlMetrics metrics;

    private AccessControlConfigMerger merger;

    @BeforeEach
    void setUp() {
        merger = new AccessControlConfigMerger(yamlProperties, ruleMatcherService, metrics);
    }

    /**
     * Test purpose          - Verify merge with no YAML overrides.
     * Test data             - Database configs only, empty YAML overrides.
     * Test expected result  - Returns database configs unchanged.
     * Test type             - Positive.
     */
    @Test
    void merge_NoYamlOverrides_ReturnsDatabaseConfigs() {
        // GIVEN: Database configs and no YAML overrides
        List<ClientAccessConfig> dbConfigs = List.of(
                createClientAccessConfig("client1"),
                createClientAccessConfig("client2")
        );
        when(yamlProperties.getOverrides()).thenReturn(List.of());

        // WHEN: Merge is performed
        List<ClientAccessConfig> result = merger.merge(dbConfigs);

        // THEN: Should return database configs unchanged
        assertEquals(2, result.size());
        assertEquals("client1", result.get(0).getClientId());
        assertEquals("client2", result.get(1).getClientId());
        verify(metrics, never()).recordYamlOverrideHit(anyString());
    }

    /**
     * Test purpose          - Verify YAML override takes precedence over database config.
     * Test data             - Same clientId in both database and YAML.
     * Test expected result  - YAML config is used.
     * Test type             - Positive.
     */
    @Test
    void merge_YamlOverrideExists_YamlTakesPrecedence() {
        // GIVEN: Database config and YAML override for same client
        List<ClientAccessConfig> dbConfigs = List.of(
                createClientAccessConfig("client1", "DB")
        );
        ClientAccessControlProperties.YamlOverride yamlOverride = createYamlOverride("client1");
        when(yamlProperties.getOverrides()).thenReturn(List.of(yamlOverride));
        when(ruleMatcherService.parseRules(anyList())).thenReturn(List.of(new AccessRule()));

        // WHEN: Merge is performed
        List<ClientAccessConfig> result = merger.merge(dbConfigs);

        // THEN: YAML config should be used
        assertEquals(1, result.size());
        assertEquals("YAML_OVERRIDE", result.get(0).getSource());
        verify(metrics, times(1)).recordYamlOverrideHit("client1");
    }

    /**
     * Test purpose          - Verify YAML-only clients are added.
     * Test data             - YAML override for client not in database.
     * Test expected result  - YAML client is added to result.
     * Test type             - Positive.
     */
    @Test
    void merge_YamlOnlyClient_AddsToResult() {
        // GIVEN: Database has client1, YAML has client2
        List<ClientAccessConfig> dbConfigs = List.of(
                createClientAccessConfig("client1")
        );
        ClientAccessControlProperties.YamlOverride yamlOverride = createYamlOverride("client2");
        when(yamlProperties.getOverrides()).thenReturn(List.of(yamlOverride));
        when(ruleMatcherService.parseRules(anyList())).thenReturn(List.of(new AccessRule()));

        // WHEN: Merge is performed
        List<ClientAccessConfig> result = merger.merge(dbConfigs);

        // THEN: Both clients should be in result
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(c -> "client1".equals(c.getClientId())));
        assertTrue(result.stream().anyMatch(c -> "client2".equals(c.getClientId())));
    }

    /**
     * Test purpose          - Verify database configs without YAML overrides are preserved.
     * Test data             - Mixed database and YAML configs.
     * Test expected result  - Non-overridden database configs remain.
     * Test type             - Positive.
     */
    @Test
    void merge_MixedConfigs_PreservesNonOverriddenDatabaseConfigs() {
        // GIVEN: Database has client1, client2, client3; YAML overrides client2
        List<ClientAccessConfig> dbConfigs = List.of(
                createClientAccessConfig("client1"),
                createClientAccessConfig("client2"),
                createClientAccessConfig("client3")
        );
        ClientAccessControlProperties.YamlOverride yamlOverride = createYamlOverride("client2");
        when(yamlProperties.getOverrides()).thenReturn(List.of(yamlOverride));
        when(ruleMatcherService.parseRules(anyList())).thenReturn(List.of(new AccessRule()));

        // WHEN: Merge is performed
        List<ClientAccessConfig> result = merger.merge(dbConfigs);

        // THEN: All three clients should be present, client2 from YAML
        assertEquals(3, result.size());
        long yamlSourceCount = result.stream()
                .filter(c -> "YAML_OVERRIDE".equals(c.getSource()))
                .count();
        assertEquals(1, yamlSourceCount);
        verify(metrics, times(1)).recordYamlOverrideHit("client2");
    }

    /**
     * Test purpose          - Verify merge handles empty database configs.
     * Test data             - Empty database list, YAML overrides present.
     * Test expected result  - Returns YAML configs only.
     * Test type             - Negative.
     */
    @Test
    void merge_EmptyDatabaseConfigs_ReturnsYamlConfigs() {
        // GIVEN: Empty database, YAML has client1
        List<ClientAccessConfig> dbConfigs = new ArrayList<>();
        ClientAccessControlProperties.YamlOverride yamlOverride = createYamlOverride("client1");
        when(yamlProperties.getOverrides()).thenReturn(List.of(yamlOverride));
        when(ruleMatcherService.parseRules(anyList())).thenReturn(List.of(new AccessRule()));

        // WHEN: Merge is performed
        List<ClientAccessConfig> result = merger.merge(dbConfigs);

        // THEN: YAML config should be returned
        assertEquals(1, result.size());
        assertEquals("client1", result.get(0).getClientId());
        assertEquals("YAML_OVERRIDE", result.get(0).getSource());
    }

    /**
     * Test purpose          - Verify merge skips YAML overrides with null clientId.
     * Test data             - YAML override with null clientId.
     * Test expected result  - YAML override is skipped.
     * Test type             - Negative.
     */
    @Test
    void merge_YamlOverrideWithNullClientId_SkipsOverride() {
        // GIVEN: YAML override with null clientId
        List<ClientAccessConfig> dbConfigs = List.of(createClientAccessConfig("client1"));
        ClientAccessControlProperties.YamlOverride yamlOverride = new ClientAccessControlProperties.YamlOverride();
        yamlOverride.setClientId(null);
        when(yamlProperties.getOverrides()).thenReturn(List.of(yamlOverride));

        // WHEN: Merge is performed
        List<ClientAccessConfig> result = merger.merge(dbConfigs);

        // THEN: Only database config should be present
        assertEquals(1, result.size());
        assertEquals("client1", result.get(0).getClientId());
    }

    /**
     * Test purpose          - Verify merge skips YAML overrides with blank clientId.
     * Test data             - YAML override with blank clientId.
     * Test expected result  - YAML override is skipped.
     * Test type             - Negative.
     */
    @Test
    void merge_YamlOverrideWithBlankClientId_SkipsOverride() {
        // GIVEN: YAML override with blank clientId
        List<ClientAccessConfig> dbConfigs = List.of(createClientAccessConfig("client1"));
        ClientAccessControlProperties.YamlOverride yamlOverride = new ClientAccessControlProperties.YamlOverride();
        yamlOverride.setClientId("   ");
        when(yamlProperties.getOverrides()).thenReturn(List.of(yamlOverride));

        // WHEN: Merge is performed
        List<ClientAccessConfig> result = merger.merge(dbConfigs);

        // THEN: Only database config should be present
        assertEquals(1, result.size());
        assertEquals("client1", result.get(0).getClientId());
    }

    /**
     * Test purpose          - Verify getYamlOverrideCount returns correct count.
     * Test data             - Multiple YAML overrides.
     * Test expected result  - Returns correct count.
     * Test type             - Positive.
     */
    @Test
    void getYamlOverrideCount_MultipleOverrides_ReturnsCorrectCount() {
        // GIVEN: Multiple YAML overrides
        when(yamlProperties.getOverrides()).thenReturn(List.of(
                createYamlOverride("client1"),
                createYamlOverride("client2"),
                createYamlOverride("client3")
        ));

        // WHEN: Count is requested
        int count = merger.getYamlOverrideCount();

        // THEN: Should return correct count
        assertEquals(3, count);
    }

    /**
     * Test purpose          - Verify getYamlOverrideCount with no overrides.
     * Test data             - Empty override list.
     * Test expected result  - Returns zero.
     * Test type             - Negative.
     */
    @Test
    void getYamlOverrideCount_NoOverrides_ReturnsZero() {
        // GIVEN: No YAML overrides
        when(yamlProperties.getOverrides()).thenReturn(List.of());

        // WHEN: Count is requested
        int count = merger.getYamlOverrideCount();

        // THEN: Should return zero
        assertEquals(0, count);
    }

    private ClientAccessConfig createClientAccessConfig(String clientId) {
        return createClientAccessConfig(clientId, "DATABASE");
    }

    private ClientAccessConfig createClientAccessConfig(String clientId, String source) {
        return ClientAccessConfig.builder()
                .clientId(clientId)
                .tenant("test-tenant")
                .active(true)
                .rules(List.of(new AccessRule()))
                .source(source)
                .build();
    }

    private ClientAccessControlProperties.YamlOverride createYamlOverride(String clientId) {
        ClientAccessControlProperties.YamlOverride override = new ClientAccessControlProperties.YamlOverride();
        override.setClientId(clientId);
        override.setTenant("test-tenant");
        override.setActive(true);
        override.setAllow(List.of("service1:route1", "service2:*"));
        return override;
    }
}
