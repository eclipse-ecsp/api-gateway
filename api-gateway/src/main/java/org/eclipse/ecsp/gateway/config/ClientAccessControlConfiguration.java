package org.eclipse.ecsp.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.ecsp.gateway.clients.ApiRegistryClient;
import org.eclipse.ecsp.gateway.conditions.ClientAccessControlEnabledCondition;
import org.eclipse.ecsp.gateway.customizers.ClientAccessControlCustomizer;
import org.eclipse.ecsp.gateway.filter.ClientAccessControlGatewayFilterFactory;
import org.eclipse.ecsp.gateway.metrics.ClientAccessControlMetrics;
import org.eclipse.ecsp.gateway.service.AccessRuleMatcherService;
import org.eclipse.ecsp.gateway.service.ClientAccessControlCacheService;
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

    public ClientAccessControlConfiguration(ClientAccessControlProperties properties) {
        this.properties = properties;
    }


    @Bean
    public ClientAccessControlCustomizer clientAccessControlCustomizer() {
        return new ClientAccessControlCustomizer(properties);
    }

    @Bean
    public AccessControlConfigMerger accessControlConfigMerger(ClientAccessControlProperties properties, 
            AccessRuleMatcherService ruleMatcherService, 
            ClientAccessControlMetrics metrics) {
        return new AccessControlConfigMerger(properties, ruleMatcherService, metrics);
    }

    @Bean
    public AccessRuleMatcherService accessRuleMatcherService() {
        return new AccessRuleMatcherService();
    }

    @Bean
    public ClientAccessControlCacheService clientAccessControlCacheService(AccessRuleMatcherService ruleMatcherService,
            AccessControlConfigMerger configMerger, ClientAccessControlMetrics metrics, 
            ApiRegistryClient apiRegistryClient) {
        return new ClientAccessControlCacheService(ruleMatcherService, apiRegistryClient, 
            configMerger, metrics);
    }

    @Bean
    public ClientAccessControlGatewayFilterFactory clientAccessControlGatewayFilterFactory(
            AccessRuleMatcherService accessRuleMatcherService,
            ClientAccessControlCacheService cacheService,
            ClientAccessControlMetrics metrics) {
        return new ClientAccessControlGatewayFilterFactory(properties, accessRuleMatcherService, cacheService,
                metrics);
    }

}
