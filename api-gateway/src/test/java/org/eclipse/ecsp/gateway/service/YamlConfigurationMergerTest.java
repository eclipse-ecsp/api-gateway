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

import org.eclipse.ecsp.gateway.config.ClientAccessControlProperties;
import org.eclipse.ecsp.gateway.metrics.ClientAccessControlMetrics;
import org.eclipse.ecsp.gateway.model.AccessRule;
import org.eclipse.ecsp.gateway.model.ClientAccessConfig;
import org.eclipse.ecsp.gateway.utils.AccessControlConfigMerger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("YamlConfigurationMerger Tests")
class YamlConfigurationMergerTest {

    @Mock
    private ClientAccessControlProperties yamlProperties;

    @Mock
    private AccessRuleMatcherService ruleMatcherService;

    @Mock
    private ClientAccessControlMetrics metrics;

    @InjectMocks
    private AccessControlConfigMerger merger;

    @Test
    void testMergeNoYamlOverridesReturnsDatabaseConfigs() {
        when(yamlProperties.getOverrides()).thenReturn(List.of());

        List<ClientAccessConfig> result = merger.merge(List.of(createDatabaseConfig("client1", "tenant1")));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClientId()).isEqualTo("client1");
        assertThat(result.get(0).getSource()).isEqualTo("DATABASE");
    }

    @Test
    void testMergeYamlOverrideReplacesDatabase() {
        ClientAccessControlProperties.YamlOverride override = new ClientAccessControlProperties.YamlOverride();
        override.setClientId("client1");
        override.setTenant("tenant-yaml");
        override.setActive(false);
        override.setAllow(List.of("*:*"));

        when(yamlProperties.getOverrides()).thenReturn(List.of(override));
        when(ruleMatcherService.parseRules(anyList())).thenReturn(List.of(
                AccessRule.builder().service("*").route("*").deny(false).build()
        ));

        List<ClientAccessConfig> result = merger.merge(List.of(createDatabaseConfig("client1", "tenant1")));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenant()).isEqualTo("tenant-yaml");
        assertThat(result.get(0).isActive()).isFalse();
        assertThat(result.get(0).getSource()).isEqualTo("YAML_OVERRIDE");
    }

    @Test
    void testMergeYamlOnlyClientAddedToResult() {
        ClientAccessControlProperties.YamlOverride override = new ClientAccessControlProperties.YamlOverride();
        override.setClientId("client2");
        override.setTenant("tenant2");
        override.setActive(true);
        override.setAllow(List.of("user-service:*"));

        when(yamlProperties.getOverrides()).thenReturn(List.of(override));
        when(ruleMatcherService.parseRules(anyList())).thenReturn(List.of(
                AccessRule.builder().service("user-service").route("*").deny(false).build()
        ));

        List<ClientAccessConfig> result = merger.merge(List.of(createDatabaseConfig("client1", "tenant1")));

        assertThat(result)
                .hasSize(2)
                .anyMatch(c -> c.getClientId().equals("client1") && c.getSource().equals("DATABASE"))
                .anyMatch(c -> c.getClientId().equals("client2") && c.getSource().equals("YAML_OVERRIDE"));
    }

    private ClientAccessConfig createDatabaseConfig(String clientId, String tenant) {
        return ClientAccessConfig.builder()
                .clientId(clientId)
                .tenant(tenant)
                .active(true)
                .rules(List.of(AccessRule.builder().service("*").route("*").deny(false).build()))
                .lastUpdated(Instant.now())
                .source("DATABASE")
                .build();
    }
}
