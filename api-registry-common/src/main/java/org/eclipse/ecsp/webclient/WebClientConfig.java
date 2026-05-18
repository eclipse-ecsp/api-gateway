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

package org.eclipse.ecsp.webclient;

import org.eclipse.ecsp.security.ValidationConfigProperties;
import org.eclipse.ecsp.utils.RegistryCommonConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration for WebClient bean with token propagation support.
 *
 * <p>Creates a {@link WebClient.Builder} bean pre-configured with
 * {@link WebClientTokenFilter} when {@code spring-webflux} is on the classpath and
 * {@code api.registry.token-propagation.web-client.enabled=true}.
 */
@Configuration
@ConditionalOnClass(WebClient.class)
@EnableConfigurationProperties(ValidationConfigProperties.class)
public class WebClientConfig {

    /**
     * Default constructor.
     */
    public WebClientConfig() {
        // Default constructor
    }

    /**
     * Creates a {@link WebClientTokenFilter} bean for token propagation.
     *
     * @param config the validation / propagation configuration properties
     * @return the filter
     */
    @Bean
    @ConditionalOnProperty(
        prefix = RegistryCommonConstants.API_REGISTRY_WEB_CLIENT_PROPAGATION_PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public WebClientTokenFilter webClientTokenFilter(ValidationConfigProperties config) {
        return new WebClientTokenFilter(config);
    }
}
