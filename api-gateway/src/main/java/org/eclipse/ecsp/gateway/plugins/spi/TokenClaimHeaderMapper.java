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

import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * SPI for mapping verified JWT claim values to downstream HTTP request headers.
 *
 * <p>This interface consolidates all downstream header-writing that is derived from the
 * JWT token:
 * <ul>
 *   <li>The {@code scope} header — the effective user scope string</li>
 *   <li>The {@code override-scope} header — the union of user scopes and route scopes</li>
 *   <li>All configured claim-to-header mappings (e.g., {@code sub → user-id})</li>
 * </ul>
 *
 * <p>The default implementation replicates the existing header-writing loop in
 * {@link org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter#filter} exactly,
 * ensuring 100% backwards compatibility.
 *
 * <p>Register a Spring bean implementing this interface to customise what is forwarded
 * to downstream services — for example to add derived headers, remap claim names, or
 * suppress specific claims. If no custom bean is present, the gateway automatically
 * uses {@link DefaultTokenClaimHeaderMapper} via {@code @ConditionalOnMissingBean}.
 */
public interface TokenClaimHeaderMapper {

    /**
     * Populate downstream HTTP request headers from the verified JWT claims.
     *
     * @param builder the mutable request builder to add headers to; must not be null
     * @param context all inputs needed to perform the mapping; see {@link ClaimHeaderMappingContext}
     * @return the same {@code builder} (with additional headers set) for fluent chaining;
     *         never null
     */
    ServerHttpRequest.Builder map(ServerHttpRequest.Builder builder,
                                  ClaimHeaderMappingContext context);
}
