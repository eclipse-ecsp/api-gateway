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

import org.eclipse.ecsp.gateway.config.JwtProperties;
import org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter;
import org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter.Config;
import org.eclipse.ecsp.gateway.service.PublicKeyService;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * Filter to validate JWT Token.
 */
@Component
public class JwtAuthValidator extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {
    private final PublicKeyService publicKeyService;
    private final JwtProperties jwtProperties;

    /**
     * Constructor to initialize the JwtAuthValidator.
     *
     * @param publicKeyService Service to load JWT public keys.
     * @param jwtProperties properties containing JWT configuration.
     */
    public JwtAuthValidator(PublicKeyService publicKeyService, JwtProperties jwtProperties) {
        super(Config.class);
        this.publicKeyService = publicKeyService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new JwtAuthFilter(config, publicKeyService, jwtProperties);
    }

}

