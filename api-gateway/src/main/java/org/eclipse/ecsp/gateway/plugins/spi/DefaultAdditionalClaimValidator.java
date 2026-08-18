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
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.web.server.ServerWebExchange;

/**
 * Default (no-op) implementation of {@link AdditionalClaimValidator}.
 *
 * <p>This implementation performs no validation — all tokens pass through unchanged.
 * It exists only as a default placeholder so that the gateway starts without requiring
 * any custom implementation.
 *
 * <p>To enable custom claim validation, register a Spring bean that implements
 * {@link AdditionalClaimValidator}. The {@code @ConditionalOnMissingBean} on
 * {@link org.eclipse.ecsp.gateway.config.GatewayConfig} ensures this default
 * is automatically bypassed when a custom bean is present.
 */
public class DefaultAdditionalClaimValidator implements AdditionalClaimValidator {

    private static final IgniteLogger LOGGER =
            IgniteLoggerFactory.getLogger(DefaultAdditionalClaimValidator.class);

    /**
     * No-op implementation — all claims pass without any additional validation.
     *
     * @param claims        verified JWT claims
     * @param publicKeyInfo the public key metadata used for verification
     * @param exchange      the incoming server web exchange
     */
    @Override
    public void validate(Claims claims, PublicKeyInfo publicKeyInfo, ServerWebExchange exchange) {
        LOGGER.debug("DefaultAdditionalClaimValidator: no additional claim validation configured (no-op).");
    }
}
