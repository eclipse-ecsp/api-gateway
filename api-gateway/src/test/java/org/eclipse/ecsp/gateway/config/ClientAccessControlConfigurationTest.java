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

package org.eclipse.ecsp.gateway.config;

import org.eclipse.ecsp.gateway.clients.ApiRegistryClient;
import org.eclipse.ecsp.gateway.customizers.ClientAccessControlCustomizer;
import org.eclipse.ecsp.gateway.filter.ClientAccessControlGatewayFilterFactory;
import org.eclipse.ecsp.gateway.metrics.ClientAccessControlMetrics;
import org.eclipse.ecsp.gateway.service.AccessRuleMatcherService;
import org.eclipse.ecsp.gateway.service.ClientAccessControlService;
import org.eclipse.ecsp.gateway.utils.AccessControlConfigMerger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test purpose    - Verify ClientAccessControlConfiguration bean creation.
 * Test data       - Mocked dependencies.
 * Test expected   - Beans are created correctly.
 * Test type       - Positive and Negative.
 */
@ExtendWith(MockitoExtension.class)
class ClientAccessControlConfigurationTest {

    @Mock
    private ClientAccessControlProperties properties;

    @Mock
    private AccessRuleMatcherService ruleMatcherService;

    @Mock
    private ClientAccessControlMetrics metrics;

    @Mock
    private ApiRegistryClient apiRegistryClient;

    private ClientAccessControlConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new ClientAccessControlConfiguration(properties);
    }

    /**
     * Test purpose          - Verify clientAccessControlCustomizer bean creation.
     * Test data             - Valid ClientAccessControlProperties.
     * Test expected result  - ClientAccessControlCustomizer bean is created.
     * Test type             - Positive.
     */
    @Test
    void clientAccessControlCustomizerValidPropertiesReturnsBean() {
        // GIVEN: Configuration with properties
        // (setup in setUp method)

        // WHEN: Bean is created
        ClientAccessControlCustomizer customizer = configuration.clientAccessControlCustomizer();

        // THEN: Bean should be created successfully
        assertNotNull(customizer);
        assertInstanceOf(ClientAccessControlCustomizer.class, customizer);
    }

    /**
     * Test purpose          - Verify accessControlConfigMerger bean creation.
     * Test data             - Valid dependencies.
     * Test expected result  - AccessControlConfigMerger bean is created.
     * Test type             - Positive.
     */
    @Test
    void accessControlConfigMergerValidDependenciesReturnsBean() {
        // GIVEN: Configuration with mocked dependencies

        // WHEN: Bean is created
        AccessControlConfigMerger merger = configuration.accessControlConfigMerger(
                properties, ruleMatcherService, metrics);

        // THEN: Bean should be created successfully
        assertNotNull(merger);
        assertInstanceOf(AccessControlConfigMerger.class, merger);
    }

    /**
     * Test purpose          - Verify accessRuleMatcherService bean creation.
     * Test data             - No dependencies.
     * Test expected result  - AccessRuleMatcherService bean is created.
     * Test type             - Positive.
     */
    @Test
    void accessRuleMatcherServiceNoDependenciesReturnsBean() {
        // GIVEN: Configuration

        // WHEN: Bean is created
        AccessRuleMatcherService service = configuration.accessRuleMatcherService();

        // THEN: Bean should be created successfully
        assertNotNull(service);
        assertInstanceOf(AccessRuleMatcherService.class, service);
    }

    /**
     * Test purpose          - Verify clientAccessControlCacheService bean creation.
     * Test data             - Valid dependencies.
     * Test expected result  - ClientAccessControlCacheService bean is created.
     * Test type             - Positive.
     */
    @Test
    void clientAccessControlCacheServiceValidDependenciesReturnsBean() {
        // GIVEN: Configuration with mocked dependencies
        AccessControlConfigMerger configMerger = new AccessControlConfigMerger(
                properties, ruleMatcherService, metrics);

        // WHEN: Bean is created
        ClientAccessControlService cacheService = configuration.clientAccessControlCacheService(
                ruleMatcherService, configMerger, metrics, apiRegistryClient);

        // THEN: Bean should be created successfully
        assertNotNull(cacheService);
        assertInstanceOf(ClientAccessControlService.class, cacheService);
    }

    /**
     * Test purpose          - Verify clientAccessControlGatewayFilterFactory bean creation.
     * Test data             - Valid dependencies.
     * Test expected result  - ClientAccessControlGatewayFilterFactory bean is created.
     * Test type             - Positive.
     */
    @Test
    void clientAccessControlGatewayFilterFactoryValidDependenciesReturnsBean() {
        // GIVEN: Configuration with mocked dependencies
        AccessControlConfigMerger configMerger = new AccessControlConfigMerger(
                properties, ruleMatcherService, metrics);
        ClientAccessControlService cacheService = new ClientAccessControlService(
                ruleMatcherService, apiRegistryClient, configMerger, metrics);

        // WHEN: Bean is created
        ClientAccessControlGatewayFilterFactory factory = 
                configuration.clientAccessControlGatewayFilterFactory(
                        ruleMatcherService, cacheService, metrics);

        // THEN: Bean should be created successfully
        assertNotNull(factory);
        assertInstanceOf(ClientAccessControlGatewayFilterFactory.class, factory);
    }
}
