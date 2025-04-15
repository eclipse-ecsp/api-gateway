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

package org.eclipse.ecsp.gateway.plugins;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter;
import org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter.Config;
import org.eclipse.ecsp.gateway.utils.JwtPublicKeyLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Filter to validate JWT Token.
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtAuthValidator extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    @Getter
    @Setter
    private Map<String, Map<String, String>> tokenHeaderValidationConfig;

    private final JwtPublicKeyLoader jwtPublicKeyLoader;

    @Value("${api.userId.field}")
    private String userIdField;

    /**
     * Constructor to initialize the JwtAuthValidator.
     *
     * @param jwtPublicKeyLoader the JWT public key loader
     */
    public JwtAuthValidator(JwtPublicKeyLoader jwtPublicKeyLoader) {
        super(Config.class);
        this.jwtPublicKeyLoader = jwtPublicKeyLoader;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new JwtAuthFilter(config, jwtPublicKeyLoader.getJwtParsers(), tokenHeaderValidationConfig, userIdField);
    }


}

