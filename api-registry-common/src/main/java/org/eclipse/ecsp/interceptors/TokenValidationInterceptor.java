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

package org.eclipse.ecsp.interceptors;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.ecsp.security.ScopeOverrideProperties;
import org.eclipse.ecsp.security.SecurityContext;
import org.eclipse.ecsp.security.ValidationConfigProperties;
import org.eclipse.ecsp.tokenvalidator.ScopeMatchMode;
import org.eclipse.ecsp.tokenvalidator.ScopeValidator;
import org.eclipse.ecsp.tokenvalidator.TokenValidator;
import org.eclipse.ecsp.tokenvalidator.exception.InvalidClaimException;
import org.eclipse.ecsp.tokenvalidator.exception.TokenValidatorException;
import org.eclipse.ecsp.tokenvalidator.impl.DefaultScopeValidator;
import org.eclipse.ecsp.tokenvalidator.model.TokenClaim;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Spring MVC interceptor that validates JWT Bearer tokens on secured endpoints.
 *
 * <p>An endpoint is considered secured when its handler method carries the
 * {@code @SecurityRequirement} annotation. Public endpoints (no annotation) are
 * passed through without any token inspection.
 *
 * <p>All {@link TokenValidatorException} sub-types are mapped to HTTP 401 to avoid
 * catch-order bugs arising from the {@code TokenExpiredException} /
 * {@code InvalidIssuerException} inheritance chain.
 *
 * <p>On successful validation the verified claims are stored in {@link SecurityContext}.
 * {@link SecurityContext#clear()} is always called in {@code afterCompletion} to
 * prevent ThreadLocal memory leaks.
 */
public class TokenValidationInterceptor implements HandlerInterceptor {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(TokenValidationInterceptor.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();
    private static final String ERROR_FIELD = "error";

    private final TokenValidator tokenValidator;
    private final ValidationConfigProperties config;
    private final SecurityRequirementCache securityRequirementCache;
    private final ObjectMapper objectMapper;
    private final ScopeValidator scopeValidator = new DefaultScopeValidator(Set.<String>of(), ScopeMatchMode.ANY);
    private final ScopeOverrideProperties scopeOverrideProperties;

    /**
     * Constructs a {@code TokenValidationInterceptor}.
     *
     * @param tokenValidator           the JWT validator delegate
     * @param config                   the validation configuration properties
     * @param securityRequirementCache the annotation-lookup cache
     * @param objectMapper             the object mapper for JSON serialization
     * @param scopeOverrideProperties  the scope-override configuration properties
     */
    public TokenValidationInterceptor(TokenValidator tokenValidator,
                                      ValidationConfigProperties config,
                                      SecurityRequirementCache securityRequirementCache,
                                      ObjectMapper objectMapper,
                                      ScopeOverrideProperties scopeOverrideProperties) {
        LOGGER.debug("Initializing TokenValidationInterceptor");
        this.tokenValidator = tokenValidator;
        this.config = config;
        this.securityRequirementCache = securityRequirementCache;
        this.objectMapper = objectMapper;
        this.scopeOverrideProperties = scopeOverrideProperties;
        LOGGER.debug("TokenValidationInterceptor initialized");
    }

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request,
                             @NotNull HttpServletResponse response,
                             @NotNull Object handler) throws IOException {
        if (!config.getSecurity().isEnabled()) {
            LOGGER.debug("security not enabled, skipping token validation");
            return true;
        }
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        if (!securityRequirementCache.isSecured(handlerMethod)) {
            LOGGER.debug("Endpoint is not secured, skipping token validation");
            return true;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            LOGGER.warn("Missing or malformed authentication token for endpoint {}", request.getRequestURI());
            return writeUnauthorized(response, "Missing or malformed authentication token");
        }

        String token = authHeader.substring(BEARER_PREFIX_LENGTH);
        try {
            List<TokenClaim> claims = tokenValidator.validate(token);
            if (!validateScopes(handlerMethod, claims, request, response)) {
                return false;
            }
            SecurityContext.set(token, claims);
            return true;
        } catch (TokenValidatorException ex) {
            LOGGER.warn("Token validation failed: {}, for the endpoint {}", ex.getMessage(), request.getRequestURI());
            return writeUnauthorized(response, ex.getMessage());
        }
    }

    private boolean validateScopes(HandlerMethod handlerMethod,
            List<TokenClaim> claims,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        // token is valid , perform authorization checks.
        List<String> requiredScopes = securityRequirementCache.getScopes(handlerMethod);
        List<String> effectiveScopes = resolveEffectiveScopes(handlerMethod, requiredScopes);
        try {
            scopeValidator.validateClaims(claims, effectiveScopes);
            return true;
        } catch (InvalidClaimException exInvalidClaimException) {
            LOGGER.warn("Token does not have required scopes for endpoint {}",
                request.getRequestURI(), exInvalidClaimException);
            return writeUnauthorized(response, "Insufficient token scopes");
        }
    }

    /**
     * Clears the {@link SecurityContext} after every request, including error cases.
     *
     * @param request  the current HTTP request
     * @param response the current HTTP response
     * @param handler  the chosen handler
     * @param ex       any exception thrown during handler execution, or {@code null}
     */
    @Override
    public void afterCompletion(@NotNull HttpServletRequest request,
                                @NotNull HttpServletResponse response,
                                @NotNull Object handler, Exception ex) {
        SecurityContext.clear();
    }

    /**
     * Merges annotation-declared scopes with the configured override scopes for the route.
     *
     * <p>When {@code scopes.override.enabled} is {@code true} and the route ID is present
     * in the {@code scopesMap}, the effective allowed scope set is the
     * configured override scopes. 
     * This makes scope validation self-contained: no dependency on the {@code override-scope} header
     * forwarded by the API Gateway.
     *
     * @param handlerMethod  the resolved handler method for the current request
     * @param annotationScopes the scopes declared on the {@code @SecurityRequirement} annotation
     * @return the effective list of allowed scopes
     */
    private List<String> resolveEffectiveScopes(HandlerMethod handlerMethod, List<String> annotationScopes) {
        Map<String, List<String>> scopesMap = scopeOverrideProperties.getScopesMap();
        if (!scopeOverrideProperties.getOverride().isEnabled() || scopesMap == null) {
            return annotationScopes;
        }
        String routeId = resolveRouteId(handlerMethod);
        List<String> overrideScopes = scopesMap.getOrDefault(routeId, scopesMap.get(routeId.toLowerCase()));
        if (overrideScopes == null) {
            return annotationScopes;
        }
        LOGGER.debug("Override scopes found for routeId {}: {}", routeId, overrideScopes);
        return overrideScopes.stream().distinct().toList();
    }

    /**
     * Derives the route ID from a {@link HandlerMethod} using the same convention as
     * {@link org.eclipse.ecsp.security.ScopeTagger}: {@code <tag>-<operationId>}.
     *
     * <p>The tag is the controller class simple name converted from CamelCase to
     * kebab-case and lowercased — matching the OpenAPI tag that SpringDoc generates,
     * which retains the full class name including the {@code Controller} suffix.
     * The operation ID is the method name with underscores replaced by hyphens.
     *
     * @param handlerMethod the resolved handler method
     * @return the derived route ID
     */
    private String resolveRouteId(HandlerMethod handlerMethod) {
        String className = handlerMethod.getBeanType().getSimpleName();
        String tag = className.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
        String operationId = handlerMethod.getMethod().getName().replace("_", "-");
        return tag + "-" + operationId;
    }

    private boolean writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, String> body = new HashMap<>();
        body.put(ERROR_FIELD, message);
        body.put("code", "api.unauthorized");
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }
}
