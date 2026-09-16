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

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.exceptions.ApiGatewayException;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link TokenDecoder}.
 *
 * <p>Uses the Nimbus JOSE+JWT library to parse the raw JWT string and extract the
 * {@code kid} (from the JWT header) and {@code tenantId} (from the JWT claims set)
 * without verifying the signature. Replicates the parsing logic previously embedded
 * in {@link org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter}.
 *
 * <p>This bean is registered only when no other {@link TokenDecoder} bean is present
 * in the Spring application context ({@code @ConditionalOnMissingBean}).
 */
public class DefaultTokenDecoder implements TokenDecoder {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(DefaultTokenDecoder.class);
    private static final String DEFAULT_KID = "DEFAULT";
    private static final String INVALID_TOKEN_CODE = "api.gateway.error.token.invalid";
    private static final String TOKEN_VERIFICATION_FAILED = "Token verification failed";

    @Override
    public DecodedToken decode(String rawToken) {
        try {
            JWT jwt = JWTParser.parse(rawToken);

            Object kidObject = jwt.getHeader().toJSONObject().get("kid");
            Object tenantIdObject = jwt.getJWTClaimsSet().toJSONObject().get("tenantId");

            String kid = (kidObject == null || StringUtils.isEmpty(kidObject.toString()))
                    ? DEFAULT_KID : kidObject.toString();
            String tenantId = (tenantIdObject == null || StringUtils.isEmpty(tenantIdObject.toString()))
                    ? "" : tenantIdObject.toString();

            if (DEFAULT_KID.equals(kid)) {
                LOGGER.warn("JWT Token Header 'kid' is missing or empty, using default key for validation. "
                        + "tenantId: {}", tenantId);
            } else {
                LOGGER.debug("JWT token decoded. kid: {}, tenantId: {}", kid, tenantId);
            }

            Map<String, Object> rawClaims = new HashMap<>(jwt.getJWTClaimsSet().getClaims());
            return new DecodedToken(kid, tenantId, rawClaims);

        } catch (Exception ex) {
            LOGGER.error("Token decoding failed - unable to parse JWT structure: {}", ex.getMessage());
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
        }
    }
}
