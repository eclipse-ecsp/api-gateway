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
 * SPI for validating that user scopes in the JWT token satisfy the route-required scopes.
 *
 * <p>This interface encapsulates scope extraction from token claims, optional prefix
 * stripping, and the matching logic against route-configured scopes.
 *
 * <p>The default implementation replicates the existing {@code validateScope()},
 * {@code extractUserScopes()}, and {@code sanitizeUserScopes()} methods from
 * {@link org.eclipse.ecsp.gateway.plugins.filters.JwtAuthFilter}, combined with
 * {@link org.eclipse.ecsp.gateway.utils.ScopeExtractorUtils#extractScopes}.
 *
 * <p>Register a Spring bean implementing this interface to support custom scope models
 * (e.g., hierarchical scopes, wildcard matching, RBAC-based checks). If no custom bean
 * is present, the gateway automatically uses {@link DefaultScopeValidator}
 * via {@code @ConditionalOnMissingBean}.
 */
public interface ScopeValidator {

    /**
     * Validate that the user's token scopes satisfy the route-required scopes.
     *
     * @param context all inputs required for scope validation; see {@link ScopeValidationContext}
     * @return comma-separated string of the effective user scopes (after prefix stripping),
     *         to be written to the downstream {@code scope} request header; never null
     * @throws org.eclipse.ecsp.gateway.exceptions.ApiGatewayException with HTTP 401
     *         if the user's scopes do not satisfy the route requirements
     */
    String validate(ScopeValidationContext context);
}
