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

import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.Collections;

/**
 * Class provides option for Cors Configuration.
 */
@Configuration
public class CorsConfiguration extends
        org.springframework.web.cors.CorsConfiguration {
    /**
     * allows a way to enable the cors configuration.
     */
    private static final IgniteLogger LOGGER
            = IgniteLoggerFactory.getLogger(CorsConfiguration.class);
    /**
     * gets the allowedOriginPatterns from configuration.
     */
    @Value("${spring.cloud.gateway.globalcors."
            + "corsConfigurations.config.allowedOriginPatterns}")
    private String allowedOriginPatterns;

    /**
     * gets the maxAge from configuration.
     */
    @Value("${spring.cloud.gateway.globalcors."
            + "corsConfigurations.config.maxAge}")
    private String maxAge;

    /**
     * gets the allowed methods for cors configuration.
     */
    @Value("${spring.cloud.gateway.globalcors."
            + "corsConfigurations.config.allowedMethods}")
    private String[] allowedMethods;

    /**
     * get allowed headers for cors configuration.
     */
    @Value("${spring.cloud.gateway.globalcors."
            + "corsConfigurations.config.allowedHeaders}")
    private String allowedHeaders;

    /**
     * method returns the cors objet.
     *
     * @return CorsWebFilter
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        final CorsConfiguration corsConfig = new CorsConfiguration();
        LOGGER.debug("allowedOrigins -> {}", allowedOriginPatterns);
        corsConfig.setMaxAge(Long.parseLong(maxAge));
        corsConfig.setAllowedMethods(Arrays.asList(allowedMethods));
        corsConfig.addAllowedHeader(allowedHeaders);
        corsConfig.setAllowedOriginPatterns(
                Collections.singletonList(allowedOriginPatterns));
        final UrlBasedCorsConfigurationSource source
                = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
