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
 * See the License for the specific language governing permissions and\
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.gateway.plugins.filters;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.gateway.config.JwtProperties;
import org.eclipse.ecsp.gateway.exceptions.ApiGatewayException;
import org.eclipse.ecsp.gateway.model.PublicKeyInfo;
import org.eclipse.ecsp.gateway.model.TokenHeaderValidationConfig;
import org.eclipse.ecsp.gateway.service.PublicKeyService;
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
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Filter to validate JWT Token.
 * This filter checks the JWT token in the request header, validates it, and extracts user information and scopes.
 * It also validates custom headers based on the provided configuration.
 */
public class JwtAuthFilter implements GatewayFilter, Ordered {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String INVALID_TOKEN_CODE = "api.gateway.error.token.invalid";
    private static final String INVALID_TOKEN = "Invalid Token";
    private static final String TOKEN_VERIFICATION_FAILED = "Token verification failed";
    private static final String DEFAULT = "DEFAULT";

    /**
     * PERFORMANCE OPTIMIZATION: Cache for compiled regex patterns.
     * Pattern.compile() is expensive (~1365 ns per call before optimization).
     * This cache stores pre-compiled Pattern objects and reuses them across requests,
     * eliminating the need to recompile the same regex pattern repeatedly.
     * Thread-safe using ConcurrentHashMap for high-concurrency scenarios.
     */
    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    /**
     * Set of route scopes.
     */
    protected final Set<String> routeScopes = new HashSet<>();
    private final Map<String, TokenHeaderValidationConfig> tokenHeaderValidationConfig;
    private final PublicKeyService publicKeyService;
    Map<String, String> tokenClaimToHeaderMapping;
    private Set<String> tokenScopePrefixes;

    /**
     * Constructor to initialize the JwtAuthFilter.
     *
     * @param config                      the JWT Auth Validator configuration
     * @param publicKeyService            the public key service to validate JWT signatures
     * @param jwtProperties               the map of token header validation configurations
     */
    public JwtAuthFilter(JwtAuthFilter.Config config,
                         PublicKeyService publicKeyService,
                         JwtProperties jwtProperties) {
        this.tokenHeaderValidationConfig = jwtProperties.getTokenHeaderValidationConfig();
        this.publicKeyService = publicKeyService;
        if (config != null && config.getScope() != null) {
            LOGGER.debug("Config: {}", config);
            routeScopes.addAll(Arrays.stream((config.getScope()).split(",")).map(String::trim).toList());
        }
        if (CollectionUtils.isEmpty(jwtProperties.getTokenClaimToHeaderMapping())) {
            LOGGER.debug("No token claim to header mapping configured");
            this.tokenClaimToHeaderMapping = new HashMap<>();
        } else {
            this.tokenClaimToHeaderMapping = jwtProperties.getTokenClaimToHeaderMapping();
            LOGGER.debug("Token claim to header mapping: {}", tokenClaimToHeaderMapping);
        }

        if (!this.tokenClaimToHeaderMapping.containsKey("user_id")) {
            LOGGER.debug("UserId claim is configured in token claim to header mapping");
            this.tokenClaimToHeaderMapping.put("sub", "user-id");
        }

        if (!CollectionUtils.isEmpty(jwtProperties.getScopePrefixes())) {
            this.tokenScopePrefixes = jwtProperties.getScopePrefixes();
            LOGGER.debug("Token scope prefixes: {}", tokenScopePrefixes);
        } else {
            this.tokenScopePrefixes = new HashSet<>();
            LOGGER.debug("No token scope prefixes configured");
        }
    }

    private static String getTokenHeader(Claims claims, String headerName) {
        String tokenHeader = null;
        if (claims != null) {
            tokenHeader = claims.keySet().stream()
                    .filter(headerKey -> headerKey.equalsIgnoreCase(headerName))
                    .findAny()
                    .orElse(null);
        }
        return tokenHeader;
    }

    private static String getTokenHeaderValue(Claims claims, String tokenHeader) {
        String tokenHeaderValue = null;
        if (tokenHeader != null && claims != null && claims.get(tokenHeader) != null) {
            Object headerValue = claims.get(tokenHeader);
            switch (headerValue) {
                case List<?> list ->
                    // If the header value is a list, join it with commas
                    tokenHeaderValue = list.stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(","));
                case String[] strArray ->
                    // If the header value is an array, join it with commas
                    tokenHeaderValue = String.join(",", strArray);
                case Set<?> setHeaders ->
                    // If the header value is a set, join it with commas
                    tokenHeaderValue = setHeaders.stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(","));
                case String str ->
                    // If the header value is a string, use it directly
                    tokenHeaderValue = str;
                default ->
                    // For other types, convert to string
                    tokenHeaderValue = String.valueOf(claims.get(tokenHeader));
            }
        }
        return tokenHeaderValue;
    }

    

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest().getId();
        final String routeId = exchange.getAttribute(
                ServerWebExchangeUtils.GATEWAY_PREDICATE_MATCHED_PATH_ROUTE_ID_ATTR);
        ServerHttpRequest request = exchange.getRequest();
        String requestPath = request.getPath().value();
        LOGGER.debug("JWT Auth Validation for requestUrl: {}, requestId: {}", requestPath, requestId);

        String token = request.getHeaders().getFirst(GatewayConstants.AUTHORIZATION);

        // authorization header not available in the request
        if (StringUtils.isBlank(token) || !token.startsWith(GatewayConstants.BEARER)) {
            LOGGER.error("Token validation failed - Token missing or invalid format. "
                    + "requestUrl: {}, requestId: {}", requestPath, requestId);
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
        }

        token = token.split(" ")[1];

        // Validate JWT
        LOGGER.debug("Token format validation passed for request: {}, requestId: {}", 
                requestPath, requestId);
        Claims claims = validateToken(token, requestId, requestPath, routeId);
        LOGGER.debug("Token validated, validating claims... for request: {}, requestId: {},"
                + " token claims: {}", requestPath, requestId, claims);

        //validate token headers
        validateTokenHeaders(claims, requestId, requestPath, routeId);

        LOGGER.debug("Token claims validated for request: {}, requestId: {}, validating route scopes",
                requestPath, requestId);

        // Validate user scopes against target route scope
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String scope = validateScope(route, claims, requestId, requestPath);

        LOGGER.debug("Scope validated for {}, scope: {},"
                        + " appending scope and override-scope headers to request",
                GatewayUtils.getLogMessage(routeId, requestPath, requestId), scope);

        Set<String> overrideScopes = new HashSet<>(Arrays.stream(scope.split(",")).map(String::trim).toList());
        overrideScopes = Stream.concat(overrideScopes.stream(), routeScopes.stream()).collect(Collectors.toSet());

        // set scopes as header
        Builder builder = exchange.getRequest().mutate();
        builder.header(GatewayConstants.SCOPE, scope);
        builder.header(GatewayConstants.OVERRIDE_SCOPE, String.join(",", overrideScopes));
        
        LOGGER.debug("Added claims to request headers: scope={}, override-scope={}, {}",
                scope, String.join(",", overrideScopes), 
                GatewayUtils.getLogMessage(routeId, requestPath, requestId));

        // Token claims are added to request headers and to be sent to downstream microservices
        for (Entry<String, String> entry : tokenClaimToHeaderMapping.entrySet()) {
            String claimKey = entry.getKey();
            String headerName = entry.getValue();
            String claimValue = getTokenHeaderValue(claims, claimKey);
            if (!StringUtils.isBlank(claimValue)) {
                builder.header(headerName, claimValue);
                LOGGER.debug("Added claim {} to request header: {} with value: {}, {}",
                        claimKey, headerName, claimValue, GatewayUtils.getLogMessage(routeId, requestPath, requestId));
            }
        }

        LOGGER.info("JWT authentication successful for request: {}, requestId: {}",
                requestPath, requestId);

        return chain.filter(exchange.mutate().request(builder.build()).build());
    }

    private void validateTokenHeaders(Claims claims, String requestId, String requestPath, String routeId) {
        try {
            LOGGER.debug("Starting token header validation for request: {}, requestId: {}", 
                    requestPath, requestId);
            
            //Header Validation
            if (!CollectionUtils.isEmpty(tokenHeaderValidationConfig)) {
                for (Entry<String, TokenHeaderValidationConfig> entry : tokenHeaderValidationConfig.entrySet()) {
                    String headerName = entry.getKey();
                    TokenHeaderValidationConfig headerConfigMap = entry.getValue();
                    
                    LOGGER.debug("Validating token header: {} for {}", 
                            headerName, GatewayUtils.getLogMessage(routeId, requestPath, requestId));

                    validateClaims(claims, headerName, headerConfigMap, requestId, requestPath, routeId);
                    
                    LOGGER.debug("Token header validation passed for header: {}, {}", 
                            headerName, GatewayUtils.getLogMessage(routeId, requestPath, requestId));
                }
            }
            
            LOGGER.debug("All token header validations passed for {}", 
                    GatewayUtils.getLogMessage(routeId, requestPath, requestId));

        } catch (PatternSyntaxException regexException) {
            LOGGER.error("Token header validation failed - Invalid regex pattern. {}, error: {}", 
                    GatewayUtils.getLogMessage(routeId, requestPath, requestId), regexException.getMessage());
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
        } catch (Exception ex) {
            LOGGER.error("Token header validation failed with unexpected error.{}, "
                    + "error: {}", 
                    GatewayUtils.getLogMessage(routeId, requestPath, requestId), ex.getMessage());
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
        }
    }

    private static void validateClaims(Claims claims, String headerName, 
                                       TokenHeaderValidationConfig headerConfigMap, 
                                       String requestId, String requestPath, String routeId) {
        if (headerConfigMap.isRequired()) {
            String tokenHeader = getTokenHeader(claims, headerName);
            String tokenHeaderValue = getTokenHeaderValue(claims, tokenHeader);
            
            if (StringUtils.isEmpty(tokenHeader) || StringUtils.isEmpty(tokenHeaderValue)) {
                LOGGER.error("Token claim validation failed - Required token header '{}' is missing. {}",
                        headerName, GatewayUtils.getLogMessage(routeId, requestPath, requestId));
                throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
            }
            
            if (StringUtils.isEmpty(headerConfigMap.getRegex())) {
                LOGGER.debug("Token header '{}' validation passed (no regex configured). {}",
                        headerName, GatewayUtils.getLogMessage(routeId, requestPath, requestId));
            } else {
                String regex = headerConfigMap.getRegex();
                // PERFORMANCE OPTIMIZATION: Use cached compiled Pattern instead of compiling every time
                // Before: Pattern.compile(regex) called on EVERY request (~1365 ns overhead)
                // After: Pattern retrieved from cache (O(1) HashMap lookup, ~10-50 ns)
                Pattern pattern = PATTERN_CACHE.computeIfAbsent(regex, Pattern::compile);
                boolean validRequestHeader = pattern.matcher(tokenHeaderValue).matches();
                if (!validRequestHeader) {
                    LOGGER.error("Token claim validation failed - Token header '{}' with value '{}' "
                            + "does not match regex pattern '{}'. {}", 
                            tokenHeader, tokenHeaderValue, regex, 
                            GatewayUtils.getLogMessage(routeId, requestPath, requestId));
                    throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
                } else {
                    LOGGER.debug("Token header '{}' validation passed with regex pattern '{}'. {}",
                            headerName, regex, GatewayUtils.getLogMessage(routeId, requestPath, requestId));
                }
            }
        } else {
            LOGGER.debug("Token header '{}' is not required, skipping validation. {}", 
                    headerName, GatewayUtils.getLogMessage(routeId, requestPath, requestId));
        }
    }

    private Claims validateToken(final String token, String requestId, String requestPath, String routeId) {
        LOGGER.debug("Starting JWT token validation for request: {}, requestId: {}", requestPath, requestId);

        try {

            JWT jwt = JWTParser.parse(token);
            Object kidObject = jwt.getHeader().toJSONObject().get("kid");
            Object tenantIdObject = jwt.getJWTClaimsSet().toJSONObject().get("tenantId");
            String kid = (kidObject == null || StringUtils.isEmpty(kidObject.toString()))
                    ? DEFAULT : kidObject.toString();
            String tenantId = (tenantIdObject == null || StringUtils.isEmpty(tenantIdObject.toString()))
                    ? "" : tenantIdObject.toString();

            LOGGER.debug("JWT token parsed successfully. Kid: {}, tenantId: {}, {}", 
                    kid, tenantId, GatewayUtils.getLogMessage(routeId, requestPath, requestId));

            if (DEFAULT.equals(kid)) {
                LOGGER.warn("JWT Token Header 'kid' is missing or empty, using default key for validation. "
                        + "tenantId: {}, {}", tenantId, 
                        GatewayUtils.getLogMessage(routeId, requestPath, requestId));
            }
            // Parse token and extract metadata
            TokenMetadata metadata = new TokenMetadata(kid, tenantId);

            // Get public key for validation
            PublicKeyInfo publicKeyInfo = getValidationKey(metadata, requestPath, requestId, routeId);

            // Validate token signature and claims
            return validateTokenSignature(token, publicKeyInfo, metadata, requestPath, requestId, routeId);

        } catch (SecurityException
                 | MalformedJwtException
                 | ExpiredJwtException
                 | UnsupportedJwtException
                 | IllegalArgumentException ex) {
            // Token validation failed due to expiration, signature, etc.
            String failureReason = GatewayUtils.getTokenValidationFailureReason(ex);
            LOGGER.error("Token validation failed - {}. {}", failureReason, 
                GatewayUtils.getLogMessage(routeId, requestPath, requestId));
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
        } catch (ApiGatewayException e) {
            throw e;
        } catch (Exception ex) {
            LOGGER.error("Token validation failed - Unexpected parsing error. {}, "
                    + "error: {}", GatewayUtils.getLogMessage(routeId, requestPath, requestId), ex.getMessage());
        }
        throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, TOKEN_VERIFICATION_FAILED);
    }

    /**
     * Get the public key for token validation.
     */
    private PublicKeyInfo getValidationKey(TokenMetadata metadata, 
                                        String requestPath, 
                                        String requestId, 
                                        String routeId) {
        // 4. Public key retrieval
        LOGGER.debug("Fetching public key for kid: {}, tenantId: {}, {}", 
                metadata.kid, metadata.tenantId, GatewayUtils.getLogMessage(routeId, requestPath, requestId));
        
        Optional<PublicKeyInfo> key = publicKeyService.findPublicKey(metadata.kid, metadata.tenantId);
        
        if (key.isEmpty() && !DEFAULT.equals(metadata.kid)) {
            LOGGER.warn("Public key not found for kid: {}, tenantId: {}, attempting fallback to default key. "
                    + "{}", metadata.kid, metadata.tenantId, 
                    GatewayUtils.getLogMessage(routeId, requestPath, requestId));
            key = publicKeyService.findPublicKey(DEFAULT, null);
        }

        if (key.isEmpty()) {
            LOGGER.error("Token validation failed - Public key not found. Kid: {}, tenantId: {}, "
                    + "{}", metadata.kid, metadata.tenantId, 
                    GatewayUtils.getLogMessage(routeId, requestPath, requestId));
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN);
        }
        PublicKeyInfo publicKeyInfo = key.get();
        LOGGER.debug("Public key found and will be used for validation. for kid: {}, sourceId: {}, {}", 
                publicKeyInfo.getKid(), publicKeyInfo.getSourceId(), 
                GatewayUtils.getLogMessage(routeId, requestPath, requestId));

        return publicKeyInfo;
    }

    /**
     * Validate token signature and return claims.
     */
    private Claims validateTokenSignature(String token, PublicKeyInfo publicKey, TokenMetadata metadata,
                                         String requestPath, String requestId, String routeId) {
        JwtParser jwtParser = Jwts.parser().verifyWith(publicKey.getPublicKey()).build();
        Jws<Claims> parsedToken = jwtParser.parseSignedClaims(token);
        
        LOGGER.info("JWT token validation successful. Kid: {}, tenantId: {}, keySource: {}, "
                + "{}", metadata.kid, metadata.tenantId, publicKey.getSourceId(),
                GatewayUtils.getLogMessage(routeId, requestPath, requestId));
        
        return parsedToken.getPayload();
    }

    /**
     * Inner class to hold token metadata.
     */
    private static class TokenMetadata {
        final String kid;
        final String tenantId;
        
        TokenMetadata(String kid, String tenantId) {
            this.kid = kid;
            this.tenantId = tenantId;
        }
    }

    private String validateScope(final Route route, final Claims claims, String requestId, 
                                String requestPath) {
        if (route == null || claims == null) {
            LOGGER.error("Scope validation failed - Invalid route or claims. {}", 
                    GatewayUtils.getLogMessage("", requestPath, requestId));
            throw new ApiGatewayException(HttpStatus.NOT_FOUND, "api.gateway.error", "Request not found");
        }

        LOGGER.debug("Starting scope validation for route: {}, requestPath: {}, requestId: {}", 
                route.getId(), requestPath, requestId);

        Set<String> userScopes = extractUserScopes(route, claims, requestId, requestPath);

        boolean valid = false;
        if (routeScopes.isEmpty()) {
            // Scope validation is not defined for the route
            LOGGER.debug("No route scopes configured, scope validation passed. {}", 
                    GatewayUtils.getLogMessage(route.getId(), requestPath, requestId));
            valid = true;
        } else {
            // Process scopes to remove configured prefixes if present
            if (!CollectionUtils.isEmpty(tokenScopePrefixes) && !CollectionUtils.isEmpty(userScopes)) {
                userScopes = sanitizeUserScopes(route, requestId, requestPath, userScopes);
                LOGGER.debug("user scope after prefixes are removed : {}", userScopes);
            }
            // at minimum one of the routeScopes must match userScopes
            valid = routeScopes.stream().anyMatch(userScopes::contains);
            if (valid) {
                LOGGER.debug("Scope validation passed - User scopes match route requirements. "
                        + "Matching scopes: {}, route: {}, requestPath: {}, requestId: {}", 
                        routeScopes.stream().filter(userScopes::contains).collect(Collectors.toSet()), 
                        route.getId(), requestPath, requestId);
            }
        }
        
        if (!valid) {
            // 5. Token and route scope validation failed
            LOGGER.error("Scope validation failed - User scopes do not match route requirements. "
                    + "User scopes: {}, required route scopes: {}, route: {}, requestUrl: {}, requestId: {}", 
                    userScopes, routeScopes, route.getId(), requestPath, requestId);
            throw new ApiGatewayException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_CODE, 
                    TOKEN_VERIFICATION_FAILED);
        }
        
        LOGGER.debug("Scope validation passed successfully. User scopes: {}, route: {}, requestPath: {}, "
                + "requestId: {}", 
                userScopes, route.getId(), requestPath, requestId);

        return String.join(",", userScopes);
    }

    private Set<String> extractUserScopes(final Route route, 
                                          final Claims claims, 
                                          String requestId, 
                                          String requestPath) {
        Set<String> userScopes = new HashSet<>();
        Object scopeObj = claims.get(GatewayConstants.SCOPE);
        if (scopeObj != null) {
            LOGGER.debug("Token scope found, type: {}, value: {}, {}", 
                    scopeObj.getClass().getSimpleName(), scopeObj, 
                    GatewayUtils.getLogMessage(route.getId(), requestPath, requestId));

            if (scopeObj instanceof List<?>) {
                // scopes are in the form of List
                userScopes = new HashSet<>((List<String>) scopeObj);
            } else if (scopeObj instanceof String scopeStr) {
                String delimiter = scopeStr.contains(",") ? "," : StringUtils.SPACE;
                userScopes = new HashSet<>(Arrays.asList(scopeStr.split(delimiter)));
            }
        } else {
            LOGGER.debug("No scope claim found in token for {}", 
                    GatewayUtils.getLogMessage(route.getId(), requestPath, requestId));
        }

        LOGGER.debug("Extracted user scopes: {}, configured route scopes: {}, {}", 
                userScopes, routeScopes, GatewayUtils.getLogMessage(route.getId(), requestPath, requestId));
        return userScopes;
    }

    private Set<String> sanitizeUserScopes(final Route route, 
                                        String requestId, 
                                        String requestPath, 
                                        Set<String> userScopes) {
        userScopes = userScopes.stream()
            .map(scope -> {
                // Check if scope starts with any configured prefix
                for (String prefix : tokenScopePrefixes) {
                    if (StringUtils.isNotBlank(scope) && StringUtils.isNotBlank(prefix) && scope.startsWith(prefix)) {
                        LOGGER.debug("removing scope prefix {} from the token scope: {} for {}", 
                                prefix, scope,
                                GatewayUtils.getLogMessage(route.getId(), requestPath, requestId));
                        return scope.substring(prefix.length());
                    }
                }
                return scope;
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
        return userScopes;
    }

    @Override
    public int getOrder() {
        return GatewayConstants.JWT_AUTH_FILTER_ORDER;
    }

    /**
     * Config class to add configuration to pass to filter.
     */
    @Setter
    @Getter
    @NoArgsConstructor
    @ToString
    public static class Config {
        /**
         * The scope.
         */
        protected String scope;
    }
}
