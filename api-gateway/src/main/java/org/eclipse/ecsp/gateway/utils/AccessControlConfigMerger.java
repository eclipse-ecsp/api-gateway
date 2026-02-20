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

package org.eclipse.ecsp.gateway.utils;

import lombok.RequiredArgsConstructor;
import org.eclipse.ecsp.gateway.config.ClientAccessControlProperties;
import org.eclipse.ecsp.gateway.metrics.ClientAccessControlMetrics;
import org.eclipse.ecsp.gateway.model.AccessRule;
import org.eclipse.ecsp.gateway.model.ClientAccessConfig;
import org.eclipse.ecsp.gateway.service.AccessRuleMatcherService;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for merging YAML configuration overrides with database configurations.
 *
 * <p>Precedence rules (FR-039 to FR-043):
 * 1. YAML overrides take precedence over database configurations
 * 2. If same clientId exists in both YAML and database, use YAML version
 * 3. Database configurations without YAML overrides are preserved
 * 4. YAML-only clients (not in database) are added to cache
 *
 * <p>Use case: Emergency access management without database changes.
 */
@RequiredArgsConstructor
public class AccessControlConfigMerger {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(AccessControlConfigMerger.class);
    private final ClientAccessControlProperties yamlProperties;
    private final AccessRuleMatcherService ruleMatcherService;
    private final ClientAccessControlMetrics metrics;

    /**
     * Merge YAML overrides with database configurations.
     *
     * @param databaseConfigs Configurations from database
     * @return Merged configurations (YAML precedence applied)
     */
    public List<ClientAccessConfig> merge(List<ClientAccessConfig> databaseConfigs) {
        List<ClientAccessConfig> yamlConfigs = loadYamlConfigurations();

        if (yamlConfigs.isEmpty()) {
            LOGGER.debug("No YAML overrides configured");
            return databaseConfigs;
        }

        // Create map of YAML configs by clientId for fast lookup
        Map<String, ClientAccessConfig> yamlConfigMap = yamlConfigs.stream()
                .collect(Collectors.toMap(ClientAccessConfig::getClientId, config -> config));

        List<ClientAccessConfig> merged = new ArrayList<>();
        List<String> overriddenClientIds = new ArrayList<>();

        // Process database configs: Replace with YAML if override exists
        for (ClientAccessConfig dbConfig : databaseConfigs) {
            if (yamlConfigMap.containsKey(dbConfig.getClientId())) {
                // YAML override exists
                ClientAccessConfig yamlConfig = yamlConfigMap.get(dbConfig.getClientId());
                merged.add(yamlConfig);
                overriddenClientIds.add(dbConfig.getClientId());
                yamlConfigMap.remove(dbConfig.getClientId()); // Mark as processed
                
                // Record YAML override hit metric
                metrics.recordYamlOverrideHit(dbConfig.getClientId());
            } else {
                // No override, keep database config
                merged.add(dbConfig);
            }
        }

        // Add YAML-only configs (not in database)
        merged.addAll(yamlConfigMap.values());

        // Log overrides
        if (!overriddenClientIds.isEmpty()) {
            LOGGER.info("Applied YAML overrides for {} clients: {}", 
                    overriddenClientIds.size(), overriddenClientIds);
        }
        if (!yamlConfigMap.isEmpty()) {
            LOGGER.info("Added {} YAML-only clients: {}", 
                    yamlConfigMap.size(), yamlConfigMap.values().stream()
                            .map(ClientAccessConfig::getClientId)
                            .toList());
        }

        return merged;
    }

    /**
     * Load YAML configurations from properties.
     *
     * @return List of ClientAccessConfig from YAML
     */
    private List<ClientAccessConfig> loadYamlConfigurations() {
        List<ClientAccessConfig> configs = new ArrayList<>();

        for (ClientAccessControlProperties.YamlOverride override : yamlProperties.getOverrides()) {
            if (override.getClientId() == null || override.getClientId().isBlank()) {
                LOGGER.warn("Skipping YAML override with missing clientId");
                continue;
            }

            // Parse rules
            List<AccessRule> rules = ruleMatcherService.parseRules(override.getAllow());

            ClientAccessConfig config = ClientAccessConfig.builder()
                    .clientId(override.getClientId())
                    .tenant(override.getTenant())
                    .active(override.isActive())
                    .rules(rules)
                    .lastUpdated(Instant.now())
                    .source("YAML_OVERRIDE")
                    .build();

            configs.add(config);
        }

        LOGGER.debug("Loaded {} YAML configurations", configs.size());
        return configs;
    }

    /**
     * Get count of YAML overrides configured.
     *
     * @return Number of overrides
     */
    public int getYamlOverrideCount() {
        return yamlProperties.getOverrides().size();
    }
}
