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

package org.eclipse.ecsp.gateway.plugins.spi;

import io.jsonwebtoken.Claims;
import org.eclipse.ecsp.gateway.model.TokenHeaderValidationConfig;
import java.util.Map;

/**
 * SPI for configuration-driven token claim validation.
 *
 * <p>This interface validates verified JWT claims against the rules defined in
 * {@link org.eclipse.ecsp.gateway.model.TokenHeaderValidationConfig}, which supports
 * {@code required} (presence check) and {@code regex} (pattern match) rules
 * per claim name.
 *
 * <p>This is the <em>configuration-driven</em> validation layer. For custom programmatic
 * validation logic (e.g., cross-claim checks, tenant allow-lists), use
 * {@link AdditionalClaimValidator}, which runs after this interface.
 *
 * <p>The default implementation replicates the existing {@code validateTokenHeaders()} and
 * {@code validateClaims()} logic from
 * {@link org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter}.
 *
 * <p>Register a Spring bean implementing this interface to replace the entire
 * configuration-driven claim validation strategy. If no custom bean is present,
 * the gateway automatically uses {@link DefaultTokenClaimValidator}
 * via {@code @ConditionalOnMissingBean}.
 */
public interface TokenClaimValidator {

    /**
     * Validate token claims against the configured validation rules.
     *
     * @param claims               verified JWT claims (post signature-verification)
     * @param skipClaimValidation  if {@code true}, skip all validation and return immediately;
     *                             driven by {@link org.eclipse.ecsp.gateway.model.PublicKeyInfo
     *                             #isSkipClaimValidation()}
     * @param validationConfig     map of claim name to {@link TokenHeaderValidationConfig}
     *                             specifying required and regex rules; may be null or empty
     * @throws org.eclipse.ecsp.gateway.exceptions.ApiGatewayException if any configured
     *         required claim is missing or a claim value does not match the configured regex
     */
    void validate(Claims claims,
                  boolean skipClaimValidation,
                  Map<String, TokenHeaderValidationConfig> validationConfig);
}
