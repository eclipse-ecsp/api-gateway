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

/**
 * SPI for decoding a raw JWT string <strong>without</strong> signature verification.
 *
 * <p>The sole purpose of this interface is to extract the structural metadata
 * ({@code kid}, {@code tenantId}) required to resolve the correct public key
 * before signature verification takes place. The returned {@link DecodedToken}
 * contains <em>unverified</em> claims that MUST NOT be used for any authorization
 * decision.
 *
 * <p>The default implementation uses the Nimbus JOSE+JWT library
 * ({@code com.nimbusds.jwt.JWTParser}) to parse the JWT header and claim set.
 *
 * <p>Register a Spring bean implementing this interface to support non-standard
 * JWT formats or alternative decode libraries. If no custom bean is present,
 * the gateway automatically uses {@link DefaultTokenDecoder}
 * via {@code @ConditionalOnMissingBean}.
 *
 * <p><strong>Note on separation from {@link TokenParser}:</strong>
 * {@link TokenParser} is responsible for HTTP transport (extracting the raw string
 * from a header, cookie, etc.). This interface is responsible for JWT structure
 * (parsing the base64-encoded sections). They are intentionally separate so each
 * can be replaced independently.
 */
public interface TokenDecoder {

    /**
     * Decode the raw JWT string and return structural metadata.
     *
     * @param rawToken the raw JWT string (without any HTTP scheme prefix such as {@code "Bearer "})
     * @return a {@link DecodedToken} carrying the {@code kid}, {@code tenantId},
     *         and unverified raw claims for key resolution
     * @throws org.eclipse.ecsp.gateway.exceptions.ApiGatewayException if the token
     *         cannot be parsed (e.g., malformed base64, invalid JWT structure)
     */
    DecodedToken decode(String rawToken);
}
