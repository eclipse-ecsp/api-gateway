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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.ecsp.interceptors.SecurityRequirementCache;
import org.eclipse.ecsp.interceptors.TokenValidationInterceptor;
import org.eclipse.ecsp.security.ScopeOverrideProperties;
import org.eclipse.ecsp.security.ValidationConfigProperties;
import org.eclipse.ecsp.tokenvalidator.TokenValidator;
import org.eclipse.ecsp.tokenvalidator.config.TokenValidatorAutoConfiguration;
import org.eclipse.ecsp.utils.RegistryCommonConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration that wires together the JWT token validation and token propagation
 * beans for the {@code api-registry-common} library.
 *
 * <p>Activated by the presence of the library on the classpath — no additional user
 * configuration is required unless behaviour needs to be overridden.
 */
@Configuration
@ConditionalOnProperty(
    prefix = RegistryCommonConstants.API_REGISTRY_SECURITY_PREFIX,
    name = "enabled",
    havingValue = "true"
)
@ImportAutoConfiguration(TokenValidatorAutoConfiguration.class)
@EnableConfigurationProperties(ValidationConfigProperties.class)
public class TokenValidationConfiguration {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(TokenValidationConfiguration.class);


    /**
     * Default constructor.
     */
    public TokenValidationConfiguration() {
        LOGGER.debug("security is enabled, configuring the token validation configuration");
    }

    /**
     * Creates the annotation-lookup cache bean when security is enabled.
     *
     * @return the cache
     */
    @Bean
    public SecurityRequirementCache securityRequirementCache() {
        LOGGER.debug("Creating SecurityRequirementCache bean");
        return new SecurityRequirementCache();
    }

    /**
     * Creates the token-validation interceptor when security is enabled.
     *
     * @param tokenValidator           the JWT validator
     * @param config                   the validation configuration properties
     * @param securityRequirementCache the annotation-lookup cache
     * @param objectMapper             the object mapper for JSON serialization
     * @param scopeOverrideProperties  the scope-override configuration properties
     * @return the interceptor
     */
    @Bean
    public TokenValidationInterceptor tokenValidationInterceptor(
            TokenValidator tokenValidator,
            ValidationConfigProperties config,
            SecurityRequirementCache securityRequirementCache,
            ObjectMapper objectMapper,
            ScopeOverrideProperties scopeOverrideProperties) {
        LOGGER.debug("Creating TokenValidationInterceptor bean");
        return new TokenValidationInterceptor(tokenValidator, config, securityRequirementCache, objectMapper,
                scopeOverrideProperties);
    }

    /**
     * Creates a default ObjectMapper bean if one is not already defined.
     *
     * @return the object mapper
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        LOGGER.debug("Creating ObjectMapper bean");
        ObjectMapper om = new ObjectMapper();
        om.findAndRegisterModules();
        return om;
    }
}
