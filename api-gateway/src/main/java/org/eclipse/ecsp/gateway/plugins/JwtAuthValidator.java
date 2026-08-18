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

package org.eclipse.ecsp.gateway.plugins;

import org.eclipse.ecsp.gateway.config.JwtProperties;
import org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter;
import org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter.Config;
import org.eclipse.ecsp.gateway.plugins.spi.AdditionalClaimValidator;
import org.eclipse.ecsp.gateway.plugins.spi.ScopeValidator;
import org.eclipse.ecsp.gateway.plugins.spi.SignatureVerifier;
import org.eclipse.ecsp.gateway.plugins.spi.TokenClaimHeaderMapper;
import org.eclipse.ecsp.gateway.plugins.spi.TokenClaimValidator;
import org.eclipse.ecsp.gateway.plugins.spi.TokenDecoder;
import org.eclipse.ecsp.gateway.plugins.spi.TokenParser;
import org.eclipse.ecsp.gateway.service.PublicKeyService;
import org.eclipse.ecsp.gateway.service.TokenValidationComponents;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * Factory that creates {@link JwtAuthFilter} instances for each gateway route.
 *
 * <p>All token-pipeline SPI beans are injected here and forwarded to each
 * {@link JwtAuthFilter} instance. Spring resolves the appropriate implementation
 * for each SPI automatically: if a custom bean is registered it takes precedence,
 * otherwise the {@code @ConditionalOnMissingBean} default from
 * {@link org.eclipse.ecsp.gateway.config.GatewayConfig} is used.
 */
@Component
public class JwtAuthValidator extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private final PublicKeyService publicKeyService;
    private final JwtProperties jwtProperties;
    private final TokenParser tokenParser;
    private final TokenDecoder tokenDecoder;
    private final SignatureVerifier signatureVerifier;
    private final TokenClaimValidator tokenClaimValidator;
    private final AdditionalClaimValidator additionalClaimValidator;
    private final ScopeValidator scopeValidator;
    private final TokenClaimHeaderMapper tokenClaimHeaderMapper;

    /**
     * Constructor to initialize the JwtAuthValidator with all token-pipeline SPIs.
     *
     * @param publicKeyService         service to resolve JWT public keys
     * @param jwtProperties            JWT configuration properties
     * @param tokenParser              SPI for extracting the raw token from the exchange
     * @param tokenDecoder             SPI for decoding JWT structure without signature verification
     * @param signatureVerifier        SPI for verifying the JWT signature
     * @param tokenClaimValidator      SPI for config-driven claim validation
     * @param additionalClaimValidator SPI for custom programmatic claim validation
     * @param scopeValidator           SPI for scope validation
     * @param tokenClaimHeaderMapper   SPI for mapping claims to downstream request headers
     */
    public JwtAuthValidator(PublicKeyService publicKeyService,
                            JwtProperties jwtProperties,
                            TokenParser tokenParser,
                            TokenDecoder tokenDecoder,
                            SignatureVerifier signatureVerifier,
                            TokenClaimValidator tokenClaimValidator,
                            AdditionalClaimValidator additionalClaimValidator,
                            ScopeValidator scopeValidator,
                            TokenClaimHeaderMapper tokenClaimHeaderMapper) {
        super(Config.class);
        this.publicKeyService = publicKeyService;
        this.jwtProperties = jwtProperties;
        this.tokenParser = tokenParser;
        this.tokenDecoder = tokenDecoder;
        this.signatureVerifier = signatureVerifier;
        this.tokenClaimValidator = tokenClaimValidator;
        this.additionalClaimValidator = additionalClaimValidator;
        this.scopeValidator = scopeValidator;
        this.tokenClaimHeaderMapper = tokenClaimHeaderMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        TokenValidationComponents validationComponents = 
            new TokenValidationComponents(
                tokenParser,
                tokenDecoder, 
                signatureVerifier, 
                tokenClaimValidator, 
                additionalClaimValidator, 
                scopeValidator, 
                tokenClaimHeaderMapper);
        return new JwtAuthFilter(config, publicKeyService, jwtProperties, validationComponents);
    }
}
