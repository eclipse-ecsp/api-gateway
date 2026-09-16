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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.exceptions.ApiGatewayException;
import org.eclipse.ecsp.gateway.utils.ScopeExtractorUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ScopeValidator}.
 *
 * <p>
 * Replicates the {@code validateScope()}, {@code extractUserScopes()}, and
 * {@code sanitizeUserScopes()} logic previously embedded in
 * {@link org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter}, combined with
 * {@link ScopeExtractorUtils#extractScopes}.
 *
 * <p>
 * Scope validation logic:
 * <ol>
 * <li>Extract user scopes from token claims using configured scope claim
 * names.</li>
 * <li>Strip configured scope prefixes from each scope value.</li>
 * <li>If {@code skipAuthz=true} or no route scopes are configured, validation
 * passes.</li>
 * <li>Otherwise, at least one user scope must match a required route
 * scope.</li>
 * </ol>
 *
 * <p>
 * This bean is registered only when no other {@link ScopeValidator} bean is
 * present
 * in the Spring application context ({@code @ConditionalOnMissingBean}).
 */
public class DefaultScopeValidator implements ScopeValidator {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(DefaultScopeValidator.class);
    private static final String INVALID_TOKEN_CODE = "api.gateway.error.token.invalid";
    private static final String TOKEN_VERIFICATION_FAILED = "Token verification failed";

    @Override
    public String validate(ScopeValidationContext context) {
        if (context.getRoute() == null || context.getClaims() == null) {
            LOGGER.error("Scope validation failed - route or claims are null.");
            throw new ApiGatewayException(HttpStatus.NOT_FOUND, "api.gateway.error", "Request not found");
        }

        String routeId = context.getRoute().getId();
        LOGGER.debug("Starting scope validation for route: {}", routeId);

        Set<String> userScopes = extractUserScopes(context);

        boolean valid;
        if (context.isSkipAuthz()) {
            LOGGER.debug("Scope validation skipped (skip-authz=true) for route: {}", routeId);
            valid = true;
        } else if (CollectionUtils.isEmpty(context.getRouteScopes())) {
            LOGGER.debug("No route scopes configured, scope validation passed for route: {}", routeId);
            valid = true;
        } else {
            if (!CollectionUtils.isEmpty(context.getTokenScopePrefixes())
                    && !CollectionUtils.isEmpty(userScopes)) {
                userScopes = sanitizeUserScopes(userScopes, context);
                LOGGER.debug("User scopes after prefix stripping: {}", userScopes);
            }
            valid = context.getRouteScopes().stream().anyMatch(userScopes::contains);
            if (valid) {
                LOGGER.debug("Scope validation passed. Matching scopes: {}, route: {}",
                        context.getRouteScopes().stream().filter(userScopes::contains).collect(Collectors.toSet()),
                        routeId);
            }
        }

        if (!valid) {
            LOGGER.error("Scope validation failed - user scopes {} do not match required route scopes {} "
                    + "for route: {}", userScopes, context.getRouteScopes(), routeId);
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
        }

        return String.join(",", userScopes);
    }

    private Set<String> extractUserScopes(ScopeValidationContext context) {
        Set<String> userScopes = ScopeExtractorUtils.extractScopes(context.getClaims(), context.getJwtProperties());
        LOGGER.debug("Extracted user scopes: {}, required route scopes: {}", userScopes, context.getRouteScopes());
        return userScopes;
    }

    private Set<String> sanitizeUserScopes(Set<String> userScopes, ScopeValidationContext context) {
        return userScopes.stream()
                .map(scope -> {
                    for (String prefix : context.getTokenScopePrefixes()) {
                        if (StringUtils.isNotBlank(scope) && StringUtils.isNotBlank(prefix)
                                && scope.startsWith(prefix)) {
                            LOGGER.debug("Removing scope prefix '{}' from scope '{}'", prefix, scope);
                            return scope.substring(prefix.length());
                        }
                    }
                    return scope;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
