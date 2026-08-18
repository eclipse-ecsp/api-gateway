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

/**
 * SPI for verifying a JWT signature and returning fully verified, trustworthy claims.
 *
 * <p>This interface is the signature-verification stage of the authentication pipeline.
 * It receives the raw JWT string and the {@link PublicKeyInfo} resolved by the
 * {@link org.eclipse.ecsp.gateway.service.PublicKeyService}, verifies the cryptographic
 * signature, and returns the verified {@link Claims}.
 *
 * <p>The default implementation uses the JJWT library:
 * {@code Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token)}.
 *
 * <p>Register a Spring bean implementing this interface to support alternative
 * signature algorithms, introspection-based verification, or any other verification
 * strategy. If no custom bean is present, the gateway automatically uses
 * {@link DefaultSignatureVerifier} via {@code @ConditionalOnMissingBean}.
 */
public interface SignatureVerifier {

    /**
     * Verify the JWT signature and return the verified claims.
     *
     * @param rawToken      the raw JWT string (without HTTP scheme prefix)
     * @param publicKeyInfo the public key information resolved from the token's {@code kid}
     *                      and {@code tenantId}; must not be null
     * @return verified {@link Claims} from the JWT payload; never null
     * @throws org.eclipse.ecsp.gateway.exceptions.ApiGatewayException if signature
     *         verification fails (expired, malformed, wrong key, etc.)
     */
    Claims verify(String rawToken, PublicKeyInfo publicKeyInfo);
}
