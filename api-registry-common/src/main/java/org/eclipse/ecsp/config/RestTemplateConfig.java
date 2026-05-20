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

import org.eclipse.ecsp.restclient.RestTemplateErrorHandler;
import org.eclipse.ecsp.restclient.RestTemplateLogInterceptor;
import org.eclipse.ecsp.restclient.RestTemplateTokenInterceptor;
import org.eclipse.ecsp.security.ValidationConfigProperties;
import org.eclipse.ecsp.utils.RegistryCommonConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Auto-configuration that binds the {@link RestTemplateTokenInterceptor} to
 * {@link RestTemplate} instances when token propagation is enabled.
 *
 * <p>A {@link RestTemplateCustomizer} bean is registered so that any
 * {@link RestTemplate} created via {@code RestTemplateBuilder} automatically
 * receives the token interceptor. A fallback {@link RestTemplate} bean is also
 * provided for applications that do not define their own.
 */
@Configuration
@ConditionalOnClass(RestTemplate.class)
@EnableConfigurationProperties(ValidationConfigProperties.class)
public class RestTemplateConfig {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RestTemplateConfig.class);

    /**
     * Default constructor.
     */
    public RestTemplateConfig() {
        // Default constructor
    }

    /**
     * Creates a {@link RestTemplateTokenInterceptor} bean for Bearer-token propagation.
     *
     * <p>Only registered when
     * {@code api.registry.token-propagation.rest-template.enabled=true}
     * (the default when the property is absent).
     *
     * @param config the validation / propagation configuration properties
     * @return the token interceptor
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = RegistryCommonConstants.API_REGISTRY_REST_TEMPLATE_PROPAGATION_PREFIX,
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
        )
    public RestTemplateTokenInterceptor restTemplateTokenInterceptor(ValidationConfigProperties config) {
        return new RestTemplateTokenInterceptor(config);
    }

    /**
     * Fallback {@link RestTemplate} bean created when the application context does not
     * already contain one.
     *
     * <p>Pre-configured with:
     * <ul>
     *   <li>A {@link RestTemplateErrorHandler} for consistent error handling</li>
     *   <li>A {@link RestTemplateLogInterceptor} when DEBUG logging is active</li>
     *   <li>The {@link RestTemplateTokenInterceptor} when token propagation is enabled</li>
     * </ul>
     *
     * @param tokenInterceptorProvider provider for the optional token interceptor
     * @return the configured {@link RestTemplate}
     */
    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate registryRestTemplate(
            @Value("${api.registry.rest-template.handle-error:true}") boolean handleError,
            ObjectProvider<RestTemplateTokenInterceptor> tokenInterceptorProvider) {
        RestTemplate restTemplate = new RestTemplate();

        if (handleError) {
            restTemplate.setErrorHandler(new RestTemplateErrorHandler());
        }
        if (LOGGER.isDebugEnabled()) {
            restTemplate.getInterceptors().add(new RestTemplateLogInterceptor());
        }
        RestTemplateTokenInterceptor tokenInterceptor = tokenInterceptorProvider.getIfAvailable();
        if (tokenInterceptor != null) {
            restTemplate.getInterceptors().add(tokenInterceptor);
        }
        return restTemplate;
    }

    /**
     * Registers a {@link RestTemplateCustomizer} that wires the token interceptor into
     * every {@link RestTemplate} created via {@code RestTemplateBuilder}.
     *
     * <p>Isolated in a nested {@link Configuration} so that Spring Boot never
     * introspects this class \u2014 and triggers a {@link NoClassDefFoundError} \u2014
     * when {@code spring-boot-restclient} (which contains {@link RestTemplateCustomizer})
     * is absent from the runtime classpath.
     */
    @Configuration
    @ConditionalOnClass(RestTemplateCustomizer.class)
    static class TokenCustomizerConfig {

        /**
         * Default constructor.
         */
        TokenCustomizerConfig() {
            // Default constructor
        }

        /**
         * Creates a {@link RestTemplateCustomizer} that attaches the token interceptor to
         * every {@link RestTemplate} built via {@code RestTemplateBuilder}.
         *
         * @param tokenInterceptorProvider provider for the optional token interceptor
         * @return the customizer
         */
        @Bean
        public RestTemplateCustomizer restTemplateTokenCustomizer(
                ObjectProvider<RestTemplateTokenInterceptor> tokenInterceptorProvider) {
            return restTemplate -> {
                RestTemplateTokenInterceptor tokenInterceptor = tokenInterceptorProvider.getIfAvailable();
                if (tokenInterceptor != null) {
                    restTemplate.getInterceptors().add(tokenInterceptor);
                }
            };
        }
    }
}
