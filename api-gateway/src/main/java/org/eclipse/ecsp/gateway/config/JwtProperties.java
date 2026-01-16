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

package org.eclipse.ecsp.gateway.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.ecsp.gateway.model.PublicKeySource;
import org.eclipse.ecsp.gateway.model.TokenHeaderValidationConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration properties for JWT authentication and validation.
 * This class holds all JWT-related configuration including token header validation,
 * public key sources, and token claim to header mapping.
 *
 * @author Abhishek Kumar
 */
@Getter
@Setter
@Component
@NoArgsConstructor
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Configuration for token header validation.
     * Key: header name, Value: validation configuration
     */
    private Map<String, TokenHeaderValidationConfig> tokenHeaderValidationConfig;

    /**
     * List of public key sources for JWT token validation.
     * Supports multiple sources including JWKS endpoints and PEM certificates.
     */
    private List<PublicKeySource> keySources;

    /**
     * Mapping from JWT token claims to HTTP headers.
     * Key: JWT claim name, Value: HTTP header name
     */
    private Map<String, String> tokenClaimToHeaderMapping;

    /**
     * A set of prefixes used to filter or match JWT scope claims.
     */
    private Set<String> scopePrefixes;

    /**
     * Retry configuration for JWKS fetching.
     */
    private RetryConfig retry = new RetryConfig();
}
