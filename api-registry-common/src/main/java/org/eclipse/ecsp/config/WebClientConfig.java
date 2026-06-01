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

package org.eclipse.ecsp.config;

import io.netty.channel.ChannelOption;
import org.eclipse.ecsp.security.ValidationConfigProperties;
import org.eclipse.ecsp.utils.RegistryCommonConstants;
import org.eclipse.ecsp.webclient.WebClientTokenFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import java.time.Duration;
import java.util.Optional;

/**
 * Auto-configuration for WebClient bean with token propagation support.
 *
 * <p>Registers a {@link WebClientCustomizer} bean so that the token-propagation
 * filter is applied to every {@link WebClient.Builder} created by Spring Boot's
 * auto-configuration. This ensures the filter is present on all
 * {@code WebClient} instances when
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
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = RegistryCommonConstants.API_REGISTRY_WEB_CLIENT_PROPAGATION_PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public WebClientTokenFilter webClientTokenFilter(ValidationConfigProperties config) {
        return new WebClientTokenFilter(config);
    }

    /**
     * Nested configuration class for WebClientCustomizer.
     */
    @Configuration
    @ConditionalOnClass(WebClientCustomizer.class)
    static class TokenCustomizerConfig {

        /**
         * Default constructor.
         */
        TokenCustomizerConfig() {
            // Default constructor
        }

        /**
         * Creates a {@link WebClientCustomizer} that registers the token-propagation filter
         * on every {@link WebClient.Builder} created in the application context.
         *
         * @param tokenFilter the filter that adds the propagated token to outgoing requests
         * @return the customizer
         */
        @Bean
        @ConditionalOnProperty(
            prefix = RegistryCommonConstants.API_REGISTRY_WEB_CLIENT_PROPAGATION_PREFIX,
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
        )
        public WebClientCustomizer webClientTokenFilterCustomizer(WebClientTokenFilter tokenFilter) {
            return builder -> builder.filter(tokenFilter);
        }
    }


    /**
     * Provides a fallback {@link WebClient.Builder} bean if none is already defined.
     *
     * <p>Spring Boot's auto-configuration will create a {@link WebClient.Builder} bean if
     * the {@code spring-boot-starter-webflux} dependency is present and no other builder is
     * defined. This method ensures that a builder is always available for the customizer to
     * apply the token filter, even in applications that do not use WebFlux.
     *
     * @return a default WebClient.Builder
     */
    @Bean
    @ConditionalOnMissingBean(WebClient.Builder.class)
    public WebClient.Builder webClientBuilder(Optional<WebClientTokenFilter> tokenFilter,
        @Value("${api.registry.web-client.connect-timeout-ms:5000}") long connectTimeoutMs,
        @Value("${api.registry.web-client.read-timeout-ms:5000}") long readTimeoutMs) {
        
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs));
        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        WebClient.Builder builder = WebClient.builder();
        tokenFilter.ifPresent(builder::filter);
        builder.clientConnector(connector);
        return builder;
    }

    /**
     * Provides a fallback {@link WebClient} bean if none is already defined.
     *
     * <p>Spring Boot's auto-configuration will create a {@link WebClient} bean if the
     * {@code spring-boot-starter-webflux} dependency is present and no other WebClient is
     * defined. This method ensures that a WebClient is always available, even in applications
     * that do not use WebFlux.
     *
     * @param builder the WebClient.Builder to build the WebClient with
     * @return a default WebClient
     */
    @Bean
    @ConditionalOnMissingBean(WebClient.class)
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
}
