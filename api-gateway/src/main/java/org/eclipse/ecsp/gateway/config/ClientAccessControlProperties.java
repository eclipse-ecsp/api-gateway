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

import lombok.Getter;
import lombok.Setter;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Client Access Control feature.
 *
 * <p>Loaded from application.yml under 'api.gateway.client-access-control.*'
 *
 * <p>Example configuration:
 * <pre>
 * api:
 *   gateway:
 *     client-access-control:
 *       enabled: true
 *       claim-names:
 *         - clientId
 *         - azp
 *         - client_id
 *         - cid
 *       skip-paths:
 *         - "/v3/api-docs/**"
 *         - "/actuator/**"
 *       overrides:
 *        - clientId: "client-123"
 *          tenant: "tenantA"
 *          description: "Override for client-123 in tenantA"
 *          active: true
 *          allow:
 *           - "user-service:*"
 *           - "!payment-service:refund"
 * </pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = GatewayConstants.CLIENT_ACCESS_CONTROL_PREFIX)
public class ClientAccessControlProperties {

    /**
     * Enable or disable client access control filtering globally.
     * Default: false (disabled). Set to true to activate the feature.
     */
    private boolean enabled = false;

    /**
     * Ordered list of JWT claim names to try for extracting client ID.
     * The first non-empty claim value is used.
     *
     * <p>Default order supports multiple IDPs:
     * - clientId: Custom claim
     * - client_id: OAuth2/OIDC standard
     * - azp: Azure AD authorized party
     * - cid: Alternative client identifier
     */
    private List<String> claimNames = List.of("clientId", "client_id", "azp", "cid");

    /**
     * Ant-style path patterns to skip validation.
     * Useful for health checks, API documentation, actuator endpoints.
     *
     * <p>Examples:
     * - "/v3/api-docs/**" - OpenAPI documentation
     * - "/actuator/**" - Spring Boot actuator
     * - "/health" - Health check endpoint
     */
    private List<String> skipPaths = new ArrayList<>();

    /**
     * List of client access control overrides from YAML.
     * Empty by default (no overrides).
     */
    private List<YamlOverride> overrides = new ArrayList<>();

    /**
     * Single YAML override entry.
     */
    @Getter
    @Setter
    public static class YamlOverride {
        /**
         * Client identifier (must match database clientId to override).
         */
        private String clientId;

        /**
         * Tenant/organization name.
         */
        private String tenant;

        /**
         * Description (optional).
         */
        private String description;

        /**
         * Active status flag.
         */
        private boolean active = true;

        /**
         * Allow rules in service:route format.
         * Example: ["user-service:*", "!payment-service:refund"]
         */
        private List<String> allow = new ArrayList<>();
    }
}
