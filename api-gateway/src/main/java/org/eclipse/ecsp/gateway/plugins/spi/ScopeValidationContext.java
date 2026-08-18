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
import org.eclipse.ecsp.gateway.config.JwtProperties;
import org.springframework.cloud.gateway.route.Route;
import java.util.Set;

/**
 * Value object carrying all inputs required by {@link ScopeValidator#validate(ScopeValidationContext)}.
 *
 * <p>This context object is constructed by
 * {@link org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter} after signature
 * verification and claim validation, and passed to the {@link ScopeValidator} for
 * scope matching.
 */
@Getter
public class ScopeValidationContext {

    /**
     * The matched gateway route for the current request.
     */
    private final Route route;

    /**
     * Fully verified JWT claims from the token.
     */
    private final Claims claims;

    /**
     * Scopes required by the route configuration (from {@code JwtAuthValidator.Config#scope}).
     */
    private final Set<String> routeScopes;

    /**
     * Prefixes to strip from token scope values before matching
     * (from {@link JwtProperties#getScopePrefixes()}).
     */
    private final Set<String> tokenScopePrefixes;

    /**
     * If {@code true}, scope validation is skipped entirely.
     * Driven by {@link org.eclipse.ecsp.gateway.model.PublicKeyInfo#isSkipAuthz()}.
     */
    private final boolean skipAuthz;

    /**
     * JWT configuration properties, used for scope claim extraction.
     */
    private final JwtProperties jwtProperties;

    /**
     * Constructs a ScopeValidationContext.
     *
     * @param route              the matched gateway route
     * @param claims             verified JWT claims
     * @param routeScopes        scopes required by the route
     * @param tokenScopePrefixes scope prefixes to strip before matching
     * @param skipAuthz          whether scope validation should be skipped
     * @param jwtProperties      JWT configuration properties
     */
    public ScopeValidationContext(Route route, Claims claims, Set<String> routeScopes,
                                  Set<String> tokenScopePrefixes, boolean skipAuthz,
                                  JwtProperties jwtProperties) {
        this.route = route;
        this.claims = claims;
        this.routeScopes = routeScopes;
        this.tokenScopePrefixes = tokenScopePrefixes;
        this.skipAuthz = skipAuthz;
        this.jwtProperties = jwtProperties;
    }
}
