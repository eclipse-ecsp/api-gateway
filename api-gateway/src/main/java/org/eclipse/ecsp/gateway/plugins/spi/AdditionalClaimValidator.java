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
import org.eclipse.ecsp.gateway.model.PublicKeyInfo;
import org.springframework.web.server.ServerWebExchange;

/**
 * SPI for custom, programmatic JWT claim validation.
 *
 * <p>This hook runs <strong>after</strong> the configuration-driven
 * {@link TokenClaimValidator}, giving implementations full access to the verified
 * {@link Claims}, the resolved {@link PublicKeyInfo}, and the original
 * {@link ServerWebExchange} to apply business-specific rules that cannot be expressed
 * as simple required/regex configuration.
 *
 * <p>Typical use cases:
 * <ul>
 *   <li>Cross-claim validation — e.g., {@code tenantId} in the token must match
 *       a path variable in the request URL</li>
 *   <li>Audience ({@code aud}) constraints beyond what static config supports</li>
 *   <li>Tenant allow-list enforcement</li>
 *   <li>Custom expiry windows beyond the standard JWT {@code exp} claim</li>
 *   <li>Role or permission checks derived from multiple claims</li>
 * </ul>
 *
 * <p>The default implementation is a <strong>no-op</strong> — all tokens pass through
 * unchanged. Register a Spring bean implementing this interface to activate custom
 * validation logic. If no custom bean is present, the gateway automatically uses
 * {@link DefaultAdditionalClaimValidator} via {@code @ConditionalOnMissingBean}.
 *
 * <p><strong>Ordering guarantee:</strong> This validator runs after
 * {@link TokenClaimValidator} and before {@link ScopeValidator}. Claims received here
 * are fully signature-verified and have already passed the required/regex checks.
 */
public interface AdditionalClaimValidator {

    /**
     * Perform custom programmatic validation on the verified JWT claims.
     *
     * <p>Throw {@link org.eclipse.ecsp.gateway.exceptions.ApiGatewayException}
     * (or any {@link RuntimeException}) to reject the request with a specific HTTP
     * status and error message.
     *
     * @param claims        verified JWT claims (post signature-verification and
     *                      post configuration-driven validation)
     * @param publicKeyInfo the public key metadata used for signature verification;
     *                      contains {@code skipAuthz}, {@code skipClaimValidation},
     *                      and {@code additionalMetaData} fields
     * @param exchange      the incoming server web exchange, providing access to
     *                      request path, headers, and attributes for context-aware
     *                      validation
     */
    void validate(Claims claims, PublicKeyInfo publicKeyInfo, ServerWebExchange exchange);
}
