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
import lombok.Getter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Value object carrying all inputs required by
 * {@link TokenClaimHeaderMapper#map(
 *     org.springframework.http.server.reactive.ServerHttpRequest.Builder,
 *     ClaimHeaderMappingContext)}.
 *
 * <p>This context is constructed by
 * {@link org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter} after successful
 * scope validation and passed to {@link TokenClaimHeaderMapper} for downstream
 * header population.
 */
@Getter
public class ClaimHeaderMappingContext {

    /**
     * Fully verified JWT claims to extract header values from.
     */
    private final Claims claims;

    /**
     * Configured mapping from JWT claim names to downstream HTTP header names.
     * From {@link org.eclipse.ecsp.gateway.config.JwtProperties#getTokenClaimToHeaderMapping()}.
     * For example: {@code {"sub" -> "user-id", "tenantId" -> "X-Tenant-Id"}}.
     */
    private final Map<String, String> claimToHeaderMapping;

    /**
     * Comma-separated effective user scope string (after prefix stripping and validation).
     * Written to the downstream {@code scope} header.
     */
    private final String scopeString;

    /**
     * Union of validated user scopes and route-configured scopes.
     * Written to the downstream {@code override-scope} header.
     */
    private final Set<String> overrideScopes;

    /**
     * Constructs a ClaimHeaderMappingContext.
     *
     * @param claims              verified JWT claims
     * @param claimToHeaderMapping  configured claim-to-header name mapping
     * @param scopeString         effective comma-separated scope string
     * @param overrideScopes      union of user scopes and route scopes
     */
    public ClaimHeaderMappingContext(Claims claims,
                                     Map<String, String> claimToHeaderMapping,
                                     String scopeString,
                                     Set<String> overrideScopes) {
        this.claims = claims;
        this.claimToHeaderMapping = Collections.unmodifiableMap(claimToHeaderMapping);
        this.scopeString = scopeString;
        this.overrideScopes = Collections.unmodifiableSet(overrideScopes);
    }
}
