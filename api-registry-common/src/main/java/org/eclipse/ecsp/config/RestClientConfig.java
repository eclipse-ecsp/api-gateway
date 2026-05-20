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

package org.eclipse.ecsp.config;

import org.eclipse.ecsp.restclient.RestClientTokenInterceptor;
import org.eclipse.ecsp.security.ValidationConfigProperties;
import org.eclipse.ecsp.utils.RegistryCommonConstants;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Auto-configuration that binds the {@link RestClientTokenInterceptor} to
 * {@link RestClient} instances when token propagation is enabled.
 *
 * <p>A {@link RestClientCustomizer} bean is registered so that any
 * {@link RestClient} created via an injected {@code RestClient.Builder} automatically
 * receives the token interceptor. A fallback {@link RestClient} bean is also
 * provided for applications that do not define their own.
 */
@Configuration
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(ValidationConfigProperties.class)
public class RestClientConfig {

    /**
     * Default constructor.
     */
    public RestClientConfig() {
        // Default constructor
    }

    /**
     * Creates a {@link RestClientTokenInterceptor} bean for Bearer-token propagation.
     *
     * <p>Only registered when
     * {@code api.registry.token-propagation.rest-client.enabled=true}
     * (the default when the property is absent).
     *
     * @param config the validation / propagation configuration properties
     * @return the token interceptor
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = RegistryCommonConstants.API_REGISTRY_REST_CLIENT_PROPAGATION_PREFIX,
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
        )
    public RestClientTokenInterceptor restClientTokenInterceptor(ValidationConfigProperties config) {
        return new RestClientTokenInterceptor(config);
    }

    /**
     * Creates a {@link RestClient.Builder} bean that applies the token interceptor to
     * every {@link RestClient} built via an injected builder.
     *
     * <p>Only registered when
     * {@code api.registry.token-propagation.rest-client.enabled=true}
     * (the default when the property is absent).
     *
     * @param tokenInterceptor the interceptor that adds the propagated token to outgoing requests
     * @return the configured builder
     */
    @Bean
    @ConditionalOnMissingBean(RestClient.Builder.class)
    @ConditionalOnProperty(
            prefix = RegistryCommonConstants.API_REGISTRY_REST_CLIENT_PROPAGATION_PREFIX,
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
        )
    public RestClient.Builder restClientBuilder(RestClientTokenInterceptor tokenInterceptor) {
        return RestClient.builder().requestInterceptor(tokenInterceptor);
    }

    /**
     * Fallback {@link RestClient} bean created when the application context does not
     * already contain one.
     *
     * <p>Pre-configured with the {@link RestClientTokenInterceptor} when token
     * propagation is enabled.
     *
     * @param tokenInterceptorProvider provider for the optional token interceptor
     * @return the configured {@link RestClient}
     */
    @Bean
    @ConditionalOnMissingBean(RestClient.class)
    public RestClient registryRestClient(
            ObjectProvider<RestClientTokenInterceptor> tokenInterceptorProvider) {
        RestClient.Builder builder = RestClient.builder();
        RestClientTokenInterceptor tokenInterceptor = tokenInterceptorProvider.getIfAvailable();
        if (tokenInterceptor != null) {
            builder.requestInterceptor(tokenInterceptor);
        }
        return builder.build();
    }

    /**
     * Registers a {@link RestClientCustomizer} that wires the token interceptor into
     * every {@link RestClient} created via an injected {@code RestClient.Builder}.
     *
     * <p>Isolated in a nested {@link Configuration} so that Spring Boot never
     * introspects this class — and triggers a {@link NoClassDefFoundError} —
     * when {@code spring-boot-restclient} (which contains {@link RestClientCustomizer})
     * is absent from the runtime classpath.
     */
    @Configuration
    @ConditionalOnClass(RestClientCustomizer.class)
    static class TokenCustomizerConfig {

        /**
         * Default constructor.
         */
        TokenCustomizerConfig() {
            // Default constructor
        }

        /**
         * Creates a {@link RestClientCustomizer} that attaches the token interceptor to
         * every {@link RestClient} built via an injected {@code RestClient.Builder}.
         *
         * @param tokenInterceptorProvider provider for the optional token interceptor
         * @return the customizer
         */
        @Bean
        public RestClientCustomizer restClientTokenCustomizer(
                ObjectProvider<RestClientTokenInterceptor> tokenInterceptorProvider) {
            return builder -> {
                RestClientTokenInterceptor tokenInterceptor = tokenInterceptorProvider.getIfAvailable();
                if (tokenInterceptor != null) {
                    builder.requestInterceptor(tokenInterceptor);
                }
            };
        }
    }
}