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
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.eclipse.ecsp.gateway.exceptions.ApiGatewayException;
import org.eclipse.ecsp.gateway.model.PublicKeyInfo;
import org.eclipse.ecsp.gateway.utils.GatewayUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * Default implementation of {@link SignatureVerifier}.
 *
 * <p>Uses the JJWT library to verify the JWT signature against the provided public key
 * and return fully-verified claims. Replicates the signature verification logic previously
 * embedded in {@link org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter}.
 *
 * <p>This bean is registered only when no other {@link SignatureVerifier} bean is present
 * in the Spring application context ({@code @ConditionalOnMissingBean}).
 */
public class DefaultSignatureVerifier implements SignatureVerifier {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(DefaultSignatureVerifier.class);
    private static final String INVALID_TOKEN_CODE = "api.gateway.error.token.invalid";
    private static final String TOKEN_VERIFICATION_FAILED = "Token verification failed";

    @Override
    public Claims verify(String rawToken, PublicKeyInfo publicKeyInfo) {
        try {
            JwtParser jwtParser = Jwts.parser()
                    .verifyWith(publicKeyInfo.getPublicKey())
                    .build();
            Jws<Claims> parsedToken = jwtParser.parseSignedClaims(rawToken);

            LOGGER.info("JWT signature verification successful. kid: {}, keySource: {}",
                    publicKeyInfo.getKid(), publicKeyInfo.getSourceId());

            return parsedToken.getPayload();

        } catch (SecurityException
                 | MalformedJwtException
                 | ExpiredJwtException
                 | UnsupportedJwtException
                 | IllegalArgumentException ex) {
            String failureReason = GatewayUtils.getTokenValidationFailureReason(ex);
            LOGGER.error("JWT signature verification failed - {}. kid: {}", failureReason, publicKeyInfo.getKid());
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
        }
    }
}
