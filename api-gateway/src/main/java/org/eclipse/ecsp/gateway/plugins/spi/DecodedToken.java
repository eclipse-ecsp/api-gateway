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

import lombok.Getter;
import java.util.Collections;
import java.util.Map;

/**
 * Value object carrying the structural result of decoding a JWT string
 * <strong>before</strong> signature verification.
 *
 * <p>This object is produced by {@link TokenDecoder} and consumed by the gateway to
 * look up the matching public key via the {@code kid} and {@code tenantId} fields.
 *
 * <p><strong>IMPORTANT:</strong> The {@code rawClaims} in this object are
 * <em>unverified</em>. They MUST NOT be used for any authorization decision.
 * They exist solely to enable public-key resolution before signature verification.
 * Verified claims are returned by {@link SignatureVerifier}.
 */
@Getter
public class DecodedToken {

    /**
     * The key identifier ({@code kid}) from the JWT header.
     * Used to locate the correct public key for signature verification.
     * Defaults to {@code "DEFAULT"} if not present in the token header.
     */
    private final String kid;

    /**
     * The tenant identifier ({@code tenantId}) from the JWT claims.
     * Used to scope the public key lookup to a specific tenant.
     * May be empty or blank if the claim is absent.
     */
    private final String tenantId;

    /**
     * Raw, <em>unverified</em> claims from the JWT payload.
     * Available for key-resolution purposes only.
     * Never use these for authorization decisions.
     */
    private final Map<String, Object> rawClaims;

    /**
     * Constructs a DecodedToken with the specified fields.
     *
     * @param kid        the key identifier from the JWT header; must not be null
     * @param tenantId   the tenant identifier from the JWT claims; may be blank
     * @param rawClaims  unverified JWT payload claims; must not be null
     */
    public DecodedToken(String kid, String tenantId, Map<String, Object> rawClaims) {
        this.kid = (kid != null) ? kid : "DEFAULT";
        this.tenantId = (tenantId != null) ? tenantId : "";
        this.rawClaims = Collections.unmodifiableMap(rawClaims);
    }
}
