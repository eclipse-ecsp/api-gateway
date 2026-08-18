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

package org.eclipse.ecsp.gateway.plugins.filters;

import io.jsonwebtoken.Claims;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.ecsp.gateway.config.JwtProperties;
import org.eclipse.ecsp.gateway.exceptions.ApiGatewayException;
import org.eclipse.ecsp.gateway.model.PublicKeyInfo;
import org.eclipse.ecsp.gateway.plugins.spi.AdditionalClaimValidator;
import org.eclipse.ecsp.gateway.plugins.spi.ClaimHeaderMappingContext;
import org.eclipse.ecsp.gateway.plugins.spi.DecodedToken;
import org.eclipse.ecsp.gateway.plugins.spi.ScopeValidationContext;
import org.eclipse.ecsp.gateway.plugins.spi.ScopeValidator;
import org.eclipse.ecsp.gateway.plugins.spi.SignatureVerifier;
import org.eclipse.ecsp.gateway.plugins.spi.TokenClaimHeaderMapper;
import org.eclipse.ecsp.gateway.plugins.spi.TokenClaimValidator;
import org.eclipse.ecsp.gateway.plugins.spi.TokenDecoder;
import org.eclipse.ecsp.gateway.plugins.spi.TokenParser;
import org.eclipse.ecsp.gateway.service.PublicKeyService;
import org.eclipse.ecsp.gateway.service.TokenValidationComponents;
import org.eclipse.ecsp.gateway.utils.GatewayConstants;
import org.eclipse.ecsp.gateway.utils.GatewayUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Gateway filter that orchestrates the full JWT authentication pipeline.
 *
 * <p>Each stage of the pipeline is delegated to an injected SPI bean,
 * allowing any individual step to be overridden by registering a custom
 * Spring bean without modifying this class:
 *
 * <ol>
 *   <li>{@link TokenParser} — extract raw token string from the HTTP exchange</li>
 *   <li>{@link TokenDecoder} — decode JWT structure (kid, tenantId) without sig verify</li>
 *   <li>{@link PublicKeyService} — resolve public key (unchanged, not an SPI)</li>
 *   <li>{@link SignatureVerifier} — verify JWT signature; return verified Claims</li>
 *   <li>{@link TokenClaimValidator} — config-driven required/regex claim checks</li>
 *   <li>{@link AdditionalClaimValidator} — custom programmatic claim checks</li>
 *   <li>{@link ScopeValidator} — validate user scopes against route scopes</li>
 *   <li>{@link TokenClaimHeaderMapper} — map claim values to downstream headers</li>
 * </ol>
 *
 * <p>This filter is created per-route by {@link org.eclipse.ecsp.gateway.plugins.JwtAuthValidator}
 * and is not itself a Spring bean.
 */
public class JwtAuthFilter implements GatewayFilter, Ordered {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String INVALID_TOKEN_CODE = "api.gateway.error.token.invalid";
    private static final String INVALID_TOKEN = "Invalid Token";
    private static final String DEFAULT = "DEFAULT";

    /** Route scopes required by this filter instance (from route configuration). */
    protected final Set<String> routeScopes = new HashSet<>();

    private final JwtProperties jwtProperties;
    private final PublicKeyService publicKeyService;
    private final TokenParser tokenParser;
    private final TokenDecoder tokenDecoder;
    private final SignatureVerifier signatureVerifier;
    private final TokenClaimValidator tokenClaimValidator;
    private final AdditionalClaimValidator additionalClaimValidator;
    private final ScopeValidator scopeValidator;
    private final TokenClaimHeaderMapper tokenClaimHeaderMapper;
    private final Map<String, String> tokenClaimToHeaderMapping;
    private final Set<String> tokenScopePrefixes;

    /**
     * Constructs a JwtAuthFilter with all required dependencies.
     *
     * @param config                   route-level filter configuration (scope)
     * @param publicKeyService         public key resolution service
     * @param jwtProperties            JWT configuration properties
     * @param tokenValidationComponents token validation components
     */
    public JwtAuthFilter(Config config,
                         PublicKeyService publicKeyService,
                         JwtProperties jwtProperties,
                         TokenValidationComponents tokenValidationComponents) {
        this.jwtProperties = jwtProperties;
        this.publicKeyService = publicKeyService;
        this.tokenParser = tokenValidationComponents.tokenParser();
        this.tokenDecoder = tokenValidationComponents.tokenDecoder();
        this.signatureVerifier = tokenValidationComponents.signatureVerifier();
        this.tokenClaimValidator = tokenValidationComponents.tokenClaimValidator();
        this.additionalClaimValidator = tokenValidationComponents.additionalClaimValidator();
        this.scopeValidator = tokenValidationComponents.scopeValidator();
        this.tokenClaimHeaderMapper = tokenValidationComponents.tokenClaimHeaderMapper();

        if (config != null && config.getScope() != null) {
            LOGGER.debug("Config: {}", config);
            routeScopes.addAll(Arrays.stream(config.getScope().split(",")).map(String::trim).toList());
        }

        if (CollectionUtils.isEmpty(jwtProperties.getTokenClaimToHeaderMapping())) {
            LOGGER.debug("No token claim to header mapping configured.");
            this.tokenClaimToHeaderMapping = new HashMap<>();
        } else {
            this.tokenClaimToHeaderMapping = jwtProperties.getTokenClaimToHeaderMapping();
            LOGGER.debug("Token claim to header mapping: {}", tokenClaimToHeaderMapping);
        }

        if (!this.tokenClaimToHeaderMapping.containsKey("user_id")) {
            LOGGER.debug("Applying default sub -> user-id claim mapping.");
            this.tokenClaimToHeaderMapping.put("sub", "user-id");
        }

        if (!CollectionUtils.isEmpty(jwtProperties.getScopePrefixes())) {
            this.tokenScopePrefixes = jwtProperties.getScopePrefixes();
            LOGGER.debug("Token scope prefixes: {}", tokenScopePrefixes);
        } else {
            this.tokenScopePrefixes = new HashSet<>();
            LOGGER.debug("No token scope prefixes configured.");
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest().getId();
        final String routeId = exchange.getAttribute(
                ServerWebExchangeUtils.GATEWAY_PREDICATE_MATCHED_PATH_ROUTE_ID_ATTR);
        String requestPath = exchange.getRequest().getPath().value();

        LOGGER.debug("JWT Auth Validation for requestUrl: {}, requestId: {}", requestPath, requestId);

        try {
            // Step 1 — Extract raw token from exchange
            String rawToken = tokenParser.parse(exchange);
    
            // Step 2 — Decode JWT structure (kid + tenantId) without signature verification
            DecodedToken decoded = tokenDecoder.decode(rawToken);
            LOGGER.debug("JWT decoded. kid: {}, tenantId: {}, {}",
                    decoded.getKid(), decoded.getTenantId(),
                    GatewayUtils.getLogMessage(routeId, requestPath, requestId));
    
            // Step 3 — Resolve public key (key management is gateway-internal, not an SPI)
            PublicKeyInfo publicKeyInfo = resolvePublicKey(decoded, requestPath, requestId, routeId);
    
            // Step 4 — Verify JWT signature; returns fully-verified Claims
            Claims claims = signatureVerifier.verify(rawToken, publicKeyInfo);
            LOGGER.debug("JWT signature verified. {}",
                    GatewayUtils.getLogMessage(routeId, requestPath, requestId));
    
            // Step 5 — Config-driven claim validation (required / regex rules)
            boolean skipClaimValidation = publicKeyInfo.isSkipClaimValidation();
            tokenClaimValidator.validate(claims, skipClaimValidation,
                    jwtProperties.getTokenHeaderValidationConfig());
            LOGGER.debug("Token claim validation passed. {}",
                    GatewayUtils.getLogMessage(routeId, requestPath, requestId));
    
            // Step 6 — Custom programmatic claim validation
            additionalClaimValidator.validate(claims, publicKeyInfo, exchange);
            LOGGER.debug("Additional claim validation passed. {}",
                    GatewayUtils.getLogMessage(routeId, requestPath, requestId));
    
            // Step 7 — Scope validation against route-required scopes
            boolean skipAuthz = publicKeyInfo.isSkipAuthz();
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            String scope = scopeValidator.validate(new ScopeValidationContext(
                    route, claims, routeScopes, tokenScopePrefixes, skipAuthz, jwtProperties));
            LOGGER.debug("Scope validation passed. scope: {}, {}",
                    scope, GatewayUtils.getLogMessage(routeId, requestPath, requestId));
    
            // Step 8 — Map claim values to downstream request headers
            Set<String> overrideScopes = Stream.concat(
                    Arrays.stream(scope.split(",")).map(String::trim),
                    routeScopes.stream()
            ).collect(Collectors.toSet());
    
            Builder builder = exchange.getRequest().mutate();
            tokenClaimHeaderMapper.map(builder, new ClaimHeaderMappingContext(
                    claims, tokenClaimToHeaderMapping, scope, overrideScopes));
    
            LOGGER.info("JWT authentication successful for request: {}, requestId: {}", requestPath, requestId);
            return chain.filter(exchange.mutate().request(builder.build()).build());

        } catch (SecurityException | IllegalStateException e) {
            LOGGER.error("Token validation failed with exception: {}, {}",
                    e.getMessage(), GatewayUtils.getLogMessage(routeId, requestPath, requestId));
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE,
                    "Token verification failed");
        } catch (ApiGatewayException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Internal server error: {}, {}",
                    e.getMessage(), GatewayUtils.getLogMessage(routeId, requestPath, requestId), e);
            throw new ApiGatewayException(HttpStatus.INTERNAL_SERVER_ERROR, "api.gateway.error.internal",
                    "Internal server error");
        }
    }

    /**
     * Resolves the public key for the given decoded token metadata.
     * Falls back to the DEFAULT key if no specific key is found for the kid/tenantId.
     */
    private PublicKeyInfo resolvePublicKey(DecodedToken decoded, String requestPath,
                                           String requestId, String routeId) {
        LOGGER.debug("Fetching public key for kid: {}, tenantId: {}, {}",
                decoded.getKid(), decoded.getTenantId(),
                GatewayUtils.getLogMessage(routeId, requestPath, requestId));

        Optional<PublicKeyInfo> key = publicKeyService.findPublicKey(decoded.getKid(), decoded.getTenantId());

        if (key.isEmpty() && !DEFAULT.equals(decoded.getKid())) {
            LOGGER.warn("Public key not found for kid: {}, tenantId: {}. Attempting fallback to DEFAULT key. {}",
                    decoded.getKid(), decoded.getTenantId(),
                    GatewayUtils.getLogMessage(routeId, requestPath, requestId));
            key = publicKeyService.findPublicKey(DEFAULT, null);
        }

        if (key.isEmpty()) {
            LOGGER.error("Token validation failed - Public key not found. kid: {}, tenantId: {}, {}",
                    decoded.getKid(), decoded.getTenantId(),
                    GatewayUtils.getLogMessage(routeId, requestPath, requestId));
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
        }

        PublicKeyInfo info = key.get();
        LOGGER.debug("Public key resolved. kid: {}, sourceId: {}, {}",
                info.getKid(), info.getSourceId(),
                GatewayUtils.getLogMessage(routeId, requestPath, requestId));
        return info;
    }

    @Override
    public int getOrder() {
        return GatewayConstants.JWT_AUTH_FILTER_ORDER;
    }

    /**
     * Route-level configuration for the JwtAuthFilter.
     */
    @Setter
    @Getter
    @NoArgsConstructor
    @ToString
    public static class Config {
        /**
         * Comma-separated list of scopes required by this route.
         */
        protected String scope;
    }
}
