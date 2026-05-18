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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.security;

import org.eclipse.ecsp.tokenvalidator.model.PublicKeySource;
import org.eclipse.ecsp.utils.RegistryCommonConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for token validation and token propagation.
 *
 * <p>Bound to the {@code api.registry} prefix. Controls whether JWT validation
 * is active and how tokens are forwarded to downstream services.
 */
@ConfigurationProperties(prefix = RegistryCommonConstants.API_REGISTRY_PREFIX)
public class ValidationConfigProperties {

    private Security security = new Security();
    private TokenPropagation tokenPropagation = new TokenPropagation();

    /**
     * Default constructor.
     */
    public ValidationConfigProperties() {
        // Default constructor
    }

    /**
     * Returns the security sub-properties.
     *
     * @return the security configuration
     */
    public Security getSecurity() {
        return security;
    }

    /**
     * Sets the security sub-properties.
     *
     * @param security the security configuration
     */
    public void setSecurity(Security security) {
        this.security = security;
    }

    /**
     * Returns the token-propagation sub-properties.
     *
     * @return the token-propagation configuration
     */
    public TokenPropagation getTokenPropagation() {
        return tokenPropagation;
    }

    /**
     * Sets the token-propagation sub-properties.
     *
     * @param tokenPropagation the token-propagation configuration
     */
    public void setTokenPropagation(TokenPropagation tokenPropagation) {
        this.tokenPropagation = tokenPropagation;
    }

    /**
     * Security sub-properties controlling whether JWT validation is enabled.
     */
    public static class Security {

        private boolean enabled = false;
        private List<PublicKeySource> keySources = new ArrayList<>();

        /**
         * Default constructor.
         */
        public Security() {
            // Default constructor
        }

        /**
         * Returns whether JWT token validation is enabled.
         *
         * @return {@code true} if validation is active
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether JWT token validation is enabled.
         *
         * @param enabled {@code true} to activate validation
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the list of public key sources for JWT validation.
         *
         * @return the list of public key sources
         */
        public List<PublicKeySource> getKeySources() {
            return keySources;
        }

        /**
         * Sets the list of public key sources for JWT validation.
         *
         * @param keySources the list of public key sources
         */
        public void setKeySources(List<PublicKeySource> keySources) {
            this.keySources = keySources;
        }
    }

    /**
     * Token-propagation sub-properties controlling how Bearer tokens are forwarded
     * to downstream services via RestTemplate, WebClient, and RestClient.
     */
    public static class TokenPropagation {

        private RestTemplate restTemplate = new RestTemplate();
        private WebClient webClient = new WebClient();
        private RestClient restClient = new RestClient();
        private List<String> includeHosts = new ArrayList<>();
        private List<String> excludeHosts = new ArrayList<>();
        private boolean allowExternalHosts = false;

        /**
         * Default constructor.
         */
        public TokenPropagation() {
            // Default constructor
        }

        /**
         * Returns the RestTemplate propagation settings.
         *
         * @return the RestTemplate propagation settings
         */
        public RestTemplate getRestTemplate() {
            return restTemplate;
        }

        /**
         * Sets the RestTemplate propagation settings.
         *
         * @param restTemplate the settings to apply
         */
        public void setRestTemplate(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
        }

        /**
         * Returns the WebClient propagation settings.
         *
         * @return the WebClient propagation settings
         */
        public WebClient getWebClient() {
            return webClient;
        }

        /**
         * Sets the WebClient propagation settings.
         *
         * @param webClient the settings to apply
         */
        public void setWebClient(WebClient webClient) {
            this.webClient = webClient;
        }

        /**
         * Returns the RestClient propagation settings.
         *
         * @return the RestClient propagation settings
         */
        public RestClient getRestClient() {
            return restClient;
        }

        /**
         * Sets the RestClient propagation settings.
         *
         * @param restClient the settings to apply
         */
        public void setRestClient(RestClient restClient) {
            this.restClient = restClient;
        }

        /**
         * Returns the hosts that should always receive the propagated token.
         *
         * @return list of included host patterns
         */
        public List<String> getIncludeHosts() {
            return includeHosts;
        }

        /**
         * Sets the hosts that should always receive the propagated token.
         *
         * @param includeHosts list of included host patterns
         */
        public void setIncludeHosts(List<String> includeHosts) {
            this.includeHosts = includeHosts;
        }

        /**
         * Returns the hosts that must never receive the propagated token.
         *
         * @return list of excluded host patterns
         */
        public List<String> getExcludeHosts() {
            return excludeHosts;
        }

        /**
         * Sets the hosts that must never receive the propagated token.
         *
         * @param excludeHosts list of excluded host patterns
         */
        public void setExcludeHosts(List<String> excludeHosts) {
            this.excludeHosts = excludeHosts;
        }

        /**
         * Returns whether tokens may be forwarded to external (non-internal) hosts.
         *
         * @return {@code true} if external hosts are allowed
         */
        public boolean isAllowExternalHosts() {
            return allowExternalHosts;
        }

        /**
         * Sets whether tokens may be forwarded to external hosts.
         *
         * @param allowExternalHosts {@code true} to permit external forwarding
         */
        public void setAllowExternalHosts(boolean allowExternalHosts) {
            this.allowExternalHosts = allowExternalHosts;
        }

        /**
         * RestTemplate-specific propagation toggle.
         */
        public static class RestTemplate {

            private boolean enabled = true;

            /**
             * Default constructor.
             */
            public RestTemplate() {
                // Default constructor
            }

            /**
             * Returns whether RestTemplate token propagation is enabled.
             *
             * @return {@code true} if enabled
             */
            public boolean isEnabled() {
                return enabled;
            }

            /**
             * Sets whether RestTemplate token propagation is enabled.
             *
             * @param enabled {@code true} to enable
             */
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }

        /**
         * WebClient-specific propagation toggle.
         */
        public static class WebClient {

            private boolean enabled = true;

            /**
             * Default constructor.
             */
            public WebClient() {
                // Default constructor
            }

            /**
             * Returns whether WebClient token propagation is enabled.
             *
             * @return {@code true} if enabled
             */
            public boolean isEnabled() {
                return enabled;
            }

            /**
             * Sets whether WebClient token propagation is enabled.
             *
             * @param enabled {@code true} to enable
             */
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }

        /**
         * RestClient-specific propagation toggle.
         */
        public static class RestClient {

            private boolean enabled = true;

            /**
             * Default constructor.
             */
            public RestClient() {
                // Default constructor
            }

            /**
             * Returns whether RestClient token propagation is enabled.
             *
             * @return {@code true} if enabled
             */
            public boolean isEnabled() {
                return enabled;
            }

            /**
             * Sets whether RestClient token propagation is enabled.
             *
             * @param enabled {@code true} to enable
             */
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }
    }
}
