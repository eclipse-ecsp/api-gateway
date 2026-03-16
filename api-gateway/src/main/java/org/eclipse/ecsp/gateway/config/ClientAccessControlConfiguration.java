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
import org.eclipse.ecsp.gateway.conditions.ClientAccessControlEnabledCondition;
import org.eclipse.ecsp.gateway.customizers.ClientAccessControlCustomizer;
import org.eclipse.ecsp.gateway.filter.ClientAccessControlGatewayFilterFactory;
import org.eclipse.ecsp.gateway.metrics.ClientAccessControlMetrics;
import org.eclipse.ecsp.gateway.service.AccessRuleMatcherService;
import org.eclipse.ecsp.gateway.service.ClientAccessControlService;
import org.eclipse.ecsp.gateway.utils.AccessControlConfigMerger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Client Access Control feature.
 *
 * <p>Defines beans and conditions to enable client access control filtering in the API Gateway.
 *
 * <p>Client Access Control allows the gateway to extract client ID from incoming requests (e.g. from JWT claims)
 * and apply route-specific access control based on client identity. This configuration is activated when
 * 'api.gateway.client-access-control.enabled' property is set to true.
 *
 * <p>The ClientAccessControlCustomizer bean adds a filter to routes that checks for client ID in the request
 * and enforces access control based on the configured properties.
 *
 * @author Abhishek Kumar
 */
@Configuration
@Conditional(ClientAccessControlEnabledCondition.class)
public class ClientAccessControlConfiguration {

    private final ClientAccessControlProperties properties;

    /**
     * Constructor for ClientAccessControlConfiguration.
     *
     * @param properties the client access control properties
     */
    public ClientAccessControlConfiguration(ClientAccessControlProperties properties) {
        this.properties = properties;
    }


    /**
     * Bean for ClientAccessControlCustomizer which adds the client access control filter to routes.
     *
     * @return a new instance of ClientAccessControlCustomizer
     */
    @Bean
    public ClientAccessControlCustomizer clientAccessControlCustomizer() {
        return new ClientAccessControlCustomizer(properties);
    }

    /**
     * Bean for AccessControlConfigMerger which merges YAML and API registry configurations.
     *
     * @param properties the client access control properties
     * @param ruleMatcherService the service used to match access rules
     * @param metrics the metrics collector for client access control
     * @return a new instance of AccessControlConfigMerger
     */
    @Bean
    public AccessControlConfigMerger accessControlConfigMerger(ClientAccessControlProperties properties, 
            AccessRuleMatcherService ruleMatcherService, 
            ClientAccessControlMetrics metrics) {
        return new AccessControlConfigMerger(properties, ruleMatcherService, metrics);
    }

    /**
     * Bean for AccessRuleMatcherService which matches incoming requests to access control rules.
     *
     * @return a new instance of AccessRuleMatcherService
     */
    @Bean
    public AccessRuleMatcherService accessRuleMatcherService() {
        return new AccessRuleMatcherService();
    }

    /**
     * Bean for ClientAccessControlService which manages the cache of client access configurations.
     *
     * @param ruleMatcherService the service used to match access rules
     * @param configMerger the service used to merge YAML and API registry configurations
     * @param metrics the metrics collector for client access control
     * @param apiRegistryClient the client to fetch configurations from API registry
     * @return a new instance of ClientAccessControlService
     */
    @Bean
    public ClientAccessControlService clientAccessControlCacheService(AccessRuleMatcherService ruleMatcherService,
            AccessControlConfigMerger configMerger, ClientAccessControlMetrics metrics, 
            ApiRegistryClient apiRegistryClient) {
        return new ClientAccessControlService(ruleMatcherService, apiRegistryClient, 
            configMerger, metrics);
    }

    /**
     * Bean for ClientAccessControlGatewayFilterFactory which creates the gateway filter for client access control.
     *
     * @param accessRuleMatcherService the service used to match access rules
     * @param cacheService the service managing the cache of client access configurations
     * @param metrics the metrics collector for client access control
     * @return a new instance of ClientAccessControlGatewayFilterFactory
     */
    @Bean
    public ClientAccessControlGatewayFilterFactory clientAccessControlGatewayFilterFactory(
            AccessRuleMatcherService accessRuleMatcherService,
            ClientAccessControlService cacheService,
            ClientAccessControlMetrics metrics) {
        return new ClientAccessControlGatewayFilterFactory(properties, accessRuleMatcherService, cacheService,
                metrics);
    }

}
